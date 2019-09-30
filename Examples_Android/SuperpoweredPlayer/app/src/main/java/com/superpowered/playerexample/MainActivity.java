package com.superpowered.playerexample;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;

public class MainActivity extends AppCompatActivity implements SuperpoweredUSBAudioHandler, Player.EventListener {

    private Button playPauseButton;
    private TextView playerProgressTextView;
    private TextView playerStateTextView;
    private TextView playerErrorTextView;

    private ExoPlayerWrapper exoPlayerWrapper;
    private boolean playing = false;

    private Timer timer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playPauseButton = findViewById(R.id.playPause);
        playerProgressTextView = findViewById(R.id.playerProgress);
        playerStateTextView = findViewById(R.id.playerState);
        playerErrorTextView = findViewById(R.id.playerError);

        timer.onTick(this::updatePlayerProgress);

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
        Superpowered.NativeInit(samplerate, buffersize); // start audio engine
        // If the application crashes, please disable Instant Run under Build, Execution, Deployment in preferences.

        exoPlayerWrapper = new ExoPlayerWrapper(this);
        exoPlayerWrapper.addListener(this);
        preparePlayer();

        SuperpoweredUSBAudio usbAudio = new SuperpoweredUSBAudio(getApplicationContext(), this);
        usbAudio.check();


        updatePlayerState();
        updatePlayerProgress();
    }

    private void updatePlayerProgress() {
        String progressText = "Progress: " + exoPlayerWrapper.getProgressInSeconds() + " / " + exoPlayerWrapper.getDurationInSeconds();
        playerProgressTextView.setText(progressText);
    }

    public void onUSBAudioDeviceAttached(int deviceIdentifier) {
    }

    public void onUSBMIDIDeviceAttached(int deviceIdentifier) {
    }

    public void onUSBDeviceDetached(int deviceIdentifier) {
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        updatePlayerState();
        PlayerState playerState = exoPlayerWrapper.getState();
        switch (playerState) {
            case PLAYING:
            case LOADING:
                playPauseButton.setText("Pause");
                break;
            default:
                playPauseButton.setText("Play");
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        playerErrorTextView.setText(error.getMessage());
    }

    // Play/Pause button event.
    public void playPause(View button) {
        if (exoPlayerWrapper.getState() == PlayerState.STOPPED) {
            preparePlayer();
        }
        if (playing) {
            pause();
        } else {
            play();
        }
        playing = !playing;
    }

    public void stop(View button) {
        exoPlayerWrapper.stop();
        playing = false;
        timer.stop();
    }

    public void seekBackward(View view) {
        exoPlayerWrapper.seekBack();
    }

    public void seekForward(View view) {
        exoPlayerWrapper.seekForward();
    }

    private void play() {
        exoPlayerWrapper.play();
        timer.start();
    }

    private void pause() {
        exoPlayerWrapper.pause();
        timer.stop();
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

    private void updatePlayerState() {
        String playerState = "Player State: " + exoPlayerWrapper.getState().name();
        playerStateTextView.setText(playerState);
    }

    private void preparePlayer() {
        exoPlayerWrapper.prepare("https://superpowered.com/500.m4a");
        //exoPlayerWrapper.prepare("http://www.hochmuth.com/mp3/Haydn_Cello_Concerto_D-1.mp3");
    }
}
