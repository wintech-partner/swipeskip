package io.github.wintech_partner.swipeskip.core.automation

import io.github.wintech_partner.swipeskip.core.model.ErrorReason
import io.github.wintech_partner.swipeskip.core.model.PauseCause
import io.github.wintech_partner.swipeskip.core.model.SessionState
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionReducerTest {
    private fun reduce(m: SessionModel, vararg e: SessionEvent) = e.fold(m) { acc, ev -> SessionReducer.reduce(acc, ev) }
    private val running = SessionModel(SessionState.Running)

    @Test fun `start without permission requires permission`() {
        val m = reduce(SessionModel(), SessionEvent.StartRequested(permissionGranted = false, targetInForeground = true))
        assertEquals(SessionState.PermissionRequired, m.state)
    }

    @Test fun `start with target in foreground runs`() {
        val m = reduce(SessionModel(), SessionEvent.StartRequested(true, true))
        assertEquals(SessionState.Running, m.state)
    }

    @Test fun `start without target in foreground waits in Starting then runs`() {
        val starting = reduce(SessionModel(), SessionEvent.StartRequested(true, false))
        assertEquals(SessionState.Starting, starting.state)
        assertEquals(SessionState.Running, reduce(starting, SessionEvent.ForegroundChanged(true)).state)
    }

    @Test fun `start while running is ignored`() {
        assertEquals(running, reduce(running, SessionEvent.StartRequested(true, true)))
    }

    @Test fun `stop goes through Stopping to Idle`() {
        val stopping = reduce(running, SessionEvent.StopRequested)
        assertEquals(SessionState.Stopping, stopping.state)
        assertEquals(SessionState.Idle, reduce(stopping, SessionEvent.StopCompleted).state)
    }

    @Test fun `stop from Error resets to Idle`() {
        val m = reduce(SessionModel(SessionState.Error(ErrorReason.WATCHDOG)), SessionEvent.StopRequested)
        assertEquals(SessionState.Idle, m.state)
    }

    @Test fun `user pause outranks ad which outranks touch`() {
        var m = reduce(running, SessionEvent.PauseAdded(PauseCause.TOUCH))
        assertEquals(SessionState.PausedByInteraction(PauseCause.TOUCH), m.state)
        m = reduce(m, SessionEvent.PauseAdded(PauseCause.AD))
        assertEquals(SessionState.PausedByAd, m.state)
        m = reduce(m, SessionEvent.PauseAdded(PauseCause.USER))
        assertEquals(SessionState.PausedByUser, m.state)
    }

    @Test fun `running returns only when every cause is cleared`() {
        var m = reduce(running, SessionEvent.PauseAdded(PauseCause.AD), SessionEvent.PauseAdded(PauseCause.COMMENTS))
        m = reduce(m, SessionEvent.PauseRemoved(PauseCause.AD))
        assertEquals(SessionState.PausedByInteraction(PauseCause.COMMENTS), m.state)
        m = reduce(m, SessionEvent.PauseRemoved(PauseCause.COMMENTS))
        assertEquals(SessionState.Running, m.state)
    }

    @Test fun `pauses are ignored when no session is active`() {
        assertEquals(SessionModel(), reduce(SessionModel(), SessionEvent.PauseAdded(PauseCause.AD)))
    }

    @Test fun `permission revoked during a session moves to PermissionRequired`() {
        val paused = reduce(running, SessionEvent.PauseAdded(PauseCause.AD))
        val m = reduce(paused, SessionEvent.PermissionChanged(false))
        assertEquals(SessionModel(SessionState.PermissionRequired), m)
    }

    @Test fun `permission granted leaves PermissionRequired for Idle`() {
        val m = reduce(SessionModel(SessionState.PermissionRequired), SessionEvent.PermissionChanged(true))
        assertEquals(SessionState.Idle, m.state)
    }

    @Test fun `failure moves an active session to Error and clears pauses`() {
        val m = reduce(running, SessionEvent.PauseAdded(PauseCause.AD), SessionEvent.Failed(ErrorReason.GESTURE_FAILURES))
        assertEquals(SessionModel(SessionState.Error(ErrorReason.GESTURE_FAILURES)), m)
    }

    @Test fun `failure while idle is ignored`() {
        assertEquals(SessionModel(), reduce(SessionModel(), SessionEvent.Failed(ErrorReason.INTERNAL)))
    }

    @Test fun `restart from Error is allowed`() {
        val m = reduce(SessionModel(SessionState.Error(ErrorReason.WATCHDOG)), SessionEvent.StartRequested(true, true))
        assertEquals(SessionState.Running, m.state)
    }
}
