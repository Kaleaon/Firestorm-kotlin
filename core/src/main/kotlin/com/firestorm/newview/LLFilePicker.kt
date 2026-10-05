package com.firestorm.newview

class LLFilePicker private constructor() {

    enum class ELoadFilter(val value: Int) {
        FFLOAD_ALL(1),
        FFLOAD_WAV(2),
        FFLOAD_IMAGE(3),
        FFLOAD_ANIM(4),
        FFLOAD_GLTF(5),
        FFLOAD_XML(6),
        FFLOAD_SLOBJECT(7),
        FFLOAD_RAW(8),
        FFLOAD_MODEL(9),
        FFLOAD_COLLADA(10),
        FFLOAD_SCRIPT(11),
        FFLOAD_DICTIONARY(12),
        FFLOAD_DIRECTORY(13),
        FFLOAD_EXE(14),
        FFLOAD_MATERIAL(15),
        FFLOAD_MATERIAL_TEXTURE(16),
        FFLOAD_HDRI(17),
        FFLOAD_IMPORT(50)
    }

    enum class ESaveFilter(val value: Int) {
        FFSAVE_ALL(1),
        FFSAVE_WAV(3),
        FFSAVE_TGA(4),
        FFSAVE_BMP(5),
        FFSAVE_AVI(6),
        FFSAVE_ANIM(7),
        FFSAVE_GLTF(8),
        FFSAVE_XML(9),
        FFSAVE_COLLADA(10),
        FFSAVE_RAW(11),
        FFSAVE_J2C(12),
        FFSAVE_PNG(13),
        FFSAVE_JPEG(14),
        FFSAVE_SCRIPT(15),
        FFSAVE_TGAPNG(16),
        FFSAVE_BEAM(50),
        FFSAVE_EXPORT(51),
        FFSAVE_CSV(52)
    }

    companion object {
        private val sInstance = LLFilePicker()
        fun instance(): LLFilePicker = sInstance
    }

    private val files: MutableList<String> = mutableListOf()
    private var currentFile: Int = 0
    private var locked: Boolean = false

    fun getCurFileNum(): Int = currentFile
    fun getFileCount(): Int = files.size

    fun getSaveFile(
        filter: ESaveFilter = ESaveFilter.FFSAVE_ALL,
        filename: String = "",
        blocking: Boolean = true
    ): Boolean {
        if (locked) return false
        if (!checkLocalFileAccessEnabled()) return false
        return false
    }

    fun getSaveFileModeless(
        filter: ESaveFilter,
        filename: String,
        callback: (Boolean, String) -> Unit
    ): Boolean {
        return false
    }

    fun getOpenFile(filter: ELoadFilter = ELoadFilter.FFLOAD_ALL, blocking: Boolean = true): Boolean {
        if (locked) return false
        if (!checkLocalFileAccessEnabled()) return false
        return false
    }

    fun getOpenFileModeless(
        filter: ELoadFilter,
        callback: (Boolean, MutableList<String>) -> Unit
    ): Boolean {
        return false
    }

    fun getMultipleOpenFiles(filter: ELoadFilter = ELoadFilter.FFLOAD_ALL, blocking: Boolean = true): Boolean {
        if (locked) return false
        if (!checkLocalFileAccessEnabled()) return false
        return false
    }

    fun getMultipleOpenFilesModeless(
        filter: ELoadFilter,
        callback: (Boolean, MutableList<String>) -> Unit
    ): Boolean {
        return false
    }

    fun getFirstFile(): String {
        currentFile = 0
        return getNextFile()
    }

    fun getNextFile(): String {
        if (currentFile >= getFileCount()) {
            locked = false
            return ""
        }
        return files[currentFile++]
    }

    fun getCurFile(): String {
        if (currentFile >= getFileCount()) {
            locked = false
            return ""
        }
        return files[currentFile]
    }

    fun reset() {
        locked = false
        files.clear()
        currentFile = 0
    }

    private fun checkLocalFileAccessEnabled(): Boolean {
        return false
    }

    private fun extensionsForLoadFilter(filter: ELoadFilter): List<String> {
        return when (filter) {
            ELoadFilter.FFLOAD_ALL, ELoadFilter.FFLOAD_EXE ->
                listOf("app","exe","wav","bvh","anim","dae","raw","lsl","dic","xcu","gif","gltf","glb","xml","oxp",
                       "jpg","jpeg","bmp","tga","png")
            ELoadFilter.FFLOAD_IMAGE ->
                listOf("jpg","jpeg","bmp","tga","png")
            ELoadFilter.FFLOAD_WAV ->
                listOf("wav")
            ELoadFilter.FFLOAD_ANIM ->
                listOf("bvh","anim")
            ELoadFilter.FFLOAD_GLTF, ELoadFilter.FFLOAD_MATERIAL ->
                listOf("gltf","glb")
            ELoadFilter.FFLOAD_HDRI ->
                listOf("exr")
            ELoadFilter.FFLOAD_MODEL ->
                listOf("gltf","glb","dae")
            ELoadFilter.FFLOAD_COLLADA ->
                listOf("dae")
            ELoadFilter.FFLOAD_XML ->
                listOf("xml")
            ELoadFilter.FFLOAD_RAW ->
                listOf("raw")
            ELoadFilter.FFLOAD_SCRIPT ->
                listOf("lsl")
            ELoadFilter.FFLOAD_DICTIONARY ->
                listOf("dic","xcu")
            ELoadFilter.FFLOAD_MATERIAL_TEXTURE ->
                listOf("gltf","glb","tga","bmp","jpg","jpeg","png")
            ELoadFilter.FFLOAD_IMPORT ->
                listOf("oxp")
            ELoadFilter.FFLOAD_SLOBJECT ->
                listOf("slobject")
            ELoadFilter.FFLOAD_DIRECTORY ->
                emptyList()
        }
    }

    private fun extensionForSaveFilter(filter: ESaveFilter): String {
        return when (filter) {
            ESaveFilter.FFSAVE_WAV -> "wav"
            ESaveFilter.FFSAVE_TGA -> "tga"
            ESaveFilter.FFSAVE_BMP -> "bmp"
            ESaveFilter.FFSAVE_AVI -> "avi"
            ESaveFilter.FFSAVE_ANIM -> "xaf"
            ESaveFilter.FFSAVE_GLTF -> "gltf"
            ESaveFilter.FFSAVE_XML, ESaveFilter.FFSAVE_BEAM -> "xml"
            ESaveFilter.FFSAVE_COLLADA -> "dae"
            ESaveFilter.FFSAVE_RAW -> "raw"
            ESaveFilter.FFSAVE_J2C -> "j2c"
            ESaveFilter.FFSAVE_PNG -> "png"
            ESaveFilter.FFSAVE_JPEG -> "jpg"
            ESaveFilter.FFSAVE_SCRIPT -> "lsl"
            ESaveFilter.FFSAVE_TGAPNG -> "png"
            ESaveFilter.FFSAVE_EXPORT -> "oxp"
            ESaveFilter.FFSAVE_CSV -> "csv"
            ESaveFilter.FFSAVE_ALL -> ""
        }
    }
}
