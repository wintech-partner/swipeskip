package io.github.wintech_partner.swipeskip.core.automation

import io.github.wintech_partner.swipeskip.core.model.ErrorReason
import io.github.wintech_partner.swipeskip.core.model.PauseCause
import io.github.wintech_partner.swipeskip.core.model.SessionState

/** State plus the set of pause causes currently holding. [state] is always derived from them. */
data class SessionModel(
    val state: SessionState = SessionState.Idle,
    val pauses: Set<PauseCause> = emptySet(),
)

sealed interface SessionEvent {
    data class StartRequested(val permissionGranted: Boolean, val targetInForeground: Boolean) : SessionEvent
    data object StopRequested : SessionEvent
    data object StopCompleted : SessionEvent
    data class ForegroundChanged(val targetInForeground: Boolean) : SessionEvent
    data class PermissionChanged(val granted: Boolean) : SessionEvent
    data class PauseAdded(val cause: PauseCause) : SessionEvent
    data class PauseRemoved(val cause: PauseCause) : SessionEvent
    data class Failed(val reason: ErrorReason) : SessionEvent
}

/** Pure transition function. No clocks, no coroutines, no side effects. */
object SessionReducer {

    fun reduce(model: SessionModel, event: SessionEvent): SessionModel {
        val state = model.state
        return when (event) {
            is SessionEvent.StartRequested -> when {
                state.isActive || state is SessionState.Starting || state is SessionState.Stopping -> model
                !event.permissionGranted -> SessionModel(SessionState.PermissionRequired)
                event.targetInForeground -> SessionModel(SessionState.Running)
                else -> SessionModel(SessionState.Starting)
            }

            SessionEvent.StopRequested -> when {
                state.isActive || state is SessionState.Starting -> SessionModel(SessionState.Stopping)
                state is SessionState.Error || state is SessionState.PermissionRequired -> SessionModel(SessionState.Idle)
                else -> model
            }

            SessionEvent.StopCompleted ->
                if (state is SessionState.Stopping) SessionModel(SessionState.Idle) else model

            is SessionEvent.ForegroundChanged ->
                if (state is SessionState.Starting && event.targetInForeground) {
                    SessionModel(SessionState.Running)
                } else {
                    model
                }

            is SessionEvent.PermissionChanged -> when {
                !event.granted && (state.isActive || state is SessionState.Starting || state is SessionState.Idle) ->
                    SessionModel(SessionState.PermissionRequired)
                event.granted && state is SessionState.PermissionRequired -> SessionModel(SessionState.Idle)
                else -> model
            }

            is SessionEvent.PauseAdded ->
                if (state.isActive) derive(model.pauses + event.cause) else model

            is SessionEvent.PauseRemoved ->
                if (state.isActive) derive(model.pauses - event.cause) else model

            is SessionEvent.Failed ->
                if (state.isActive || state is SessionState.Starting) {
                    SessionModel(SessionState.Error(event.reason))
                } else {
                    model
                }
        }
    }

    /** User pause outranks ad, ad outranks the interaction causes. */
    private fun derive(pauses: Set<PauseCause>): SessionModel {
        val state = when {
            pauses.isEmpty() -> SessionState.Running
            PauseCause.USER in pauses -> SessionState.PausedByUser
            PauseCause.AD in pauses -> SessionState.PausedByAd
            else -> SessionState.PausedByInteraction(
                listOf(PauseCause.TOUCH, PauseCause.COMMENTS, PauseCause.NOT_IN_FEED).first { it in pauses },
            )
        }
        return SessionModel(state, pauses)
    }
}
