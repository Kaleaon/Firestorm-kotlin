package com.firestorm.llui

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class LLNotificationsTest {

    @Before
    fun setUp() {
        Notifications.clear()
        Notifications.init()
    }

    @Test
    fun testNotificationChannelStorageAndOrderPreservation() {
        val entry1 = NotificationEntry("Test1")
        val entry2 = NotificationEntry("Test2")
        val entry3 = NotificationEntry("Test3")

        Notifications.add(entry1)
        Notifications.add(entry2)
        Notifications.add(entry3)

        val retrievedOrder = mutableListOf<NotificationEntry>()
        val visibleChannel = Notifications.getChannel("Visible")
        assertNotNull(visibleChannel)
        visibleChannel!!.forEachNotification { retrievedOrder.add(it) }

        assertEquals(3, retrievedOrder.size)
        assertEquals(entry1.id, retrievedOrder[0].id)
        assertEquals(entry2.id, retrievedOrder[1].id)
        assertEquals(entry3.id, retrievedOrder[2].id)
    }

    @Test
    fun testConstantTimeFind() {
        val entry = NotificationEntry("TestFind")
        Notifications.add(entry)

        val found = Notifications.find(entry.id)
        assertNotNull(found)
        assertEquals(entry.id, found!!.id)
        assertEquals(entry.getName(), found.getName())

        val nonExistent = Notifications.find(UUID.randomUUID())
        assertNull(nonExistent)
    }

    @Test
    fun testBatchCancelByName() {
        val targetName = "GroupInvite"
        val otherName = "IMAlert"

        val n1 = NotificationEntry(targetName)
        val n2 = NotificationEntry(targetName)
        val n3 = NotificationEntry(otherName)

        Notifications.add(n1)
        Notifications.add(n2)
        Notifications.add(n3)

        assertNotNull(Notifications.find(n1.id))
        assertNotNull(Notifications.find(n2.id))
        assertNotNull(Notifications.find(n3.id))

        Notifications.cancelByName(targetName)

        assertTrue(n1.isCancelled())
        assertTrue(n2.isCancelled())
        assertFalse(n3.isCancelled())

        assertNull(Notifications.find(n1.id))
        assertNull(Notifications.find(n2.id))
        assertNotNull(Notifications.find(n3.id))
    }

    @Test
    fun testBatchCancelByOwner() {
        val ownerUuid = UUID.randomUUID()
        val otherOwnerUuid = UUID.randomUUID()

        val n1 = NotificationEntry("Notif1", payload = mutableMapOf("owner_id" to ownerUuid.toString()))
        val n2 = NotificationEntry("Notif2", payload = mutableMapOf("owner_id" to ownerUuid.toString()))
        val n3 = NotificationEntry("Notif3", payload = mutableMapOf("owner_id" to otherOwnerUuid.toString()))

        Notifications.add(n1)
        Notifications.add(n2)
        Notifications.add(n3)

        Notifications.cancelByOwner(ownerUuid)

        assertTrue(n1.isCancelled())
        assertTrue(n2.isCancelled())
        assertFalse(n3.isCancelled())

        assertNull(Notifications.find(n1.id))
        assertNull(Notifications.find(n2.id))
        assertNotNull(Notifications.find(n3.id))
    }

    @Test
    fun testCascadedChannelsPropagation() {
        val entry = NotificationEntry("TestCascade")
        Notifications.add(entry)

        val enabledChannel = Notifications.getChannel("Enabled")
        val unexpiredChannel = Notifications.getChannel("Unexpired")
        val visibleChannel = Notifications.getChannel("Visible")

        assertNotNull(enabledChannel)
        assertNotNull(unexpiredChannel)
        assertNotNull(visibleChannel)

        assertEquals(1, enabledChannel!!.size())
        assertEquals(1, unexpiredChannel!!.size())
        assertEquals(1, visibleChannel!!.size())

        Notifications.cancel(entry)

        assertEquals(0, visibleChannel.size())
        assertNull(Notifications.find(entry.id))
    }

    @Test
    fun testConcurrentReadsAndMutations() {
        val latch = CountDownLatch(1)
        val numThreads = 4
        val itemsPerThread = 250

        val threads = List(numThreads) { threadId ->
            thread {
                latch.await()
                for (i in 0 until itemsPerThread) {
                    val entry = NotificationEntry("ConcurrentNotif_${threadId}_${i}")
                    Notifications.add(entry)
                    val found = Notifications.find(entry.id)
                    assertNotNull(found)
                    if (i % 2 == 0) {
                        Notifications.cancel(entry)
                    }
                }
            }
        }

        latch.countDown()
        threads.forEach { it.join(5000) }

        val remainingCount = Notifications.getChannel("Visible")?.size() ?: 0
        assertTrue("Remaining count should equal uncancelled items", remainingCount > 0)
    }

    @Test
    fun testLargeScaleBatchOperationPerformance() {
        val count = 2000
        val targetName = "BatchTarget"
        val targetEntries = mutableListOf<NotificationEntry>()

        for (i in 0 until count) {
            val name = if (i % 2 == 0) targetName else "Other_$i"
            val entry = NotificationEntry(name)
            if (i % 2 == 0) targetEntries.add(entry)
            Notifications.add(entry)
        }

        val startTime = System.nanoTime()
        Notifications.cancelByName(targetName)
        val durationMs = (System.nanoTime() - startTime) / 1_000_000.0

        assertTrue("Batch cancel of 1000 matching notifications should complete quickly (< 500ms)", durationMs < 500.0)

        for (entry in targetEntries) {
            assertNull("Cancelled entry should no longer be found", Notifications.find(entry.id))
        }
    }
}
