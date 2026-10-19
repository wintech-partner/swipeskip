package org.example.autoshorts.platform.youtube

/**
 * Every YouTube-specific matcher lives in this file, as data. When a YouTube update breaks
 * detection, this is the only file to edit.
 *
 * STATUS: none of these ids or labels has been verified against a real YouTube build yet
 * (no device was available when this was written). They are candidates. See docs/TESTED_VERSIONS.md
 * and docs/OPEN_ITEMS.md. Until verified, the adapter answers `Unknown` rather than guessing.
 */
object YouTubeMatchers {
    const val PACKAGE = "com.google.android.youtube"

    /** Resource ids are matched on their short name, after the "package:id/" prefix. */
    object Ids {
        /** Present only on the Shorts feed. */
        val feedMarkers = setOf("reel_recycler", "reel_player_page_container", "reel_watch_fragment_root")

        /** Present only on the comments panel while it is open. */
        val commentsPanelMarkers = setOf("engagement_panel_container", "comment_list", "comments_entry_point_header")

        /** Positive evidence of a non-Shorts screen (regular watch page, search results, home). */
        val otherScreenMarkers = setOf("watch_player", "results", "browse_results")

        /** Ad overlay structure inside the Shorts player. */
        val adMarkers = setOf("ad_badge", "ad_progress_text", "shorts_ad_overlay", "reel_ad_info")
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
