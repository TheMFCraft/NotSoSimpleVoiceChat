package dev.pyrostudios.notsosimplevoicechat.audio;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import dev.pyrostudios.notsosimplevoicechat.NotSoSimpleVoiceChatClient;
import dev.pyrostudios.notsosimplevoicechat.config.ModConfig;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Streaming DeepFilterNet3 @ 48 kHz (480-Sample-Hops), ONNX aus dem SpeechDenoiser-Projekt.
 */
public final class LocalDeepFilterProcessor implements VoiceProcessor {
    public static final int HOP_SAMPLES = 480;
    public static final int STATE_SIZE = 45_304;

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final float[] states;
    private final float[] attenLimDb;
    private final double strength;
    private boolean closed;

    public LocalDeepFilterProcessor(ModConfig config) throws IOException, OrtException {
        this.environment = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.setIntraOpNumThreads(1);
        options.setInterOpNumThreads(1);
        options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        options.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL);

        this.session = environment.createSession(DeepFilterModelStore.ensureModel().toString(), options);
        this.states = new float[STATE_SIZE];
        this.attenLimDb = new float[]{0f};
        this.strength = Math.clamp(config.strengthPercent, 0.0, 100.0) / 100.0;
    }

    @Override
    public boolean isAvailable() {
        return !closed;
    }

    @Override
    public short[] process(short[] svcFrame) {
        if (!isAvailable() || svcFrame == null || svcFrame.length == 0) {
            return svcFrame;
        }
        if (strength <= 0.0) {
            return svcFrame;
        }
        if (svcFrame.length % HOP_SAMPLES != 0) {
            return svcFrame;
        }

        short[] dry = svcFrame.clone();
        short[] wet = new short[svcFrame.length];

        for (int offset = 0; offset < svcFrame.length; offset += HOP_SAMPLES) {
            float[] frame = new float[HOP_SAMPLES];
            for (int i = 0; i < HOP_SAMPLES; i++) {
                frame[i] = svcFrame[offset + i] / 32768.0f;
            }
            float[] enhanced = runFrame(frame);
            for (int i = 0; i < HOP_SAMPLES; i++) {
                wet[offset + i] = floatToShort(enhanced[i]);
            }
        }

        PcmFrames.mix(dry, wet, strength);
        return dry;
    }

    private float[] runFrame(float[] frame) {
        try (OnnxTensor inputFrame = OnnxTensor.createTensor(environment, FloatBuffer.wrap(frame), new long[]{HOP_SAMPLES});
             OnnxTensor stateTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(states), new long[]{STATE_SIZE});
             OnnxTensor attenTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(attenLimDb), new long[]{1})) {

            Map<String, OnnxTensor> feeds = new HashMap<>();
            feeds.put("input_frame", inputFrame);
            feeds.put("states", stateTensor);
            feeds.put("atten_lim_db", attenTensor);

            try (OrtSession.Result result = session.run(feeds)) {
                copyState(result.get(1));
                return readEnhancedFrame(result.get(0));
            }
        } catch (OrtException e) {
            NotSoSimpleVoiceChatClient.LOGGER.error("DeepFilter-Inferenz fehlgeschlagen", e);
            return frame;
        }
    }

    private void copyState(OnnxValue stateValue) throws OrtException {
        Object value = stateValue.getValue();
        if (value instanceof float[] stateVector && stateVector.length == STATE_SIZE) {
            System.arraycopy(stateVector, 0, states, 0, STATE_SIZE);
        }
    }

    private float[] readEnhancedFrame(OnnxValue enhancedValue) throws OrtException {
        Object value = enhancedValue.getValue();
        if (value instanceof float[] vector && vector.length == HOP_SAMPLES) {
            return vector;
        }
        if (value instanceof float[][] matrix && matrix.length > 0 && matrix[0].length == HOP_SAMPLES) {
            return matrix[0];
        }
        throw new OrtException("Unerwartetes enhanced_audio_frame-Format: " + value.getClass());
    }

    private static short floatToShort(float sample) {
        float scaled = sample * 32768.0f;
        int rounded = Math.round(scaled);
        return (short) Math.max(Math.min(rounded, Short.MAX_VALUE), Short.MIN_VALUE);
    }

    @Override
    public void close() {
        closed = true;
        try {
            session.close();
        } catch (OrtException e) {
            NotSoSimpleVoiceChatClient.LOGGER.warn("DeepFilter-Session konnte nicht geschlossen werden", e);
        }
    }

    public static VoiceProcessor tryCreate(ModConfig config) {
        try {
            return new LocalDeepFilterProcessor(config);
        } catch (IOException | OrtException e) {
            NotSoSimpleVoiceChatClient.LOGGER.error("DeepFilterNet konnte nicht geladen werden", e);
            return null;
        }
    }
}
