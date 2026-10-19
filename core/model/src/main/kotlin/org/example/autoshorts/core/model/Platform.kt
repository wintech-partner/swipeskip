package org.example.autoshorts.core.model

/** Platforms the app knows about. v1 ships only [YOUTUBE_SHORTS]. */
enum class PlatformId { YOUTUBE_SHORTS }

enum class ScrollDirection { UP, DOWN }

/** How a session advances to the next item. See docs/DECISIONS.md (ADR 2). */
enum class AutomationMode { AUTO_TIMER, MANUAL_TAP }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class MotionSensitivity { LOW, MEDIUM, HIGH }
