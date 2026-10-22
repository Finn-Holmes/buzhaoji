package com.kexu.app.widget

import com.kexu.app.settings.FocusTimerSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PomodoroRemoteWidgetTest {
    @Test
    fun countdownUsesWallClockWhileRunning() {
        val timer = FocusTimerSettings(
            durationMinutes = 25,
            remainingSeconds = 1_500,
            running = true,
            anchorAtMillis = 10_000L,
        )

        assertEquals(
            1_488,
            PomodoroRemoteWidgetReceiver.displayedSeconds(timer, nowMillis = 22_900L),
        )
    }

    @Test
    fun pausingPersistsDisplayedCountdown() {
        val timer = FocusTimerSettings(
            durationMinutes = 25,
            remainingSeconds = 1_500,
            running = true,
            startedAtMillis = 10_000L,
            anchorAtMillis = 10_000L,
        )

        val paused = PomodoroRemoteWidgetReceiver.toggledTimer(timer, nowMillis = 25_500L)

        assertFalse(paused.running)
        assertEquals(1_485, paused.remainingSeconds)
        assertEquals(0L, paused.anchorAtMillis)
    }

    @Test
    fun restartingFinishedCountdownRestoresConfiguredDuration() {
        val timer = FocusTimerSettings(
            durationMinutes = 45,
            remainingSeconds = 0,
            running = false,
        )

        val started = PomodoroRemoteWidgetReceiver.toggledTimer(timer, nowMillis = 42_000L)

        assertTrue(started.running)
        assertEquals(45 * 60, started.remainingSeconds)
        assertEquals(42_000L, started.anchorAtMillis)
    }

    @Test
    fun positiveTimerContinuesFromStoredElapsedTime() {
        val timer = FocusTimerSettings(
            positiveMode = true,
            elapsedSeconds = 30,
            running = true,
            anchorAtMillis = 5_000L,
        )

        assertEquals(
            39,
            PomodoroRemoteWidgetReceiver.displayedSeconds(timer, nowMillis = 14_900L),
        )
    }
}
