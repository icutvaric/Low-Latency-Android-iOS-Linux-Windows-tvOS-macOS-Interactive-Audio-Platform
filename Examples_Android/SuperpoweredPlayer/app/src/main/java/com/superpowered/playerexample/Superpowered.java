package com.superpowered.playerexample;

public class Superpowered {

    public static native void StartAudio(int samplerate, int buffersize);
    public static native void onForeground();
    public static native void onBackground();
    public static native void Cleanup();
    public static native boolean writeRawPcm(float[] rawPcm);

}
