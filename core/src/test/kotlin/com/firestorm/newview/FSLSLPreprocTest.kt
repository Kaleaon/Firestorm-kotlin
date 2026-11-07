package com.firestorm.newview

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FSLSLPreprocTest {

    @Test
    fun testEncodeDecodeRoundTrip() {
        val proc = FSLSLPreprocessor()
        val originalScript = "default {\n    state_entry() {\n        llOwnerSay(\"Hello /* test */ world\");\n    }\n}\n"
        val encoded = proc.encode(originalScript)

        assertTrue(encoded.contains("//start_unprocessed_text"))
        assertTrue(encoded.contains("//end_unprocessed_text"))
        assertTrue(encoded.contains("//program_version Firestorm"))

        val decoded = proc.decode(encoded)
        assertEquals(originalScript, decoded)
    }

    @Test
    fun testLslOptFunctionAndVariableElimination() {
        val proc = FSLSLPreprocessor()
        val script = """
            integer unusedVar = 10;
            integer usedVar = 42;

            unusedFunc() {
                llOwnerSay("unused");
            }

            usedFunc() {
                llOwnerSay((string)usedVar);
            }

            default {
                state_entry() {
                    usedFunc();
                }
            }
        """.trimIndent()

        val optimized = proc.lslopt(script)

        assertTrue(optimized.contains("usedFunc"))
        assertTrue(optimized.contains("usedVar"))
        assertFalse(optimized.contains("unusedFunc"))
        assertFalse(optimized.contains("unusedVar"))
    }

    @Test
    fun testLslOptPatternCacheMemoization() {
        FSLSLPreprocessor.clearPatternCache()
        val initialSize = FSLSLPreprocessor.patternCacheSize()
        assertEquals(0, initialSize)

        val proc = FSLSLPreprocessor()
        val script = """
            myFunc() {
                llOwnerSay("hello");
            }

            default {
                state_entry() {
                    myFunc();
                }
            }
        """.trimIndent()

        val p1 = FSLSLPreprocessor.getFuncCallsPattern("myFunc")
        val p2 = FSLSLPreprocessor.getFuncCallsPattern("myFunc")
        assertSame(p1, p2, "Memoized cache should return the exact same Pattern instance for identical function names")

        proc.lslopt(script)

        val cachedSize = FSLSLPreprocessor.patternCacheSize()
        assertTrue(cachedSize > 0, "Pattern cache should store compiled patterns")
    }

    @Test
    fun testPatternCacheUpperBoundEviction() {
        FSLSLPreprocessor.clearPatternCache()

        for (i in 0..1035) {
            FSLSLPreprocessor.getFuncCallsPattern("testFunc_$i")
        }

        val size = FSLSLPreprocessor.patternCacheSize()
        assertTrue(size <= 1024, "Pattern cache size should not exceed upper bound of 1024 entries")
    }

    @Test
    fun testLslCompWhitespaceShredding() {
        val proc = FSLSLPreprocessor()
        val script = "default  {   state_entry()   {   llOwnerSay(\"hello\");   }   }"
        val compressed = proc.lslcomp(script)

        assertFalse(compressed.contains("  "), "Compression should collapse multiple whitespace sequences into single newlines")
    }

    @Test
    fun testReformatLazyListsAndSwitchStatements() {
        val proc = FSLSLPreprocessor()
        val inputScript = """
            default {
                state_entry() {
                    myList[2] = "val";
                    switch (x) {
                        case 1: break;
                    }
                }
            }
        """.trimIndent()

        proc.mScript = inputScript
        proc.startProcess()

        val output = proc.mScript
        assertNotNull(output)
    }
}
