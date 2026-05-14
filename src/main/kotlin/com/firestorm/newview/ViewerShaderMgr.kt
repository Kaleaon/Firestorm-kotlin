package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import com.firestorm.llrender.Shader

enum class ShaderClass {
    LIGHTING,
    OBJECT,
    AVATAR,
    ENVIRONMENT,
    INTERFACE,
    EFFECT,
    WINDLIGHT,
    WATER,
    DEFERRED;

    companion object {
        val COUNT = entries.size
    }
}

enum class NormalDebugShaderVariant { DEFAULT, WITH_TANGENTS }

enum class TerrainPBRDetail(val level: Int) {
    EMISSIVE(0),
    OCCLUSION(-1),
    NORMAL(-2),
    METALLIC_ROUGHNESS(-3),
    BASE_COLOR(-4);

    companion object {
        val MAX = EMISSIVE.level
        val MIN = BASE_COLOR.level
    }
}

enum class TerrainPaintType(val id: UInt) {
    HEIGHTMAP_WITH_NOISE(0u),
    PBR_PAINTMAP(1u);

    companion object { val COUNT = entries.size }
}

object ViewerShaderMgr {

    companion object {
        const val DEFERRED_MULTI_LIGHT_COUNT = 16

        var sInitialized: Boolean = false
        var sSkipReload: Boolean = false
    }

    val shaderLevel: MutableList<Int> = MutableList(ShaderClass.COUNT) { 0 }
    var maxAvatarShaderLevel: Int = 0

    val shinyOrigin: Vector4 = Vector4()

    val occlusionProgram        = Shader("gOcclusionProgram")
    val occlusionCubeProgram    = Shader("gOcclusionCubeProgram")
    val glowCombineProgram      = Shader("gGlowCombineProgram")
    val reflectionMipProgram    = Shader("gReflectionMipProgram")
    val gaussianProgram         = Shader("gGaussianProgram")
    val radianceGenProgram      = Shader("gRadianceGenProgram")
    val heroRadianceGenProgram  = Shader("gHeroRadianceGenProgram")
    val irradianceGenProgram    = Shader("gIrradianceGenProgram")
    val glowCombineFXAAProgram  = Shader("gGlowCombineFXAAProgram")
    val debugProgram            = Shader("gDebugProgram")
    val clipProgram             = Shader("gClipProgram")
    val benchmarkProgram        = Shader("gBenchmarkProgram")
    val copyProgram             = Shader("gCopyProgram")
    val copyDepthProgram        = Shader("gCopyDepthProgram")
    val drawColorProgram        = Shader("gDrawColorProgram")
    val pbrTerrainBakeProgram   = Shader("gPBRTerrainBakeProgram")
    val twoTextureCompareProgram = Shader("gTwoTextureCompareProgram")
    val oneTextureFilterProgram  = Shader("gOneTextureFilterProgram")

    val normalDebugProgram      = Array(NormalDebugShaderVariant.entries.size) { Shader("gNormalDebugProgram[$it]") }

    val waterProgram            = Shader("gWaterProgram")
    val underWaterProgram       = Shader("gUnderWaterProgram")
    val glowProgram             = Shader("gGlowProgram")
    val glowExtractProgram      = Shader("gGlowExtractProgram")

    val highlightProgram        = Shader("gHighlightProgram")
    val highlightNormalProgram  = Shader("gHighlightNormalProgram")
    val highlightSpecularProgram = Shader("gHighlightSpecularProgram")
    val deferredHighlightProgram = Shader("gDeferredHighlightProgram")

    val avatarProgram           = Shader("gAvatarProgram")
    val avatarEyeballProgram    = Shader("gAvatarEyeballProgram")
    val impostorProgram         = Shader("gImpostorProgram")

    val deferredWLSkyProgram    = Shader("gDeferredWLSkyProgram")
    val deferredWLCloudProgram  = Shader("gDeferredWLCloudProgram")
    val deferredWLSunProgram    = Shader("gDeferredWLSunProgram")
    val deferredWLMoonProgram   = Shader("gDeferredWLMoonProgram")
    val deferredStarProgram     = Shader("gDeferredStarProgram")
    val deferredSunProgram      = Shader("gDeferredSunProgram")
    val deferredSoftenProgram   = Shader("gDeferredSoftenProgram")
    val deferredShadowProgram   = Shader("gDeferredShadowProgram")
    val deferredBlurLightProgram = Shader("gDeferredBlurLightProgram")
    val deferredMultiLightProgram = Array(DEFERRED_MULTI_LIGHT_COUNT) { Shader("gDeferredMultiLightProgram[$it]") }

    val fxaaProgram             = Array(4) { Shader("gFXAAProgram[$it]") }
    val smaaEdgeDetectProgram   = Array(4) { Shader("gSMAAEdgeDetectProgram[$it]") }
    val smaaBlendWeightsProgram = Array(4) { Shader("gSMAABlendWeightsProgram[$it]") }
    val smaaNeighborhoodBlendProgram = Array(4) { Shader("gSMAANeighborhoodBlendProgram[$it]") }

    val deferredPBROpaqueProgram = Shader("gDeferredPBROpaqueProgram")
    val deferredPBRAlphaProgram  = Shader("gDeferredPBRAlphaProgram")
    val deferredPBRTerrainProgram = Array(TerrainPaintType.COUNT) { Shader("gDeferredPBRTerrainProgram[$it]") }
    val gltfPBRMetallicRoughnessProgram = Shader("gGLTFPBRMetallicRoughnessProgram")
    val rlvSphereProgram        = Shader("gRlvSphereProgram")

    private val shaderList: MutableList<Shader> = mutableListOf()

    fun getShaderLevel(type: ShaderClass): Int = shaderLevel[type.ordinal]

    fun initAttribsAndUniforms() {
        // no-op
    }

    fun setShaders() {
        if (!sInitialized || sSkipReload) return
        // no-op
    }

    fun unloadShaders() {
        ShaderClass.entries.indices.forEach { shaderLevel[it] = 0 }
        maxAvatarShaderLevel = 0
        // no-op
    }

    fun loadBasicShaders(): String {
        // no-op
        return ""
    }

    fun loadShadersEffects(): Boolean {
        // no-op
        return false
    }

    fun loadShadersDeferred(): Boolean {
        // no-op
        return false
    }

    fun loadShadersObject(): Boolean {
        // no-op
        return false
    }

    fun loadShadersAvatar(): Boolean {
        // no-op
        return false
    }

    fun loadShadersWater(): Boolean {
        // no-op
        return false
    }

    fun loadShadersInterface(): Boolean {
        // no-op
        return false
    }

    fun finalizeShaderList() {
        // no-op
    }

    fun updateShaderUniforms(shader: Shader) {
        // no-op
    }

    fun getShaderDirPrefix(): String = "shaders/class"
}
