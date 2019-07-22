package com.superpowered.playerexample;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.audio.AudioSink;
import com.google.android.exoplayer2.audio.AuxEffectInfo;
import com.google.android.exoplayer2.audio.DefaultAudioSink;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

class SuperPoweredAudioSink implements AudioSink {

    private final DefaultAudioSink defaultAudioSink;

    public SuperPoweredAudioSink(Context context, AudioProcessor[] audioProcessors) {
        defaultAudioSink = new DefaultAudioSink(AudioCapabilities.getCapabilities(context), audioProcessors);
    }

    @Override
    public void setListener(Listener listener) {
        defaultAudioSink.setListener(listener);
    }

    @Override
    public boolean supportsOutput(int channelCount, int encoding) {
        return defaultAudioSink.supportsOutput(channelCount, encoding);
    }

    @Override
    public long getCurrentPositionUs(boolean sourceEnded) {
        return defaultAudioSink.getCurrentPositionUs(sourceEnded);
    }

    @Override
    public void configure(int inputEncoding, int inputChannelCount, int inputSampleRate, int specifiedBufferSize, @Nullable int[] outputChannels, int trimStartFrames, int trimEndFrames) throws ConfigurationException {
        defaultAudioSink.configure(inputEncoding, inputChannelCount, inputSampleRate, specifiedBufferSize, outputChannels, trimStartFrames, trimEndFrames);
    }

    @Override
    public void play() {
        defaultAudioSink.play();
    }

    @Override
    public void handleDiscontinuity() {
        defaultAudioSink.handleDiscontinuity();
    }

    @Override
    public boolean handleBuffer(ByteBuffer buffer, long presentationTimeUs) throws InitializationException, WriteException {
        FloatBuffer floatBuffer = buffer.asFloatBuffer();
        float[] pcm = new float[floatBuffer.capacity()];
        for (int i=0; i<floatBuffer.limit(); i++) {
            pcm[i] = floatBuffer.get(i);
        }
        Superpowered.writeRawPcm(pcm);
        return true; // returning true means that the whole floatBuffer should be written at once.
    }

    @Override
    public void playToEndOfStream() throws WriteException {
        defaultAudioSink.playToEndOfStream();
    }

    @Override
    public boolean isEnded() {
        return defaultAudioSink.isEnded();
    }

    @Override
    public boolean hasPendingData() {
        return defaultAudioSink.hasPendingData();
    }

    @Override
    public PlaybackParameters setPlaybackParameters(PlaybackParameters playbackParameters) {
        return defaultAudioSink.setPlaybackParameters(playbackParameters);
    }

    @Override
    public PlaybackParameters getPlaybackParameters() {
        return defaultAudioSink.getPlaybackParameters();
    }

    @Override
    public void setAudioAttributes(AudioAttributes audioAttributes) {
        defaultAudioSink.setAudioAttributes(audioAttributes);
    }

    @Override
    public void setAudioSessionId(int audioSessionId) {
        defaultAudioSink.setAudioSessionId(audioSessionId);
    }

    @Override
    public void setAuxEffectInfo(AuxEffectInfo auxEffectInfo) {
        defaultAudioSink.setAuxEffectInfo(auxEffectInfo);
    }

    @Override
    public void enableTunnelingV21(int tunnelingAudioSessionId) {
        defaultAudioSink.enableTunnelingV21(tunnelingAudioSessionId);
    }

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
