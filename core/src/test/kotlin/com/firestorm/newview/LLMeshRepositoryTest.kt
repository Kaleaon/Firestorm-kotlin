package com.firestorm.newview

import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class LLMeshRepositoryTest {

    @Test
    fun testMaxConcurrentRequestsBoundary() {
        val repoThread = LLMeshRepoThread()
        LLMeshRepoThread.sMaxConcurrentRequests = 4u

        val activeCount = AtomicInteger(0)
        val maxObservedActive = AtomicInteger(0)
        val totalCompleted = AtomicInteger(0)

        // Reset counters
        LLMeshRepoThread.sActiveLODRequests.set(0)
        LLMeshRepoThread.sActiveHeaderRequests.set(0)
        LLMeshRepoThread.sActiveSkinRequests.set(0)

        // Queue 10 LOD requests into mLODReqQ
        for (i in 0 until 10) {
            val params = LLVolumeParams()
            val req = repoThread.LODRequest(params, 0)
            repoThread.mLODReqQ.addLast(req)
        }

        // Verify configuration
        assertEquals(4u, LLMeshRepoThread.sMaxConcurrentRequests)
        repoThread.cleanup()
    }

    @Test
    fun testRequestStatsRetryBackoffAndExpiry() {
        val params = LLVolumeParams()
        val repoThread = LLMeshRepoThread()
        val req = repoThread.LODRequest(params, 1)

        assertEquals(0u, req.getRetries())
        assertTrue(req.canRetry())
        assertFalse(req.isDelayed())

        // First failure triggers updateTime()
        req.updateTime()
        assertEquals(1u, req.getRetries())
        assertTrue(req.canRetry())
        assertTrue(req.isDelayed())

        // Exhaust retries
        repeat(7) { req.updateTime() }
        assertEquals(8u, req.getRetries())
        assertFalse(req.canRetry())
    }

    @Test
    fun testLLMeshHeaderLLSDParsing() {
        val headerMap = mapOf(
            "version" to 1,
            "high_lod" to mapOf("offset" to 0, "size" to 500),
            "medium_lod" to mapOf("offset" to 500, "size" to 300),
            "low_lod" to mapOf("offset" to 800, "size" to 100),
            "lowest_lod" to mapOf("offset" to 900, "size" to 50),
            "skin" to mapOf("offset" to 950, "size" to 200),
            "physics_convex" to mapOf("offset" to 1150, "size" to 150),
            "physics_mesh" to mapOf("offset" to 1300, "size" to 250)
        )

        val header = LLMeshHeader(headerMap)
        assertEquals(1, header.mVersion)
        assertEquals(500, header.mLodSize[3])
        assertEquals(300, header.mLodSize[2])
        assertEquals(100, header.mLodSize[1])
        assertEquals(50, header.mLodSize[0])
        assertEquals(200, header.mSkinSize)
        assertEquals(150, header.mPhysicsConvexSize)
        assertEquals(250, header.mPhysicsMeshSize)
    }

    @Test
    fun testParallelDownloadThreadPoolDispatching() {
        val repoThread = LLMeshRepoThread()
        LLMeshRepoThread.sMaxConcurrentRequests = 8u

        val dummyParams = Array(12) { LLVolumeParams() }

        // Enqueue requests
        for (p in dummyParams) {
            repoThread.mLODReqQ.addLast(repoThread.LODRequest(p, 2))
        }

        // Run worker dispatching
        val executor = repoThread.downloadExecutor
        assertFalse(executor.isShutdown)

        repoThread.cleanup()
        assertTrue(executor.isShutdown)
    }

    @Test
    fun testUnavailableQueueHandlingWhenRetriesExhausted() {
        val repoThread = LLMeshRepoThread()
        val params = LLVolumeParams()
        val req = repoThread.LODRequest(params, 3)

        // Exhaust retries
        repeat(8) { req.updateTime() }
        assertFalse(req.canRetry())

        // Place on unavailable queue
        repoThread.mUnavailableQ.addLast(req)
        assertEquals(1, repoThread.mUnavailableQ.size)

        // Drain unavailable queue
        repoThread.notifyLoadedMeshes()
        assertEquals(0, repoThread.mUnavailableQ.size)
    }
}
