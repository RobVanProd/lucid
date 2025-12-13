package com.lucid.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lucid.app.ai.LlamaInference
import com.lucid.app.ai.ModelStatus
import kotlinx.coroutines.launch

/**
 * Model Management Screen
 *
 * Allows users to download and manage the on-device AI model.
 * This enables true offline, private inference - no data leaves the device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val inference = remember { LlamaInference.getInstance(context) }

    val modelStatus by inference.status.collectAsState()
    val downloadProgress by inference.downloadProgress.collectAsState()

    var isDownloading by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var useSmallModel by remember { mutableStateOf(true) } // Default to smaller model

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI Model",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.background(MaterialTheme.colorScheme.background)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Icon
            StatusIcon(modelStatus)

            Spacer(modifier = Modifier.height(24.dp))

            // Status Text
            StatusText(modelStatus, downloadProgress)

            Spacer(modifier = Modifier.height(32.dp))

            // Model Selection (only show when not downloaded)
            if (modelStatus is ModelStatus.NotDownloaded ||
                modelStatus is ModelStatus.NativeNotAvailable) {
                ModelSelector(
                    useSmallModel = useSmallModel,
                    onSelectionChange = { useSmallModel = it },
                    enabled = !isDownloading
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Progress Bar (when downloading)
            if (modelStatus is ModelStatus.Downloading) {
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${(downloadProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Action Button
            ActionButton(
                modelStatus = modelStatus,
                isDownloading = isDownloading,
                isLoading = isLoading,
                onDownload = {
                    isDownloading = true
                    scope.launch {
                        inference.downloadModel(useSmallModel = useSmallModel)
                        isDownloading = false
                    }
                },
                onLoad = {
                    isLoading = true
                    scope.launch {
                        inference.loadModel()
                        isLoading = false
                    }
                },
                onUnload = {
                    inference.unloadModel()
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Info Section
            InfoSection(modelStatus)
        }
    }
}

@Composable
private fun StatusIcon(status: ModelStatus) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(
                when (status) {
                    is ModelStatus.Ready -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    is ModelStatus.Error -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    is ModelStatus.Downloading, is ModelStatus.Loading ->
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            is ModelStatus.Ready -> {
                Icon(
                    imageVector = Icons.Outlined.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            is ModelStatus.Downloading, is ModelStatus.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(56.dp),
                    strokeWidth = 4.dp
                )
            }
            is ModelStatus.Downloaded -> {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            is ModelStatus.Error -> {
                Icon(
                    imageVector = Icons.Outlined.Error,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            is ModelStatus.NativeNotAvailable -> {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Outlined.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusText(status: ModelStatus, progress: Float) {
    val (title, subtitle) = when (status) {
        is ModelStatus.Ready -> "AI Ready" to "On-device inference enabled"
        is ModelStatus.Downloading -> "Downloading Model" to "This may take a few minutes"
        is ModelStatus.Loading -> "Loading Model" to "Initializing neural network..."
        is ModelStatus.Downloaded -> "Model Downloaded" to "Ready to load into memory"
        is ModelStatus.NotDownloaded -> "No Model" to "Download to enable AI features"
        is ModelStatus.NativeNotAvailable -> "Native Library Missing" to "Rebuild with NDK to enable"
        is ModelStatus.Error -> "Error" to status.message
    }

    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ModelSelector(
    useSmallModel: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Select Model",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Small Model Option
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = useSmallModel,
                    onClick = { onSelectionChange(true) },
                    enabled = enabled
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SmolLM2 135M",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "~144 MB • Fast • Good for classification",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Large Model Option
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = !useSmallModel,
                    onClick = { onSelectionChange(false) },
                    enabled = enabled
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Qwen2.5 0.5B",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "~394 MB • Balanced • Better generation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    modelStatus: ModelStatus,
    isDownloading: Boolean,
    isLoading: Boolean,
    onDownload: () -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit
) {
    when (modelStatus) {
        is ModelStatus.NotDownloaded -> {
            Button(
                onClick = onDownload,
                enabled = !isDownloading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Download Model")
                }
            }
        }
        is ModelStatus.Downloaded -> {
            Button(
                onClick = onLoad,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Load Model")
                }
            }
        }
        is ModelStatus.Ready -> {
            OutlinedButton(
                onClick = onUnload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.Outlined.Stop, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Unload Model")
            }
        }
        is ModelStatus.Error -> {
            Button(
                onClick = onDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Retry Download")
            }
        }
        else -> {
            // Downloading or Loading - show disabled button
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun InfoSection(status: ModelStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Privacy First",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "All AI inference runs entirely on your device. No data is sent to external servers. Your intentions and browsing remain completely private.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
