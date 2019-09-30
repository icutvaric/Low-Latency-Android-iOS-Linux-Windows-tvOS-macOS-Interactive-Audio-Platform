package com.superpowered.playerexample;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.audio.TeeAudioProcessor;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class ExoPlayerWrapper {
    private static final String TAG = "ExoPlayer";
    private static final long SEEK_INTERVAL_IN_SECONDS = 5;
    private final SimpleExoPlayer exoPlayer;
    private final DefaultDataSourceFactory defaultDataSourceFactory;

    ExoPlayerWrapper(Context context) {
        RenderersFactory renderersFactory = new DefaultRenderersFactory(context) {
            @Override
            protected void buildAudioRenderers(Context context, int extensionRendererMode, MediaCodecSelector mediaCodecSelector, DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys, AudioProcessor[] audioProcessors, Handler eventHandler, AudioRendererEventListener eventListener, ArrayList<Renderer> out) {
                out.add(new MediaCodecAudioRenderer(context, mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys, eventHandler, eventListener, new SuperPoweredAudioSink(context, audioProcessors)));
            }

            @Override
            protected AudioProcessor[] buildAudioProcessors() {
                return new AudioProcessor[]{ new TeeAudioProcessor(new TeeAudioProcessor.AudioBufferSink() {
                    @Override
                    public void flush(int sampleRateHz, int channelCount, int encoding) {
                        Log.d(TAG, "Sample Rate: " + sampleRateHz + " Channel Count: " + channelCount + " Encoding: " + encoding);
                    }

                    @Override
                    public void handleBuffer(ByteBuffer buffer) {}
                })};
            }
        };
        exoPlayer = ExoPlayerFactory.newSimpleInstance(context, renderersFactory, new DefaultTrackSelector());
        defaultDataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, context.getString(R.string.app_name)));
    }

    void addListener(Player.EventListener eventListener) {
        exoPlayer.addListener(eventListener);
    }

    long getDurationInSeconds() {
        long duration = exoPlayer.getDuration();
        if (duration < 0) {
            return 0;
        } else {
            return TimeUnit.MILLISECONDS.toSeconds(duration);
        }
    }

    long getProgressInSeconds() {
        long progress = exoPlayer.getCurrentPosition();
        if (progress < 0) {
            return 0;
        } else {
            return TimeUnit.MILLISECONDS.toSeconds(progress);
        }
    }

    PlayerState getState() {
        return PlayerState.fromExoPlayerState(exoPlayer.getPlayWhenReady(), exoPlayer.getPlaybackState());
    }

    void prepare(String url) {
        exoPlayer.prepare(new ExtractorMediaSource.Factory(defaultDataSourceFactory).createMediaSource(Uri.parse(url)));
    }

    void play() {
        exoPlayer.setPlayWhenReady(true);
    }

    void pause() {
        exoPlayer.setPlayWhenReady(false);
    }

    public void stop() {
        exoPlayer.stop(true);
    }

    void seekForward() {
        long currentProgress = getProgressInSeconds();
        long newProgress = currentProgress + SEEK_INTERVAL_IN_SECONDS;
        if (newProgress > getDurationInSeconds()) {
            newProgress = getDurationInSeconds();
        }
        exoPlayer.seekTo(TimeUnit.SECONDS.toMillis(newProgress));
    }

    void seekBack() {
        long currentProgress = getProgressInSeconds();
        long newProgress = currentProgress - SEEK_INTERVAL_IN_SECONDS;
        if (newProgress < 0) {
            newProgress = 0;
        }
        exoPlayer.seekTo(TimeUnit.SECONDS.toMillis(newProgress));
    }

}
