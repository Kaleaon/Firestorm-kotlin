package com.firestorm.llcommon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class LLDateTest {

    @Test
    fun testNullability() {
        val nullDate = LLDate.NULL
        assertTrue(nullDate.isNull())
        assertFalse(nullDate.notNull())

        val validDate = LLDate(100.0)
        assertFalse(validDate.isNull())
        assertTrue(validDate.notNull())
    }

    @Test
    fun testToISOString() {
        // Epoch 0
        val epoch = LLDate(0.0)
        assertEquals("1970-01-01T00:00:00Z", epoch.toISOString())

        // Example: 2023-10-01T12:00:00Z -> epoch 1696161600.0
        val d1 = LLDate(1696161600.0)
        assertEquals("2023-10-01T12:00:00Z", d1.toISOString())

        // With subseconds: 0.15s -> 150ms -> 15 centiseconds
        val d2 = LLDate(1696161600.15)
        assertEquals("2023-10-01T12:00:00.15Z", d2.toISOString())
    }

    @Test
    fun testFromISOString() {
        val d1 = LLDate.fromISOString("1970-01-01T00:00:00Z")
        assertNotNull(d1)
        assertEquals(0.0, d1.secondsSinceEpoch)

        val d2 = LLDate.fromISOString("2023-10-01T12:00:00Z")
        assertNotNull(d2)
        assertEquals(1696161600.0, d2.secondsSinceEpoch)

        // Subseconds
        val d3 = LLDate.fromISOString("2023-10-01T12:00:00.15Z")
        assertNotNull(d3)
        assertEquals(1696161600.15, d3.secondsSinceEpoch)

        // Without Z
        val d4 = LLDate.fromISOString("2023-10-01T12:00:00")
        assertNotNull(d4)
        assertEquals(1696161600.0, d4.secondsSinceEpoch)

        // Invalid
        assertNull(LLDate.fromISOString("invalid date"))
    }

    @Test
    fun testArithmeticOperations() {
        val d1 = LLDate(100.0)
        val d2 = LLDate(50.0)

        // Plus LLDate
        assertEquals(150.0, (d1 + d2).secondsSinceEpoch)

        // Minus LLDate
        assertEquals(50.0, (d1 - d2).secondsSinceEpoch)

        // Plus Double
        assertEquals(120.0, (d1 + 20.0).secondsSinceEpoch)

        // Minus Double
        assertEquals(80.0, (d1 - 20.0).secondsSinceEpoch)
    }

    @Test
    fun testCompareTo() {
        val d1 = LLDate(100.0)
        val d2 = LLDate(50.0)
        val d3 = LLDate(100.0)

        assertTrue(d1 > d2)
        assertTrue(d2 < d1)
        assertEquals(0, d1.compareTo(d3))
    }

    @Test
    fun testNowAndFromSeconds() {
        val now = LLDate.now()
        assertTrue(now.notNull())
        assertTrue(now.secondsSinceEpoch > 0.0)

        val d1 = LLDate.fromSeconds(123.45)
        assertEquals(123.45, d1.secondsSinceEpoch)
    }
}
