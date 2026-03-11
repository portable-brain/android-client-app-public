package com.portablebrain.client.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.portablebrain.client.data.ActionType
import com.portablebrain.client.data.ExecutionCommand
import com.portablebrain.client.data.ScrollDirection
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PortableBrainAccessibilityService : AccessibilityService() {

    data class A11ySnapshot(
        val packageName: String,
        val activity: String,
        val formattedText: String,
        val isTappable: Boolean,
        val isTextField: Boolean,
        val focusedNodeId: String?
    )

    companion object {
        @Volatile
        var instance: PortableBrainAccessibilityService? = null
            private set
    }

    // Updated on TYPE_WINDOW_STATE_CHANGED events
    private var currentActivity: String = ""
    private var currentPackage: String = ""

    // Indexed node list from the last snapshot — used for action execution
    private var indexedNodes: List<AccessibilityNodeInfo> = emptyList()

    override fun onServiceConnected() {
        instance = this
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        // Track activity changes via window state events
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.toString()?.let { currentPackage = it }
            event.className?.toString()?.let { name ->
                if (name.contains("Activity") || name.contains("Fragment")) {
                    currentActivity = name
                }
            }
        }
    }

    override fun onInterrupt() {}

    /**
     * Captures the current UI state as an indexed snapshot.
     * Each visible node gets an index like: [0] [Button] Settings
     * The indexed node list is retained for subsequent action execution.
     */
    fun getCurrentSnapshot(): A11ySnapshot? {
        val root = rootInActiveWindow ?: return null

        val pkg = root.packageName?.toString()
            ?: currentPackage.ifEmpty { "unknown" }
        val activity = currentActivity.ifEmpty { pkg }

        val sb = StringBuilder()
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        var focusedNode: AccessibilityNodeInfo? = null
        var isTappable = false
        var isTextField = false
        var focusedNodeId: String? = null

        // Breadth-first traversal with indexing
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            // Track focused node
            if (node.isAccessibilityFocused || node.isFocused) {
                focusedNode = node
                isTappable = node.isClickable || node.actionList.any {
                    it.id == AccessibilityNodeInfo.ACTION_CLICK
                }
                isTextField = node.isEditable ||
                    node.className?.contains("EditText") == true
                focusedNodeId = buildString {
                    append(node.className ?: "")
                    append(":")
                    append(node.text ?: "")
                    append(":")
                    append(node.viewIdResourceName ?: "")
                }
            }

            // Collect visible text with node index
            val text = node.text?.toString()?.trim()
            val contentDesc = node.contentDescription?.toString()?.trim()
            val nodeClass = node.className?.toString()?.substringAfterLast('.') ?: ""

            val label = when {
                !text.isNullOrEmpty() -> text
                !contentDesc.isNullOrEmpty() -> contentDesc
                else -> null
            }

            if (label != null && label.length <= 500) {
                val idx = nodes.size
                nodes.add(node)
                sb.append("[$idx] [$nodeClass] $label\n")
            } else {
                // Still add interactive nodes without visible text (buttons with just an icon, etc.)
                if (node.isClickable || node.isLongClickable || node.isEditable || node.isScrollable) {
                    val idx = nodes.size
                    nodes.add(node)
                    sb.append("[$idx] [$nodeClass] (interactive)\n")
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        indexedNodes = nodes

        return A11ySnapshot(
            packageName = pkg,
            activity = activity,
            formattedText = sb.toString().trim(),
            isTappable = isTappable,
            isTextField = isTextField,
            focusedNodeId = focusedNodeId
        )
    }

    // --- Action Execution ---

    /**
     * Executes a command on the current UI. Returns true on success, false on failure.
     */
    suspend fun executeCommand(command: ExecutionCommand): Boolean {
        return when (command.action) {
            ActionType.CLICK -> {
                val node = findTargetNode(command) ?: return false
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            ActionType.LONG_CLICK -> {
                val node = findTargetNode(command) ?: return false
                node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
            }
            ActionType.SET_TEXT -> {
                val node = findTargetNode(command) ?: return false
                val args = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        command.value ?: ""
                    )
                }
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
            ActionType.SCROLL -> {
                val node = findScrollableNode(command)
                if (node != null) {
                    val scrollAction = when (command.direction) {
                        ScrollDirection.DOWN, ScrollDirection.RIGHT ->
                            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                        ScrollDirection.UP, ScrollDirection.LEFT ->
                            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                        null -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                    }
                    node.performAction(scrollAction)
                } else {
                    // Fallback: gesture-based scroll if no scrollable node found
                    performScrollGesture(command.direction ?: ScrollDirection.DOWN)
                }
            }
            ActionType.BACK -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            ActionType.HOME -> {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            ActionType.OPEN_APP -> {
                openApp(command.target)
            }
            ActionType.WAIT -> {
                delay(1000)
                true
            }
        }
    }

    /**
     * Finds a target node by index (preferred) or by text match (fallback).
     */
    private fun findTargetNode(command: ExecutionCommand): AccessibilityNodeInfo? {
        // Prefer target_index — it's precise
        command.targetIndex?.let { idx ->
            if (idx in indexedNodes.indices) return indexedNodes[idx]
        }

        // Fallback: text match against current indexed nodes
        val target = command.target ?: return null
        return indexedNodes.firstOrNull { node ->
            node.text?.toString()?.contains(target, ignoreCase = true) == true ||
                node.contentDescription?.toString()?.contains(target, ignoreCase = true) == true ||
                node.viewIdResourceName?.contains(target, ignoreCase = true) == true
        }
    }

    /**
     * Finds a scrollable node — either a specific target or the first scrollable container.
     */
    private fun findScrollableNode(command: ExecutionCommand): AccessibilityNodeInfo? {
        // If target specified, try to find that node
        if (command.target != null || command.targetIndex != null) {
            val node = findTargetNode(command)
            if (node?.isScrollable == true) return node
        }
        // Otherwise find the first scrollable node in the tree
        return indexedNodes.firstOrNull { it.isScrollable }
            ?: findFirstScrollableInTree()
    }

    private fun findFirstScrollableInTree(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isScrollable) return node
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    /**
     * Gesture-based scroll as fallback when no scrollable node is found.
     */
    private suspend fun performScrollGesture(direction: ScrollDirection): Boolean {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        val path = Path()
        when (direction) {
            ScrollDirection.DOWN -> {
                path.moveTo(width / 2f, height * 0.7f)
                path.lineTo(width / 2f, height * 0.3f)
            }
            ScrollDirection.UP -> {
                path.moveTo(width / 2f, height * 0.3f)
                path.lineTo(width / 2f, height * 0.7f)
            }
            ScrollDirection.LEFT -> {
                path.moveTo(width * 0.3f, height / 2f)
                path.lineTo(width * 0.7f, height / 2f)
            }
            ScrollDirection.RIGHT -> {
                path.moveTo(width * 0.7f, height / 2f)
                path.lineTo(width * 0.3f, height / 2f)
            }
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()

        return suspendCancellableCoroutine { cont ->
            val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (cont.isActive) cont.resume(true)
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (cont.isActive) cont.resume(false)
                }
            }, null)
            if (!dispatched && cont.isActive) cont.resume(false)
        }
    }

    /**
     * Opens an app by package name or app label using the launcher intent.
     */
    private fun openApp(target: String?): Boolean {
        target ?: return false
        val pm = applicationContext.packageManager

        // Try as package name first
        val launchIntent = pm.getLaunchIntentForPackage(target)
        if (launchIntent != null) {
            launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            applicationContext.startActivity(launchIntent)
            return true
        }

        // Fallback: search installed apps by label
        val apps = pm.getInstalledApplications(0)
        val match = apps.firstOrNull { app ->
            pm.getApplicationLabel(app).toString().equals(target, ignoreCase = true)
        }
        if (match != null) {
            val intent = pm.getLaunchIntentForPackage(match.packageName)
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                applicationContext.startActivity(intent)
                return true
            }
        }

        return false
    }
}
