package org.example.autoshorts.billing.noop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.example.autoshorts.billing.api.Entitlements
import org.example.autoshorts.billing.api.RestoreResult

/** foss flavor: everything unlocked, no network, no billing code. */
class NoopEntitlements : Entitlements {
    override val isStoreAvailable = false
    override val isPremium: StateFlow<Boolean> = MutableStateFlow(true)
    override suspend fun restorePurchases() = RestoreResult.UNAVAILABLE
}
