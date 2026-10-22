package com.kexu.app.widget

import com.kexu.app.data.model.CountdownEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RhythmWidgetsTest {
    @Test fun `yearly leap day recovers February 29 in leap year`() {
        val item=CountdownEntity(name="生日",targetDateEpochDay=LocalDate.of(2020,2,29).toEpochDay(),repeatYearly=true)
        assertEquals(365L,countdownDays(item,LocalDate.of(2023,3,1)))
        assertEquals(0L,countdownDays(item,LocalDate.of(2024,2,29)))
    }
    @Test fun `auto selection prefers upcoming dates but respects explicit past selection`() {
        val today=LocalDate.of(2026,9,10)
        val past=CountdownEntity(id=1,name="过去",targetDateEpochDay=today.minusDays(1).toEpochDay())
        val future=CountdownEntity(id=2,name="将来",targetDateEpochDay=today.plusDays(8).toEpochDay())
        assertEquals(future,selectCountdown(listOf(past,future),0,today))
        assertEquals(past,selectCountdown(listOf(past,future),1,today))
        assertEquals("已经走过",countdownLabel(-1))
        assertEquals("就是今天",countdownLabel(0))
        assertNull(selectCountdown(emptyList(),0,today))
    }
}
