package com.superpowered.playerexample;

import android.os.Handler;

import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

public class Timer {

    // Create the Handler object (on the main thread by default)
    private Handler handler = new Handler();

    @NonNull
    private Runnable onTickRunnable = () -> { };

    // Define the code block to be executed
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            onTickRunnable.run();
            handler.postDelayed(this, TimeUnit.SECONDS.toMillis(1));
        }
    };

    public void onTick(@NonNull Runnable runnable) {
        onTickRunnable = runnable;
    }

    public void start() {
        handler.post(timerRunnable);
    }

    public void stop() {
        handler.removeCallbacks(timerRunnable);
    }

}
