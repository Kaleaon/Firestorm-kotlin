/**
 * LLFileSystem.kt
 * Cached-asset file I/O — converted from llfilesystem.h / llfilesystem.cpp
 *
 * Original: Second Life Viewer Source Code
 * Copyright (C) 2010, Linden Research, Inc.
 * LGPL v2.1
 *
 * Simulates random-access file operations against files stored in the disk
 * cache.  File paths are resolved via [LLDiskCache.metaDataToFilepath].
 * The position cursor is maintained in-memory; actual I/O uses
 * [java.io.RandomAccessFile] or [java.io.FileOutputStream].
 */

package com.firestorm.llfilesystem

import com.firestorm.llcommon.*
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

/**
 * Read/write handle to a cached asset file.
 *
 * Open modes mirror the C++ constants:
 *  - [READ]       — read-only; also touches the file's last-modified time.
 *  - [WRITE]      — overwrite (truncate).
 *  - [READ_WRITE] — read/write without truncation.
 *  - [APPEND]     — append-only writes.
 *
 * @param fileId   The asset UUID.
 * @param fileType The asset type integer (see LLAssetType).
 * @param mode     One of [READ], [WRITE], [READ_WRITE], [APPEND].
 */
class LLFileSystem(
    private var fileId: LLUUID,
    private var fileType: Int,
    private val mode: Int = READ
) {
    private var position: Int = 0
    private var bytesRead: Int = 0

    init {
        // On READ, touch the last-access time so the cache purge logic remains
        // accurate (mirrors the C++ constructor behaviour documented in
        // llfilesystem.cpp lines 56-71).
        if (mode == READ) {
            val filename = LLDiskCache.metaDataToFilepath(fileId, fileType)
            if (File(filename).exists()) updateFileAccessTime(filename)
        }
    }

    // ── Instance read / write / seek ──────────────────────────────────────────

    /**
     * Read up to [bytes] bytes from the current position into [buffer].
     * @return `true` if any bytes were transferred.
     */
    fun read(buffer: ByteArray, bytes: Int): Boolean {
        val filename = LLDiskCache.metaDataToFilepath(fileId, fileType)
        val f = File(filename)
        if (!f.exists()) return false
        return try {
            RandomAccessFile(f, "r").use { raf ->
                raf.seek(position.toLong())
                val n = raf.read(buffer, 0, bytes)
                bytesRead = maxOf(0, n)
                position += bytesRead
                bytesRead > 0
            }
        } catch (_: Exception) { false }
    }

    /** Number of bytes transferred by the most recent [read] call. */
    fun getLastBytesRead(): Int = bytesRead

    /** `true` when the position cursor is at or past end-of-file. */
    fun eof(): Boolean = position >= getSize()

    /**
     * Write [bytes] bytes from [buffer] at the current position (or file end
     * in [APPEND] mode).
     * @return `true` if all bytes were written.
     */
    fun write(buffer: ByteArray, bytes: Int): Boolean {
        val filename = LLDiskCache.metaDataToFilepath(fileId, fileType)
        val f = File(filename)
        return try {
            when (mode) {
                APPEND -> {
                    FileOutputStream(f, true).use { fos ->
                        fos.write(buffer, 0, bytes)
                    }
                    position = f.length().toInt()
                    true
                }
                READ_WRITE -> {
                    if (!f.exists()) f.createNewFile()
                    RandomAccessFile(f, "rw").use { raf ->
                        raf.seek(position.toLong())
                        raf.write(buffer, 0, bytes)
                        position = raf.filePointer.toInt()
                    }
                    true
                }
                else -> {
                    // WRITE — truncate then write
                    FileOutputStream(f, false).use { fos ->
                        fos.write(buffer, 0, bytes)
                    }
                    position += bytes
                    true
                }
            }
        } catch (_: Exception) { false }
    }

    /**
     * Seek within the file.
     *
     * @param offset Byte offset relative to [origin].
     * @param origin Base position; `-1` means "from current position".
     * @return `true` on success, `false` if the resulting position is out of bounds.
     */
    fun seek(offset: Int, origin: Int = -1): Boolean {
        val base = if (origin == -1) position else origin
        val newPos = base + offset
        val size = getSize()
        return when {
            newPos > size -> { position = size;  false }
            newPos < 0   -> { position = 0;      false }
            else         -> { position = newPos; true  }
        }
    }

    /** Current byte offset within the file. */
    fun tell(): Int = position

    /** Size in bytes of the backing cache file. */
    fun getSize(): Int = getFileSize(fileId, fileType)

    /** No enforced maximum in disk-cache mode; returns [Int.MAX_VALUE]. */
    fun getMaxSize(): Int = Int.MAX_VALUE

    /**
     * Rename this file to a new asset id/type within the cache.
     * Always returns `true` (mirrors C++ behaviour).
     */
    fun rename(newId: LLUUID, newType: Int): Boolean {
        renameFile(fileId, fileType, newId, newType)
        fileId = newId
        fileType = newType
        return true
    }

    /** Delete the backing cache file.  Always returns `true`. */
    fun remove(): Boolean {
        removeFile(fileId, fileType)
        return true
    }

    /**
     * Touch [filePath]'s last-modified timestamp if more than one hour has
     * elapsed since the previous update — limiting unnecessary SSD writes
     * (see SL-14582 in the C++ sources).
     */
    fun updateFileAccessTime(filePath: String) {
        val f = File(filePath)
        if (!f.exists()) return
        val thresholdMs = 60L * 60 * 1_000   // 1 hour
        if (System.currentTimeMillis() - f.lastModified() > thresholdMs) {
            try { f.setLastModified(System.currentTimeMillis()) } catch (_: Exception) {}
        }
    }

    // ── Companion — static helpers ────────────────────────────────────────────

    companion object {
        /** Open for reading only. */
        const val READ       = 0x00000001
        /** Open for writing (truncates existing content). */
        const val WRITE      = 0x00000002
        /** Open for reading and writing without truncation. */
        const val READ_WRITE = 0x00000003
        /** Open for appending. */
        const val APPEND     = 0x00000006

        /**
         * Check whether a non-empty cache file exists for [fileId]/[fileType].
         */
        fun getExists(fileId: LLUUID, fileType: Int): Boolean {
            val f = File(LLDiskCache.metaDataToFilepath(fileId, fileType))
            return f.isFile && f.length() > 0
        }

        /**
         * Delete the cache file for [fileId]/[fileType].
         * Always returns `true`.
         */
        fun removeFile(fileId: LLUUID, fileType: Int, suppressError: Int = 0): Boolean {
            try { File(LLDiskCache.metaDataToFilepath(fileId, fileType)).delete() }
            catch (e: Exception) {
                if (suppressError == 0) System.err.println("removeFile failed: ${e.message}")
            }
            return true
        }

        /**
         * Rename a cache file from one asset id/type to another.
         * Always returns `true` to preserve C++ API compatibility.
         */
        fun renameFile(
            oldFileId: LLUUID, oldFileType: Int,
            newFileId: LLUUID, newFileType: Int
        ): Boolean {
            val src = File(LLDiskCache.metaDataToFilepath(oldFileId, oldFileType))
            val dst = File(LLDiskCache.metaDataToFilepath(newFileId, newFileType))
            if (!src.renameTo(dst)) {
                System.err.println("renameFile: Failed to rename $oldFileId → $newFileId")
            }
            return true
        }

        /**
         * Return size in bytes of the cache file, or 0 if the file does not exist.
         */
        fun getFileSize(fileId: LLUUID, fileType: Int): Int {
            val f = File(LLDiskCache.metaDataToFilepath(fileId, fileType))
            return if (f.isFile) f.length().toInt() else 0
        }
    }
}
