package io.github.kutlu24.swipeskip.diagnostics

import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.kutlu24.swipeskip.core.model.SessionState

class DiagnosticsReportTest {
    private val info = DiagnosticsInfo(
        appVersion = "0.1.0", flavor = "foss", androidVersion = "15 (API 35)", device = "Google Pixel",
        youtubeVersion = "20.01.35", serviceEnabled = true, notificationsAllowed = false, targetInForeground = true,
        mode = "AUTO_TIMER", sessionState = "Running", lastTransition = "Running at 1", lastGestureResult = "COMPLETED",
        lastError = null, lastStopReason = "USER", scrollCount = 4, consecutiveFailures = 0,
        screenState = "InTargetFeed", adState = "NoAdEvidence", adEvidence = emptyList(),
    )

    @Test fun `report lists the facts support needs`() {
        val text = DiagnosticsReport.build(info)
        listOf("0.1.0 (foss)", "API 35", "YouTube: 20.01.35", "service on: true", "Last gesture result: COMPLETED", "Screen classification: InTargetFeed")
            .forEach { assertTrue(it, text.contains(it)) }
    }

    @Test fun `missing values are spelled out`() {
        val text = DiagnosticsReport.build(info.copy(youtubeVersion = null, lastGestureResult = null, lastError = null))
        assertTrue(text.contains("YouTube: not installed"))
        assertTrue(text.contains("Last gesture result: none"))
    }

    @Test fun `report only contains fields of the info class`() {
        // Guards the privacy rule: if someone adds a free-text field it must show up in this list on purpose.
        val fields = DiagnosticsInfo::class.java.declaredFields.map { it.name }.filterNot { it.contains("$") }.toSet()
        val allowed = setOf(
            "appVersion", "flavor", "androidVersion", "device", "youtubeVersion", "serviceEnabled", "notificationsAllowed",
            "targetInForeground", "mode", "sessionState", "lastTransition", "lastGestureResult", "lastError", "lastStopReason",
            "scrollCount", "consecutiveFailures", "screenState", "adState", "adEvidence",
        )
        assertTrue("unexpected fields: ${fields - allowed}", (fields - allowed).isEmpty())
    }

    @Test fun `transition log keeps a bounded history and skips repeats`() {
        val log = TransitionLog(capacity = 3)
        log.record(SessionState.Idle, 1)
        log.record(SessionState.Idle, 2)
        log.record(SessionState.Running, 3)
        log.record(SessionState.PausedByAd, 4)
        log.record(SessionState.Running, 5)
        assertTrue(log.all().size == 3)
        assertTrue(log.last()!!.state == SessionState.Running)
    }
}
