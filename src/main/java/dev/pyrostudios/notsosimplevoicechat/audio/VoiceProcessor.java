package dev.pyrostudios.notsosimplevoicechat.audio;

public interface VoiceProcessor extends AutoCloseable {
    boolean isAvailable();

    short[] process(short[] svcFrame);

    @Override
    void close();
}
