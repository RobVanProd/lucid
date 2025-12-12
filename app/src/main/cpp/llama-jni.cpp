/**
 * LUCID AI - JNI Bridge for llama.cpp
 *
 * This file provides the JNI interface between Kotlin and llama.cpp.
 * Currently provides stub implementations that return false/empty.
 *
 * To enable actual inference:
 * 1. Build llama.cpp for Android
 * 2. Include llama.h header
 * 3. Implement the functions below using llama.cpp API
 */

#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "LucidAI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Uncomment when llama.cpp is integrated:
// #include "llama.h"

// Global state (would hold llama context when integrated)
// static llama_context* g_ctx = nullptr;
// static llama_model* g_model = nullptr;

extern "C" {

/**
 * Load model from file path
 * Returns true on success, false on failure
 */
JNIEXPORT jboolean JNICALL
Java_com_lucid_app_ai_LlamaInference_nativeLoadModel(
    JNIEnv* env,
    jobject thiz,
    jstring model_path
) {
    const char* path = env->GetStringUTFChars(model_path, nullptr);
    LOGI("Loading model from: %s", path);

    // Stub implementation - actual loading would use llama.cpp:
    //
    // llama_model_params model_params = llama_model_default_params();
    // model_params.n_gpu_layers = 0; // CPU only for mobile
    //
    // g_model = llama_load_model_from_file(path, model_params);
    // if (!g_model) {
    //     LOGE("Failed to load model");
    //     env->ReleaseStringUTFChars(model_path, path);
    //     return JNI_FALSE;
    // }
    //
    // llama_context_params ctx_params = llama_context_default_params();
    // ctx_params.n_ctx = 2048;
    // ctx_params.n_threads = 4;
    //
    // g_ctx = llama_new_context_with_model(g_model, ctx_params);
    // if (!g_ctx) {
    //     LOGE("Failed to create context");
    //     llama_free_model(g_model);
    //     g_model = nullptr;
    //     env->ReleaseStringUTFChars(model_path, path);
    //     return JNI_FALSE;
    // }

    env->ReleaseStringUTFChars(model_path, path);

    // Return false to indicate native model not available
    // Caller will use rule-based fallback
    LOGI("Native inference not available - using fallback");
    return JNI_FALSE;
}

/**
 * Unload model and free resources
 */
JNIEXPORT void JNICALL
Java_com_lucid_app_ai_LlamaInference_nativeUnloadModel(
    JNIEnv* env,
    jobject thiz
) {
    LOGI("Unloading model");

    // Actual implementation:
    // if (g_ctx) {
    //     llama_free(g_ctx);
    //     g_ctx = nullptr;
    // }
    // if (g_model) {
    //     llama_free_model(g_model);
    //     g_model = nullptr;
    // }
}

/**
 * Generate text completion
 */
JNIEXPORT jstring JNICALL
Java_com_lucid_app_ai_LlamaInference_nativeGenerate(
    JNIEnv* env,
    jobject thiz,
    jstring prompt,
    jint max_tokens,
    jfloat temperature
) {
    const char* prompt_str = env->GetStringUTFChars(prompt, nullptr);
    LOGI("Generate called with prompt length: %zu", strlen(prompt_str));

    // Actual implementation would:
    // 1. Tokenize the prompt
    // 2. Run inference loop
    // 3. Decode tokens to string
    // 4. Return result
    //
    // std::string result;
    // ... inference code ...
    // return env->NewStringUTF(result.c_str());

    env->ReleaseStringUTFChars(prompt, prompt_str);

    // Return empty string to signal fallback needed
    return env->NewStringUTF("");
}

} // extern "C"
