package dev.pyrostudios.notsosimplevoicechat.krisp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.pyrostudios.notsosimplevoicechat.NotSoSimpleVoiceChatClient;
import dev.pyrostudios.notsosimplevoicechat.audio.AudioResampler;
import dev.pyrostudios.notsosimplevoicechat.config.ModConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public final class KrispVoiceIsolationClient implements AutoCloseable {
    private static final URI WS_URI = URI.create("wss://streaming.krisp.ai/voice-isolation");

    private final ModConfig config;
    private final HttpClient httpClient;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean ready = new AtomicBoolean();
    private final ReentrantLock stateLock = new ReentrantLock();
    private final ArrayDeque<byte[]> pendingKrispFrames = new ArrayDeque<>();
    private final ArrayDeque<short[]> processedSvcFrames = new ArrayDeque<>();

    private short[] inputAccumulator = new short[0];
    private WebSocket webSocket;
    private Thread workerThread;
    private String lastError = "";

    public KrispVoiceIsolationClient(ModConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(20))
                .build();
    }

    public boolean isReady() {
        return ready.get() && running.get();
    }

    public String getLastError() {
        return lastError;
    }

    public synchronized void start() {
        if (running.get()) {
            return;
        }
        running.set(true);
        ready.set(false);
        lastError = "";
        workerThread = new Thread(this::runSession, "Krisp-Voice-Isolation");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public synchronized void close() {
        running.set(false);
        ready.set(false);
        WebSocket ws = webSocket;
        webSocket = null;
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }
        if (workerThread != null) {
            try {
                workerThread.join(2_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            workerThread = null;
        }
        stateLock.lock();
        try {
            pendingKrispFrames.clear();
            processedSvcFrames.clear();
            inputAccumulator = new short[0];
        } finally {
            stateLock.unlock();
        }
    }

    public short[] processFrame(short[] svcFrame) {
        if (!config.enabled || svcFrame == null) {
            return svcFrame;
        }
        if (svcFrame.length == 0) {
            flushInput();
            return svcFrame;
        }
        if (svcFrame.length != AudioResampler.SVC_FRAME_SAMPLES) {
            return svcFrame;
        }

        appendInput(svcFrame);
        drainInputToKrisp();

        short[] processed = pollProcessedFrame();
        return processed != null ? processed : svcFrame;
    }

    private void appendInput(short[] frame) {
        short[] merged = new short[inputAccumulator.length + frame.length];
        System.arraycopy(inputAccumulator, 0, merged, 0, inputAccumulator.length);
        System.arraycopy(frame, 0, merged, inputAccumulator.length, frame.length);
        inputAccumulator = merged;
    }

    private void flushInput() {
        if (inputAccumulator.length >= AudioResampler.SVC_SAMPLES_PER_KRISP_FRAME) {
            drainInputToKrisp();
        }
    }

    private void drainInputToKrisp() {
        while (inputAccumulator.length >= AudioResampler.SVC_SAMPLES_PER_KRISP_FRAME) {
            short[] chunk48k = new short[AudioResampler.SVC_SAMPLES_PER_KRISP_FRAME];
            System.arraycopy(inputAccumulator, 0, chunk48k, 0, chunk48k.length);
            short[] remainder = new short[inputAccumulator.length - chunk48k.length];
            System.arraycopy(inputAccumulator, chunk48k.length, remainder, 0, remainder.length);
            inputAccumulator = remainder;

            short[] chunk16k = AudioResampler.downsample48kTo16k(chunk48k, 0, chunk48k.length);
            byte[] krispFrame = AudioResampler.shortsToLittleEndianBytes(chunk16k);
            if (krispFrame.length != AudioResampler.KRISP_FRAME_SAMPLES * 2) {
                continue;
            }

            stateLock.lock();
            try {
                pendingKrispFrames.addLast(krispFrame);
            } finally {
                stateLock.unlock();
            }
        }
    }

    private short[] pollProcessedFrame() {
        stateLock.lock();
        try {
            return processedSvcFrames.pollFirst();
        } finally {
            stateLock.unlock();
        }
    }

    private void enqueueProcessed48k(short[] samples48k) {
        for (int offset = 0; offset + AudioResampler.SVC_FRAME_SAMPLES <= samples48k.length; offset += AudioResampler.SVC_FRAME_SAMPLES) {
            short[] frame = new short[AudioResampler.SVC_FRAME_SAMPLES];
            System.arraycopy(samples48k, offset, frame, 0, frame.length);
            stateLock.lock();
            try {
                processedSvcFrames.addLast(frame);
            } finally {
                stateLock.unlock();
            }
        }
    }

    private void runSession() {
        try {
            String sessionToken = resolveSessionToken();
            connectAndStream(sessionToken);
        } catch (Exception e) {
            lastError = e.getMessage() == null ? e.toString() : e.getMessage();
            NotSoSimpleVoiceChatClient.LOGGER.error("Krisp-Sitzung fehlgeschlagen: {}", lastError, e);
        } finally {
            ready.set(false);
            running.set(false);
        }
    }

    private String resolveSessionToken() throws IOException {
        String configured = config.resolveSessionToken();
        if (!configured.isBlank()) {
            return configured;
        }
        return KrispSessionTokenService.createVoiceIsolationToken(
                config.resolveApiKey(),
                config.sessionTokenTtlMinutes
        );
    }

    private void connectAndStream(String sessionToken) throws Exception {
        CompletableFuture<WebSocket> socketFuture = httpClient.newWebSocketBuilder()
                .header("Authorization", "api-key " + sessionToken)
                .buildAsync(WS_URI, new Listener());

        webSocket = socketFuture.get(30, TimeUnit.SECONDS);

        JsonObject hello = new JsonObject();
        hello.addProperty("model", config.model);
        hello.addProperty("sample_rate", AudioResampler.KRISP_SAMPLE_RATE);
        hello.addProperty("suppression_level", config.suppressionLevel);

        webSocket.sendText(hello.toString(), true).get(10, TimeUnit.SECONDS);

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (running.get() && !ready.get() && System.nanoTime() < deadline) {
            TimeUnit.MILLISECONDS.sleep(20);
        }
        if (!ready.get()) {
            throw new IOException("Krisp WebSocket nicht bereit: " + lastError);
        }

        while (running.get()) {
            byte[] frame = pollPendingFrame();
            if (frame != null) {
                webSocket.sendBinary(ByteBuffer.wrap(frame), true).get(5, TimeUnit.SECONDS);
            } else {
                TimeUnit.MILLISECONDS.sleep(2);
            }
        }
    }

    private byte[] pollPendingFrame() {
        stateLock.lock();
        try {
            return pendingKrispFrames.pollFirst();
        } finally {
            stateLock.unlock();
        }
    }

    private final class Listener implements WebSocket.Listener {
        private final StringBuilder textBuffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textBuffer.append(data);
            if (last) {
                handleTextMessage(textBuffer.toString());
                textBuffer.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            if (last) {
                byte[] bytes = new byte[data.remaining()];
                data.get(bytes);
                handleBinaryFrame(bytes);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            ready.set(false);
            NotSoSimpleVoiceChatClient.LOGGER.info("Krisp WebSocket geschlossen ({}): {}", statusCode, reason);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            lastError = error.getMessage() == null ? error.toString() : error.getMessage();
            NotSoSimpleVoiceChatClient.LOGGER.error("Krisp WebSocket Fehler", error);
            ready.set(false);
        }
    }

    private void handleTextMessage(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String type = root.has("type") ? root.get("type").getAsString() : "";
            if ("ready".equals(type)) {
                ready.set(true);
                NotSoSimpleVoiceChatClient.LOGGER.info("Krisp Voice Isolation bereit");
                return;
            }
            if ("error".equals(type) && root.has("error")) {
                lastError = root.getAsJsonObject("error").toString();
                NotSoSimpleVoiceChatClient.LOGGER.error("Krisp Fehler: {}", lastError);
            } else {
                NotSoSimpleVoiceChatClient.LOGGER.debug("Krisp Nachricht: {}", json);
            }
        } catch (Exception e) {
            NotSoSimpleVoiceChatClient.LOGGER.warn("Krisp JSON nicht lesbar: {}", json, e);
        }
    }

    private void handleBinaryFrame(byte[] bytes) {
        if (bytes.length != AudioResampler.KRISP_FRAME_SAMPLES * 2) {
            NotSoSimpleVoiceChatClient.LOGGER.warn("Unerwartete Krisp-Framegröße: {} Bytes", bytes.length);
            return;
        }
        short[] cleaned16k = AudioResampler.littleEndianBytesToShorts(bytes);
        short[] cleaned48k = AudioResampler.upsample16kTo48k(cleaned16k);
        enqueueProcessed48k(cleaned48k);
    }
}
