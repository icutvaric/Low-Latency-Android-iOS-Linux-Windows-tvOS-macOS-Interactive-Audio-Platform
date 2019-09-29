#include <jni.h>
#include <string>
#include <android/log.h>
#include <OpenSource/SuperpoweredAndroidAudioIO.h>
#include <Superpowered.h>
#include <SuperpoweredSimple.h>
#include <malloc.h>
#include <SLES/OpenSLES_AndroidConfiguration.h>
#include <SLES/OpenSLES.h>

static SuperpoweredAndroidAudioIO *audioIO;
static unsigned int oneSecondNumFrames = 0;
static unsigned int bufferCapacityFrames = 0;
static unsigned int readPosFrames = 0;
static unsigned int writePosFrames = 0;
static unsigned int numFramesInBuffer = 0;
static short int *buffer = NULL;

// This is called periodically by the audio engine.
static bool audioProcessing (
        void * __unused clientdata, // custom pointer
        short int *audio,           // output buffer
        int numberOfFrames,         // number of frames to process
        int __unused samplerate     // current sample rate in Hz
) {
    if (__sync_fetch_and_add(&numFramesInBuffer, 0) >= numberOfFrames) {
        unsigned int framesCopied = 0;

        while (numberOfFrames > 0) {
            unsigned int numFramesToCopy = bufferCapacityFrames - readPosFrames;
            if (numFramesToCopy > numberOfFrames) numFramesToCopy = (unsigned int)numberOfFrames;
            memcpy(audio + framesCopied * 2, buffer + readPosFrames * 2, numFramesToCopy * 4);

            readPosFrames += numFramesToCopy;
            framesCopied += numFramesToCopy;
            numberOfFrames -= numFramesToCopy;
            if (readPosFrames >= bufferCapacityFrames) readPosFrames = 0;
            __sync_fetch_and_sub(&numFramesInBuffer, numFramesToCopy);
        }
        //__android_log_print(ANDROID_LOG_DEBUG, "SuperpoweredNative", "consume ok, available %i", numFramesInBuffer);
        return true;
    } else {
        //__android_log_print(ANDROID_LOG_DEBUG, "SuperpoweredNative", "dropout, has only %i", numFramesInBuffer);
        return false;
    }
}

// StartAudio - Start audio engine and initialize player.
extern "C" JNIEXPORT void
Java_com_superpowered_playerexample_Superpowered_NativeInit(JNIEnv *env, jobject __unused obj, jint samplerate, jint buffersize) {
    Superpowered::Initialize(
            "ExampleLicenseKey-WillExpire-OnNextUpdate",
            false, // enableAudioAnalysis (using SuperpoweredAnalyzer, SuperpoweredLiveAnalyzer, SuperpoweredWaveform or SuperpoweredBandpassFilterbank)
            false, // enableFFTAndFrequencyDomain (using SuperpoweredFrequencyDomain, SuperpoweredFFTComplex, SuperpoweredFFTReal or SuperpoweredPolarFFT)
            false, // enableAudioTimeStretching (using SuperpoweredTimeStretching)
            false, // enableAudioEffects (using any SuperpoweredFX class)
            false,  // enableAudioPlayerAndDecoder (using SuperpoweredAdvancedAudioPlayer or SuperpoweredDecoder)
            false, // enableCryptographics (using Superpowered::RSAPublicKey, Superpowered::RSAPrivateKey, Superpowered::hasher or Superpowered::AES)
            false  // enableNetworking (using Superpowered::httpRequest)
    );

    oneSecondNumFrames = (unsigned int)samplerate;
    bufferCapacityFrames = oneSecondNumFrames / 2;
    buffer = (short int *)malloc((size_t)bufferCapacityFrames * 4);

    audioIO = new SuperpoweredAndroidAudioIO (
            samplerate,                     // device native sampling rate
            buffersize,                     // device native buffer size
            false,                          // enableInput
            true,                           // enableOutput
            audioProcessing,                // process callback function
            NULL,                           // clientData
            -1,                             // inputStreamType (-1 = default)
            SL_ANDROID_STREAM_MEDIA         // outputStreamType (-1 = default)
    );
}

// onBackground - Put audio processing to sleep if no audio is playing.
extern "C" JNIEXPORT void
Java_com_superpowered_playerexample_Superpowered_onBackground(JNIEnv * __unused env, jobject __unused obj) {
    audioIO->onBackground();
}

// onForeground - Resume audio processing.
extern "C" JNIEXPORT void
Java_com_superpowered_playerexample_Superpowered_onForeground(JNIEnv * __unused env, jobject __unused obj) {
    audioIO->onForeground();
}

// Cleanup - Free resources.
extern "C" JNIEXPORT void
Java_com_superpowered_playerexample_Superpowered_Cleanup(JNIEnv * __unused env, jobject __unused obj) {
    delete audioIO;
    free(buffer);
}

extern "C" JNIEXPORT jboolean
Java_com_superpowered_playerexample_Superpowered_HasEnoughAudio(JNIEnv * __unused env, jobject __unused obj) {
    if (__sync_fetch_and_add(&numFramesInBuffer, 0) > oneSecondNumFrames / 8) return JNI_TRUE; else return JNI_FALSE;
}

extern "C" JNIEXPORT void
Java_com_superpowered_playerexample_Superpowered_Append(JNIEnv * __unused env, jobject __unused obj, jshortArray jbuf, jint numFramesInBuf) {
    jshort *shortArray = env->GetShortArrayElements(jbuf, nullptr);
    if (!shortArray) return;
    unsigned int framesConsumed = 0;

    while (numFramesInBuf > 0) {
        unsigned int numFramesToCopy = bufferCapacityFrames - writePosFrames;
        if (numFramesToCopy > numFramesInBuf) numFramesToCopy = (unsigned int)numFramesInBuf;
        memcpy(buffer + writePosFrames * 2, shortArray + framesConsumed * 2, (size_t)numFramesToCopy * 4);

        numFramesInBuf -= numFramesToCopy;
        writePosFrames += numFramesToCopy;
        framesConsumed += numFramesToCopy;
        if (writePosFrames >= bufferCapacityFrames) writePosFrames = 0;
        __sync_fetch_and_add(&numFramesInBuffer, numFramesToCopy);
    }

    env->ReleaseShortArrayElements(jbuf, shortArray, 0);
}
