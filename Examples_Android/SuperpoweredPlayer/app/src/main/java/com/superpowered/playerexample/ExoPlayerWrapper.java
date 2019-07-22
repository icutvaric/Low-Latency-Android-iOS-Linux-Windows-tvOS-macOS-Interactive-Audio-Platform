package com.superpowered.playerexample;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
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
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ExoPlayerWrapper {

    private static final String TAG = "ExoPlayer";

    private final SimpleExoPlayer exoPlayer;
    private final DefaultDataSourceFactory defaultDataSourceFactory;

    public ExoPlayerWrapper(Context context) {
        exoPlayer = ExoPlayerFactory.newSimpleInstance(context, createRenderersFactory(context), new DefaultTrackSelector());
        exoPlayer.addListener(new Player.EventListener() {
            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Log.e(TAG, error.getMessage(), error);
            }
        });
        defaultDataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, context.getString(R.string.app_name)));
    }

    public void play(String url) {
        exoPlayer.prepare(createMediaSource(url));
        exoPlayer.setPlayWhenReady(true);
    }

    public void mute() {
        exoPlayer.setVolume(0);
    }

    public void pause() {
        exoPlayer.setPlayWhenReady(false);
    }

    public void resume() {
        exoPlayer.setPlayWhenReady(true);
    }

    public void stop() {
        exoPlayer.stop(true);
    }

    public void release() {
        exoPlayer.release();
    }

    private MediaSource createMediaSource(String url) {
        return new ExtractorMediaSource.Factory(defaultDataSourceFactory)
                .createMediaSource(Uri.parse(url));
    }

    private RenderersFactory createRenderersFactory(Context context) {
        return new DefaultRenderersFactory(context) {
            @Override
            protected void buildAudioRenderers(Context context, int extensionRendererMode, MediaCodecSelector mediaCodecSelector, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys, AudioProcessor[] audioProcessors, Handler eventHandler, AudioRendererEventListener eventListener, ArrayList<Renderer> out) {
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
                    public void handleBuffer(ByteBuffer buffer) { }
                })};
            }
        };
    }

}
