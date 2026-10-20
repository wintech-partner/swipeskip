package io.github.kutlu24.swipeskip.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.kutlu24.swipeskip.R

/**
 * Step 1 is the dedicated disclosure with an explicit consent button (not buried in a policy).
 * Steps 2 and 3 only appear after consent, and nothing sends the user to system settings before it.
 */
@Composable
fun OnboardingScreen(vm: MainViewModel, state: UiState, onDone: () -> Unit) {
    val context = LocalContext.current
    val consent = state.settings?.consentGiven == true
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    ScreenScaffold(stringResource(R.string.onb_title), onBack = null) {
        Text(stringResource(R.string.onb_intro))
        Section(R.string.onb_does_title, R.string.onb_does)
        Section(R.string.onb_sees_title, R.string.onb_sees)
        Section(R.string.onb_never_title, R.string.onb_never)
        Section(R.string.onb_control_title, R.string.onb_control)

        if (!consent) {
            Button(onClick = vm::giveConsent, Modifier.fillMaxWidth()) { Text(stringResource(R.string.onb_consent)) }
        } else {
            HorizontalDivider()
            Text(stringResource(R.string.onb_step_enable), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.onb_step_enable_body))
            Text(stringResource(if (state.serviceConnected) R.string.onb_service_on else R.string.onb_service_off))
            Button(onClick = { context.openAccessibilitySettings() }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onb_open_accessibility))
            }
            Text(stringResource(R.string.onb_restricted_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.onb_restricted_body), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { context.openAppInfo() }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onb_open_app_info))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                HorizontalDivider()
                Text(stringResource(R.string.onb_step_notifications), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.onb_step_notifications_body))
                OutlinedButton(onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }, Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.onb_allow_notifications))
                }
            }
            Button(onClick = onDone, Modifier.fillMaxWidth()) { Text(stringResource(R.string.onb_done)) }
        }
    }
}

@Composable
private fun Section(title: Int, body: Int) {
    androidx.compose.foundation.layout.Column {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(body))
    }
}
