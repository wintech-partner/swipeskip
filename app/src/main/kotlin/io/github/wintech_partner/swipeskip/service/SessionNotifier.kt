package io.github.wintech_partner.swipeskip.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.wintech_partner.swipeskip.R
import io.github.wintech_partner.swipeskip.core.automation.EngineStats
import io.github.wintech_partner.swipeskip.core.model.AdState
import io.github.wintech_partner.swipeskip.core.model.AutomationMode
import io.github.wintech_partner.swipeskip.core.model.SessionState
import io.github.wintech_partner.swipeskip.ui.MainActivity
import io.github.wintech_partner.swipeskip.ui.labelRes

/**
 * The ongoing notification is the always-visible Stop control. It is posted from the app process while a
 * session is live and removed the moment it ends. See docs/DECISIONS.md (ADR 4): no foreground service.
 */
class SessionNotifier(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    init {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SESSION, context.getString(R.string.notif_channel_session), NotificationManager.IMPORTANCE_LOW),
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ADS, context.getString(R.string.notif_channel_ads), NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    private fun canPost() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun action(action: String, titleRes: Int, requestCode: Int): NotificationCompat.Action {
        val intent = Intent(context, SessionActionReceiver::class.java).setAction(action)
        val pi = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action.Builder(0, context.getString(titleRes), pi).build()
    }

    @SuppressLint("MissingPermission") // canPost() checks POST_NOTIFICATIONS right before notify
    fun update(state: SessionState, stats: EngineStats, mode: AutomationMode, ad: AdState) {
        val live = state.isActive || state is SessionState.Starting
        if (!live) {
            manager.cancel(ID_SESSION)
            return
        }
        if (!canPost()) return
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(context, CHANNEL_SESSION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_title_session))
            .setContentText(context.getString(state.labelRes()))
            .setSubText(context.resources.getQuantityString(R.plurals.scroll_count, stats.scrollCount, stats.scrollCount))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(action(SessionActionReceiver.ACTION_STOP, R.string.notif_action_stop, 1))
        if (mode == AutomationMode.MANUAL_TAP && state is SessionState.Running) {
            builder.addAction(action(SessionActionReceiver.ACTION_SCROLL, R.string.notif_action_scroll, 2))
        }
        if (state is SessionState.PausedByUser) {
            builder.addAction(action(SessionActionReceiver.ACTION_RESUME, R.string.notif_action_resume, 3))
        } else if (state is SessionState.Running) {
            builder.addAction(action(SessionActionReceiver.ACTION_PAUSE, R.string.notif_action_pause, 4))
        }
        manager.notify(ID_SESSION, builder.build())
    }

    /** Tells the user a skip control is likely on screen. The app never taps it. */
    @SuppressLint("MissingPermission") // canPost() checks POST_NOTIFICATIONS right before notify
    fun updateAd(state: SessionState, ad: AdState) {
        if (!(state.isActive) || ad !is AdState.SkipLikelyAvailable) {
            manager.cancel(ID_AD)
            return
        }
        if (!canPost()) return
        val n = NotificationCompat.Builder(context, CHANNEL_ADS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_ad_skip_title))
            .setContentText(context.getString(R.string.notif_ad_skip_text))
            .setTimeoutAfter(15_000)
            .setOnlyAlertOnce(true)
            .build()
        manager.notify(ID_AD, n)
    }

    fun cancelAll() {
        manager.cancel(ID_SESSION)
        manager.cancel(ID_AD)
    }

    private companion object {
        const val CHANNEL_SESSION = "session"
        const val CHANNEL_ADS = "ads"
        const val ID_SESSION = 1
        const val ID_AD = 2
    }
}
