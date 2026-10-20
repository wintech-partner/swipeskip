package io.github.wintech_partner.swipeskip.billing.noop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import io.github.wintech_partner.swipeskip.billing.api.Entitlements
import io.github.wintech_partner.swipeskip.billing.api.RestoreResult

/** foss flavor: everything unlocked, no network, no billing code. */
class NoopEntitlements : Entitlements {
    override val isStoreAvailable = false
    override val isPremium: StateFlow<Boolean> = MutableStateFlow(true)
    override suspend fun restorePurchases() = RestoreResult.UNAVAILABLE
}
