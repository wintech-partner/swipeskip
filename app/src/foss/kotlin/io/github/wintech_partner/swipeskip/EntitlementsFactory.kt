package io.github.wintech_partner.swipeskip

import android.content.Context
import io.github.wintech_partner.swipeskip.billing.api.Entitlements
import io.github.wintech_partner.swipeskip.billing.noop.NoopEntitlements

@Suppress("UNUSED_PARAMETER")
fun createEntitlements(context: Context): Entitlements = NoopEntitlements()
