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
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object LLDir {
    private var appRODataDir: String = System.getProperty("user.dir", "")
    private var cacheDir: String = System.getProperty("java.io.tmpdir", "") + File.separator + "firestorm_cache"
    private var tempDir: String = System.getProperty("java.io.tmpdir", "")

    fun setAppRODataDir(path: String) { appRODataDir = path }
    fun setCacheDir(path: String) { cacheDir = path }

    fun getAppRODataDir(): String = appRODataDir

    fun getOSUserDir(): String = System.getProperty("user.home", "")

    fun getCacheDir(): String = cacheDir

    fun getTempDir(): String = tempDir

    fun getExpandedFilename(vararg parts: String): String =
        parts.filter { it.isNotEmpty() }.joinToString(File.separator)

    fun fileExists(path: String): Boolean = File(path).exists()
}

object LLFile {
    fun read(path: String): ByteArray? = runCatching { File(path).readBytes() }.getOrNull()

    fun write(path: String, data: ByteArray): Boolean =
        runCatching { File(path).writeBytes(data); true }.getOrDefault(false)

    fun remove(path: String): Boolean = runCatching { File(path).delete() }.getOrDefault(false)

    fun copy(src: String, dst: String): Boolean = runCatching {
        Files.copy(Paths.get(src), Paths.get(dst), StandardCopyOption.REPLACE_EXISTING)
        true
    }.getOrDefault(false)

    fun mkdir(path: String): Boolean =
        runCatching { File(path).mkdirs() }.getOrDefault(false)
}

class LLFileSystem(val filename: String, val accessType: AccessType) {

    enum class AccessType { READ, READ_WRITE, WRITE }

    private val raf: RandomAccessFile = RandomAccessFile(
        filename,
        when (accessType) {
            AccessType.READ -> "r"
            AccessType.READ_WRITE, AccessType.WRITE -> "rw"
        }
    )

    private var lastBytesRead: Int = 0

    fun read(dst: ByteArray, bytes: Int): Int {
        val n = raf.read(dst, 0, bytes)
        lastBytesRead = maxOf(0, n)
        return lastBytesRead
    }

    fun write(src: ByteArray, bytes: Int): Boolean = runCatching {
        raf.write(src, 0, bytes)
        true
    }.getOrDefault(false)

    fun seek(pos: Int) {
        raf.seek(pos.toLong())
    }

    fun tell(): Int = raf.filePointer.toInt()

    fun eof(): Boolean = raf.filePointer >= raf.length()

    fun getSize(): Int = raf.length().toInt()

    fun getLastBytesRead(): Int = lastBytesRead

    fun close() = raf.close()
}
