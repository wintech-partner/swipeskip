package org.example.autoshorts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.example.autoshorts.R
import org.example.autoshorts.core.model.AdState
import org.example.autoshorts.core.model.AutomationMode
import org.example.autoshorts.core.model.SessionState

@Composable
fun HomeScreen(
    vm: MainViewModel,
    state: UiState,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onPremium: () -> Unit,
    onSetup: () -> Unit,
) {
    val settings = state.settings ?: return
    val session = state.session
    val ready = state.serviceConnected && settings.consentGiven
    val live = session.isActive || session is SessionState.Starting

    ScreenScaffold(stringResource(R.string.app_name), onBack = null) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(session.labelRes()), style = MaterialTheme.typography.headlineSmall)
                if (session is SessionState.Error) Text(stringResource(session.reason.labelRes()))
                if (session is SessionState.Idle) {
                    state.stats.lastStopReason?.let {
                        Text(stringResource(R.string.home_last_stop, stringResource(it.labelRes())))
                    }
                }
                if (live) {
                    Text(pluralStringResource(R.plurals.scroll_count, state.stats.scrollCount, state.stats.scrollCount))
                    Text(stringResource(state.observation.adState.labelRes()), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        val s = settings.session
        Text(
            buildString {
                append(stringResource(R.string.home_interval_summary, s.intervalSeconds))
                append(" · ")
                append(stringResource(R.string.home_limit_summary, s.maxDurationMinutes))
                s.maxScrolls?.let { append(" "); append(stringResource(R.string.home_limit_scrolls_summary, it)) }
                append(" · ")
                append(stringResource(if (s.mode == AutomationMode.AUTO_TIMER) R.string.home_mode_auto else R.string.home_mode_manual))
            },
            style = MaterialTheme.typography.bodyMedium,
        )

        if (!ready) {
            Text(stringResource(R.string.home_needs_setup))
            Button(onClick = onSetup, Modifier.fillMaxWidth().height(56.dp)) { Text(stringResource(R.string.home_continue_setup)) }
        } else if (!live) {
            Button(onClick = vm::start, Modifier.fillMaxWidth().height(72.dp)) {
                Text(stringResource(R.string.home_start), style = MaterialTheme.typography.titleLarge)
            }
        } else {
            Button(onClick = vm::stop, Modifier.fillMaxWidth().height(72.dp)) {
                Text(stringResource(R.string.home_stop), style = MaterialTheme.typography.titleLarge)
            }
            if (s.mode == AutomationMode.MANUAL_TAP && session is SessionState.Running) {
                Button(onClick = vm::manualScroll, Modifier.fillMaxWidth().height(56.dp)) {
                    Text(stringResource(R.string.home_scroll_next))
                }
            }
            if (session is SessionState.PausedByUser) {
                OutlinedButton(onClick = vm::resume, Modifier.fillMaxWidth()) { Text(stringResource(R.string.home_resume)) }
            } else if (session is SessionState.PausedByInteraction) {
                OutlinedButton(onClick = vm::resume, Modifier.fillMaxWidth()) { Text(stringResource(R.string.home_resume)) }
            } else if (session is SessionState.Running) {
                OutlinedButton(onClick = vm::pause, Modifier.fillMaxWidth()) { Text(stringResource(R.string.home_pause)) }
            }
        }

        if (state.adSkipAllowed && live && state.observation.adState is AdState.SkipLikelyAvailable) {
            OutlinedButton(onClick = vm::skipAd, Modifier.fillMaxWidth()) { Text(stringResource(R.string.home_skip_ad)) }
        }

        if (s.mode == AutomationMode.MANUAL_TAP) Text(stringResource(R.string.home_manual_hint), style = MaterialTheme.typography.bodySmall)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onSettings) { Text(stringResource(R.string.home_open_settings)) }
            TextButton(onClick = onHelp) { Text(stringResource(R.string.home_open_help)) }
            if (state.storeAvailable) TextButton(onClick = onPremium) { Text(stringResource(R.string.home_open_premium)) }
        }
    }
}
