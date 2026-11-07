package com.firestorm.llcrashlogger

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrashLockTest {
    @Test
    fun processListRoundTripsThroughXmlLockFile() {
        val tempLock = Files.createTempFile("crash-lock", ".xml")
        try {
            val crashLock = CrashLock()
            crashLock.setSaveName(tempLock.toString())

            val processList = mapOf(
                "array" to listOf(
                    mapOf(
                        "pid" to 42L,
                        "dumpdir" to "/tmp/dump",
                        "procname" to "firestorm-bin"
                    )
                )
            )

            assertTrue(crashLock.putProcessList(processList))

            val restored = crashLock.getProcessList()
            val entries = restored["array"] as? List<*> ?: emptyList<Any>()
            val first = entries.firstOrNull() as? Map<*, *> ?: emptyMap<Any, Any>()

            assertEquals(42L, (first["pid"] as Number).toLong())
            assertEquals("/tmp/dump", first["dumpdir"])
            assertEquals("firestorm-bin", first["procname"])
        } finally {
            Files.deleteIfExists(tempLock)
        }
    }

    @Test
    fun isProcessAliveRejectsInvalidIdentifiers() {
        val crashLock = CrashLock()
        assertFalse(crashLock.isProcessAlive(0u, "java"))
        assertFalse(crashLock.isProcessAlive(ProcessHandle.current().pid().toUInt(), ""))
    }
}
