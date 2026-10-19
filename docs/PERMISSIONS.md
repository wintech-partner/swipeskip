# Permissions

| Permission / capability | Why | When requested |
|---|---|---|
| Accessibility service (`BIND_ACCESSIBILITY_SERVICE`, `canPerformGestures`, `canRetrieveWindowContent`) | Swipe inside YouTube; read screen *structure* for ad/comments/feed detection | User turns it on in system settings after the consent screen |
| `POST_NOTIFICATIONS` | Ongoing session notification with Stop; optional "skip likely available" hint | Runtime, onboarding step 3 (Android 13+) |
| `VIBRATE` | Optional reminder buzz in manual-tap mode | Install-time (normal permission) |
| `<queries>` YouTube package | Show the installed YouTube version in diagnostics | Install-time |
| `INTERNET`, `BILLING`, `ACCESS_NETWORK_STATE` | **play flavor only**, added by Play Billing for the optional premium purchase | Install-time |

The `foss` flavor has no network permission. Backups are disabled (`allowBackup=false`, empty data-extraction rules).
