package com.firestorm.llcommon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LLStringTest {

    @Test
    fun testTrimHead() {
        assertEquals("hello ", LLString.trimHead("  hello "))
        assertEquals("hello", LLString.trimHead("hello"))
        assertEquals("", LLString.trimHead("   "))
    }

    @Test
    fun testTrimTail() {
        assertEquals("  hello", LLString.trimTail("  hello  "))
        assertEquals("hello", LLString.trimTail("hello"))
        assertEquals("", LLString.trimTail("   "))
    }

    @Test
    fun testTrim() {
        assertEquals("hello", LLString.trim("  hello  "))
        assertEquals("hello", LLString.trim("hello"))
        assertEquals("", LLString.trim("   "))
    }

    @Test
    fun testToUpper() {
        assertEquals("HELLO", LLString.toUpper("hello"))
        assertEquals("HELLO", LLString.toUpper("HELLO"))
        assertEquals("HËLLÖ", LLString.toUpper("hëllö"))
    }

    @Test
    fun testToLower() {
        assertEquals("hello", LLString.toLower("HELLO"))
        assertEquals("hello", LLString.toLower("hello"))
    }

    @Test
    fun testIsValidIndex() {
        assertFalse(LLString.isValidIndex("", 0))
        assertTrue(LLString.isValidIndex("hello", 0))
        assertTrue(LLString.isValidIndex("hello", 5))
        assertFalse(LLString.isValidIndex("hello", -1))
        assertFalse(LLString.isValidIndex("hello", 6))
    }

    @Test
    fun testTruncate() {
        assertEquals("hel", LLString.truncate("hello", 3))
        assertEquals("hello", LLString.truncate("hello", 5))
        assertEquals("hello", LLString.truncate("hello", 10))
        assertEquals("", LLString.truncate("hello", 0))
    }

    @Test
    fun testContainsNonprintable() {
        assertTrue(LLString.containsNonprintable("hello\n"))
        assertTrue(LLString.containsNonprintable("hello\t"))
        assertTrue(LLString.containsNonprintable("hello\u007F"))
        assertFalse(LLString.containsNonprintable("hello world!~"))
    }

    @Test
    fun testStripNonprintable() {
        assertEquals("hello", LLString.stripNonprintable("hello\n\t\u007F"))
        assertEquals("hello world", LLString.stripNonprintable("hello world"))
    }

    @Test
    fun testCompareInsensitive() {
        assertEquals(0, LLString.compareInsensitive("hello", "HELLO"))
        assertTrue(LLString.compareInsensitive("apple", "BANANA") < 0)
        assertTrue(LLString.compareInsensitive("zebra", "apple") > 0)
    }

    @Test
    fun testStartsWith() {
        assertTrue(LLString.startsWith("hello world", "hello"))
        assertFalse(LLString.startsWith("hello world", "Hello"))
        assertTrue(LLString.startsWith("hello world", "Hello", false))

        assertFalse(LLString.startsWith("", "hello"))
        assertFalse(LLString.startsWith("hello", ""))
    }

    @Test
    fun testEndsWith() {
        assertTrue(LLString.endsWith("hello world", "world"))
        assertFalse(LLString.endsWith("hello world", "World"))
        assertTrue(LLString.endsWith("hello world", "World", false))

        assertFalse(LLString.endsWith("", "world"))
        assertFalse(LLString.endsWith("world", ""))
    }

    @Test
    fun testFormat() {
        assertEquals(
            "Hello John, your score is 100",
            LLString.format("Hello [NAME], your score is [SCORE]", mapOf("NAME" to "John", "SCORE" to "100"))
        )
        assertEquals("No placeholders here", LLString.format("No placeholders here", mapOf("A" to "B")))
    }

    @Test
    fun testReplaceAll() {
        assertEquals("hi there hi", LLString.replaceAll("hello there hello", "hello", "hi"))
        assertEquals("hello world", LLString.replaceAll("hello world", "xyz", "abc"))
    }

    @Test
    fun testTokenize() {
        assertEquals(listOf("a", "b", "c"), LLString.tokenize("a,b,c", ","))
        assertEquals(listOf("a", "b", "c"), LLString.tokenize("a, b, c", ", "))
        assertEquals(listOf("a", "b", "c"), LLString.tokenize("a,,b,c,", ","))
        assertEquals(emptyList(), LLString.tokenize("", ","))
        assertEquals(emptyList(), LLString.tokenize(",,,", ","))
    }

    @Test
    fun testQuote() {
        assertEquals("\"hello\"", LLString.quote("hello"))
        assertEquals("\"hello \\\"world\\\"\"", LLString.quote("hello \"world\""))
        assertEquals("\"hello \\\\ world\"", LLString.quote("hello \\ world"))
        assertEquals("\"already quoted\"", LLString.quote("\"already quoted\""))
    }

    @Test
    fun testUnquote() {
        assertEquals("hello", LLString.unquote("\"hello\""))
        assertEquals("hello \"world\"", LLString.unquote("\"hello \\\"world\\\"\""))
        assertEquals("hello \\ world", LLString.unquote("\"hello \\\\ world\""))
        assertEquals("not quoted", LLString.unquote("not quoted"))
    }

    @Test
    fun testToBoolean() {
        assertTrue(LLString.toBoolean("true"))
        assertTrue(LLString.toBoolean("TRUE"))
        assertTrue(LLString.toBoolean("1"))
        assertTrue(LLString.toBoolean("yes"))
        assertTrue(LLString.toBoolean(" YES "))

        assertFalse(LLString.toBoolean("false"))
        assertFalse(LLString.toBoolean("0"))
        assertFalse(LLString.toBoolean("no"))
        assertFalse(LLString.toBoolean("random"))
        assertFalse(LLString.toBoolean(""))
    }

    @Test
    fun testFormatNumber() {
        assertEquals("1,234.57", LLString.formatNumber(1234.567, 2))
        assertEquals("1,235", LLString.formatNumber(1234.567, 0))
        assertEquals("0.00", LLString.formatNumber(0.0, 2))
        assertEquals("1,000,000", LLString.formatNumber(1000000.0, 0))
    }
}
