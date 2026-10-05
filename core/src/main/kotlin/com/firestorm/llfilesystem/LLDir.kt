/**
 * LLDir.kt
 * Directory/path management — converted from lldir.h / lldir.cpp
 *
 * Original: Second Life Viewer Source Code
 * Copyright (C) 2010, Linden Research, Inc.
 * LGPL v2.1
 */

package com.firestorm.llfilesystem

import com.firestorm.llcommon.*
import java.io.File
import java.util.UUID

// ─── Path-location enum (mirrors ELLPath) ────────────────────────────────────

enum class ELLPath(val id: Int) {
    LL_PATH_NONE(0),
    LL_PATH_USER_SETTINGS(1),
    LL_PATH_APP_SETTINGS(2),
    LL_PATH_PER_SL_ACCOUNT(3),
    LL_PATH_CACHE(4),
    LL_PATH_CHARACTER(5),
    LL_PATH_HELP(6),
    LL_PATH_LOGS(7),
    LL_PATH_TEMP(8),
    LL_PATH_SKINS(9),
    LL_PATH_TOP_SKIN(10),
    LL_PATH_CHAT_LOGS(11),
    LL_PATH_PER_ACCOUNT_CHAT_LOGS(12),
    LL_PATH_USER_SKIN(14),
    LL_PATH_LOCAL_ASSETS(15),
    LL_PATH_EXECUTABLE(16),
    LL_PATH_DEFAULT_SKIN(17),
    LL_PATH_FONTS(18),
    LL_PATH_DUMP(19),
    LL_PATH_TOP_SKINTHEME(20),
    LL_PATH_FS_RESOURCES(21),
    LL_PATH_FS_SOUND_CACHE(22),
    LL_PATH_LAST(23)
}

// ─── Skin constraint for findSkinnedFilenames ─────────────────────────────────

enum class ESkinConstraint { CURRENT_SKIN, ALL_SKINS }

// ─── LLDir singleton ──────────────────────────────────────────────────────────

/**
 * Directory utilities singleton.
 *
 * Mirrors the C++ LLDir base class (lldir.h / lldir.cpp). Platform-specific
 * initialisation (initAppDirs) is left abstract so that a subclass can supply
 * it; for JVM use a concrete [LLDirImpl] is provided below.
 *
 * All file operations delegate to [java.io.File] so that no native code is
 * required.
 */
abstract class LLDir {

    // ── Mutable directory state ───────────────────────────────────────────────

    var appName: String = ""
    var executablePathAndName: String = ""
    var executableFilename: String = ""
    var executableDir: String = ""
    var workingDir: String = ""
    var appRODataDir: String = ""
    var osUserDir: String = ""
    var osUserAppDir: String = ""
    var lindenUserDir: String = ""
    var perAccountChatLogsDir: String = ""
    var chatLogsDir: String = ""
    var caFile: String = ""
    var tempDir: String = ""
    protected var cacheDir: String = ""
    protected var defaultCacheDir: String = ""
    var osCacheDir: String = ""
    val dirDelimiter: String = File.separator
    var skinName: String = ""
    var skinThemeName: String = ""
    var skinBaseDir: String = ""
    var defaultSkinDir: String = ""
    var skinDir: String = ""
    var skinThemeDir: String = ""
    var userDefaultSkinDir: String = ""
    var userSkinDir: String = ""
    protected val searchSkinDirs: MutableList<String> = mutableListOf()
    var language: String = "en"
    var llPluginDir: String = ""
    var userName: String = "undefined"
    var soundCacheDir: String = ""

    /** Cache of skin-dir existence checks (path → exists). */
    private val skinDirCache: MutableMap<String, Boolean> = mutableMapOf()

    // ── Subclass contract ─────────────────────────────────────────────────────

    /** Platform-specific initialisation.  Must populate all directory fields. */
    abstract fun initAppDirs(appName: String, appReadOnlyDataDir: String = "")

    abstract fun countFilesInDir(dirname: String, mask: String): UInt

    abstract fun getNextFileInDir(dirname: String, mask: String, fname: StringBuilder): Boolean

    abstract fun getCurPath(): String

    abstract fun fileExists(filename: String): Boolean

    abstract fun getLLPluginLauncher(): String

    abstract fun getLLPluginFilename(baseName: String): String

    // ── Accessors (mirrors C++ const-ref getters) ─────────────────────────────

    fun getExecutablePathAndName(): String = executablePathAndName
    fun getExecutableFilename(): String = executableFilename
    fun getExecutableDir(): String = executableDir
    fun getWorkingDir(): String = workingDir
    fun getAppName(): String = appName
    fun getAppRODataDir(): String = appRODataDir
    fun getOSUserDir(): String = osUserDir
    fun getOSUserAppDir(): String = osUserAppDir
    fun getLindenUserDir(): String = lindenUserDir
    fun getChatLogsDir(): String = chatLogsDir
    fun getPerAccountChatLogsDir(): String = perAccountChatLogsDir
    fun getTempDir(): String = tempDir
    fun getOSCacheDir(): String = osCacheDir
    fun getCAFile(): String = caFile
    fun getDirDelimiter(): String = dirDelimiter
    fun getDefaultSkinDir(): String = defaultSkinDir
    fun getSkinDir(): String = skinDir
    fun getSkinThemeDir(): String = skinThemeDir
    fun getUserDefaultSkinDir(): String = userDefaultSkinDir
    fun getUserSkinDir(): String = userSkinDir
    fun getSkinBaseDir(): String = skinBaseDir
    fun getLLPluginDir(): String = llPluginDir
    fun getUserName(): String = userName
    fun getSoundCacheDir(): String = soundCacheDir
    fun getSkinFolder(): String = skinName
    fun getSkinThemeFolder(): String = skinThemeName
    fun getLanguage(): String = language

    fun getDumpDir(): String {
        if (sDumpDir.isEmpty()) {
            sDumpDir = add(getExpandedFilename(ELLPath.LL_PATH_LOGS, ""), "dump-${UUID.randomUUID()}")
            File(sDumpDir).mkdirs()
        }
        return sDumpDir
    }

    fun dumpDirExists(): Boolean = sDumpDir.isNotEmpty()

    fun setDumpDir(path: String) {
        sDumpDir = path.trimEnd(dirDelimiter[0])
    }

    fun getCacheDir(getDefault: Boolean = false): String {
        return if (cacheDir.isEmpty() || getDefault) {
            if (defaultCacheDir.isNotEmpty()) defaultCacheDir
            else buildSLOSCacheDir()
        } else {
            cacheDir
        }
    }

    // ── Mutators ──────────────────────────────────────────────────────────────

    open fun setChatLogsDir(path: String) {
        if (path.isNotEmpty()) chatLogsDir = path
    }

    /**
     * Set the Linden user directory, incorporating an optional grid name so
     * that per-grid subdirectories are created (Firestorm extension).
     */
    open fun setLindenUserDir(username: String, gridname: String = "") {
        require(username.isNotEmpty()) { "NULL name for LLDir::setLindenUserDir" }
        val userLower = username.lowercase().replace(' ', '_')
        val gridLower = gridname.lowercase().replace(' ', '_')
        lindenUserDir = add(getOSUserAppDir(), userLower)
        if (gridLower.isNotEmpty() && gridLower != "second_life") {
            lindenUserDir += ".$gridLower"
        }
    }

    open fun setPerAccountChatLogsDir(username: String, gridname: String = "") {
        require(username.isNotEmpty()) { "NULL name for LLDir::setPerAccountChatLogsDir" }
        val userLower = username.lowercase().replace(' ', '_')
        val gridLower = gridname.lowercase().replace(' ', '_')
        userName = userLower
        updatePerAccountChatLogsDir()
        if (gridLower.isNotEmpty() && gridLower != "second_life") {
            perAccountChatLogsDir += ".$gridLower"
        }
    }

    open fun updatePerAccountChatLogsDir() {
        perAccountChatLogsDir = add(getChatLogsDir(), userName)
    }

    open fun setSkinFolder(skinFolder: String, themeFolder: String, lang: String) {
        skinName = skinFolder
        skinThemeName = themeFolder
        language = lang
        searchSkinDirs.clear()
        skinDirCache.clear()

        defaultSkinDir = add(getSkinBaseDir(), "default")
        addSearchSkinDir(defaultSkinDir)

        skinDir = add(getSkinBaseDir(), skinFolder)
        addSearchSkinDir(skinDir)
        updatePerAccountChatLogsDir()

        if (themeFolder.isNotEmpty()) {
            skinThemeDir = add(getSkinDir(), "themes", themeFolder)
            addSearchSkinDir(skinThemeDir)
        }

        userSkinDir = add(getOSUserAppDir(), "skins")
        userDefaultSkinDir = add(userSkinDir, "default")
        userSkinDir = add(userSkinDir, skinFolder)
        addSearchSkinDir(userDefaultSkinDir)
        addSearchSkinDir(userSkinDir)
    }

    open fun setCacheDir(path: String): Boolean {
        if (path.isEmpty()) {
            cacheDir = ""
            return true
        }
        File(path).mkdirs()
        val tempFile = File(path, "temp")
        return try {
            tempFile.createNewFile()
            tempFile.delete()
            cacheDir = path
            true
        } catch (_: Exception) {
            false
        }
    }

    open fun setSoundCacheDir(path: String): Boolean {
        soundCacheDir = getCacheDir()
        if (path.isEmpty()) return true
        File(path).mkdirs()
        val tempFile = File(path, "temp")
        return try {
            tempFile.createNewFile()
            tempFile.delete()
            soundCacheDir = path
            true
        } catch (_: Exception) {
            false
        }
    }

    // ── Path building ─────────────────────────────────────────────────────────

    /**
     * Append [name] to [destpath] using [dirDelimiter], avoiding double
     * separators.  Mirrors LLDir::append(std::string&, const std::string&).
     */
    fun append(destpath: StringBuilder, name: String) {
        if (destpath.isEmpty() || name.isEmpty()) {
            destpath.append(name)
            return
        }
        val pathEndsSep = destpath.endsWith(dirDelimiter)
        val nameStartsSep = name.startsWith(dirDelimiter)
        when {
            !pathEndsSep && !nameStartsSep -> destpath.append(dirDelimiter).append(name)
            pathEndsSep && nameStartsSep   -> destpath.append(name.substring(dirDelimiter.length))
            else                           -> destpath.append(name)
        }
    }

    /** Return [path] with each [names] segment appended, separator-aware. */
    fun add(path: String, vararg names: String): String {
        val sb = StringBuilder(path)
        for (n in names) append(sb, n)
        return sb.toString()
    }

    // ── Expanded filename ─────────────────────────────────────────────────────

    fun getExpandedFilename(location: ELLPath, filename: String): String =
        getExpandedFilename(location, "", "", filename)

    fun getExpandedFilename(location: ELLPath, subdir: String, filename: String): String =
        getExpandedFilename(location, "", subdir, filename)

    fun getExpandedFilename(
        location: ELLPath,
        subdir1: String,
        subdir2: String,
        inFilename: String
    ): String {
        val prefix: String = when (location) {
            ELLPath.LL_PATH_NONE                -> ""
            ELLPath.LL_PATH_APP_SETTINGS        -> add(getAppRODataDir(), "app_settings")
            ELLPath.LL_PATH_CHARACTER           -> add(getAppRODataDir(), "character")
            ELLPath.LL_PATH_FS_RESOURCES        -> add(getAppRODataDir(), "fs_resources")
            ELLPath.LL_PATH_HELP                -> "help"
            ELLPath.LL_PATH_CACHE               -> getCacheDir()
            ELLPath.LL_PATH_DUMP                -> getDumpDir()
            ELLPath.LL_PATH_USER_SETTINGS       -> add(getOSUserAppDir(), "user_settings")
            ELLPath.LL_PATH_PER_SL_ACCOUNT      -> getLindenUserDir().ifEmpty { return "" }
            ELLPath.LL_PATH_CHAT_LOGS           -> getChatLogsDir()
            ELLPath.LL_PATH_PER_ACCOUNT_CHAT_LOGS -> getPerAccountChatLogsDir().ifEmpty { return "" }
            ELLPath.LL_PATH_LOGS                -> add(getOSUserAppDir(), "logs")
            ELLPath.LL_PATH_TEMP                -> getTempDir()
            ELLPath.LL_PATH_TOP_SKIN            -> getSkinDir()
            ELLPath.LL_PATH_TOP_SKINTHEME       -> getSkinThemeDir()
            ELLPath.LL_PATH_DEFAULT_SKIN        -> getDefaultSkinDir()
            ELLPath.LL_PATH_USER_SKIN           -> getUserSkinDir()
            ELLPath.LL_PATH_SKINS               -> getSkinBaseDir()
            ELLPath.LL_PATH_LOCAL_ASSETS        -> add(getAppRODataDir(), "local_assets")
            ELLPath.LL_PATH_EXECUTABLE          -> getExecutableDir()
            ELLPath.LL_PATH_FONTS               -> add(getAppRODataDir(), "fonts")
            ELLPath.LL_PATH_FS_SOUND_CACHE      -> getSoundCacheDir()
            else                                -> ""
        }

        val expandedBase = add(prefix, subdir1, subdir2)
        if (expandedBase.isEmpty() && inFilename.isEmpty()) return ""
        return expandedBase + dirDelimiter + inFilename
    }

    // ── Filename decomposition ────────────────────────────────────────────────

    fun getBaseFileName(filepath: String, stripExtension: Boolean = false): String {
        val base = File(filepath).name
        if (!stripExtension) return base
        val dot = base.lastIndexOf('.')
        return if (dot > 0) base.substring(0, dot) else base
    }

    fun getDirName(filepath: String): String = File(filepath).parent ?: ""

    /** Returns the lowercase extension, without the leading dot. */
    fun getExtension(filepath: String): String {
        if (filepath.isEmpty()) return ""
        val base = getBaseFileName(filepath, false)
        val dot = base.lastIndexOf('.')
        return if (dot <= 0) "" else base.substring(dot + 1).lowercase()
    }

    // ── Skin file search ──────────────────────────────────────────────────────

    fun findSkinnedFilenames(
        subdir: String,
        filename: String,
        constraint: ESkinConstraint = ESkinConstraint.CURRENT_SKIN
    ): List<String> {
        if (".." in filename) return emptyList()

        val defaultLang = resolveDefaultLanguage(subdir)
        val subsubdirs: List<String> = if (defaultLang.isEmpty()) {
            listOf("")
        } else {
            if (language != defaultLang) listOf(defaultLang, language) else listOf(defaultLang)
        }

        return if (constraint == ESkinConstraint.ALL_SKINS) {
            buildList {
                walkSearchSkinDirs(subdir, subsubdirs, filename) { _, fullPath -> add(fullPath) }
            }
        } else {
            val pathFor = mutableMapOf<String, String>()
            walkSearchSkinDirs(subdir, subsubdirs, filename) { sub, fullPath -> pathFor[sub] = fullPath }
            buildList { subsubdirs.forEach { s -> pathFor[s]?.let { add(it) } } }
        }
    }

    fun findSkinnedFilenameBaseLang(
        subdir: String,
        filename: String,
        constraint: ESkinConstraint = ESkinConstraint.CURRENT_SKIN
    ): String = findSkinnedFilenames(subdir, filename, constraint).firstOrNull() ?: ""

    fun findSkinnedFilename(
        subdir: String,
        filename: String,
        constraint: ESkinConstraint = ESkinConstraint.CURRENT_SKIN
    ): String = findSkinnedFilenames(subdir, filename, constraint).lastOrNull() ?: ""

    private fun walkSearchSkinDirs(
        subdir: String,
        subsubdirs: List<String>,
        filename: String,
        action: (subsubdir: String, fullPath: String) -> Unit
    ) {
        for (skindir in searchSkinDirs) {
            val subdirPath = add(skindir, subdir)
            for (sub in subsubdirs) {
                val fullPath = add(subdirPath, sub, filename)
                val exists = skinDirCache.getOrPut(fullPath) { fileExists(fullPath) }
                if (exists) action(sub, fullPath)
            }
        }
    }

    private val localizedCache = mutableMapOf<String, String>()
    private val unlocalizedSubdirs = setOf("", "textures")

    private fun resolveDefaultLanguage(subdir: String): String {
        localizedCache[subdir]?.let { return it }
        if (subdir in unlocalizedSubdirs) {
            localizedCache[subdir] = ""
            return ""
        }
        val subdirPath = add(getDefaultSkinDir(), subdir)
        val result = when {
            fileExists(add(subdirPath, "en"))    -> "en"
            fileExists(add(subdirPath, "en-us")) -> "en-us"
            else                                 -> ""
        }
        localizedCache[subdir] = result
        return result
    }

    // ── File / directory operations ───────────────────────────────────────────

    fun getFilesInDir(dirname: String): List<String> {
        val dir = File(dirname)
        if (!dir.isDirectory) return emptyList()
        return dir.listFiles()?.filter { it.isFile }?.map { it.name } ?: emptyList()
    }

    fun getDirectoriesInDir(dirname: String): List<String> {
        val dir = File(dirname)
        if (!dir.isDirectory) return emptyList()
        return dir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
    }

    fun deleteFilesInDir(dirname: String, mask: String): Int {
        if (!fileExists(dirname)) return 0
        var count = 0
        val iter = DirIterator(dirname, mask)
        val fname = StringBuilder()
        while (iter.next(fname)) {
            val fullpath = add(dirname, fname.toString())
            val f = File(fullpath)
            if (f.isDirectory) { count++; continue }
            var retries = 0
            while (retries < 5) {
                if (f.delete()) break
                retries++
                if (retries >= 5) return count
                Thread.sleep(100)
            }
            count++
        }
        return count
    }

    fun deleteDirAndContents(dirName: String): UInt {
        val dir = File(dirName)
        if (!dir.exists()) return 0u
        val deleted = dir.walkBottomUp().count { it.delete() }
        return deleted.toUInt()
    }

    fun findFile(
        filename: String,
        searchPath1: String = "",
        searchPath2: String = "",
        searchPath3: String = ""
    ): String = findFile(filename, listOf(searchPath1, searchPath2, searchPath3))

    fun findFile(filename: String, searchPaths: List<String>): String {
        for (path in searchPaths) {
            if (path.isNotEmpty()) {
                val full = if (filename.isNotEmpty()) path + dirDelimiter + filename else path
                if (fileExists(full)) return full
            }
        }
        return ""
    }

    // ── Temp filename ─────────────────────────────────────────────────────────

    fun getTempFilename(): String = add(getTempDir(), "${UUID.randomUUID()}.tmp")

    // ── Static asset cache path ───────────────────────────────────────────────

    fun buildSLOSCacheDir(): String {
        return if (getOSCacheDir().isEmpty()) {
            if (getOSUserAppDir().isEmpty()) "data" else add(getOSUserAppDir(), "cache")
        } else {
            add(getOSCacheDir(), APP_NAME)
        }
    }

    // ── Skin directory helpers ────────────────────────────────────────────────

    protected fun addSearchSkinDir(skindir: String) {
        if (skindir !in searchSkinDirs) searchSkinDirs.add(skindir)
    }

    // ── Dump log path ─────────────────────────────────────────────────────────

    fun getDumpLogsDirPath(fileName: String = ""): String =
        getExpandedFilename(ELLPath.LL_PATH_LOGS, "dump_logs", fileName)

    // ── Filename sanitisation ─────────────────────────────────────────────────

    fun getScrubbedFileName(uncleanFileName: String): String {
        var name = uncleanFileName
        for (ch in FORBIDDEN_FILE_CHARS) name = name.replace(ch, '_')
        return name
    }

    // ── Diagnostics ───────────────────────────────────────────────────────────

    open fun dumpCurrentDirectories() {
        println("Current Directories:")
        println("  CurPath:               ${getCurPath()}")
        println("  AppName:               ${getAppName()}")
        println("  ExecutableFilename:    ${getExecutableFilename()}")
        println("  ExecutableDir:         ${getExecutableDir()}")
        println("  WorkingDir:            ${getWorkingDir()}")
        println("  AppRODataDir:          ${getAppRODataDir()}")
        println("  OSUserDir:             ${getOSUserDir()}")
        println("  OSUserAppDir:          ${getOSUserAppDir()}")
        println("  LindenUserDir:         ${getLindenUserDir()}")
        println("  TempDir:               ${getTempDir()}")
        println("  CAFile:                ${getCAFile()}")
        println("  SkinBaseDir:           ${getSkinBaseDir()}")
        println("  SkinDir:               ${getSkinDir()}")
        println("  SkinThemeDir:          ${getSkinThemeDir()}")
    }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        const val APP_NAME = "Firestorm"

        const val XUI      = "xui"
        const val TEXTURES = "textures"
        const val SKINBASE = ""

        const val FORBIDDEN_FILE_CHARS = "\\/:*?\"<>|"

        /** Per-run crash-dump subdirectory (static, shared across instances). */
        var sDumpDir: String = ""

        fun getForbiddenFileChars(): String = FORBIDDEN_FILE_CHARS
    }
}

// ─── Concrete JVM implementation ──────────────────────────────────────────────

/**
 * Concrete implementation of [LLDir] for the JVM.
 *
 * [initAppDirs] populates all directory fields using standard JVM APIs and
 * [java.io.File].  This replaces the three platform-specific C++ subclasses
 * (LLDir_Linux, LLDir_Mac, LLDir_Win32).
 */
class LLDirImpl : LLDir() {

    override fun initAppDirs(appName: String, appReadOnlyDataDir: String) {
        this.appName = appName

        val javaHome = System.getProperty("java.home") ?: ""
        executableDir = javaHome
        executableFilename = "java"
        executablePathAndName = add(executableDir, executableFilename)
        workingDir = System.getProperty("user.dir") ?: ""

        appRODataDir = if (appReadOnlyDataDir.isNotEmpty()) appReadOnlyDataDir else workingDir

        val userHome = System.getProperty("user.home") ?: ""
        osUserDir = userHome

        val os = System.getProperty("os.name", "").lowercase()
        osUserAppDir = when {
            os.contains("win") ->
                add(System.getenv("APPDATA") ?: userHome, appName)
            os.contains("mac") ->
                add(userHome, "Library", "Application Support", appName)
            else ->
                add(userHome, ".${appName.lowercase()}")
        }

        tempDir = System.getProperty("java.io.tmpdir") ?: "/tmp"
        defaultCacheDir = buildSLOSCacheDir()
        skinBaseDir = add(appRODataDir, "skins")
        llPluginDir = add(executableDir, "plugins")
        caFile = add(appRODataDir, "app_settings", "ca-bundle.crt")
    }

    override fun countFilesInDir(dirname: String, mask: String): UInt {
        var count = 0u
        val iter = DirIterator(dirname, mask)
        val buf = StringBuilder()
        while (iter.next(buf)) count++
        return count
    }

    override fun getNextFileInDir(dirname: String, mask: String, fname: StringBuilder): Boolean {
        // Single-shot: not thread-safe; wraps DirIterator for one call.
        val iter = DirIterator(dirname, mask)
        return iter.next(fname)
    }

    override fun getCurPath(): String = System.getProperty("user.dir") ?: ""

    override fun fileExists(filename: String): Boolean = File(filename).exists()

    override fun getLLPluginLauncher(): String = add(llPluginDir, "SLPlugin")

    override fun getLLPluginFilename(baseName: String): String {
        val os = System.getProperty("os.name", "").lowercase()
        val libName = when {
            os.contains("win") -> "$baseName.dll"
            os.contains("mac") -> "lib$baseName.dylib"
            else               -> "lib$baseName.so"
        }
        return add(llPluginDir, libName)
    }
}

// ─── Global singleton (mirrors `LLDir* gDirUtilp`) ───────────────────────────

/** Global directory utilities instance — equivalent to C++ `gDirUtilp`. */
val gDirUtilp: LLDir by lazy { LLDirImpl() }

// ─── Utility ─────────────────────────────────────────────────────────────────

/**
 * Ensure a directory exists, creating it if necessary.
 * Throws [IllegalStateException] if creation fails.
 */
fun dirExistsOrCrash(dirName: String) {
    val dir = File(dirName)
    if (!dir.exists()) {
        check(dir.mkdirs()) { "Unable to create directory: $dirName" }
    } else {
        check(dir.isDirectory) { "Data directory collision: $dirName" }
    }
}
