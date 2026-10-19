package org.example.autoshorts.billing.api

import kotlinx.coroutines.flow.StateFlow

enum class RestoreResult { RESTORED, NOTHING_TO_RESTORE, UNAVAILABLE, ERROR }

/**
 * Premium is supporter-style extras only (themes, profiles, statistics, schedules).
 * Start/Stop, every interval, session limits, ad pausing, diagnostics and all languages are always free,
 * so nothing in the core path may ask this interface for permission.
 */
interface Entitlements {
    /** False when no store is configured: the UI then hides every premium and restore control. */
    val isStoreAvailable: Boolean

    val isPremium: StateFlow<Boolean>

    suspend fun restorePurchases(): RestoreResult
}
