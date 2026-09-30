package com.example.gridime

import android.os.Handler
import kotlin.math.abs
import kotlin.math.max

/**
 * Turns an analog stick into one clean direction, the way console menus do:
 * - a deliberate push to start (ENGAGE), a near-centre return to stop (RELEASE)
 * - a push must be clearly more along one axis than the other (DOMINANCE)
 * - the small bounce the other way when you let go is ignored (REBOUND_MS)
 */
class StickDirection {
    var dx = 0
        private set
    var dy = 0
        private set

    private var lastDx = 0
    private var lastDy = 0
    private var releasedAt = 0L

    /** Feeds one stick sample. Returns true when the direction changed. */
    fun update(x: Float, y: Float, now: Long): Boolean {
        val ax = abs(x)
        val ay = abs(y)
        val oldDx = dx
        val oldDy = dy

        if (dx == 0 && dy == 0) {
            if (max(ax, ay) >= ENGAGE) {
                val horizontal = ax >= ay * DOMINANCE
                val vertical = ay >= ax * DOMINANCE
                if (horizontal || vertical) {
                    val nx = if (horizontal) (if (x > 0) 1 else -1) else 0
                    val ny = if (vertical) (if (y > 0) 1 else -1) else 0
                    val bounce = now - releasedAt < REBOUND_MS && nx == -lastDx && ny == -lastDy
                    if (!bounce) {
                        dx = nx
                        dy = ny
                    }
                }
            }
        } else if (max(ax, ay) < RELEASE) {
            lastDx = dx
            lastDy = dy
            releasedAt = now
            dx = 0
            dy = 0
        } else {
            // Turn only on a clear push along the other axis
            val along = if (dx != 0) ax else ay
            val across = if (dx != 0) ay else ax
            if (across >= ENGAGE && across >= along * DOMINANCE) {
                if (dx != 0) {
                    dx = 0
                    dy = if (y > 0) 1 else -1
                } else {
                    dy = 0
                    dx = if (x > 0) 1 else -1
                }
            }
        }
        return dx != oldDx || dy != oldDy
    }

    fun reset() {
        dx = 0
        dy = 0
    }

    private companion object {
        const val ENGAGE = 0.5f
        const val RELEASE = 0.3f
        const val DOMINANCE = 1.2f
        const val REBOUND_MS = 150L
    }
}

/**
 * Runs an action once right away, then again and again while a button is held.
 * [intervalFor] gives the wait before each repeat, so repeats can speed up.
 */
class Repeater(private val handler: Handler) {

    private var action: ((Int) -> Unit)? = null
    private var intervalFor: (Int) -> Long = { 100L }
    private var count = 0

    private val tick = object : Runnable {
        override fun run() {
            val a = action ?: return
            count++
            a(count)
            handler.postDelayed(this, intervalFor(count))
        }
    }

    val isRunning: Boolean get() = action != null

    fun start(firstDelay: Long, intervalFor: (Int) -> Long, action: (Int) -> Unit) {
        stop()
        this.action = action
        this.intervalFor = intervalFor
        count = 0
        action(0)
        handler.postDelayed(tick, firstDelay)
    }

    fun stop() {
        handler.removeCallbacks(tick)
        action = null
    }
}
