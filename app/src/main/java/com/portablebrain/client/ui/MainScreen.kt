package com.portablebrain.client.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.portablebrain.client.MainViewModel
import com.portablebrain.client.data.ExecutionStatus

@Composable
fun MainScreen(viewModel: MainViewModel, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isTracking by viewModel.isTracking.collectAsState()
    val a11yEnabled by viewModel.isAccessibilityEnabled.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    val executeError by viewModel.executeError.collectAsState()
    val executionLog by viewModel.executionLog.collectAsState()
    val executionStatus by viewModel.executionStatus.collectAsState()
    val executionSummary by viewModel.executionSummary.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()

    var commandText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.checkAccessibilityEnabled(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Portable Brain", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onSignOut) { Text("Sign Out") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Accessibility service warning banner
        if (!a11yEnabled) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(12.dp)
            ) {
                Text(
                    "Accessibility Service is not enabled",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(onClick = { openAccessibilitySettings(context) }) {
                    Text("Open Settings")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Tracking status + toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isTracking) "Tracking active" else "Paused",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isTracking)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    if (isTracking) viewModel.stopTracking(context)
                    else viewModel.startTracking(context)
                },
                enabled = a11yEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTracking)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isTracking) "Stop Tracking" else "Start Tracking")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Command input
        Text("Command", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = commandText,
            onValueChange = { commandText = it },
            placeholder = { Text("e.g. Open Settings and turn on Wi-Fi") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            enabled = !isExecuting
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                viewModel.sendCommand(commandText)
                commandText = ""
            },
            enabled = commandText.isNotBlank() && !isExecuting && a11yEnabled,
            modifier = Modifier.align(Alignment.End)
        ) {
            if (isExecuting) {
                CircularProgressIndicator(
                    modifier = Modifier.height(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Execute")
            }
        }

        // Step progress indicator during execution
        if (isExecuting && currentStep != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Executing step $currentStep...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error display
        if (executeError != null) {
            Text(
                text = "Error: $executeError",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Execution summary (on completion)
        if (executionStatus != null && !isExecuting) {
            val statusColor = when (executionStatus) {
                ExecutionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                ExecutionStatus.FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                text = when (executionStatus) {
                    ExecutionStatus.COMPLETED -> "Completed"
                    ExecutionStatus.FAILED -> "Failed"
                    else -> "Stopped"
                },
                color = statusColor,
                style = MaterialTheme.typography.titleSmall
            )
            if (executionSummary != null) {
                Text(
                    text = executionSummary!!,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Execution log
        if (executionLog.isNotEmpty()) {
            Text("Execution Log", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                executionLog.forEachIndexed { index, entry ->
                    Text(
                        text = "${index + 1}. $entry",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    if (index < executionLog.lastIndex) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

private fun openAccessibilitySettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    })
}
