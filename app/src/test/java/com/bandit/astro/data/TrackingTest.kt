package com.bandit.astro.data

import org.junit.Assert.*
import org.junit.Test

class TrackingTest {
    @Test fun `unknown totals can advance and known totals stop at the end`() {
        assertEquals(28, nextProgress(28, 28))
        assertEquals(29, nextProgress(28, 0))
        assertEquals(1, nextProgress(-2, 0))
        assertEquals(Int.MAX_VALUE, nextProgress(Int.MAX_VALUE, 0))
    }
    @Test fun `airing reminders exclude future old and watched episodes`() {
        val now = 1_800_000_000L
        assertTrue(episodeIsDue(now - 60, now, 8, 9))
        assertFalse(episodeIsDue(now + 60, now, 8, 9))
        assertFalse(episodeIsDue(now - 7 * 3600, now, 8, 9))
        assertFalse(episodeIsDue(now - 60, now, 9, 9))
    }
}
