package org.example.autoshorts.diagnostics

/**
 * Everything that goes into the copyable report. Every field is a label, number or enum name:
 * there is deliberately no field that could carry screen text.
 */
data class DiagnosticsInfo(
    val appVersion: String,
    val flavor: String,
    val androidVersion: String,
    val device: String,
    val youtubeVersion: String?,
    val serviceEnabled: Boolean,
    val notificationsAllowed: Boolean,
    val targetInForeground: Boolean,
    val mode: String,
    val sessionState: String,
    val lastTransition: String?,
    val lastGestureResult: String?,
    val lastError: String?,
    val lastStopReason: String?,
    val scrollCount: Int,
    val consecutiveFailures: Int,
    val screenState: String,
    val adState: String,
    val adEvidence: List<String>,
)

object DiagnosticsReport {
    fun build(i: DiagnosticsInfo): String = buildString {
        appendLine("SwipeSkip diagnostics (no screen content)")
        appendLine("App: ${i.appVersion} (${i.flavor})")
        appendLine("Android: ${i.androidVersion}")
        appendLine("Device: ${i.device}")
        appendLine("YouTube: ${i.youtubeVersion ?: "not installed"}")
        appendLine("Accessibility service on: ${i.serviceEnabled}")
        appendLine("Notifications allowed: ${i.notificationsAllowed}")
        appendLine("YouTube in front: ${i.targetInForeground}")
        appendLine("Mode: ${i.mode}")
        appendLine("Session state: ${i.sessionState}")
        appendLine("Last transition: ${i.lastTransition ?: "none"}")
        appendLine("Last gesture result: ${i.lastGestureResult ?: "none"}")
        appendLine("Last error: ${i.lastError ?: "none"}")
        appendLine("Last stop reason: ${i.lastStopReason ?: "none"}")
        appendLine("Scrolls this session: ${i.scrollCount}")
        appendLine("Consecutive gesture failures: ${i.consecutiveFailures}")
        appendLine("Screen classification: ${i.screenState}")
        appendLine("Ad state: ${i.adState}")
        append("Ad evidence: ${if (i.adEvidence.isEmpty()) "none" else i.adEvidence.joinToString()}")
    }
}
