package com.firestorm.newview

import java.io.File

enum class EGPUClass(val value: Int) {
    GPU_CLASS_UNKNOWN(-1),
    GPU_CLASS_0(0),
    GPU_CLASS_1(1),
    GPU_CLASS_2(2),
    GPU_CLASS_3(3),
    GPU_CLASS_4(4),
    GPU_CLASS_5(5);

    companion object {
        fun fromValue(v: Int): EGPUClass = entries.firstOrNull { it.value == v } ?: GPU_CLASS_UNKNOWN
    }
}

class LLFeatureInfo() {
    var valid: Boolean = false
    var name: String = ""
    var available: Boolean = false
    var recommendedLevel: Float = -1f

    constructor(name: String, available: Boolean, level: Float) : this() {
        this.valid = true
        this.name = name
        this.available = available
        this.recommendedLevel = level
    }

    fun isValid(): Boolean = valid
}

open class LLFeatureList(val name: String) {
    protected val features: MutableMap<String, LLFeatureInfo> = mutableMapOf()

    fun getFeatures(): MutableMap<String, LLFeatureInfo> = features

    fun addFeature(name: String, available: Boolean, level: Float) {
        if (features.containsKey(name)) return
        features[name] = LLFeatureInfo(name, available, level)
    }

    fun isFeatureAvailable(name: String): Boolean {
        return features[name]?.available ?: true
    }

    fun getRecommendedValue(name: String): Float {
        val fi = features[name]
        if (fi != null && isFeatureAvailable(name)) return fi.recommendedLevel
        return 0f
    }

    fun setFeatureAvailable(name: String, available: Boolean) {
        features[name]?.available = available
    }

    fun setRecommendedLevel(name: String, level: Float) {
        features[name]?.recommendedLevel = level
    }

    fun maskList(mask: LLFeatureList): Boolean {
        for ((_, maskFi) in mask.features) {
            val curFi = features[maskFi.name] ?: continue
            if (maskFi.available && !curFi.available) continue
            curFi.available = maskFi.available
            curFi.recommendedLevel = minOf(curFi.recommendedLevel, maskFi.recommendedLevel)
        }
        return true
    }

    fun loadFeatureList(file: File): Boolean {
        TODO("APR: use JVM equivalent — parse feature list text file into this LLFeatureList")
    }

    fun dump() {
        for ((_, fi) in features) {
            println("Feature '${fi.name}' available=${fi.available} level=${fi.recommendedLevel}")
        }
    }
}

object LLFeatureManager : LLFeatureList("default") {

    private val GRAPHICS_LEVEL_NAMES = listOf(
        "Low", "LowMid", "Mid", "MidHigh", "High", "HighUltra", "Ultra"
    )

    private val maskList: MutableMap<String, LLFeatureList> = mutableMapOf()
    private val skippedFeatures: MutableSet<String> = mutableSetOf()

    var inited: Boolean = false
        private set
    var tableVersion: Int = 0
        private set
    var safe: Boolean = false
    var gpuClass: EGPUClass = EGPUClass.GPU_CLASS_UNKNOWN
        private set
    var gpuMemoryBandwidth: Float = 0f
        private set
    var expectedGLVersion: Float = 0f
        private set
    var gpuString: String = ""
        private set
    var gpuSupported: Boolean = false
        private set
    var skipProfiling: Boolean = false
        private set

    fun initSingleton() {
        loadFeatureTables()
        loadGPUClass()
        applyBaseMasks()
        inited = true
    }

    fun getMaxGraphicsLevel(): UInt = (GRAPHICS_LEVEL_NAMES.size - 1).toUInt()

    fun isValidGraphicsLevel(level: UInt): Boolean = level <= getMaxGraphicsLevel()

    fun getNameForGraphicsLevel(level: UInt): String {
        return if (isValidGraphicsLevel(level)) {
            GRAPHICS_LEVEL_NAMES[level.toInt()]
        } else {
            "Invalid graphics level $level, valid are 0 .. ${getMaxGraphicsLevel()}"
        }
    }

    fun getGraphicsLevelForName(name: String): Int {
        val fixedFunction = "FixedFunction"
        val rname = if (name.endsWith(fixedFunction)) name.dropLast(fixedFunction.length) else name
        for (i in GRAPHICS_LEVEL_NAMES.indices) {
            if (GRAPHICS_LEVEL_NAMES[i] == rname) return i
        }
        return -1
    }

    fun findMask(name: String): LLFeatureList? = maskList[name]

    fun maskFeatures(name: String): Boolean {
        val maskp = findMask(name) ?: return false
        return maskList(maskp)
    }

    fun loadFeatureTables(): Boolean {
        skippedFeatures.addAll(listOf(
            "RenderAnisotropic",
            "RenderGamma",
            "RenderVBOEnable",
            "RenderFogRatio"
        ))

        val featureTableFilename = when {
            System.getProperty("os.name").startsWith("Mac")   -> "featuretable_mac.txt"
            System.getProperty("os.name").startsWith("Linux") -> "featuretable_linux.txt"
            else                                               -> "featuretable.txt"
        }

        TODO("APR: use JVM equivalent — resolve app data dir, build full path to $featureTableFilename, call parseFeatureTable")
    }

    private fun parseFeatureTable(filename: String): Boolean {
        val file = File(filename)
        if (!file.exists()) return false

        cleanupFeatureTables()

        val lines = file.readLines().iterator()
        if (!lines.hasNext()) return false

        val tokens = lines.next().trim().split(Regex("\\s+"))
        if (tokens.size < 2 || tokens[0] != "version") return false
        tableVersion = tokens[1].toIntOrNull() ?: return false

        var currentList: LLFeatureList? = null
        var parseOk = true

        for (line in lines) {
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.isEmpty() || parts[0].startsWith("//")) continue

            when (parts[0]) {
                "list" -> {
                    if (parts.size < 2) { parseOk = false; break }
                    val listName = parts[1]
                    if (maskList.containsKey(listName)) { parseOk = false; break }
                    currentList = LLFeatureList(listName)
                    maskList[listName] = currentList
                }
                else -> {
                    val fl = currentList
                    if (fl == null) { parseOk = false; break }
                    if (parts.size < 3) { parseOk = false; break }
                    val available = parts[1].toIntOrNull() ?: run { parseOk = false; break.also {} }
                    val recommended = parts[2].toFloatOrNull() ?: run { parseOk = false; break.also {} }
                    fl.addFeature(parts[0], available != 0, recommended)
                }
            }
        }

        if (!parseOk) cleanupFeatureTables()
        return parseOk
    }

    private fun loadGPUClass(): Boolean {
        TODO("GPU: query GPU string, run gpu_benchmark if not skipped, classify into GPU_CLASS_0..5 based on measured GB/s vs RenderClass1MemoryBandwidth threshold, adjust for CPU bias and physical memory on Windows; set gpuString, gpuSupported, gpuMemoryBandwidth, gpuClass")
    }

    fun cleanupFeatureTables() {
        maskList.clear()
    }

    fun applyRecommendedSettings() {
        val level = gpuClass.value.coerceIn(EGPUClass.GPU_CLASS_0.value, EGPUClass.GPU_CLASS_5.value).toUInt()
        setGraphicsLevel(level, false)
        TODO("APR: use JVM equivalent — persist RenderQualityPerformance=$level; apply draw-distance overrides from Disregard96/128DefaultDrawDistance settings")
    }

    fun applyFeatures(skipFeatures: Boolean) {
        for ((featureName, _) in features) {
            if (skipFeatures && featureName in skippedFeatures) continue
            val value = getRecommendedValue(featureName)
            TODO("APR: use JVM equivalent — look up control '$featureName' in gSavedSettings, set it to $value cast to the appropriate type (bool/S32/U32/F32)")
        }
    }

    fun setGraphicsLevel(level: UInt, skipFeatures: Boolean) {
        applyBaseMasks()
        val featureName = if (isValidGraphicsLevel(level)) getNameForGraphicsLevel(level) else "Low"
        maskFeatures(featureName)
        applyFeatures(skipFeatures)
        TODO("GPU: call LLViewerShaderMgr.setShaders() and gPipeline.refreshCachedSettings()")
    }

    fun applyBaseMasks() {
        features.clear()

        val all = findMask("all") ?: return
        features.putAll(all.getFeatures())

        val gpuVal = gpuClass.value
        if (gpuVal in 0..5) {
            val classNames = arrayOf("Class0", "Class1", "Class2", "Class3", "Class4", "Class5")
            maskFeatures(classNames[gpuVal])
        } else {
            maskFeatures("Unknown")
        }

        TODO("GPU: query gGLManager for vendor flags (mIsNVIDIA, mIsAMD, mIsIntel, mIsApple), GL version, texture units, VRAM, etc.; call maskFeatures for each matching condition; adjust gGLManager.mGLVersion for pre-Haswell Intel fallback")
    }

    fun maskCurrentList(name: String) {
        maskFeatures(name)
    }

    fun getVersion(): Int = tableVersion
    fun isSafe(): Boolean = safe

    fun getRecommendedSettingsMap(): Map<String, Any> {
        val level = gpuClass.value.coerceIn(EGPUClass.GPU_CLASS_0.value, EGPUClass.GPU_CLASS_5.value).toUInt()
        val featureName = if (isValidGraphicsLevel(level)) getNameForGraphicsLevel(level) else "Low"
        maskFeatures(featureName)

        val map = mutableMapOf<String, Any>()
        map["RenderQualityPerformance"] = mapOf("Value" to level.toInt(), "Persist" to 1)

        for ((featureName2, _) in features) {
            val value = getRecommendedValue(featureName2)
            map[featureName2] = mapOf("Value" to value, "Persist" to 1)
        }

        return map
    }
}
