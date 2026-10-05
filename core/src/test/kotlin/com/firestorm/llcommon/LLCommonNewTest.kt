package com.firestorm.llcommon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LLRandTest {
    @Test fun randInRange() {
        repeat(100) { assertTrue(llRand(10) in 0..9) }
    }
    @Test fun frandInRange() {
        repeat(100) { val v = llFRand(1f); assertTrue(v >= 0f && v < 1f) }
    }
}

class LLFormatTest {
    @Test fun formatsIntegers() = assertEquals("x=42", llformat("x=%d", 42))
    @Test fun formatsFloats() = assertEquals("3.14", llformat("%.2f", 3.14159))
}

class LLMemoryTest {
    @Test fun heapPositive() { assertTrue(LLMemory.getMaxHeapKB() > 0L) }
    @Test fun captureInfo() { val i = LLMemoryInfo.capture(); assertTrue(i.maxHeapKB > 0) }
}

class LLVersionInfoTest {
    @Test fun versionString() = assertEquals("7.1.0.0", LLVersionInfo.getVersionString())
    @Test fun channel() = assertEquals("Firestorm-Kotlin", LLVersionInfo.CHANNEL)
}

class LLChatTest {
    @Test fun chatTypeRoundTrip() = assertEquals(EChatType.NORMAL, EChatType.fromInt(1))
    @Test fun unknownChatType() = assertEquals(EChatType.UNKNOWN, EChatType.fromInt(999))
    @Test fun sourceTypeRoundTrip() = assertEquals(EChatSourceType.AGENT, EChatSourceType.fromInt(1))
}

class LLStatsAccumulatorTest {
    @Test fun basicStats() {
        val acc = LLStatsAccumulator()
        acc.record(1.0); acc.record(3.0); acc.record(5.0)
        assertEquals(3L, acc.getCount())
        assertEquals(3.0, acc.getMean())
        assertEquals(1.0, acc.getMin())
        assertEquals(5.0, acc.getMax())
    }
    @Test fun stdDevNonNegative() {
        val acc = LLStatsAccumulator()
        acc.record(2.0); acc.record(4.0)
        assertTrue(acc.getStdDev() >= 0.0)
    }
    @Test fun resetClearsStats() {
        val acc = LLStatsAccumulator()
        acc.record(10.0); acc.reset()
        assertEquals(0L, acc.getCount())
    }
}

class LLSysTest {
    @Test fun processorCountPositive() = assertTrue(LLSys.getProcessorCount() > 0)
    @Test fun osStringNonEmpty() = assertTrue(LLSys.getOSString().isNotEmpty())
}

class LLHandleTest {
    @Test fun handleReturnsTarget() {
        val target = StringBuilder("hi")
        val h = LLHandle.of(target)
        assertNotNull(h.get())
        assertTrue(h.isValid())
    }
    @Test fun deadHandleIsNull() {
        val h = LLHandle.dead<String>()
        assertNull(h.get())
        assertFalse(h.isValid())
    }
}

class LLPointerTest {
    @Test fun holdsValue() {
        val p = llPointerOf("hello")
        assertFalse(p.isNull())
        assertEquals("hello", p.get())
    }
    @Test fun releaseBecomesNull() {
        val p = llPointerOf("x")
        p.release()
        assertTrue(p.isNull())
    }
}

class LLMutexTest {
    @Test fun lockUnlock() {
        val m = LLMutex()
        m.lock()
        assertTrue(m.isLocked())
        m.unlock()
    }
    @Test fun withLockReturnsValue() {
        val m = LLMutex()
        val v = m.withLock { 42 }
        assertEquals(42, v)
    }
}

class LLThreadStatusTest {
    @Test fun enumValues() {
        assertTrue(LLThreadStatus.entries.isNotEmpty())
        assertTrue(LLThreadStatus.STOPPED in LLThreadStatus.entries)
    }
}

class LLProcessorInfoTest {
    @Test fun processorCountPositive() = assertTrue(LLProcessorInfo.getProcessorCount() > 0)
    @Test fun architectureNonEmpty() = assertTrue(LLProcessorInfo.getArchitecture().isNotEmpty())
}
