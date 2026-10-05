package com.firestorm.llrender

import kotlin.random.Random

class PostProcessTweaks {
    private val data: MutableMap<String, Any> = mutableMapOf()

    var brightMult: Float
        get() = (data["brightness_multiplier"] as? Double)?.toFloat() ?: 0f
        set(v) { data["brightness_multiplier"] = v.toDouble() }

    var noiseStrength: Float
        get() = (data["noise_strength"] as? Double)?.toFloat() ?: 0f
        set(v) { data["noise_strength"] = v.toDouble() }

    var noiseSize: Float
        get() = (data["noise_size"] as? Double)?.toFloat() ?: 0f
        set(v) { data["noise_size"] = v.toDouble() }

    var extractLow: Float
        get() = (data["extract_low"] as? Double)?.toFloat() ?: 0f
        set(v) { data["extract_low"] = v.toDouble() }

    var extractHigh: Float
        get() = (data["extract_high"] as? Double)?.toFloat() ?: 0f
        set(v) { data["extract_high"] = v.toDouble() }

    var bloomWidth: Float
        get() = (data["bloom_width"] as? Double)?.toFloat() ?: 0f
        set(v) { data["bloom_width"] = v.toDouble() }

    var bloomStrength: Float
        get() = (data["bloom_strength"] as? Double)?.toFloat() ?: 0f
        set(v) { data["bloom_strength"] = v.toDouble() }

    var brightness: Float
        get() = (data["brightness"] as? Double)?.toFloat() ?: 0f
        set(v) { data["brightness"] = v.toDouble() }

    var contrast: Float
        get() = (data["contrast"] as? Double)?.toFloat() ?: 0f
        set(v) { data["contrast"] = v.toDouble() }

    var saturation: Float
        get() = (data["saturation"] as? Double)?.toFloat() ?: 0f
        set(v) { data["saturation"] = v.toDouble() }

    @Suppress("UNCHECKED_CAST")
    private fun contrastBase(): MutableList<Double> =
        data.getOrPut("contrast_base") { mutableListOf(1.0, 1.0, 1.0, 0.5) } as MutableList<Double>

    var contrastBaseR: Float
        get() = contrastBase().getOrElse(0) { 1.0 }.toFloat()
        set(v) { contrastBase()[0] = v.toDouble() }

    var contrastBaseG: Float
        get() = contrastBase().getOrElse(1) { 1.0 }.toFloat()
        set(v) { contrastBase()[1] = v.toDouble() }

    var contrastBaseB: Float
        get() = contrastBase().getOrElse(2) { 1.0 }.toFloat()
        set(v) { contrastBase()[2] = v.toDouble() }

    var contrastBaseIntensity: Float
        get() = contrastBase().getOrElse(3) { 0.5 }.toFloat()
        set(v) { contrastBase()[3] = v.toDouble() }

    var useNightVisionShader: Boolean
        get() = data["enable_night_vision"] as? Boolean ?: false
        set(v) { data["enable_night_vision"] = v }

    var useBloomShader: Boolean
        get() = data["enable_bloom"] as? Boolean ?: false
        set(v) { data["enable_bloom"] = v }

    var useColorFilter: Boolean
        get() = data["enable_color_filter"] as? Boolean ?: false
        set(v) { data["enable_color_filter"] = v }

    fun copyFrom(source: Map<String, Any>) {
        data.clear()
        data.putAll(source)
    }

    fun toMap(): Map<String, Any> = data.toMap()
}

enum class QuadType {
    NORMAL,
    NOISE,
    BLOOM_EXTRACT,
    BLOOM_COMBINE
}

class PostProcess {

    companion object {
        private const val NOISE_SIZE: UInt = 512u

        var instance: PostProcess? = null

        fun initClass() {
            if (instance != null) return
            instance = PostProcess()
        }

        fun cleanupClass() {
            instance?.invalidate()
            instance = null
        }
    }

    var initialized: Boolean = false
    val tweaks: PostProcessTweaks = PostProcessTweaks()
    val allEffects: MutableMap<String, Map<String, Any>> = mutableMapOf()

    private var sceneRenderTexture: UInt = 0u
    private var noiseTexture: UInt = 0u
    private var tempBloomTexture: UInt = 0u

    private var shaderErrorString: String = ""
    private var screenW: UInt = 1u
    private var screenH: UInt = 1u
    private var noiseTextureScale: Float = 1.0f

    private var selectedEffectName: String = ""

    private val nightVisionUniforms: MutableMap<String, UInt> = mutableMapOf()
    private val bloomExtractUniforms: MutableMap<String, UInt> = mutableMapOf()
    private val bloomBlurUniforms: MutableMap<String, UInt> = mutableMapOf()
    private val colorFilterUniforms: MutableMap<String, UInt> = mutableMapOf()

    fun apply(width: UInt, height: UInt) {
        if (!initialized || width != screenW || height != screenH) {
            initialize(width, height)
        }
        if (shadersEnabled()) doEffects()
    }

    fun invalidate() {
        sceneRenderTexture = 0u
        noiseTexture = 0u
        tempBloomTexture = 0u
        initialized = false
    }

    fun setSelectedEffect(effectName: String) {
        selectedEffectName = effectName
        val effect = allEffects[effectName] ?: return
        tweaks.copyFrom(effect)
    }

    fun getSelectedEffect(): String = selectedEffectName

    fun saveEffect(effectName: String) {
        allEffects[effectName] = tweaks.toMap()
        // Persisting to disk is a platform operation; omitted here.
    }

    private fun initialize(width: UInt, height: UInt) {
        screenW = width
        screenH = height
        sceneRenderTexture = createTexture(screenW, screenH)
        initialized = true

        checkError()
        createNightVisionShader()
        createBloomShader()
        createColorFilterShader()
        checkError()
    }

    private fun shadersEnabled(): Boolean =
        tweaks.useColorFilter || tweaks.useNightVisionShader || tweaks.useBloomShader

    private fun applyShaders() {
        if (tweaks.useColorFilter) {
            applyColorFilterShader()
            checkError()
        }
        if (tweaks.useNightVisionShader) {
            if (tweaks.useColorFilter) copyFrameBuffer(sceneRenderTexture, screenW, screenH)
            applyNightVisionShader()
            checkError()
        }
        if (tweaks.useBloomShader) {
            if (tweaks.useColorFilter || tweaks.useNightVisionShader) {
                copyFrameBuffer(sceneRenderTexture, screenW, screenH)
            }
            applyBloomShader()
            checkError()
        }
    }

    private fun doEffects() {
        // no-op
    }

    private fun applyColorFilterShader() {
        // no-op
    }

    private fun createColorFilterShader() {
        colorFilterUniforms["RenderTexture"] = 0u
        colorFilterUniforms["brightness"] = 0u
        colorFilterUniforms["contrast"] = 0u
        colorFilterUniforms["contrastBase"] = 0u
        colorFilterUniforms["saturation"] = 0u
        colorFilterUniforms["lumWeights"] = 0u
    }

    private fun applyNightVisionShader() {
        // no-op
    }

    private fun createNightVisionShader() {
        nightVisionUniforms["RenderTexture"] = 0u
        nightVisionUniforms["NoiseTexture"] = 0u
        nightVisionUniforms["brightMult"] = 0u
        nightVisionUniforms["noiseStrength"] = 0u
        nightVisionUniforms["lumWeights"] = 0u
        noiseTexture = createNoiseTexture()
    }

    private fun applyBloomShader() {
        // no-op
    }

    private fun createBloomShader() {
        tempBloomTexture = createTexture(screenW / 2u, screenH / 2u)

        bloomExtractUniforms["RenderTexture"] = 0u
        bloomExtractUniforms["extractLow"] = 0u
        bloomExtractUniforms["extractHigh"] = 0u
        bloomExtractUniforms["lumWeights"] = 0u

        bloomBlurUniforms["RenderTexture"] = 0u
        bloomBlurUniforms["bloomStrength"] = 0u
        bloomBlurUniforms["texelSize"] = 0u
        bloomBlurUniforms["blurDirection"] = 0u
        bloomBlurUniforms["blurWidth"] = 0u
    }

    private fun getShaderUniforms(uniforms: MutableMap<String, UInt>, prog: UInt) {
        for (key in uniforms.keys) {
            // no-op
        }
    }

    private fun createTexture(width: UInt, height: UInt): UInt {
        // no-op
        return 0u
    }

    private fun createNoiseTexture(): UInt {
        val buffer = ByteArray((NOISE_SIZE * NOISE_SIZE).toInt()) { Random.nextInt(256).toByte() }
        // no-op
        return 0u
    }

    private fun copyFrameBuffer(texture: UInt, width: UInt, height: UInt) {
        // no-op
    }

    private fun drawOrthoQuad(width: UInt, height: UInt, type: QuadType) {
        // no-op
    }

    private fun viewOrthogonal(width: UInt, height: UInt) {
        // no-op
    }

    private fun viewPerspective() {
        // no-op
    }

    private fun changeOrthogonal(width: UInt, height: UInt) {
        viewPerspective()
        viewOrthogonal(width, height)
    }

    private fun checkError(): Boolean {
        // no-op
        return false
    }

    private fun checkShaderError(shader: UInt) {
        // no-op
    }
}
