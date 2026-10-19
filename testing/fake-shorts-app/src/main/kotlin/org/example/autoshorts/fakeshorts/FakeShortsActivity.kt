package org.example.autoshorts.fakeshorts

import android.app.Activity
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Test double for a Shorts feed. Swiping up moves to the next page and bumps the counter.
 * "Toggle ad" shows an ad badge and a Skip button. The Skip button only counts clicks, so a test can prove
 * that Autoshorts never taps it. Nothing here talks to a network.
 */
class FakeShortsActivity : Activity() {
    private var page = 1
    private var swipeCounted = false
    private var skipClicks = 0
    private lateinit var counter: TextView
    private lateinit var skipCounter: TextView
    private lateinit var adBadge: TextView
    private lateinit var skip: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this).apply { id = R.id.reel_recycler }

        counter = TextView(this).apply {
            id = R.id.page_counter
            textSize = 32f
            gravity = Gravity.CENTER
            text = "Page 1"
        }
        root.addView(counter, FrameLayout.LayoutParams(-1, -1))

        val overlay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.TOP }
        adBadge = TextView(this).apply {
            id = R.id.ad_badge
            text = "Ad"
            contentDescription = "Ad"
            visibility = View.GONE
        }
        skip = Button(this).apply {
            id = R.id.skip_button
            text = "Skip ad"
            contentDescription = "Skip ad"
            visibility = View.GONE
            setOnClickListener { skipClicks++; skipCounter.text = "Skip clicks: $skipClicks" }
        }
        skipCounter = TextView(this).apply { id = R.id.skip_clicks; text = "Skip clicks: 0" }
        val toggle = Button(this).apply {
            id = R.id.toggle_ad
            text = "Toggle ad"
            setOnClickListener {
                val show = adBadge.visibility != View.VISIBLE
                adBadge.visibility = if (show) View.VISIBLE else View.GONE
                skip.visibility = if (show) View.VISIBLE else View.GONE
            }
        }
        overlay.addView(adBadge); overlay.addView(skip); overlay.addView(skipCounter); overlay.addView(toggle)
        root.addView(overlay, FrameLayout.LayoutParams(-1, -2))

        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                swipeCounted = false
                return true
            }
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
                val start = e1 ?: return false
                if (!swipeCounted && start.y - e2.y > 200) { // finger moved up: next page, once per swipe
                    swipeCounted = true
                    page++
                    counter.text = "Page $page"
                }
                return true
            }
        })
        root.setOnTouchListener { _, ev -> detector.onTouchEvent(ev) }
        setContentView(root)
    }
}
