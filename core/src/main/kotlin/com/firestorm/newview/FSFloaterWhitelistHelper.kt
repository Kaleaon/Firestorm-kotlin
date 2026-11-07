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
        System.err.println("FSFloaterWhitelistHelper: resolvePlatformPaths not yet implemented")
        return PlatformPaths("", "")
    }

    // -------------------------------------------------------------------------
    // Platform stubs
    // -------------------------------------------------------------------------

    private fun setTextEditorValue(name: String, value: String) {
        System.err.println("FSFloaterWhitelistHelper: setTextEditorValue not yet implemented")
    }
}

// =============================================================================
// DirUtils — thin stub for gDirUtilp
// =============================================================================

object DirUtils {
    val executableDir: String get() = ""
    val executableFilename: String get() = ""
    val executablePathAndName: String get() = ""
    val osUserAppDir: String get() = ""
    val cacheDir: String get() = ""
    val llPluginDir: String get() = ""
    val llPluginLauncher: String get() = ""
    val appRODataDir: String get() = ""

    fun append(base: String, segment: String): String = ""

    fun baseFileName(path: String, stripExtension: Boolean = false): String = ""
}
