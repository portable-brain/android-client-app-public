package com.portablebrain.client

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import com.portablebrain.client.data.ExecutionStatus
import com.portablebrain.client.service.TrackingForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel that drives the main screen: tracking toggle, accessibility checks,
 * and the command execution loop.
 *
 * This is a simplified stub for the public build. The full implementation includes
 * the proprietary execution loop that orchestrates multi-step command execution
 * between the client and backend — managing sessions, dispatching actions via the
 * AccessibilityService, capturing resulting UI state, and reporting back to the
 * backend for the next instruction.
 *
 * To build a complete implementation, the execution loop follows this cycle:
 *   1. POST to /execution/start with the user's request
 *   2. Receive a session ID and first command from the backend
 *   3. Execute the command via [PortableBrainAccessibilityService.executeCommand]
 *   4. Capture the resulting UI state via [PortableBrainAccessibilityService.getCurrentSnapshot]
 *   5. POST the result + new UI state to /execution/{session_id}/step
 *   6. Receive the next command (or completion status)
 *   7. Repeat steps 3-6 until the backend signals completion or failure
 *
 * See the data models in data/Models.kt and the API surface in network/ApiService.kt
 * for the expected request/response formats.
 */
class MainViewModel : ViewModel() {

    val isTracking: StateFlow<Boolean> = TrackingForegroundService.isRunning

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting

    private val _executionLog = MutableStateFlow<List<String>>(emptyList())
    val executionLog: StateFlow<List<String>> = _executionLog

    private val _executionStatus = MutableStateFlow<ExecutionStatus?>(null)
    val executionStatus: StateFlow<ExecutionStatus?> = _executionStatus

    private val _executionSummary = MutableStateFlow<String?>(null)
    val executionSummary: StateFlow<String?> = _executionSummary

    private val _currentStep = MutableStateFlow<Int?>(null)
    val currentStep: StateFlow<Int?> = _currentStep

    private val _executeError = MutableStateFlow<String?>(null)
    val executeError: StateFlow<String?> = _executeError

    fun checkAccessibilityEnabled(context: Context) {
        val pkg = context.packageName
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        _isAccessibilityEnabled.value = enabledServices.split(':').any { component ->
            component.startsWith("$pkg/")
        }
    }

    fun startTracking(context: Context) {
        val intent = Intent(context, TrackingForegroundService::class.java).apply {
            action = TrackingForegroundService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    fun stopTracking(context: Context) {
        val intent = Intent(context, TrackingForegroundService::class.java).apply {
            action = TrackingForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }

    /**
     * Executes a natural-language command via the Portable Brain backend.
     *
     * In the public build, this is a no-op placeholder. See the class-level
     * documentation for a description of the full execution loop.
     */
    fun sendCommand(request: String) {
        if (request.isBlank()) return
        _executionLog.value = listOf("Execution loop not available in the public build.")
        _executeError.value = "This is the open-source stub. The execution loop is part of the full Portable Brain client."
    }
}
