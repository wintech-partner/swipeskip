package org.example.autoshorts

import android.content.Context
import org.example.autoshorts.billing.api.Entitlements
import org.example.autoshorts.billing.noop.NoopEntitlements

@Suppress("UNUSED_PARAMETER")
fun createEntitlements(context: Context): Entitlements = NoopEntitlements()
