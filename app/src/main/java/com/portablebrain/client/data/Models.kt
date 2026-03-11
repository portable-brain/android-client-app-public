package com.portablebrain.client.data

import com.google.gson.annotations.SerializedName

// --- Tracking models (background monitoring) ---

data class UIActivity(
    @SerializedName("activity_name") val activityName: String
)

data class UIStateSnapshot(
    @SerializedName("formatted_text") val formattedText: String,
    @SerializedName("activity") val activity: UIActivity,
    @SerializedName("package") val packageName: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("is_app_switch") val isAppSwitch: Boolean = false,
    @SerializedName("app_switch_info") val appSwitchInfo: String? = null,
    @SerializedName("is_intentional_interaction") val isIntentionalInteraction: Boolean
)

// --- Execution loop enums ---

enum class ActionType {
    @SerializedName("click") CLICK,
    @SerializedName("long_click") LONG_CLICK,
    @SerializedName("set_text") SET_TEXT,
    @SerializedName("scroll") SCROLL,
    @SerializedName("back") BACK,
    @SerializedName("home") HOME,
    @SerializedName("open_app") OPEN_APP,
    @SerializedName("wait") WAIT
}

enum class ScrollDirection {
    @SerializedName("up") UP,
    @SerializedName("down") DOWN,
    @SerializedName("left") LEFT,
    @SerializedName("right") RIGHT
}

enum class ActionResult {
    @SerializedName("success") SUCCESS,
    @SerializedName("failed") FAILED,
    @SerializedName("node_not_found") NODE_NOT_FOUND,
    @SerializedName("timeout") TIMEOUT
}

enum class ExecutionStatus {
    @SerializedName("in_progress") IN_PROGRESS,
    @SerializedName("completed") COMPLETED,
    @SerializedName("failed") FAILED
}

// --- Execution request models (client -> backend) ---

data class ExecutionStartRequest(
    @SerializedName("user_request") val userRequest: String,
    @SerializedName("max_steps") val maxSteps: Int = 5
)

data class UIStatePayload(
    @SerializedName("formatted_text") val formattedText: String,
    @SerializedName("activity") val activity: UIActivity,
    @SerializedName("package") val packageName: String,
    @SerializedName("timestamp") val timestamp: String
)

data class ExecutionStepRequest(
    @SerializedName("action_result") val actionResult: ActionResult,
    @SerializedName("error_detail") val errorDetail: String? = null,
    @SerializedName("ui_state") val uiState: UIStatePayload
)

// --- Execution response models (backend -> client) ---

data class ExecutionCommand(
    @SerializedName("action") val action: ActionType,
    @SerializedName("target") val target: String? = null,
    @SerializedName("target_index") val targetIndex: Int? = null,
    @SerializedName("value") val value: String? = null,
    @SerializedName("direction") val direction: ScrollDirection? = null
)

data class ExecutionResponse(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("command") val command: ExecutionCommand? = null,
    @SerializedName("status") val status: ExecutionStatus,
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("step") val step: Int? = null
)
