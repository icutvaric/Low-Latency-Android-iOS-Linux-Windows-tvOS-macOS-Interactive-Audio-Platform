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
#include <thread>
#include <mutex>

#define log_print __android_log_print

static SuperpoweredAndroidAudioIO *audioIO;

std::mutex mtx;
static float * circledBuffer;
static int CIRCLED_BUFFER_SIZE = 0;
static int bufferLength = 0;
static int readIndex = 0;
static int writeIndex = 0;

bool accessCircledBufferforReading(bool read, float inputArray[], short int *audio, int numberOfFrames) {
    mtx.lock();
    std::thread::id this_id = std::this_thread::get_id();
    log_print(ANDROID_LOG_DEBUG, "PlayerExample", "thread %d locked mutex", this_id);

    if(read) {
        log_print(ANDROID_LOG_DEBUG, "PlayerExample", "Try reading...");
        if (CIRCLED_BUFFER_SIZE != 0 && (bufferLength == CIRCLED_BUFFER_SIZE)) {
            float outPutArray[CIRCLED_BUFFER_SIZE];
            for (int i = 0; i < CIRCLED_BUFFER_SIZE; i++) {
                outPutArray[i] = *(circledBuffer + readIndex);
                readIndex++;
                bufferLength--;
                if (readIndex == CIRCLED_BUFFER_SIZE) {
                    readIndex = 0;
                }
            }
            SuperpoweredFloatToShortInt(outPutArray, audio, (unsigned int) numberOfFrames);
            log_print(ANDROID_LOG_DEBUG, "PlayerExample", "Reading success!");
            log_print(ANDROID_LOG_DEBUG, "PlayerExample", "thread %d unlocked mutex", this_id);
            mtx.unlock();
            return true;
        } else {
            log_print(ANDROID_LOG_DEBUG, "PlayerExample", "Skip reading!");
            log_print(ANDROID_LOG_DEBUG, "PlayerExample", "thread %d unlocked mutex", this_id);
            mtx.unlock();
            return false;
        }

    } else {
        log_print(ANDROID_LOG_DEBUG, "PlayerExample", "Try writing...");
        for (int i = 0; i < CIRCLED_BUFFER_SIZE; i++) {
            if (bufferLength == CIRCLED_BUFFER_SIZE) {
                log_print(ANDROID_LOG_DEBUG, "PlayerExample", "Buffer is full, skip writing!");
                log_print(ANDROID_LOG_DEBUG, "PlayerExample", "thread %d unlocked mutex", this_id);
                mtx.unlock();
                return false;
            }
            *(circledBuffer + writeIndex) = inputArray[i];
            bufferLength++;
            writeIndex++;
            if (writeIndex == CIRCLED_BUFFER_SIZE) {
                writeIndex = 0;
            }
        }
        log_print(ANDROID_LOG_DEBUG, "PlayerExample", "Writing success!");
        log_print(ANDROID_LOG_DEBUG, "PlayerExample", "thread %d unlocked mutex", this_id);
        mtx.unlock();
        return true;
    }
}


// This is called periodically by the audio engine.
static bool audioProcessing (
        void * __unused clientdata, // custom pointer
        short int *audio,           // buffer of interleaved samples
        int numberOfFrames,         // number of frames to process
        int __unused samplerate     // sampling rate
) {
    float dummyArray [CIRCLED_BUFFER_SIZE];
    return accessCircledBufferforReading(true, dummyArray, audio, numberOfFrames);
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
    CIRCLED_BUFFER_SIZE = buffersize; // TODO try with 2 * buffersize
    circledBuffer = (float *)malloc(sizeof(float) * 1 * CIRCLED_BUFFER_SIZE);
    log_print(ANDROID_LOG_DEBUG, "PlayerExample", "Initialize circled buffer size: %d", buffersize);
}

// onBackground - Put audio processing to sleep.
extern "C" JNIEXPORT void
Java_com_superpowered_playerexample_Superpowered_onBackground (
        JNIEnv * __unused env,
        jclass type
) {
    audioIO->onBackground();
}

// onForeground - Resume audio processing.
extern "C" JNIEXPORT void
Java_com_superpowered_playerexample_Superpowered_onForeground (
        JNIEnv * __unused env,
        jclass type
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
    free(circledBuffer);
}

extern "C" JNIEXPORT jboolean
Java_com_superpowered_playerexample_Superpowered_writeRawPcm (
        JNIEnv * __unused env,
        jclass type,
        jfloatArray rawPcm
) {

    jfloat *array = env->GetFloatArrayElements(rawPcm, nullptr);
    int arraySize = env->GetArrayLength(rawPcm); // arraySize == CIRCLED_BUFFER_SIZE

    log_print(ANDROID_LOG_DEBUG, "PlayerExample", "Input array size: %d; circled buffer size: %d", arraySize, CIRCLED_BUFFER_SIZE);

    float inputArray[arraySize];
    for (int i = 0; i < arraySize; i++) {
        inputArray[i] = array[i];
    }
    env->ReleaseFloatArrayElements(rawPcm, array, 0);

    short int *dummyAudio;
    return (jboolean) accessCircledBufferforReading(false, inputArray, dummyAudio, 0);
}



