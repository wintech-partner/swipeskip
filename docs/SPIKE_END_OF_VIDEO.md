# Spike: end-of-video detection

**Result: not implemented, not shipped.** Fixed intervals only (3-120 s, custom allowed).

Time-boxed investigation notes (desk research only, nothing was measurable without a device):
- Progress bar in the accessibility tree: YouTube may expose a seek/progress node for Shorts, but the id and whether it updates events without polling are unknown. Polling is not allowed by the design rules.
- Media session state: apps can read active media sessions only with a notification-listener permission, which is a heavier, more sensitive permission than this app should request. Rejected.
- Reviewer demand exists (fixed timers fit Shorts of different lengths poorly), so this stays worth a device-based retry: dump the hierarchy during playback, look for a progress node whose bounds/description change, and check event frequency.

Ship criteria if retried: demonstrably reliable across at least three YouTube versions, no extra sensitive permission, behind an "Experimental" setting.
