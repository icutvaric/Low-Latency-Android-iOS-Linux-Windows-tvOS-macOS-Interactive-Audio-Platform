package com.superpowered.playerexample;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private ExoPlayerWrapper exoPlayerWrapper;
    private boolean playing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        exoPlayerWrapper = new ExoPlayerWrapper(this);

        // Get the device's sample rate and buffer size to enable
        // low-latency Android audio output, if available.
        String samplerateString = null, buffersizeString = null;
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        }
        if (samplerateString == null) samplerateString = "48000";
        if (buffersizeString == null) buffersizeString = "480";
        int samplerate = Integer.parseInt(samplerateString);
        int buffersize = Integer.parseInt(buffersizeString);

        System.loadLibrary("PlayerExample");    // load native library
        Superpowered.StartAudio(samplerate, buffersize);             // start audio engine
        // If the application crashes, please disable Instant Run under Build, Execution, Deployment in preferences.
    }

    // Handle Play/Pause button toggle.
    public void PlayerExample_PlayPause (View button) {
        if (playing) {
            exoPlayerWrapper.stop();
        } else {
            exoPlayerWrapper.play("http://www.hochmuth.com/mp3/Haydn_Cello_Concerto_D-1.mp3");
        }
        playing = !playing;
        Button b = findViewById(R.id.playPause);
        b.setText(playing ? "Stop" : "Play");
    }

    @Override
    public void onPause() {
        super.onPause();
        Superpowered.onBackground();
    }

    @Override
    public void onResume() {
        super.onResume();
        Superpowered.onForeground();
    }

    protected void onDestroy() {
        super.onDestroy();
        Superpowered.Cleanup();
    }
}
