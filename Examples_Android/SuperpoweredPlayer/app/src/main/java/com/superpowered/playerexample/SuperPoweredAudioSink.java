package com.superpowered.playerexample;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

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
    private final int chunkSize;

    private float[] persistedPcmSlice;
    private int persistedPcmSlicePosition = 0;

    private int nextFloatBufferPosition = 0;

    public SuperPoweredAudioSink(Context context, AudioProcessor[] audioProcessors, int bufferSize) {
        defaultAudioSink = new DefaultAudioSink(AudioCapabilities.getCapabilities(context), audioProcessors);
        this.chunkSize = bufferSize; // TODO try with 2 * bufferSize
        this.persistedPcmSlice = new float[chunkSize];
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
        floatBuffer.position(nextFloatBufferPosition);
        int inputBufferSize = floatBuffer.limit() - floatBuffer.position();
        Log.d("SuperPoweredAudioSink", "HANDLE BUFFER START: FloatBuff capacity: " + floatBuffer.capacity() +  " limit: " + floatBuffer.limit() + " position : " + floatBuffer.position() + " inp buff size: " + inputBufferSize);
        if(persistedPcmSlicePosition + inputBufferSize <= chunkSize) {
            for (int i = 0; i< inputBufferSize; i++) {
                persistedPcmSlice[persistedPcmSlicePosition + i] = floatBuffer.get(i);
            }
            int totalFilled = persistedPcmSlicePosition + inputBufferSize;

            if(totalFilled == chunkSize) {
                boolean chunkProcessed = Superpowered.writeRawPcm(persistedPcmSlice);
                if(chunkProcessed) {
                    // chunk was fully processed, in next call start filling new chunk, reset persistedPcmSlicePosition to 0
                    persistedPcmSlicePosition = 0;
                    nextFloatBufferPosition = 0;
                    return true;
                } else {
                    // chunk was NOT fully processed, in next call continue filling same chunk, (do not reset persistedPcmSlicePosition)
                    // and also read same slice from same buffer (do not reset nextFloatBufferPosition)
                    return false;
                }
            } else {
                persistedPcmSlicePosition += totalFilled;
                nextFloatBufferPosition = 0;
                return true; // new buffer object will be provided in next function call, so reset position
            }
        } else {
            int leftToFillTotalChunk = chunkSize - persistedPcmSlicePosition;
            for (int i = 0; i < leftToFillTotalChunk; i++) {
                persistedPcmSlice[persistedPcmSlicePosition + i] = floatBuffer.get(i);
            }
            boolean chunkProcessed = Superpowered.writeRawPcm(persistedPcmSlice);
            if(chunkProcessed) {
                // this slice was processed in SuperPowered, so in next call return same buffer, but start reading from nextFloatBufferPosition
                int currentPosition = floatBuffer.position();
                nextFloatBufferPosition = currentPosition + leftToFillTotalChunk;
                // chunk was processed, start filling new chunk from next call
                persistedPcmSlicePosition = 0;
            } else {
                // this slice was not processed in SuperPowered, so in next call return same buffer, but also start reading from same position to get same slice again (do not update nextFloatBufferPosition)
                // also do not reset persistedPcmSlicePosition, as it might have been half - filled from some previous call
            }
            Log.d("SuperPoweredAudioSink", "HANDLE BUFFER END");
            return false; // same buffer object will be provided in next function call
        }
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
