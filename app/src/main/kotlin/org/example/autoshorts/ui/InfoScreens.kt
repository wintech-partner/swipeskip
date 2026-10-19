package org.example.autoshorts.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.example.autoshorts.BuildConfig
import org.example.autoshorts.R
import org.example.autoshorts.billing.api.RestoreResult
import org.example.autoshorts.diagnostics.DiagnosticsReport

@Composable
fun DiagnosticsScreen(vm: MainViewModel, state: UiState, onBack: () -> Unit) {
    val context = LocalContext.current
    val youtube = vm.youtubeVersion()
    val checks = listOf(
        Triple(R.string.diag_check_service, state.serviceConnected, R.string.diag_fix_service),
        Triple(R.string.diag_check_youtube, youtube != null, R.string.diag_fix_youtube),
        Triple(R.string.diag_check_foreground, state.observation.targetInForeground, R.string.diag_fix_foreground),
        Triple(R.string.diag_check_notifications, vm.notificationsAllowed(), R.string.diag_fix_notifications),
    )
    val report = DiagnosticsReport.build(vm.diagnostics(state))
    var copied by remember { mutableStateOf(false) }

    ScreenScaffold(stringResource(R.string.diag_title), onBack) {
        Text(stringResource(R.string.diag_why), style = MaterialTheme.typography.titleMedium)
        checks.forEach { (label, ok, fix) ->
            Text("${if (ok) "✓" else "✗"} ${stringResource(label)}: ${stringResource(if (ok) R.string.diag_ok else R.string.diag_problem)}")
            if (!ok) Text(stringResource(fix), style = MaterialTheme.typography.bodySmall)
        }
        Text(stringResource(R.string.diag_detection_note), style = MaterialTheme.typography.bodySmall)

        Text(stringResource(R.string.diag_report), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.diag_report_hint), style = MaterialTheme.typography.bodySmall)
        Text(report, style = MaterialTheme.typography.bodySmall)
        Button(
            onClick = {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("Autoshorts diagnostics", report))
                copied = true
            },
            Modifier.fillMaxWidth(),
        ) { Text(stringResource(if (copied) R.string.diag_copied else R.string.diag_copy)) }
        OutlinedButton(onClick = { context.openUrl(context.getString(R.string.url_issues)) }, Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.diag_report_problem))
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    ScreenScaffold(stringResource(R.string.about_title), onBack) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME))
        Text(stringResource(R.string.about_license))
        Text(stringResource(R.string.about_not_affiliated))
        Text(stringResource(R.string.about_ai))
        OutlinedButton(onClick = { context.openUrl(context.getString(R.string.url_source)) }, Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.about_source))
        }
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    ScreenScaffold(stringResource(R.string.privacy_title), onBack) {
        Text(stringResource(R.string.privacy_body))
    }
}

/** Only reachable when a store is configured (play flavor with a product id). */
@Composable
fun PremiumScreen(vm: MainViewModel, onBack: () -> Unit) {
    var result by remember { mutableStateOf<RestoreResult?>(null) }
    ScreenScaffold(stringResource(R.string.premium_title), onBack) {
        Text(stringResource(R.string.premium_body))
        OutlinedButton(onClick = { vm.restorePurchases { result = it } }, Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.premium_restore))
        }
        result?.let {
            Text(
                stringResource(
                    when (it) {
                        RestoreResult.RESTORED -> R.string.premium_restore_done
                        RestoreResult.NOTHING_TO_RESTORE -> R.string.premium_restore_none
                        else -> R.string.premium_restore_failed
                    },
                ),
            )
        }
    }
}
