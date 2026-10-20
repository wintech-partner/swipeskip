package io.github.kutlu24.swipeskip

import android.content.Context
import io.github.kutlu24.swipeskip.billing.api.Entitlements
import io.github.kutlu24.swipeskip.billing.noop.NoopEntitlements

@Suppress("UNUSED_PARAMETER")
fun createEntitlements(context: Context): Entitlements = NoopEntitlements()
