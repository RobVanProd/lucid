package com.lucid.app.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Llama.cpp Native Inference Engine
 *
 * This class provides the interface for on-device LLM inference using llama.cpp.
 * The native library is built as part of the Android build process.
 *
 * Features:
 * - Automatic model downloading with progress
 * - Thread-safe inference
 * - Memory-efficient streaming generation
 */
class LlamaInference private constructor(private val context: Context) {

    private val _status = MutableStateFlow<ModelStatus>(ModelStatus.NotDownloaded)
    val status: StateFlow<ModelStatus> = _status

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    private var isNativeLoaded = false
    private var modelLoaded = false
    private var backendInitialized = false

    // Model configuration - Qwen2.5 0.5B is small enough for mobile (~400MB)
    private val modelFileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
    private val modelUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
    private val modelSizeBytes = 394_000_000L // ~394 MB

    // Alternative smaller model (~150MB)
    private val smallModelFileName = "smollm2-135m-instruct-q8_0.gguf"
    private val smallModelUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-135M-Instruct-GGUF/resolve/main/smollm2-135m-instruct-q8_0.gguf"
    private val smallModelSizeBytes = 144_000_000L

    // HTTP client for downloads
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .build()

    init {
        tryLoadNativeLibrary()
        checkModelStatus()
    }

    /**
     * Try to load the native llama.cpp library
     */
    private fun tryLoadNativeLibrary() {
        try {
            System.loadLibrary("llama-jni")
            isNativeLoaded = true
            Log.i(TAG, "Native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library not available: ${e.message}")
            isNativeLoaded = false
            _status.value = ModelStatus.NativeNotAvailable
        }
    }

    /**
     * Check if native inference is available
     */
    fun isNativeAvailable(): Boolean = isNativeLoaded

    /**
     * Check if the model is downloaded and update status
     */
    private fun checkModelStatus() {
        if (!isNativeLoaded) {
            _status.value = ModelStatus.NativeNotAvailable
            return
        }

        val modelFile = File(context.filesDir, "models/$modelFileName")
        val smallModelFile = File(context.filesDir, "models/$smallModelFileName")

        when {
            modelFile.exists() && modelFile.length() > modelSizeBytes * 0.9 -> {
                _status.value = ModelStatus.Downloaded(modelFile.absolutePath)
            }
            smallModelFile.exists() && smallModelFile.length() > smallModelSizeBytes * 0.9 -> {
                _status.value = ModelStatus.Downloaded(smallModelFile.absolutePath)
            }
            else -> {
                _status.value = ModelStatus.NotDownloaded
            }
        }
    }

    /**
     * Check if the model is downloaded
     */
    fun isModelDownloaded(): Boolean {
        return when (_status.value) {
            is ModelStatus.Downloaded -> true
            is ModelStatus.Ready -> true
            else -> false
        }
    }

    /**
     * Get the model file path
     */
    fun getModelPath(): String? {
        return when (val status = _status.value) {
            is ModelStatus.Downloaded -> status.path
            is ModelStatus.Ready -> {
                val modelFile = File(context.filesDir, "models/$modelFileName")
                if (modelFile.exists()) modelFile.absolutePath else null
            }
            else -> null
        }
    }

    /**
     * Download the model with progress updates
     */
    suspend fun downloadModel(
        useSmallModel: Boolean = false,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            _status.value = ModelStatus.Downloading(0f)
            _downloadProgress.value = 0f

            val fileName = if (useSmallModel) smallModelFileName else modelFileName
            val url = if (useSmallModel) smallModelUrl else modelUrl
            val expectedSize = if (useSmallModel) smallModelSizeBytes else modelSizeBytes

            // Create models directory
            val modelsDir = File(context.filesDir, "models")
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
            }

            val modelFile = File(modelsDir, fileName)

            // Check if already downloaded
            if (modelFile.exists() && modelFile.length() > expectedSize * 0.9) {
                Log.i(TAG, "Model already downloaded: ${modelFile.absolutePath}")
                _status.value = ModelStatus.Downloaded(modelFile.absolutePath)
                _downloadProgress.value = 1f
                onProgress(1f)
                return@withContext Result.success(modelFile.absolutePath)
            }

            // Download the model
            Log.i(TAG, "Downloading model from: $url")

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "LUCID-Android/1.0")
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("Download failed: ${response.code} ${response.message}")
            }

            val body = response.body ?: throw Exception("Empty response body")
            val contentLength = body.contentLength().let { if (it > 0) it else expectedSize }

            // Write to file with progress
            FileOutputStream(modelFile).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        val progress = totalBytesRead.toFloat() / contentLength
                        _downloadProgress.value = progress
                        _status.value = ModelStatus.Downloading(progress)
                        onProgress(progress)
                    }
                }
            }

            Log.i(TAG, "Model downloaded successfully: ${modelFile.absolutePath}")
            _status.value = ModelStatus.Downloaded(modelFile.absolutePath)
            _downloadProgress.value = 1f
            onProgress(1f)

            Result.success(modelFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            _status.value = ModelStatus.Error(e.message ?: "Download failed")
            Result.failure(e)
        }
    }

    /**
     * Initialize the llama backend (call once at app start)
     */
    suspend fun initBackend(): Boolean = withContext(Dispatchers.Default) {
        if (!isNativeLoaded) return@withContext false
        if (backendInitialized) return@withContext true

        try {
            nativeInit()
            backendInitialized = true
            Log.i(TAG, "Backend initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize backend", e)
            false
        }
    }

    /**
     * Load the model into memory
     */
    suspend fun loadModel(
        modelPath: String? = null,
        nThreads: Int = Runtime.getRuntime().availableProcessors().coerceIn(2, 8),
        nCtx: Int = 2048
    ): Boolean = withContext(Dispatchers.Default) {
        if (!isNativeLoaded) {
            Log.w(TAG, "Native library not loaded")
            return@withContext false
        }

        val path = modelPath ?: getModelPath()
        if (path == null) {
            Log.w(TAG, "No model path available")
            return@withContext false
        }

        if (!File(path).exists()) {
            Log.w(TAG, "Model file does not exist: $path")
            return@withContext false
        }

        _status.value = ModelStatus.Loading

        try {
            if (!backendInitialized) {
                nativeInit()
                backendInitialized = true
            }

            val result = nativeLoadModel(path, nThreads, nCtx)
            if (result) {
                modelLoaded = true
                _status.value = ModelStatus.Ready
                Log.i(TAG, "Model loaded: $path")
            } else {
                _status.value = ModelStatus.Error("Failed to load model")
                Log.e(TAG, "Failed to load model: $path")
            }
            result
        } catch (e: Exception) {
            _status.value = ModelStatus.Error(e.message ?: "Load failed")
            Log.e(TAG, "Exception loading model", e)
            false
        }
    }

    /**
     * Unload the model from memory
     */
    fun unloadModel() {
        if (isNativeLoaded && modelLoaded) {
            nativeUnloadModel()
            modelLoaded = false
            checkModelStatus()
            Log.i(TAG, "Model unloaded")
        }
    }

    /**
     * Check if model is loaded and ready
     */
    fun isModelLoaded(): Boolean {
        return if (isNativeLoaded) {
            nativeIsModelLoaded()
        } else {
            false
        }
    }

    /**
     * Run inference on the model
     */
    suspend fun generate(
        prompt: String,
        maxTokens: Int = 256,
        temperature: Float = 0.7f,
        topP: Float = 0.95f,
        topK: Int = 40
    ): String = withContext(Dispatchers.Default) {
        if (!isNativeLoaded || !modelLoaded) {
            Log.w(TAG, "Cannot generate: native=$isNativeLoaded, model=$modelLoaded")
            return@withContext ""
        }

        try {
            nativeGenerate(prompt, maxTokens, temperature, topP, topK)
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed", e)
            ""
        }
    }

    /**
     * Classify intent using the model
     */
    suspend fun classifyIntent(text: String): IntentClassification? {
        if (!isNativeLoaded || !modelLoaded) {
            return null
        }

        return withContext(Dispatchers.Default) {
            val prompt = buildClassificationPrompt(text)
            val response = generate(prompt, maxTokens = 50, temperature = 0.1f)
            parseClassificationResponse(response, text)
        }
    }

    /**
     * Summarize content using the model
     */
    suspend fun summarize(content: String, maxLength: Int = 150): String {
        if (!isNativeLoaded || !modelLoaded) {
            return content.take(maxLength)
        }

        val prompt = """<|im_start|>system
You are a helpful assistant that summarizes text concisely.<|im_end|>
<|im_start|>user
Summarize this in 2-3 sentences:

$content<|im_end|>
<|im_start|>assistant
"""
        return generate(prompt, maxTokens = 100, temperature = 0.3f)
    }

    /**
     * Extract key information from content
     */
    suspend fun extractInfo(content: String, infoType: String): String {
        if (!isNativeLoaded || !modelLoaded) {
            return ""
        }

        val prompt = """<|im_start|>system
You extract specific information from text.<|im_end|>
<|im_start|>user
Extract the $infoType from this text:

$content<|im_end|>
<|im_start|>assistant
"""
        return generate(prompt, maxTokens = 100, temperature = 0.2f)
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
        val category = response.trim().uppercase().replace(Regex("[^A-Z]"), "")
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

    /**
     * Free all resources
     */
    fun cleanup() {
        if (isNativeLoaded) {
            if (modelLoaded) {
                nativeUnloadModel()
                modelLoaded = false
            }
            if (backendInitialized) {
                nativeFree()
                backendInitialized = false
            }
        }
    }

    // Native method declarations
    private external fun nativeInit()
    private external fun nativeLoadModel(modelPath: String, nThreads: Int, nCtx: Int): Boolean
    private external fun nativeUnloadModel()
    private external fun nativeIsModelLoaded(): Boolean
    private external fun nativeGenerate(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        topK: Int
    ): String
    private external fun nativeGetModelInfo(): String
    private external fun nativeFree()

    companion object {
        private const val TAG = "LlamaInference"

        @Volatile
        private var instance: LlamaInference? = null

        fun getInstance(context: Context): LlamaInference {
            return instance ?: synchronized(this) {
                instance ?: LlamaInference(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * Model status states
 */
sealed class ModelStatus {
    data object NativeNotAvailable : ModelStatus()
    data object NotDownloaded : ModelStatus()
    data class Downloading(val progress: Float) : ModelStatus()
    data class Downloaded(val path: String) : ModelStatus()
    data object Loading : ModelStatus()
    data object Ready : ModelStatus()
    data class Error(val message: String) : ModelStatus()
}

/**
 * Intent types for classification
 */
enum class IntentType {
    RECIPE,
    DEFINITION,
    LEARN,
    SEARCH,
    NAVIGATE,
    CALCULATE,
    WEATHER,
    NEWS
}

/**
 * Intent classification result
 */
data class IntentClassification(
    val intentType: IntentType,
    val confidence: Float,
    val topic: String
)
