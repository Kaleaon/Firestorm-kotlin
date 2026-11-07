/**
 * LLDiskCache.kt
 * Asset disk cache — converted from lldiskcache.h / lldiskcache.cpp
 *
 * Original: Second Life Viewer Source Code
 * Copyright (C) 2020, Linden Research, Inc.
 * LGPL v2.1
 *
 * The cache stores viewer assets as flat files.  Each filename encodes the
 * asset's UUID and type so that no separate metadata database is required.
 * File format:
 *   <cacheDir>/<first-hex-char>/<prefix>_<uuid>_0.asset
 *
 * Purging is handled by [LLPurgeDiskCacheThread] which runs every 60 s.
 * High/low watermarks (Firestorm extension) reduce churn: purge is only
 * triggered when total size exceeds [highWaterPercent]% of [maxSizeBytes],
 * and stops once [lowWaterPercent]% is reached.
 */

package com.firestorm.llfilesystem

import com.firestorm.llcommon.*
import java.io.File
import java.time.Instant
import kotlin.concurrent.thread

// ─── LLDiskCache singleton ────────────────────────────────────────────────────

/**
 * Disk-based asset cache singleton.
 *
 * Must be initialised exactly once via [init] before any other methods are
 * called (mirrors C++ LLParamSingleton / LLSINGLETON pattern).
 */
object LLDiskCache {

    private const val CACHE_FILENAME_PREFIX = "sl_cache"
    private val HEX_SUBDIRS = "0123456789abcdef".toList()

    // ── Configuration (set by init) ───────────────────────────────────────────

    private var cacheDir: String = ""
    private var maxSizeBytes: Long = 0L
    private var enableCacheDebugInfo: Boolean = false
    private var highWaterPercent: Float = 95.0f
    private var lowWaterPercent: Float = 70.0f

    /** Assets that must never be purged (Firestorm static-asset extension). */
    private val skipList: MutableList<String> = mutableListOf()

    // ── Cached directory-size state ───────────────────────────────────────────

    @Volatile private var storedCacheSize: Long = 0L
    @Volatile private var lastScanInstant: Instant = Instant.EPOCH

    // ── Initialisation ────────────────────────────────────────────────────────

    /**
     * Initialise the cache.  Creates [cacheDir] and its 16 hex subdirectories,
     * then pre-populates it with static assets.
     */
    fun init(
        cacheDir: String,
        maxSizeBytes: Long,
        enableCacheDebugInfo: Boolean = false,
        highWaterPercent: Float = 95.0f,
        lowWaterPercent: Float = 70.0f
    ) {
        this.cacheDir = cacheDir
        this.maxSizeBytes = maxSizeBytes
        this.enableCacheDebugInfo = enableCacheDebugInfo
        this.highWaterPercent = highWaterPercent.coerceIn(lowWaterPercent, 100.0f)
        this.lowWaterPercent = lowWaterPercent.coerceIn(0.0f, highWaterPercent)

        File(cacheDir).mkdirs()
        for (c in HEX_SUBDIRS) {
            File(cacheDir, c.toString()).mkdirs()
        }

        prepopulateCacheWithStatic()
    }

    // ── Path building ─────────────────────────────────────────────────────────

    /**
     * Build the full path to a cache file for [id] and asset type [at].
     *
     * Format mirrors the C++ llformat in lldiskcache.cpp:
     *   `<cacheDir>/<firstChar>/<prefix>_<uuid>_0.asset`
     */
    fun metaDataToFilepath(id: LLUUID, at: Int): String {
        val uuidStr = id.toString()
        val firstChar = if (uuidStr.isNotEmpty()) uuidStr[0].lowercaseChar() else '0'
        return "$cacheDir${File.separator}$firstChar${File.separator}" +
               "${CACHE_FILENAME_PREFIX}_${uuidStr}_0.asset"
    }

    // ── Size accessors ────────────────────────────────────────────────────────

    fun setMaxSizeBytes(size: Long) { maxSizeBytes = size }
    fun setHighWaterPercentage(pct: Float) {
        highWaterPercent = pct.coerceIn(lowWaterPercent, 100.0f)
    }
    fun setLowWaterPercentage(pct: Float) {
        lowWaterPercent = pct.coerceIn(0.0f, highWaterPercent)
    }

    // ── Purge ─────────────────────────────────────────────────────────────────

    /**
     * Purge the oldest cache files until total size is below
     * [lowWaterPercent]% of [maxSizeBytes].
     *
     * Safe to call from a background thread — all access is through the
     * filesystem, with no shared mutable state beyond [storedCacheSize] /
     * [lastScanInstant] (updated atomically).
     */
    fun purge() {
        data class FileInfo(val time: Long, val size: Long, val path: String)

        val startMs = System.currentTimeMillis()

        val fileInfos = mutableListOf<FileInfo>()
        var fileSizeTotal = 0L

        val root = File(cacheDir)
        if (!root.isDirectory) return

        root.walkTopDown()
            .filter { it.isFile && it.name.contains(CACHE_FILENAME_PREFIX) }
            .forEach { f ->
                val sz = f.length()
                fileSizeTotal += sz
                fileInfos.add(FileInfo(f.lastModified(), sz, f.absolutePath))
            }

        val highWaterBytes = (maxSizeBytes * (highWaterPercent / 100.0)).toLong()
        if (fileSizeTotal < highWaterBytes) {
            if (enableCacheDebugInfo)
                println("LLDiskCache: below high-water mark — nothing to purge")
            return
        }

        // Sort oldest-first so we remove the least-recently-used files first.
        fileInfos.sortBy { it.time }

        val targetBytes = (maxSizeBytes * (lowWaterPercent / 100.0)).toLong()
        var deletedTotal = 0L
        var deletedCount = 0
        var skippedCount = 0

        for (entry in fileInfos) {
            if (fileSizeTotal - deletedTotal <= targetBytes) break

            // Extract UUID from filename: skip prefix + "_" (9 chars), take 36.
            val baseName = File(entry.path).nameWithoutExtension  // e.g. sl_cache_<uuid>_0
            val uuidStart = CACHE_FILENAME_PREFIX.length + 1
            val uuidStr = if (baseName.length >= uuidStart + 36)
                baseName.substring(uuidStart, uuidStart + 36)
            else ""

            if (uuidStr in skipList) {
                skippedCount++
                // Touch the file so it sorts later next run.
                try { File(entry.path).setLastModified(System.currentTimeMillis()) } catch (_: Exception) {}
                continue
            }

            val deleted = try { File(entry.path).delete() } catch (_: Exception) { false }
            if (deleted) {
                deletedTotal += entry.size
                deletedCount++
            }
        }

        val elapsed = System.currentTimeMillis() - startMs
        updateCacheSize(fileSizeTotal - deletedTotal)

        println("LLDiskCache: purge took ${elapsed}ms — deleted $deletedCount, skipped $skippedCount")
        println("LLDiskCache: removed ${deletedTotal} bytes; new total ${fileSizeTotal - deletedTotal}")
    }

    // ── Static asset pre-population (Firestorm extension) ─────────────────────

    /**
     * Copy static assets from the app-settings folder into the cache and add
     * their UUIDs to [skipList] so they are never purged.
     */
    fun prepopulateCacheWithStatic() {
        skipList.clear()
        val fromDir = File(
            gDirUtilp.getExpandedFilename(ELLPath.LL_PATH_APP_SETTINGS, "fs_static_assets")
        )
        if (!fromDir.isDirectory) return

        fromDir.listFiles()?.forEach { src ->
            val uuidStr = src.nameWithoutExtension
            val uuid = try { LLUUID.fromString(uuidStr) } catch (_: Exception) { return@forEach }
            val dst = File(metaDataToFilepath(uuid, 0 /* AT_UNKNOWN */))

            if (!dst.exists()) {
                try {
                    dst.parentFile?.mkdirs()
                    src.copyTo(dst)
                    if (enableCacheDebugInfo)
                        println("LLDiskCache: copied static asset $src → $dst")
                } catch (e: Exception) {
                    System.err.println("LLDiskCache: failed to copy $src: ${e.message}")
                }
            }

            if (uuidStr !in skipList) skipList.add(uuidStr)
        }
    }

    // ── Cache-clear ───────────────────────────────────────────────────────────

    /** Remove all cache files matching [CACHE_FILENAME_PREFIX], then re-seed. */
    fun clearCache() {
        println("LLDiskCache: clearing cache $cacheDir")
        File(cacheDir).walkTopDown()
            .filter { it.isFile && it.name.contains(CACHE_FILENAME_PREFIX) }
            .forEach { f ->
                try { f.delete() }
                catch (e: Exception) { System.err.println("LLDiskCache: delete failed: ${e.message}") }
            }
        prepopulateCacheWithStatic()
        println("LLDiskCache: cleared $cacheDir")
    }

    // ── Legacy VFS cleanup ────────────────────────────────────────────────────

    /**
     * Remove any leftover VFS-format files ("inv.llsd" / "db2.x") from the
     * cache directory.  These will not be re-created by the new cache.
     */
    fun removeOldVFSFiles() {
        val cacheRoot = File(gDirUtilp.getExpandedFilename(ELLPath.LL_PATH_CACHE, ""))
        if (!cacheRoot.isDirectory) return
        cacheRoot.listFiles()?.forEach { f ->
            if (f.isFile && (f.name.contains("inv.llsd") || f.name.contains("db2.x"))) {
                try { f.delete() }
                catch (e: Exception) { System.err.println("LLDiskCache: removeOldVFS: ${e.message}") }
            }
        }
    }

    // ── Info string ───────────────────────────────────────────────────────────

    /** Return a human-readable summary suitable for display in an About Box. */
    fun getCacheInfo(): String {
        val maxMb = maxSizeBytes / (1024.0 * 1024.0)
        val currentSize = if (storedCacheSize > 0) storedCacheSize.toDouble()
                          else dirFileSize(cacheDir).toDouble()
        val pctUsed = (currentSize / maxSizeBytes) * 100.0
        return "Max size %.1f MB (%.1f%% used)".format(maxMb, pctUsed)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private val CACHE_SCAN_TTL_MS = 120_000L  // 2 minutes (mirrors C++ 120 s)

    private fun dirFileSize(dir: String, force: Boolean = false): Long {
        val elapsed = System.currentTimeMillis() - lastScanInstant.toEpochMilli()
        if (!force && elapsed < CACHE_SCAN_TTL_MS && storedCacheSize > 0) {
            return storedCacheSize
        }
        var total = 0L
        File(dir).walkTopDown()
            .filter { it.isFile && it.name.contains(CACHE_FILENAME_PREFIX) }
            .forEach { total += it.length() }
        return updateCacheSize(total)
    }

    private fun updateCacheSize(newSize: Long): Long {
        storedCacheSize = newSize
        lastScanInstant = Instant.now()
        return newSize
    }
}

// ─── Background purge thread ──────────────────────────────────────────────────

/**
 * Background thread that calls [LLDiskCache.purge] every 60 seconds.
 *
 * Mirrors C++ `LLPurgeDiskCacheThread`.  Use [start] / [stop] rather than
 * manipulating the internal thread directly.
 */
class LLPurgeDiskCacheThread {
    @Volatile private var running = false
    private var worker: Thread? = null

    fun start() {
        running = true
        worker = thread(name = "PurgeDiskCacheThread", isDaemon = true) {
            val intervalMs = 60_000L
            while (running) {
                try { Thread.sleep(intervalMs) } catch (_: InterruptedException) { break }
                if (running) {
                    try { LLDiskCache.purge() }
                    catch (e: Exception) { System.err.println("LLPurgeDiskCacheThread: ${e.message}") }
                }
            }
        }
    }

    fun stop() {
        running = false
        worker?.interrupt()
        worker = null
    }
}
