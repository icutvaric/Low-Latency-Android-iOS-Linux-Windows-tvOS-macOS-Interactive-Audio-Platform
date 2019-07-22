#include <jni.h>
#include <string>
#include <android/log.h>
#include <AndroidIO/SuperpoweredAndroidAudioIO.h>
#include <Superpowered.h>
#include <SuperpoweredSimple.h>
#include <SuperpoweredCPU.h>
#include <malloc.h>
#include <SLES/OpenSLES_AndroidConfiguration.h>
#include <SLES/OpenSLES.h>
#include <queue>

#define log_print __android_log_print

static SuperpoweredAndroidAudioIO *audioIO;

static std::queue<float*> bufferQueue;


// This is called periodically by the audio engine.
static bool audioProcessing (
        void * __unused clientdata, // custom pointer
        short int *audio,           // buffer of interleaved samples
        int numberOfFrames,         // number of frames to process
        int __unused samplerate     // sampling rate
) {

    if (!bufferQueue.empty()) {
        SuperpoweredFloatToShortInt(bufferQueue.front(), audio, (unsigned int)numberOfFrames);
        bufferQueue.pop();
        log_print(ANDROID_LOG_DEBUG, "PlayerExample", "AudioProcessing. Size: %d", (int)bufferQueue.size());
        return true;
    } else {
        return false;
    }
}

// StartAudio - Start audio engine and initialize player.
extern "C" JNIEXPORT void
Java_com_superpowered_playerexample_Superpowered_StartAudio (
        JNIEnv * __unused env,
        jobject  __unused obj,
        jint samplerate,
        jint buffersize
) {
    SuperpoweredInitialize(
            "ExampleLicenseKey-WillExpire-OnNextUpdate",
            false, // enableAudioAnalysis (using SuperpoweredAnalyzer, SuperpoweredLiveAnalyzer, SuperpoweredWaveform or SuperpoweredBandpassFilterbank)
            false, // enableFFTAndFrequencyDomain (using SuperpoweredFrequencyDomain, SuperpoweredFFTComplex, SuperpoweredFFTReal or SuperpoweredPolarFFT)
            false, // enableAudioTimeStretching (using SuperpoweredTimeStretching)
            false, // enableAudioEffects (using any SuperpoweredFX class)
            false, // enableAudioPlayerAndDecoder (using SuperpoweredAdvancedAudioPlayer or SuperpoweredDecoder)
            false, // enableCryptographics (using Superpowered::RSAPublicKey, Superpowered::RSAPrivateKey, Superpowered::hasher or Superpowered::AES)
            false  // enableNetworking (using Superpowered::httpRequest)
    );

    // Initialize audio with audio callback function.
    audioIO = new SuperpoweredAndroidAudioIO (
            samplerate,                     // sampling rate
            buffersize,                     // buffer size
            false,                          // enableInput
            true,                           // enableOutput
            audioProcessing,                // process callback function
            NULL,                           // clientData
            -1,                             // inputStreamType (-1 = default)
            SL_ANDROID_STREAM_MEDIA         // outputStreamType (-1 = default)
    );
}

// onBackground - Put audio processing to sleep.
extern "C" JNIEXPORT void
Java_com_superpowered_playerexample_Superpowered_onBackground (
        JNIEnv * __unused env,
        jobject __unused obj
) {
    audioIO->onBackground();
}

// onForeground - Resume audio processing.
extern "C" JNIEXPORT void
Java_com_superpowered_playerexample_Superpowered_onForeground (
        JNIEnv * __unused env,
        jobject __unused obj
) {
    audioIO->onForeground();
}

// Cleanup - Free resources.
extern "C" JNIEXPORT void
Java_com_superpowered_playerexample_Superpowered_Cleanup (
        JNIEnv * __unused env,
        jobject __unused obj
) {
    delete audioIO;
    while(!bufferQueue.empty()) bufferQueue.pop();
}

extern "C" JNIEXPORT void
Java_com_superpowered_playerexample_Superpowered_writeRawPcm (
        JNIEnv * __unused env,
        jobject __unused obj,
        jfloatArray rawPcm
) {
    log_print(ANDROID_LOG_DEBUG, "PlayerExample", "Add raw PCM to buffer queue. Size: %d", (int)bufferQueue.size());
    bufferQueue.push(env->GetFloatArrayElements(rawPcm, nullptr));
}


