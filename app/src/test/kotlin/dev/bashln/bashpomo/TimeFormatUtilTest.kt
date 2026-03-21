package dev.bashln.bashpomo

import dev.bashln.bashpomo.util.TimeFormatUtil
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatUtilTest {

    @Test
    fun `formatMMSS handles zero`() {
        assertEquals("0:00", TimeFormatUtil.formatMMSS(0))
    }

    @Test
    fun `formatMMSS formats 3661 as 61 minutes 01 second`() {
        assertEquals("61:01", TimeFormatUtil.formatMMSS(3661))
    }

    @Test
    fun `formatMMSS formats 1500 as 25 minutes`() {
        assertEquals("25:00", TimeFormatUtil.formatMMSS(1500))
    }

    @Test
    fun `formatMMSS uses absolute value for negative input`() {
        assertEquals("1:30", TimeFormatUtil.formatMMSS(-90))
    }
}
