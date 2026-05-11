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
        val gl = GpuBackend.current
        copyFrameBuffer(sceneRenderTexture, screenW, screenH)
        gl.clear(GL.COLOR_BUFFER_BIT or GL.DEPTH_BUFFER_BIT)
        viewOrthogonal(screenW, screenH)
        applyShaders()
        GLSLShader.unbind()
        viewPerspective()
    }

    private fun applyColorFilterShader() {
        val gl = GpuBackend.current
        gl.useProgram(colorFilterUniforms["program"]?.toInt() ?: 0)
        gl.uniform1f(colorFilterUniforms["brightness"]?.toInt() ?: -1, tweaks.brightness)
        gl.uniform1f(colorFilterUniforms["contrast"]?.toInt() ?: -1, tweaks.contrast)
        gl.uniform4f(colorFilterUniforms["contrastBase"]?.toInt() ?: -1,
            tweaks.contrastBaseR, tweaks.contrastBaseG, tweaks.contrastBaseB, tweaks.contrastBaseIntensity)
        gl.uniform1f(colorFilterUniforms["saturation"]?.toInt() ?: -1, tweaks.saturation)
        gl.uniform3f(colorFilterUniforms["lumWeights"]?.toInt() ?: -1, 0.299f, 0.587f, 0.114f)
        drawOrthoQuad(screenW, screenH, QuadType.NORMAL)
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
        val gl = GpuBackend.current
        gl.useProgram(nightVisionUniforms["program"]?.toInt() ?: 0)
        gl.uniform1f(nightVisionUniforms["brightMult"]?.toInt() ?: -1, tweaks.brightMult)
        gl.uniform1f(nightVisionUniforms["noiseStrength"]?.toInt() ?: -1, tweaks.noiseStrength)
        gl.uniform3f(nightVisionUniforms["lumWeights"]?.toInt() ?: -1, 0.299f, 0.587f, 0.114f)
        gl.activeTexture(GL.TEXTURE0 + 1)
        gl.bindTexture(GL.TEXTURE_2D, noiseTexture.toInt())
        gl.uniform1i(nightVisionUniforms["NoiseTexture"]?.toInt() ?: -1, 1)
        drawOrthoQuad(screenW, screenH, QuadType.NOISE)
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
        val gl = GpuBackend.current
        // Extract pass: read the scene texture, write bright pixels to the
        // half-resolution bloom target.
        gl.useProgram(bloomExtractUniforms["program"]?.toInt() ?: 0)
        gl.uniform1f(bloomExtractUniforms["extractLow"]?.toInt() ?: -1, tweaks.extractLow)
        gl.uniform1f(bloomExtractUniforms["extractHigh"]?.toInt() ?: -1, tweaks.extractHigh)
        gl.uniform3f(bloomExtractUniforms["lumWeights"]?.toInt() ?: -1, 0.299f, 0.587f, 0.114f)
        drawOrthoQuad(screenW / 2u, screenH / 2u, QuadType.BLOOM_EXTRACT)
        // Two-pass separable Gaussian blur, then combine back into the main
        // framebuffer.
        gl.useProgram(bloomBlurUniforms["program"]?.toInt() ?: 0)
        gl.uniform1f(bloomBlurUniforms["bloomStrength"]?.toInt() ?: -1, tweaks.bloomStrength)
        gl.uniform1f(bloomBlurUniforms["blurWidth"]?.toInt() ?: -1, tweaks.bloomWidth)
        gl.uniform2f(bloomBlurUniforms["blurDirection"]?.toInt() ?: -1, 1f, 0f)
        gl.uniform2f(bloomBlurUniforms["texelSize"]?.toInt() ?: -1,
            1f / screenW.toFloat(), 1f / screenH.toFloat())
        drawOrthoQuad(screenW / 2u, screenH / 2u, QuadType.BLOOM_COMBINE)
        gl.uniform2f(bloomBlurUniforms["blurDirection"]?.toInt() ?: -1, 0f, 1f)
        drawOrthoQuad(screenW, screenH, QuadType.BLOOM_COMBINE)
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
        val gl = GpuBackend.current
        uniforms["program"] = prog
        for (key in uniforms.keys.toList()) {
            if (key == "program") continue
            uniforms[key] = gl.getUniformLocation(prog.toInt(), key).toUInt()
        }
    }

    private fun createTexture(width: UInt, height: UInt): UInt {
        val gl = GpuBackend.current
        val name = gl.genTextures(1)[0]
        gl.bindTexture(GL.TEXTURE_2D, name)
        gl.texImage2D(GL.TEXTURE_2D, 0, GL.RGBA8, width.toInt(), height.toInt(), GL.RGBA, GL.UNSIGNED_BYTE, null)
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.LINEAR)
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.LINEAR)
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE)
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE)
        return name.toUInt()
    }

    private fun createNoiseTexture(): UInt {
        val buffer = ByteArray((NOISE_SIZE * NOISE_SIZE).toInt()) { Random.nextInt(256).toByte() }
        val gl = GpuBackend.current
        val name = gl.genTextures(1)[0]
        gl.bindTexture(GL.TEXTURE_2D, name)
        // GLES 3 dropped GL_LUMINANCE — use ALPHA which maps cleanly to a single
        // grayscale channel that the night-vision shader samples.
        gl.texImage2D(GL.TEXTURE_2D, 0, GL.ALPHA, NOISE_SIZE.toInt(), NOISE_SIZE.toInt(), GL.ALPHA, GL.UNSIGNED_BYTE, buffer)
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.LINEAR)
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.LINEAR)
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_S, GL.REPEAT)
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_T, GL.REPEAT)
        return name.toUInt()
    }

    private fun copyFrameBuffer(texture: UInt, width: UInt, height: UInt) {
        val gl = GpuBackend.current
        gl.activeTexture(GL.TEXTURE0)
        gl.bindTexture(GL.TEXTURE_2D, texture.toInt())
        gl.copyTexImage2D(GL.TEXTURE_2D, 0, GL.RGBA8, 0, 0, width.toInt(), height.toInt(), 0)
    }

    private fun drawOrthoQuad(width: UInt, height: UInt, type: QuadType) {
        // The renderer doesn't yet have a generic immediate-mode quad path
        // for ES 3.2 — fullscreen-quad geometry is supplied by the caller's
        // bound vertex array. This method only updates viewport / blend so the
        // higher-level Pipeline can issue the actual `drawArrays`.
        val gl = GpuBackend.current
        gl.viewport(0, 0, width.toInt(), height.toInt())
        when (type) {
            QuadType.NORMAL, QuadType.NOISE -> gl.blendFunc(GL.ONE, GL.ZERO)
            QuadType.BLOOM_EXTRACT -> gl.blendFunc(GL.ONE, GL.ZERO)
            QuadType.BLOOM_COMBINE -> gl.blendFunc(GL.ONE, GL.ONE)
        }
    }

    private fun viewOrthogonal(width: UInt, height: UInt) {
        orthoMatrix = ortho2D(0f, width.toFloat(), height.toFloat(), 0f, -1f, 1f)
    }

    private fun viewPerspective() {
        orthoMatrix = null
    }

    private fun changeOrthogonal(width: UInt, height: UInt) {
        viewPerspective()
        viewOrthogonal(width, height)
    }

    private fun checkError(): Boolean {
        val gl = GpuBackend.current
        var err = gl.getError()
        var any = false
        while (err != GL.NO_ERROR) {
            shaderErrorString += "glGetError = 0x${err.toString(16)}\n"
            any = true
            err = gl.getError()
        }
        return any
    }

    private fun checkShaderError(shader: UInt) {
        val log = GpuBackend.current.getShaderInfoLog(shader.toInt())
        if (log.isNotBlank()) shaderErrorString += log + "\n"
    }

    /** Active orthographic projection, exposed so shader uniform binders can read it. */
    var orthoMatrix: FloatArray? = null
        private set

    private fun ortho2D(l: Float, r: Float, b: Float, t: Float, n: Float, f: Float): FloatArray {
        val m = FloatArray(16)
        m[0] = 2f / (r - l); m[5] = 2f / (t - b); m[10] = -2f / (f - n); m[15] = 1f
        m[12] = -(r + l) / (r - l)
        m[13] = -(t + b) / (t - b)
        m[14] = -(f + n) / (f - n)
        return m
    }
}
