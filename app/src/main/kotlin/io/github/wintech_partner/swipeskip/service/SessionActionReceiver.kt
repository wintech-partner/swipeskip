package io.github.wintech_partner.swipeskip.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.wintech_partner.swipeskip.AutoshortsApp

/** Handles the buttons on the ongoing session notification. */
class SessionActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val host = (context.applicationContext as AutoshortsApp).container.host
        when (intent.action) {
            ACTION_STOP -> host.stop()
            ACTION_SCROLL -> host.manualScroll()
            ACTION_PAUSE -> host.pause()
            ACTION_RESUME -> host.resume()
        }
    }

    companion object {
        const val ACTION_STOP = "io.github.wintech_partner.swipeskip.action.STOP"
        const val ACTION_SCROLL = "io.github.wintech_partner.swipeskip.action.SCROLL"
        const val ACTION_PAUSE = "io.github.wintech_partner.swipeskip.action.PAUSE"
        const val ACTION_RESUME = "io.github.wintech_partner.swipeskip.action.RESUME"
    }
}
