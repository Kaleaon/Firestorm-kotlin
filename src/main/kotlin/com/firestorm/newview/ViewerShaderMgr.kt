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
        TODO("GPU: init attribs and uniforms")
    }

    fun setShaders() {
        if (!sInitialized || sSkipReload) return
        TODO("GPU: detect feature classes, populate shaderLevel[], call load* functions")
    }

    fun unloadShaders() {
        ShaderClass.entries.indices.forEach { shaderLevel[it] = 0 }
        maxAvatarShaderLevel = 0
        TODO("GPU: unload all GLSL shader programs")
    }

    fun loadBasicShaders(): String {
        TODO("GPU: compile and link foundational GLSL shader modules; return empty string on success or failing filename")
    }

    fun loadShadersEffects(): Boolean {
        TODO("GPU: load post-process/effect shaders (glow, FXAA, SMAA, CAS, DoF)")
    }

    fun loadShadersDeferred(): Boolean {
        TODO("GPU: load deferred-rendering G-buffer and lighting shaders")
    }

    fun loadShadersObject(): Boolean {
        TODO("GPU: load object/terrain/PBR shaders")
    }

    fun loadShadersAvatar(): Boolean {
        TODO("GPU: load avatar/impostor shaders")
    }

    fun loadShadersWater(): Boolean {
        TODO("GPU: load water/underwater shaders")
    }

    fun loadShadersInterface(): Boolean {
        TODO("GPU: load UI/highlight/pathfinding shaders")
    }

    fun finalizeShaderList() {
        TODO("GPU: collect all active shaders into shaderList for uniform propagation")
    }

    fun updateShaderUniforms(shader: Shader) {
        TODO("GPU: push environment, lighting, and windlight uniforms to shader")
    }

    fun getShaderDirPrefix(): String = "shaders/class"
}
