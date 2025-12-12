package com.lucid.app.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Llama.cpp Native Inference Engine
 *
 * This class provides the interface for on-device LLM inference using llama.cpp.
 * The native library (libllama.so) needs to be compiled separately and placed in jniLibs.
 *
 * For the MVP, this uses enhanced rule-based inference with the native integration
 * available as an upgrade path.
 *
 * To enable native inference:
 * 1. Build llama.cpp for Android (arm64-v8a, armeabi-v7a)
 * 2. Place the .so files in app/src/main/jniLibs/{abi}/
 * 3. Download a GGUF model (e.g., Qwen2.5-0.5B-Instruct-Q4_K_M.gguf)
 * 4. Call loadModel() with the model path
 */
class LlamaInference(private val context: Context) {

    private val _status = MutableStateFlow<ModelStatus>(ModelStatus.NotDownloaded)
    val status: StateFlow<ModelStatus> = _status

    private var isNativeLoaded = false
    private var modelLoaded = false

    // Model configuration
    private val modelFileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
    private val modelUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"

    init {
        tryLoadNativeLibrary()
    }

    /**
     * Try to load the native llama.cpp library
     */
    private fun tryLoadNativeLibrary() {
        try {
            System.loadLibrary("llama")
            System.loadLibrary("llama-jni")
            isNativeLoaded = true
            _status.value = ModelStatus.NotDownloaded
        } catch (e: UnsatisfiedLinkError) {
            // Native library not available - this is expected for MVP
            isNativeLoaded = false
            _status.value = ModelStatus.Ready // Use fallback mode
        }
    }

    /**
     * Check if native inference is available
     */
    fun isNativeAvailable(): Boolean = isNativeLoaded

    /**
     * Check if the model is downloaded
     */
    fun isModelDownloaded(): Boolean {
        val modelFile = File(context.filesDir, "models/$modelFileName")
        return modelFile.exists()
    }

    /**
     * Get the model file path
     */
    fun getModelPath(): String {
        return File(context.filesDir, "models/$modelFileName").absolutePath
    }

    /**
     * Download the model (would need to be implemented with actual download logic)
     */
    suspend fun downloadModel(onProgress: (Float) -> Unit): Boolean {
        return withContext(Dispatchers.IO) {
            _status.value = ModelStatus.Downloading(0f)

            // Create models directory
            val modelsDir = File(context.filesDir, "models")
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
            }

            // For MVP, we skip actual download and use rule-based system
            // In production, implement OkHttp download with progress

            _status.value = ModelStatus.Ready
            false // Return false to indicate native model not downloaded
        }
    }

    /**
     * Load the model into memory
     */
    suspend fun loadModel(): Boolean {
        return withContext(Dispatchers.IO) {
            if (!isNativeLoaded) {
                return@withContext false
            }

            val modelPath = getModelPath()
            if (!File(modelPath).exists()) {
                return@withContext false
            }

            _status.value = ModelStatus.Loading

            try {
                val result = nativeLoadModel(modelPath)
                if (result) {
                    modelLoaded = true
                    _status.value = ModelStatus.Ready
                } else {
                    _status.value = ModelStatus.Error("Failed to load model")
                }
                result
            } catch (e: Exception) {
                _status.value = ModelStatus.Error(e.message ?: "Load failed")
                false
            }
        }
    }

    /**
     * Unload the model from memory
     */
    fun unloadModel() {
        if (isNativeLoaded && modelLoaded) {
            nativeUnloadModel()
            modelLoaded = false
        }
    }

    /**
     * Run inference on the model
     */
    suspend fun generate(
        prompt: String,
        maxTokens: Int = 256,
        temperature: Float = 0.7f
    ): String {
        return withContext(Dispatchers.Default) {
            if (!isNativeLoaded || !modelLoaded) {
                // Fallback: return empty string (caller should use rule-based system)
                return@withContext ""
            }

            try {
                nativeGenerate(prompt, maxTokens, temperature)
            } catch (e: Exception) {
                ""
            }
        }
    }

    /**
     * Classify intent using the model
     */
    suspend fun classifyIntent(text: String): IntentClassification? {
        if (!isNativeLoaded || !modelLoaded) {
            return null // Use fallback
        }

        return withContext(Dispatchers.Default) {
            val prompt = buildClassificationPrompt(text)
            val response = generate(prompt, maxTokens = 50, temperature = 0.1f)
            parseClassificationResponse(response, text)
        }
    }

    private fun buildClassificationPrompt(text: String): String {
        return """<|im_start|>system
You are an intent classifier. Classify the user query into one of these categories:
RECIPE, DEFINITION, LEARN, SEARCH, NAVIGATE, CALCULATE, WEATHER, NEWS
Respond with only the category name.<|im_end|>
<|im_start|>user
$text<|im_end|>
<|im_start|>assistant
"""
    }

    private fun parseClassificationResponse(response: String, originalText: String): IntentClassification? {
        val category = response.trim().uppercase()
        val intentType = try {
            IntentType.valueOf(category)
        } catch (e: Exception) {
            return null
        }

        return IntentClassification(
            intentType = intentType,
            confidence = 0.85f,
            topic = originalText
        )
    }

    // Native method declarations (implemented in C++ via JNI)
    private external fun nativeLoadModel(modelPath: String): Boolean
    private external fun nativeUnloadModel()
    private external fun nativeGenerate(prompt: String, maxTokens: Int, temperature: Float): String

    companion object {
        // Singleton instance
        @Volatile
        private var instance: LlamaInference? = null

        fun getInstance(context: Context): LlamaInference {
            return instance ?: synchronized(this) {
                instance ?: LlamaInference(context.applicationContext).also { instance = it }
            }
        }
    }
}
