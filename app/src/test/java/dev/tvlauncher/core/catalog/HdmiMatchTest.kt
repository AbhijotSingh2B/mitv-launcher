package dev.tvlauncher.core.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HdmiMatchTest {
    @Test
    fun matchesByLabelIgnoringSpaceAndCase() {
        val inputs = listOf(HdmiInput("b", "HDMI 2"), HdmiInput("a", "hdmi1"))
        assertEquals("a", matchHdmiInput(1, inputs)?.id)
    }

    @Test
    fun fallsBackToNthSortedById() {
        val inputs = listOf(HdmiInput("x2", "Game"), HdmiInput("x1", "Box"))
        assertEquals("x2", matchHdmiInput(2, inputs)?.id)
    }

    @Test
    fun returnsNullWhenPortOutOfRange() {
        val inputs = listOf(HdmiInput("x2", "Game"), HdmiInput("x1", "Box"))
        assertNull(matchHdmiInput(3, inputs))
    }

    @Test
    fun returnsNullForEmptyList() {
        assertNull(matchHdmiInput(1, emptyList()))
    }
}
