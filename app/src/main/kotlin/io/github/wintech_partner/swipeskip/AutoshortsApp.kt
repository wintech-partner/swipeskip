package io.github.wintech_partner.swipeskip

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import io.github.wintech_partner.swipeskip.billing.api.Entitlements
import io.github.wintech_partner.swipeskip.data.settings.SettingsRepository
import io.github.wintech_partner.swipeskip.platform.youtube.YouTubeShortsAdapter
import io.github.wintech_partner.swipeskip.service.SessionHost

const val FAKE_SHORTS_PACKAGE = "io.github.wintech_partner.swipeskip.fakeshorts"

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
    val host = SessionHost(app, scope, settings, adapter, policy.adSkipAllowed)
}
