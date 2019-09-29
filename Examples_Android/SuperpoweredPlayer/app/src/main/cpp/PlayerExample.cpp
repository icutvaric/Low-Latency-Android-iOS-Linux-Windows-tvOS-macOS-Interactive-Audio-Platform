#include <jni.h>
#include <string>
#include <android/log.h>
#include <OpenSource/SuperpoweredAndroidAudioIO.h>
#include <SuperpoweredCPU.h>
#include <SuperpoweredAndroidUSB.h>
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
static unsigned int deviceSamplerate = 0;
static short int *buffer = NULL;

// This is called periodically by the audio engine.
static bool audioProcessingBuiltIn (
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
        return true;
    } else {
        return false;
    }
}

// This is called periodically when we output via USB.
static bool audioProcessingUSB (
        void * __unused clientdata,
        int __unused deviceID,
        float *audioIO,
        int numberOfFrames,
        int samplerate,
        int __unused numInputChannels,
        int __unused numOutputChannels
) {
    short int buf[numberOfFrames * 2];
    bool r = audioProcessingBuiltIn(NULL, buf, numberOfFrames, samplerate);
    if (r && audioIO) Superpowered::ShortIntToFloat(buf, audioIO, (unsigned int)numberOfFrames);
    return r;
}

// StartAudio - Start audio engine and initialize player.
extern "C" JNIEXPORT void
Java_com_superpowered_playerexample_Superpowered_NativeInit(JNIEnv * __unused env, jclass __unused obj, jint samplerate, jint buffersize) {
    Superpowered::Initialize(
            "ExampleLicenseKey-WillExpire-OnNextUpdate",
            false, // enableAudioAnalysis (using SuperpoweredAnalyzer, SuperpoweredLiveAnalyzer, SuperpoweredWaveform or SuperpoweredBandpassFilterbank)
            false, // enableFFTAndFrequencyDomain (using SuperpoweredFrequencyDomain, SuperpoweredFFTComplex, SuperpoweredFFTReal or SuperpoweredPolarFFT)
            false, // enableAudioTimeStretching (using SuperpoweredTimeStretching)
            false, // enableAudioEffects (using any SuperpoweredFX class)
            false, // enableAudioPlayerAndDecoder (using SuperpoweredAdvancedAudioPlayer or SuperpoweredDecoder)
            false, // enableCryptographics (using Superpowered::RSAPublicKey, Superpowered::RSAPrivateKey, Superpowered::hasher or Superpowered::AES)
            false  // enableNetworking (using Superpowered::httpRequest)
    );
    Superpowered::AndroidUSB::initialize(NULL, NULL, NULL, NULL, NULL);

    deviceSamplerate = (unsigned int)samplerate;
    oneSecondNumFrames = (unsigned int)samplerate;
    bufferCapacityFrames = oneSecondNumFrames / 2;
    buffer = (short int *)malloc((size_t)bufferCapacityFrames * 4);

    audioIO = new SuperpoweredAndroidAudioIO (
            samplerate,                     // device native sampling rate
            buffersize,                     // device native buffer size
            false,                          // enableInput
            true,                           // enableOutput
            audioProcessingBuiltIn,         // process callback function
            NULL,                           // clientData
            -1,                             // inputStreamType (-1 = default)
            SL_ANDROID_STREAM_MEDIA         // outputStreamType (-1 = default)
    );

    Superpowered::CPU::setSustainedPerformanceMode(true);
}

// onBackground - Put audio processing to sleep if no audio is playing.
extern "C" JNIEXPORT void
Java_com_superpowered_playerexample_Superpowered_onBackground(JNIEnv * __unused env, jclass __unused obj) {
    audioIO->onBackground();
}

// onForeground - Resume audio processing.
extern "C" JNIEXPORT void
Java_com_superpowered_playerexample_Superpowered_onForeground(JNIEnv * __unused env, jclass __unused obj) {
    audioIO->onForeground();
}

// Cleanup - Free resources.
extern "C" JNIEXPORT void
Java_com_superpowered_playerexample_Superpowered_Cleanup(JNIEnv * __unused env, jclass __unused obj) {
    delete audioIO;
    free(buffer);
    Superpowered::AndroidUSB::destroy();
}

extern "C" JNIEXPORT jboolean
Java_com_superpowered_playerexample_Superpowered_HasEnoughAudio(JNIEnv * __unused env, jclass __unused obj) {
    if (__sync_fetch_and_add(&numFramesInBuffer, 0) > oneSecondNumFrames / 8) return JNI_TRUE; else return JNI_FALSE;
}

extern "C" JNIEXPORT void
Java_com_superpowered_playerexample_Superpowered_Append(JNIEnv * __unused env, jclass __unused obj, jshortArray jbuf, jint numFramesInBuf) {
    jshort *shortArray = env->GetShortArrayElements(jbuf, NULL);
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

extern "C" JNIEXPORT jint
Java_com_superpowered_playerexample_Superpowered_onUSBConnect(JNIEnv * __unused env, jclass __unused obj, jint deviceID, jint fd, jbyteArray rawDescriptor) {
    jbyte *rd = env->GetByteArrayElements(rawDescriptor, NULL);
    int dataBytes = env->GetArrayLength(rawDescriptor);
    int r = Superpowered::AndroidUSB::onConnect(deviceID, fd, (unsigned char *)rd, dataBytes);
    env->ReleaseByteArrayElements(rawDescriptor, rd, JNI_ABORT);

    // r & 1 is true if the device has audio. Start output.
    if (r & 1) {
        Superpowered::AndroidUSBAudio::setConfiguration(deviceID, 0);
        Superpowered::AndroidUSBAudioIOInfo *outputs;
        int numOutputs = Superpowered::AndroidUSBAudio::getOutputs(deviceID, &outputs);

        // ExoPlayer will output with the Android device's built-in samplerate.
        // You can either hack ExoPlayer to output hi-res audio, or use Superpowered AdvancedAudioPlayer to play hi-res audio.
        // For this example we're selecting the USB output with the same sample rate.
        for (int n = 0; n < numOutputs; n++) if ((outputs[n].numChannels == 2) && (outputs[n].samplerate == deviceSamplerate)) {
            audioIO->stop();
            Superpowered::AndroidUSBAudio::startIO(deviceID, -1, n, Superpowered::AndroidUSBAudioBufferSize_Mid, NULL, audioProcessingUSB);
            break;
        }
        free(outputs);
    }
    return r;
}

extern "C" JNIEXPORT void
Java_com_superpowered_playerexample_Superpowered_onUSBDisconnect(JNIEnv * __unused env, jclass __unused obj, jint deviceID) {
    Superpowered::AndroidUSBAudio::stopIO(deviceID);
    audioIO->start();
}
