package org.example.autoshorts.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.example.autoshorts.AutoshortsApp
import org.example.autoshorts.BuildConfig
import org.example.autoshorts.billing.api.RestoreResult
import org.example.autoshorts.core.automation.EngineStats
import org.example.autoshorts.core.model.AdState
import org.example.autoshorts.core.model.AppSettings
import org.example.autoshorts.core.model.AutomationMode
import org.example.autoshorts.core.model.SessionState
import org.example.autoshorts.diagnostics.DiagnosticsInfo
import org.example.autoshorts.service.LiveObservation

data class UiState(
    val settings: AppSettings? = null,
    val session: SessionState = SessionState.Idle,
    val stats: EngineStats = EngineStats(),
    val observation: LiveObservation = LiveObservation(),
    val serviceConnected: Boolean = false,
    val allowedModes: Set<AutomationMode> = AutomationMode.entries.toSet(),
    val storeAvailable: Boolean = false,
    val adSkipAllowed: Boolean = false,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as AutoshortsApp).container
    private val host = container.host

    val state: StateFlow<UiState> = combine(
        container.settings.settings,
        host.engine.model,
        host.engine.stats,
        host.observation,
        host.serviceConnected,
    ) { settings, model, stats, obs, connected ->
        UiState(
            settings = settings,
            session = model.state,
            stats = stats,
            observation = obs,
            serviceConnected = connected,
            allowedModes = container.policy.allowedModes,
            storeAvailable = container.entitlements.isStoreAvailable,
            adSkipAllowed = container.policy.adSkipAllowed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    val premium: StateFlow<Boolean> = container.entitlements.isPremium

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { container.settings.update(transform) }
    }

    fun giveConsent() = update { it.copy(consentGiven = true) }

    fun start() = host.start()
    fun stop() = host.stop()
    fun pause() = host.pause()
    fun resume() = host.resume()
    fun manualScroll() = host.manualScroll()

    fun restorePurchases(onResult: (RestoreResult) -> Unit) {
        viewModelScope.launch { onResult(container.entitlements.restorePurchases()) }
    }

    fun notificationsAllowed(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun youtubeVersion(): String? = runCatching {
        @Suppress("DEPRECATION")
        getApplication<Application>().packageManager.getPackageInfo("com.google.android.youtube", 0).versionName
    }.getOrNull()

    fun diagnostics(s: UiState): DiagnosticsInfo {
        val adEvidence = when (val ad = s.observation.adState) {
            is AdState.AdLikely -> ad.evidence.signals
            is AdState.SkipLikelyAvailable -> ad.evidence.signals
            else -> emptyList()
        }
        val last = host.transitions.last()
        return DiagnosticsInfo(
            appVersion = BuildConfig.VERSION_NAME,
            flavor = BuildConfig.FLAVOR,
            androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
            youtubeVersion = youtubeVersion(),
            serviceEnabled = s.serviceConnected,
            notificationsAllowed = notificationsAllowed(),
            targetInForeground = s.observation.targetInForeground,
            mode = s.settings?.session?.mode?.name ?: "unknown",
            sessionState = s.session::class.simpleName ?: s.session.toString(),
            lastTransition = last?.let { "${it.state::class.simpleName} at ${it.atMillis}" },
            lastGestureResult = s.stats.lastGestureResult?.name,
            lastError = (s.session as? SessionState.Error)?.reason?.name,
            lastStopReason = s.stats.lastStopReason?.name,
            scrollCount = s.stats.scrollCount,
            consecutiveFailures = s.stats.consecutiveFailures,
            screenState = s.observation.screenState.name,
            adState = s.observation.adState::class.simpleName ?: "Unknown",
            adEvidence = adEvidence,
        )
    }
}
