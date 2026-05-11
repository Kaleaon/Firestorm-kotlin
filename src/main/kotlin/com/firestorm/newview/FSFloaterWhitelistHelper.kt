package com.firestorm.newview

// =============================================================================
// FSFloaterWhitelistHelper
// =============================================================================

/**
 * Displays filesystem paths the user should whitelist in their antivirus tool:
 * the viewer executable, the voice executable, the SLPlugin launcher, and
 * Dullahan (the embedded browser host), plus the relevant data/cache folders.
 *
 * Mirrors `FSFloaterWhiteListHelper` from `fsfloaterwhitelisthelper.h/.cpp`.
 *
 * @param key LLSD construction key from the floater registry.
 */
class FSFloaterWhitelistHelper(val key: Any) {

    fun postBuild(): Boolean {
        populateWhitelistInfo()
        return true
    }

    private fun populateWhitelistInfo() {
        val (voiceExePath, dullahanPath) = resolvePlatformPaths()

        val slPluginLauncherPath = DirUtils.llPluginLauncher

        val folderInfo = buildString {
            append(DirUtils.executableDir).append("\n")
            append(DirUtils.osUserAppDir).append("\n")
            append(DirUtils.cacheDir)
        }

        val exeInfo = buildString {
            append(DirUtils.executableFilename).append("\n")
            append(DirUtils.executablePathAndName).append("\n")
            append(DirUtils.baseFileName(voiceExePath)).append("\n")
            append(voiceExePath).append("\n")
            append(DirUtils.baseFileName(slPluginLauncherPath)).append("\n")
            append(slPluginLauncherPath).append("\n")
            append(DirUtils.baseFileName(dullahanPath)).append("\n")
            append(dullahanPath).append("\n")
        }

        setTextEditorValue("whitelist_folders_editor", folderInfo)
        setTextEditorValue("whitelist_exes_editor", exeInfo)
    }

    private data class PlatformPaths(val voiceExePath: String, val dullahanPath: String)

    private fun resolvePlatformPaths(): PlatformPaths {
        TODO("APR: use JVM equivalent — detect OS via System.getProperty(\"os.name\") " +
            "and build platform-appropriate paths for SLVoice and dullahan_host; " +
            "on Linux consult the FSLinuxEnableWin64VoiceProxy setting to pick " +
            "the native or Wine-bundled win64/SLVoice.exe path")
    }

    // -------------------------------------------------------------------------
    // Platform stubs
    // -------------------------------------------------------------------------

    private fun setTextEditorValue(name: String, value: String): Unit =
        TODO("Platform: getChild<LLTextEditor>(\"$name\").setText(\"$value\")")
}

// =============================================================================
// DirUtils — thin stub for gDirUtilp
// =============================================================================

object DirUtils {
    val executableDir: String get() = TODO("APR: use JVM equivalent — gDirUtilp->getExecutableDir()")
    val executableFilename: String get() = TODO("APR: use JVM equivalent — gDirUtilp->getExecutableFilename()")
    val executablePathAndName: String get() = TODO("APR: use JVM equivalent — gDirUtilp->getExecutablePathAndName()")
    val osUserAppDir: String get() = TODO("APR: use JVM equivalent — gDirUtilp->getOSUserAppDir()")
    val cacheDir: String get() = TODO("APR: use JVM equivalent — gDirUtilp->getCacheDir()")
    val llPluginDir: String get() = TODO("APR: use JVM equivalent — gDirUtilp->getLLPluginDir()")
    val llPluginLauncher: String get() = TODO("APR: use JVM equivalent — gDirUtilp->getLLPluginLauncher()")
    val appRODataDir: String get() = TODO("APR: use JVM equivalent — gDirUtilp->getAppRODataDir()")

    fun append(base: String, segment: String): String =
        TODO("APR: use JVM equivalent — gDirUtilp->append(base, segment)")

    fun baseFileName(path: String, stripExtension: Boolean = false): String =
        TODO("APR: use JVM equivalent — gDirUtilp->getBaseFileName(path, $stripExtension)")
}
