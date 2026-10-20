package io.github.wintech_partner.swipeskip

import android.content.Context
import io.github.wintech_partner.swipeskip.billing.api.Entitlements
import io.github.wintech_partner.swipeskip.billing.play.PlayEntitlements

fun createEntitlements(context: Context): Entitlements = PlayEntitlements(context, BuildConfig.PREMIUM_PRODUCT_ID)
