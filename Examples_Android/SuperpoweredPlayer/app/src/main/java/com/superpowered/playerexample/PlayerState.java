package com.superpowered.playerexample;

import com.google.android.exoplayer2.Player;

public enum PlayerState {
    PLAYING, PAUSED, STOPPED, LOADING;

    public static PlayerState fromExoPlayerState(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case Player.STATE_READY:
                if (playWhenReady) {
                    return PLAYING;
                } else {
                    return PAUSED;
                }
            case Player.STATE_BUFFERING:
                return LOADING;
            default:
                return STOPPED;
        }
    }
}
