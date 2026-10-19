package org.example.autoshorts

import android.content.Context
import org.example.autoshorts.billing.api.Entitlements
import org.example.autoshorts.billing.play.PlayEntitlements

fun createEntitlements(context: Context): Entitlements = PlayEntitlements(context, BuildConfig.PREMIUM_PRODUCT_ID)
