package org.example.autoshorts

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.example.autoshorts.billing.api.Entitlements
import org.example.autoshorts.data.settings.SettingsRepository
import org.example.autoshorts.platform.youtube.YouTubeShortsAdapter
import org.example.autoshorts.service.SessionHost

const val FAKE_SHORTS_PACKAGE = "org.example.autoshorts.fakeshorts"

private val Context.settingsStore by preferencesDataStore(name = "settings")

class AutoshortsApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(app: Application) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** One flag per flavor decides which modes exist at all (docs/DECISIONS.md, ADR 2). */
    val policy = if (BuildConfig.AUTO_TIMER_ENABLED) SettingsRepository.BOTH_AUTO_DEFAULT else SettingsRepository.MANUAL_ONLY

    val settings = SettingsRepository(app.settingsStore, policy)
    val entitlements: Entitlements = createEntitlements(app)
    // Debug builds also accept the fake Shorts test app so the service can be exercised without YouTube.
    val adapter = YouTubeShortsAdapter(if (BuildConfig.DEBUG) setOf(FAKE_SHORTS_PACKAGE) else emptySet())
    val host = SessionHost(app, scope, settings, adapter)
}
