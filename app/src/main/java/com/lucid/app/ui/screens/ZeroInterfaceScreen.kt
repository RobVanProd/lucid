package com.lucid.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lucid.app.ui.components.BreathingDot
import com.lucid.app.ui.components.IntentionInput
import com.lucid.app.ui.components.ModeIndicator
import com.lucid.app.ui.components.ModeSwitch

/**
 * The Zero Interface - The Blank Canvas
 *
 * When you unlock your device, you do not see a grid of apps.
 * You see a single, pulsing cursor. A question: "What is the intention?"
 *
 * This is the cognitive firewall.
 */
@Composable
fun ZeroInterfaceScreen(
    isLucidMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    onIntentionSubmit: (String) -> Unit,
    onBriefingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var intentionText by remember { mutableStateOf("") }
    var showQuickActions by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Mode indicator at top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BreathingDot()
            ModeIndicator(isLucidMode = isLucidMode)
        }

        // Central intention input
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IntentionInput(
                value = intentionText,
                onValueChange = {
                    intentionText = it
                    showQuickActions = it.isEmpty()
                },
                onSubmit = { intention ->
                    onIntentionSubmit(intention)
                    intentionText = ""
                }
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Submit button when text is entered
            AnimatedVisibility(
                visible = intentionText.isNotBlank(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Button(
                    onClick = {
                        onIntentionSubmit(intentionText.trim())
                        intentionText = ""
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    Text(
                        text = "Begin",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }

            // Quick action hints when empty
            AnimatedVisibility(
                visible = intentionText.isEmpty() && showQuickActions,
                enter = fadeIn(animationSpec = tween(delayMillis = 500)),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 48.dp)
                ) {
                    Text(
                        text = "or",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        QuickActionChip(
                            icon = Icons.Outlined.Mail,
                            label = "Briefing",
                            onClick = onBriefingClick
                        )
                    }
                }
            }
        }

        // Mode switch at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .align(Alignment.BottomCenter)
        ) {
            ModeSwitch(
                isLucidMode = isLucidMode,
                onModeChange = onModeChange,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
