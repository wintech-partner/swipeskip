package org.example.autoshorts.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"
    const val ABOUT = "about"
    const val PRIVACY = "privacy"
    const val PREMIUM = "premium"
}

@Composable
fun AppNav(vm: MainViewModel, state: UiState) {
    val nav = rememberNavController()
    val settings = state.settings ?: return // first DataStore read not back yet
    val start = if (settings.consentGiven) Routes.HOME else Routes.ONBOARDING
    NavHost(navController = nav, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(vm, state, onDone = {
                nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                vm, state,
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onHelp = { nav.navigate(Routes.DIAGNOSTICS) },
                onPremium = { nav.navigate(Routes.PREMIUM) },
                onSetup = { nav.navigate(Routes.ONBOARDING) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(vm, state, onBack = { nav.popBackStack() },
                onPrivacy = { nav.navigate(Routes.PRIVACY) }, onAbout = { nav.navigate(Routes.ABOUT) })
        }
        composable(Routes.DIAGNOSTICS) { DiagnosticsScreen(vm, state, onBack = { nav.popBackStack() }) }
        composable(Routes.ABOUT) { AboutScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.PRIVACY) { PrivacyScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.PREMIUM) { PremiumScreen(vm, onBack = { nav.popBackStack() }) }
    }
}
