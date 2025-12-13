/**
 * LUCID AI - JNI Bridge for llama.cpp
 *
 * This file provides the JNI interface between Kotlin and llama.cpp.
 * Implements on-device LLM inference for the Reality Filter.
 */

#include <jni.h>
#include <string>
#include <vector>
#include <mutex>
#include <android/log.h>

#include "llama.h"

#define LOG_TAG "LucidAI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// Global state
static llama_model* g_model = nullptr;
static llama_context* g_ctx = nullptr;
static std::mutex g_mutex;

// Tokenizer helper
static std::vector<llama_token> tokenize(const std::string& text, bool add_bos) {
    std::vector<llama_token> tokens;
    if (!g_model) return tokens;

    int n_tokens = text.length() + (add_bos ? 1 : 0);
    tokens.resize(n_tokens);

    n_tokens = llama_tokenize(g_model, text.c_str(), text.length(),
                               tokens.data(), tokens.size(), add_bos, false);

    if (n_tokens < 0) {
        tokens.resize(-n_tokens);
        n_tokens = llama_tokenize(g_model, text.c_str(), text.length(),
                                   tokens.data(), tokens.size(), add_bos, false);
    }

    tokens.resize(n_tokens);
    return tokens;
}

extern "C" {

/**
 * Initialize llama backend (called once)
 */
JNIEXPORT void JNICALL
Java_com_lucid_app_ai_LlamaInference_nativeInit(
    JNIEnv* env,
    jobject thiz
) {
    LOGI("Initializing llama backend");
    llama_backend_init();
}

/**
 * Load model from file path
 * Returns true on success, false on failure
 */
JNIEXPORT jboolean JNICALL
Java_com_lucid_app_ai_LlamaInference_nativeLoadModel(
    JNIEnv* env,
    jobject thiz,
    jstring model_path,
    jint n_threads,
    jint n_ctx
) {
    std::lock_guard<std::mutex> lock(g_mutex);

    const char* path = env->GetStringUTFChars(model_path, nullptr);
    LOGI("Loading model from: %s", path);
    LOGI("Threads: %d, Context: %d", n_threads, n_ctx);

    // Unload existing model if any
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_free_model(g_model);
        g_model = nullptr;
    }

    // Model parameters
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0; // CPU only for mobile
    model_params.use_mmap = true;
    model_params.use_mlock = false;

    // Load model
    g_model = llama_load_model_from_file(path, model_params);
    env->ReleaseStringUTFChars(model_path, path);

    if (!g_model) {
        LOGE("Failed to load model");
        return JNI_FALSE;
    }
    LOGI("Model loaded successfully");

    // Context parameters
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = n_ctx > 0 ? n_ctx : 2048;
    ctx_params.n_threads = n_threads > 0 ? n_threads : 4;
    ctx_params.n_threads_batch = n_threads > 0 ? n_threads : 4;
    ctx_params.flash_attn = false;

    // Create context
    g_ctx = llama_new_context_with_model(g_model, ctx_params);
    if (!g_ctx) {
        LOGE("Failed to create context");
        llama_free_model(g_model);
        g_model = nullptr;
        return JNI_FALSE;
    }

    LOGI("Context created successfully");
    return JNI_TRUE;
}

/**
 * Unload model and free resources
 */
JNIEXPORT void JNICALL
Java_com_lucid_app_ai_LlamaInference_nativeUnloadModel(
    JNIEnv* env,
    jobject thiz
) {
    std::lock_guard<std::mutex> lock(g_mutex);

    LOGI("Unloading model");

    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_free_model(g_model);
        g_model = nullptr;
    }
}

/**
 * Check if model is loaded
 */
JNIEXPORT jboolean JNICALL
Java_com_lucid_app_ai_LlamaInference_nativeIsModelLoaded(
    JNIEnv* env,
    jobject thiz
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return (g_model != nullptr && g_ctx != nullptr) ? JNI_TRUE : JNI_FALSE;
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
    jfloat temperature,
    jfloat top_p,
    jint top_k
) {
    std::lock_guard<std::mutex> lock(g_mutex);

    if (!g_model || !g_ctx) {
        LOGE("Model not loaded");
        return env->NewStringUTF("");
    }

    const char* prompt_str = env->GetStringUTFChars(prompt, nullptr);
    std::string prompt_text(prompt_str);
    env->ReleaseStringUTFChars(prompt, prompt_str);

    LOGD("Generating with prompt length: %zu, max_tokens: %d", prompt_text.length(), max_tokens);

    // Tokenize prompt
    std::vector<llama_token> tokens = tokenize(prompt_text, true);
    if (tokens.empty()) {
        LOGE("Failed to tokenize prompt");
        return env->NewStringUTF("");
    }

    LOGD("Tokenized to %zu tokens", tokens.size());

    // Check context size
    int n_ctx = llama_n_ctx(g_ctx);
    if ((int)tokens.size() > n_ctx - 4) {
        LOGE("Prompt too long: %zu tokens, max: %d", tokens.size(), n_ctx - 4);
        return env->NewStringUTF("");
    }

    // Clear KV cache
    llama_kv_cache_clear(g_ctx);

    // Process prompt in a batch
    llama_batch batch = llama_batch_init(tokens.size(), 0, 1);
    for (size_t i = 0; i < tokens.size(); i++) {
        llama_batch_add(batch, tokens[i], i, { 0 }, false);
    }
    batch.logits[batch.n_tokens - 1] = true;

    if (llama_decode(g_ctx, batch) != 0) {
        LOGE("Failed to decode prompt");
        llama_batch_free(batch);
        return env->NewStringUTF("");
    }
    llama_batch_free(batch);

    // Sampling parameters
    llama_sampler* sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(top_k > 0 ? top_k : 40));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(top_p > 0 ? top_p : 0.95f, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature > 0 ? temperature : 0.8f));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(42));

    // Generate tokens
    std::string result;
    int n_cur = tokens.size();
    int n_generated = 0;

    while (n_generated < max_tokens) {
        // Sample next token
        llama_token new_token = llama_sampler_sample(sampler, g_ctx, -1);

        // Check for end of stream
        if (llama_token_is_eog(g_model, new_token)) {
            break;
        }

        // Decode token to text
        char buf[256];
        int n = llama_token_to_piece(g_model, new_token, buf, sizeof(buf), 0, false);
        if (n > 0) {
            result.append(buf, n);
        }

        // Prepare next batch
        llama_batch next_batch = llama_batch_init(1, 0, 1);
        llama_batch_add(next_batch, new_token, n_cur, { 0 }, true);

        if (llama_decode(g_ctx, next_batch) != 0) {
            LOGE("Failed to decode token");
            llama_batch_free(next_batch);
            break;
        }
        llama_batch_free(next_batch);

        n_cur++;
        n_generated++;
    }

    llama_sampler_free(sampler);

    LOGD("Generated %d tokens, result length: %zu", n_generated, result.length());

    return env->NewStringUTF(result.c_str());
}

/**
 * Get model info as JSON
 */
JNIEXPORT jstring JNICALL
Java_com_lucid_app_ai_LlamaInference_nativeGetModelInfo(
    JNIEnv* env,
    jobject thiz
) {
    std::lock_guard<std::mutex> lock(g_mutex);

    if (!g_model) {
        return env->NewStringUTF("{}");
    }

    char buf[512];
    snprintf(buf, sizeof(buf),
        "{\"n_vocab\":%d,\"n_ctx_train\":%d,\"n_embd\":%d}",
        llama_n_vocab(g_model),
        llama_n_ctx_train(g_model),
        llama_n_embd(g_model)
    );

    return env->NewStringUTF(buf);
}

/**
 * Free llama backend
 */
JNIEXPORT void JNICALL
Java_com_lucid_app_ai_LlamaInference_nativeFree(
    JNIEnv* env,
    jobject thiz
) {
    std::lock_guard<std::mutex> lock(g_mutex);

    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_free_model(g_model);
        g_model = nullptr;
    }

    llama_backend_free();
    LOGI("Backend freed");
}

} // extern "C"
