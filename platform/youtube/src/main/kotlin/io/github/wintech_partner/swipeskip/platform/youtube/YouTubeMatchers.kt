package io.github.wintech_partner.swipeskip.platform.youtube

/**
 * Every YouTube-specific matcher lives in this file, as data. When a YouTube update breaks
 * detection, this is the only file to edit.
 *
 * STATUS: the ids below were observed on a real phone (YouTube 21.41.525, Android 14, German UI, 2026-10-19,
 * see docs/YOUTUBE_OBSERVED_IDS.md and the fixtures in platform/youtube/src/test/resources). Verified there:
 * the Shorts feed, the open comments panel, the Home screen and two Shorts ads. NOT verified: other YouTube
 * versions, other UI languages (labels for non-German languages are from general knowledge), skippable ads
 * (no Skip button appeared in either Shorts ad), a regular full-screen watch page.
 */
object YouTubeMatchers {
    const val PACKAGE = "com.google.android.youtube"

    /** Resource ids are matched on their short name, after the "package:id/" prefix. */
    object Ids {
        /** Present on the Shorts feed (and nowhere on Home). Observed. */
        val feedMarkers = setOf("reel_recycler", "reel_player_page_container", "reel_watch_fragment_root")

        /** Present only while the comments panel is open. Observed; `reel_recycler` stays in the tree underneath. */
        val commentsPanelMarkers = setOf("app_engagement_panel", "engagement_panel_content", "panel_header")

        /**
         * Positive evidence of a non-Shorts screen (observed on Home). `results` also appears inside the comments
         * panel, so comments are checked first. `pivot_bar` exists on the Shorts tab too and must NOT be listed here.
         */
        val otherScreenMarkers = setOf("watch_player", "results", "filter_bar")

        /**
         * Evidence of an advertisement. The first two appeared on both observed Shorts ads and on none of ~28 normal
         * Shorts pages. The last three are ad-specific names observed on two regular-video ads (2026-10-20) and absent
         * from a regular video without an ad.
         */
        val adMarkers = setOf(
            "reel_player_delegated_overlay", "reel_progress_bar",
            "skip_ad_button", "ad_progress_text", "ad_details_container_layout",
        )

        /**
         * Subtrees inside which a Skip control may be clicked. On a regular video this is the Skip button itself
         * (`skip_ad_button` is the clickable FrameLayout; its child carries the "Werbung überspringen" description).
         */
        val skipContainers = setOf("reel_player_delegated_overlay", "skip_ad_button")

        /**
         * The Skip button itself, independent of the UI language. It appeared only once the ad was skippable (absent
         * from the real "ad before skip" and "no ad" dumps, present in the skippable one, 2026-10-20).
         */
        val skipButtons = setOf("skip_ad_button")
    }

    object Categories {
        const val SKIP_AD = "SKIP_AD"
        const val AD_BADGE = "AD_BADGE"
    }

    /**
     * Content-description labels by category and language. Lower-cased and compared after trimming.
     * Machine-drafted from general knowledge of YouTube's localisation; needs native verification.
     */
    val labels: Map<String, Set<String>> = mapOf(
        Categories.SKIP_AD to setOf(
            "skip ad", "skip ads", // en
            "reklamı atla", // tr
            "anzeige überspringen", "werbung überspringen", // de
            "omitir anuncio", // es
            "passer l'annonce", "ignorer l'annonce", // fr
            "pular anúncio", // pt-BR
            "пропустить рекламу", // ru
            "تخطي الإعلان", // ar
            "lewati iklan", // id
            // Fallback only: the language-independent id above is what normally finds the button. Unverified on a device.
            "विज्ञापन छोड़ें", // hi
            "広告をスキップ", // ja
            "광고 건너뛰기", // ko
            "bỏ qua quảng cáo", // vi
            "ข้ามโฆษณา", // th
            "i-skip ang ad", // fil
            "বিজ্ঞাপন এড়িয়ে যান", // bn
            "اشتہار چھوڑیں", // ur
        ),
        Categories.AD_BADGE to setOf(
            "ad", "sponsored", // en
            "reklam", // tr
            "anzeige", "werbung", // de
            "anuncio", // es
            "annonce", "publicité", // fr
            "anúncio", // pt-BR
            "реклама", // ru
            "إعلان", // ar
            "iklan", // id
            "विज्ञापन", // hi
            "広告", // ja
            "광고", // ko
            "quảng cáo", // vi
            "โฆษณา", // th
            "ad", // fil (same word as en)
            "বিজ্ঞাপন", // bn
            "اشتہار", // ur
        ),
    )

    /** Swipe geometry as fractions of the window. */
    const val SWIPE_X_FRACTION = 0.5f
    const val SWIPE_FROM_FRACTION = 0.75f
    const val SWIPE_TO_FRACTION = 0.25f
    const val SWIPE_DURATION_MS = 250L

    /** Smallest tree we trust enough to call a screen "other". */
    const val MIN_NODES_FOR_OTHER_SCREEN = 5
}
