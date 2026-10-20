package io.github.wintech_partner.swipeskip.core.model

/** What the adapter concluded about the foreground screen. [Unknown] means "no evidence either way". */
enum class ScreenState { InTargetFeed, OtherScreen, CommentsOpen, Unknown }

enum class Confidence { LOW, MEDIUM, HIGH }

/**
 * Which structural signals matched, for display and diagnostics. Never contains screen text:
 * only matcher ids (view id, class name, content-description category).
 */
data class Evidence(
    val signals: List<String>,
    val confidence: Confidence,
)

/**
 * Wording rule: [AdLikely] and [SkipLikelyAvailable] are "likely", never "detected".
 * A skip signal implies an ad is present; the UI shows both facts.
 */
sealed interface AdState {
    data object Unknown : AdState
    data object NoAdEvidence : AdState
    data class AdLikely(val evidence: Evidence) : AdState
    data class SkipLikelyAvailable(val evidence: Evidence) : AdState

    val isAdPresent: Boolean get() = this is AdLikely || this is SkipLikelyAvailable
}
