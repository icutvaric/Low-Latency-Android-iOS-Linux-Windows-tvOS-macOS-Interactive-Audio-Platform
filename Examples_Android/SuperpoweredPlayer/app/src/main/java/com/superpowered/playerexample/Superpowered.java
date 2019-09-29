package com.superpowered.playerexample;

public class Superpowered {

    public static native void NativeInit(int samplerate, int buffersize);
    public static native void onForeground();
    public static native void onBackground();
    public static native void Cleanup();
    public static native boolean HasEnoughAudio();
    public static native void Append(short[] buf, int numFramesInBuf);

}
