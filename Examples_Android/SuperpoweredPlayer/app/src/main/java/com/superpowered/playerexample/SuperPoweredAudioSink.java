package com.superpowered.playerexample;

import android.content.Context;

import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.audio.AudioSink;
import com.google.android.exoplayer2.audio.AuxEffectInfo;
import com.google.android.exoplayer2.audio.DefaultAudioSink;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

class SuperPoweredAudioSink implements AudioSink {
    private static final int bufferCapacitySamples = 1024;
    private final DefaultAudioSink defaultAudioSink;
    private short[] directBuffer;
    private int inputBufferPosition = 0;

    SuperPoweredAudioSink(Context context, AudioProcessor[] audioProcessors) {
        defaultAudioSink = new DefaultAudioSink(AudioCapabilities.getCapabilities(context), audioProcessors);
        directBuffer = new short[bufferCapacitySamples];
    }

    @Override
    public boolean handleBuffer(ByteBuffer buffer, long presentationTimeUs) {
        if (Superpowered.HasEnoughAudio()) return false;

        ShortBuffer shortBuffer = buffer.asShortBuffer();
        int numberOfSamplesInShortBuffer = shortBuffer.limit() - inputBufferPosition;
        if (numberOfSamplesInShortBuffer < 1) return true;

        int samplesToCopy = numberOfSamplesInShortBuffer < bufferCapacitySamples ? numberOfSamplesInShortBuffer : bufferCapacitySamples;
        for (int n = 0; n < samplesToCopy; n++) directBuffer[n] = shortBuffer.get(inputBufferPosition++);

        Superpowered.Append(directBuffer, samplesToCopy / 2);

        shortBuffer.position(inputBufferPosition);
        boolean fullyConsumed = (inputBufferPosition >= shortBuffer.limit());
        if (fullyConsumed) inputBufferPosition = 0;
        return fullyConsumed;
    }

    @Override
    public void configure(int inputEncoding, int inputChannelCount, int inputSampleRate, int specifiedBufferSize, int[] outputChannels, int trimStartFrames, int trimEndFrames) throws ConfigurationException {
        defaultAudioSink.configure(inputEncoding, inputChannelCount, inputSampleRate, specifiedBufferSize, outputChannels, trimStartFrames, trimEndFrames);
    }

    @Override
    public void setListener(Listener listener) {
        defaultAudioSink.setListener(listener);
    }

    @Override
    public boolean supportsOutput(int channelCount, int encoding) { return defaultAudioSink.supportsOutput(channelCount, encoding); }

    @Override
    public long getCurrentPositionUs(boolean sourceEnded) { return defaultAudioSink.getCurrentPositionUs(sourceEnded); }

    @Override
    public void play() {
        defaultAudioSink.play();
    }

    @Override
    public void handleDiscontinuity() {
        defaultAudioSink.handleDiscontinuity();
    }

    @Override
    public void playToEndOfStream() throws WriteException { defaultAudioSink.playToEndOfStream(); }

    @Override
    public boolean isEnded() {
        return defaultAudioSink.isEnded();
    }

    @Override
    public boolean hasPendingData() {
        return defaultAudioSink.hasPendingData();
    }

    @Override
    public PlaybackParameters setPlaybackParameters(PlaybackParameters playbackParameters) { return defaultAudioSink.setPlaybackParameters(playbackParameters); }

    @Override
    public PlaybackParameters getPlaybackParameters() { return defaultAudioSink.getPlaybackParameters(); }

    @Override
    public void setAudioAttributes(AudioAttributes audioAttributes) { defaultAudioSink.setAudioAttributes(audioAttributes); }

    @Override
    public void setAudioSessionId(int audioSessionId) { defaultAudioSink.setAudioSessionId(audioSessionId); }

    @Override
    public void setAuxEffectInfo(AuxEffectInfo auxEffectInfo) { defaultAudioSink.setAuxEffectInfo(auxEffectInfo); }

    @Override
    public void enableTunnelingV21(int tunnelingAudioSessionId) { defaultAudioSink.enableTunnelingV21(tunnelingAudioSessionId); }

    @Override
    public void disableTunneling() {
        defaultAudioSink.disableTunneling();
    }

    @Override
    public void setVolume(float volume) {
        defaultAudioSink.setVolume(volume);
    }

    @Override
    public void pause() {
        defaultAudioSink.pause();
    }

    @Override
    public void reset() {
        defaultAudioSink.reset();
    }

    @Override
    public void release() {
        defaultAudioSink.release();
    }
}
