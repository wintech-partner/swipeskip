package io.github.kutlu24.swipeskip

import android.content.Context
import io.github.kutlu24.swipeskip.billing.api.Entitlements
import io.github.kutlu24.swipeskip.billing.play.PlayEntitlements

fun createEntitlements(context: Context): Entitlements = PlayEntitlements(context, BuildConfig.PREMIUM_PRODUCT_ID)
