package io.github.wintech_partner.swipeskip.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import io.github.wintech_partner.swipeskip.R
import io.github.wintech_partner.swipeskip.core.model.AutomationMode
import io.github.wintech_partner.swipeskip.core.model.MotionSensitivity
import io.github.wintech_partner.swipeskip.core.model.ScrollDirection
import io.github.wintech_partner.swipeskip.core.model.SessionState
import io.github.wintech_partner.swipeskip.core.model.SettingsLimits
import io.github.wintech_partner.swipeskip.core.model.ThemeMode

/** Language names are shown in their own language on purpose, so people can find theirs. */
private val LANGUAGES = listOf(
    "" to null, "en" to "English", "tr" to "Türkçe", "de" to "Deutsch", "es" to "Español",
    "fr" to "Français", "pt-BR" to "Português (Brasil)", "ru" to "Русский", "ar" to "العربية", "in" to "Indonesia",
    "hi" to "हिन्दी", "ja" to "日本語", "vi" to "Tiếng Việt", "ko" to "한국어", "th" to "ไทย",
    "fil" to "Filipino", "bn" to "বাংলা", "ur" to "اردو",
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, state: UiState, onBack: () -> Unit, onPrivacy: () -> Unit, onAbout: () -> Unit) {
    val settings = state.settings ?: return
    val s = settings.session

    ScreenScaffold(stringResource(R.string.settings_title), onBack) {
        // The engine reads session settings once, at Start, so changes made mid-session only count next time.
        if (state.session.isActive || state.session is SessionState.Starting) {
            Text(stringResource(R.string.settings_applies_next_session), color = MaterialTheme.colorScheme.error)
        }
        // Interval
        Text(stringResource(R.string.settings_interval), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsLimits.INTERVAL_PRESETS_SECONDS.forEach { v ->
                FilterChip(
                    selected = s.intervalSeconds == v,
                    onClick = { vm.update { it.copy(session = it.session.copy(intervalSeconds = v)) } },
                    label = { Text(stringResource(R.string.settings_interval_value, v)) },
                )
            }
        }
        var custom by remember(s.intervalSeconds) { mutableFloatStateOf(s.intervalSeconds.toFloat()) }
        Text(stringResource(R.string.settings_interval_custom, custom.toInt()))
        Slider(
            value = custom,
            onValueChange = { custom = it },
            onValueChangeFinished = { vm.update { it.copy(session = it.session.copy(intervalSeconds = custom.toInt())) } },
            valueRange = SettingsLimits.MIN_INTERVAL_SECONDS.toFloat()..SettingsLimits.MAX_INTERVAL_SECONDS.toFloat(),
        )
        HorizontalDivider()

        // Session limits
        Text(stringResource(R.string.settings_duration), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsLimits.DURATION_OPTIONS_MINUTES.forEach { v ->
                FilterChip(
                    selected = s.maxDurationMinutes == v,
                    onClick = { vm.update { it.copy(session = it.session.copy(maxDurationMinutes = v)) } },
                    label = { Text(stringResource(R.string.settings_duration_value, v)) },
                )
            }
        }
        SwitchRow(stringResource(R.string.settings_max_scrolls), s.maxScrolls != null) { on ->
            vm.update { it.copy(session = it.session.copy(maxScrolls = if (on) 100 else null)) }
        }
        s.maxScrolls?.let { limit ->
            var f by remember(limit) { mutableFloatStateOf(limit.toFloat()) }
            Text(stringResource(R.string.settings_max_scrolls_value, f.toInt()))
            Slider(
                value = f, onValueChange = { f = it },
                onValueChangeFinished = { vm.update { it.copy(session = it.session.copy(maxScrolls = f.toInt())) } },
                valueRange = 10f..1000f,
            )
        }
        HorizontalDivider()

        // Direction
        Text(stringResource(R.string.settings_direction), style = MaterialTheme.typography.titleMedium)
        val directions = listOf(ScrollDirection.UP to R.string.settings_direction_up, ScrollDirection.DOWN to R.string.settings_direction_down)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            directions.forEachIndexed { i, (d, label) ->
                SegmentedButton(
                    selected = s.direction == d,
                    onClick = { vm.update { it.copy(session = it.session.copy(direction = d)) } },
                    shape = SegmentedButtonDefaults.itemShape(i, directions.size),
                ) { Text(stringResource(label)) }
            }
        }

        // Mode: only the options the flavor allows
        if (state.allowedModes.size > 1) {
            Text(stringResource(R.string.settings_mode), style = MaterialTheme.typography.titleMedium)
            val modes = AutomationMode.entries.filter { it in state.allowedModes }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = s.mode == m,
                        onClick = { vm.update { it.copy(session = it.session.copy(mode = m)) } },
                        shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                    ) {
                        Text(stringResource(m.labelRes()))
                    }
                }
            }
        }
        if (s.mode == AutomationMode.MANUAL_TAP) Text(stringResource(R.string.settings_mode_manual_desc), style = MaterialTheme.typography.bodySmall)
        if (s.mode == AutomationMode.WATCH_ADS) Text(stringResource(R.string.settings_mode_watch_desc), style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()

        // Interaction
        SwitchRow(stringResource(R.string.settings_pause_on_touch), s.pauseOnTouch) { on ->
            vm.update { it.copy(session = it.session.copy(pauseOnTouch = on)) }
        }
        if (s.pauseOnTouch) {
            Text(stringResource(R.string.settings_resume_delay))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 3, 5, 10, 30).forEach { v ->
                    FilterChip(
                        selected = s.resumeDelaySeconds == v,
                        onClick = { vm.update { it.copy(session = it.session.copy(resumeDelaySeconds = v)) } },
                        label = { Text(if (v == 0) stringResource(R.string.settings_resume_ask) else stringResource(R.string.settings_resume_value, v)) },
                    )
                }
            }
        }
        SwitchRow(stringResource(R.string.settings_ad_notifications), settings.adNotifications, stringResource(R.string.settings_ad_notifications_desc)) { on ->
            vm.update { it.copy(adNotifications = on) }
        }
        SwitchRow(stringResource(R.string.settings_haptics), settings.haptics) { on -> vm.update { it.copy(haptics = on) } }
        SwitchRow(stringResource(R.string.settings_motion_next), settings.motionNext, stringResource(R.string.settings_motion_next_desc)) { on ->
            vm.update { it.copy(motionNext = on) }
        }
        if (settings.motionNext) {
            val levels = listOf(
                MotionSensitivity.LOW to R.string.settings_level_low,
                MotionSensitivity.MEDIUM to R.string.settings_level_medium,
                MotionSensitivity.HIGH to R.string.settings_level_high,
            )
            Text(stringResource(R.string.settings_motion_sensitivity))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                levels.forEachIndexed { i, (lvl, label) ->
                    SegmentedButton(
                        selected = settings.motionSensitivity == lvl,
                        onClick = { vm.update { it.copy(motionSensitivity = lvl) } },
                        shape = SegmentedButtonDefaults.itemShape(i, levels.size),
                    ) { Text(stringResource(label)) }
                }
            }
        }
        if (state.adSkipAllowed) {
            SwitchRow(stringResource(R.string.settings_auto_skip), settings.autoSkipAds, stringResource(R.string.settings_auto_skip_desc)) { on ->
                vm.update { it.copy(autoSkipAds = on) }
            }
        }
        HorizontalDivider()

        // Appearance
        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
        val themes = listOf(
            ThemeMode.SYSTEM to R.string.settings_theme_system,
            ThemeMode.LIGHT to R.string.settings_theme_light,
            ThemeMode.DARK to R.string.settings_theme_dark,
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            themes.forEachIndexed { i, (t, label) ->
                SegmentedButton(
                    selected = settings.theme == t,
                    onClick = { vm.update { it.copy(theme = t) } },
                    shape = SegmentedButtonDefaults.itemShape(i, themes.size),
                ) { Text(stringResource(label)) }
            }
        }

        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LANGUAGES.forEach { (tag, name) ->
                FilterChip(
                    selected = current == tag,
                    onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag)) },
                    label = { Text(name ?: stringResource(R.string.settings_language_system)) },
                )
            }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onPrivacy) { Text(stringResource(R.string.settings_privacy)) }
            TextButton(onClick = onAbout) { Text(stringResource(R.string.settings_about)) }
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, description: String? = null, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(title)
            if (description != null) Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
