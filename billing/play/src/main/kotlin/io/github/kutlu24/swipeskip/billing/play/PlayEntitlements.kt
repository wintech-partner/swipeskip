package io.github.kutlu24.swipeskip.billing.play

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import io.github.kutlu24.swipeskip.billing.api.Entitlements
import io.github.kutlu24.swipeskip.billing.api.RestoreResult
import kotlin.coroutines.resume

/**
 * Play Billing implementation. [premiumProductId] comes from build config and is blank during development,
 * which keeps the whole store disabled (see [isStoreAvailable]). Nothing here is hardcoded.
 *
 * Purchase flow UI is intentionally not implemented yet: it needs a Play Console product, which is a
 * human-only step (docs/OPEN_ITEMS.md). Restore works against whatever the account already owns.
 */
class PlayEntitlements(
    context: Context,
    private val premiumProductId: String,
) : Entitlements {

    private val premium = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> = premium
    override val isStoreAvailable: Boolean = premiumProductId.isNotBlank()

    private val client: BillingClient by lazy {
        BillingClient.newBuilder(context.applicationContext)
            .setListener(PurchasesUpdatedListener { _, _ -> })
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
    }

    override suspend fun restorePurchases(): RestoreResult {
        if (!isStoreAvailable) return RestoreResult.UNAVAILABLE
        if (!connect()) return RestoreResult.ERROR
        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        )
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return RestoreResult.ERROR
        val owned = result.purchasesList.any { purchase ->
            purchase.products.contains(premiumProductId) &&
                purchase.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED
        }
        premium.value = owned
        return if (owned) RestoreResult.RESTORED else RestoreResult.NOTHING_TO_RESTORE
    }

    private suspend fun connect(): Boolean {
        if (client.isReady) return true
        return suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (cont.isActive) cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() = Unit
            })
        }
    }
}
