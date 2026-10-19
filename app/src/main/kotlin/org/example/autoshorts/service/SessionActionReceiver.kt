package org.example.autoshorts.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.example.autoshorts.AutoshortsApp

/** Handles the buttons on the ongoing session notification. */
class SessionActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val host = (context.applicationContext as AutoshortsApp).container.host
        when (intent.action) {
            ACTION_STOP -> host.stop()
            ACTION_SCROLL -> host.manualScroll()
            ACTION_PAUSE -> host.pause()
            ACTION_RESUME -> host.resume()
            ACTION_SKIP_AD -> host.skipAd()
        }
    }

    companion object {
        const val ACTION_STOP = "org.example.autoshorts.action.STOP"
        const val ACTION_SCROLL = "org.example.autoshorts.action.SCROLL"
        const val ACTION_PAUSE = "org.example.autoshorts.action.PAUSE"
        const val ACTION_RESUME = "org.example.autoshorts.action.RESUME"
        const val ACTION_SKIP_AD = "org.example.autoshorts.action.SKIP_AD"
    }
}
