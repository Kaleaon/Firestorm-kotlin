package com.firestorm.llrender

abstract class ShaderMgr {

    enum class GlslReservedUniform {
        MODELVIEW_MATRIX,
        PROJECTION_MATRIX,
        INVERSE_PROJECTION_MATRIX,
        MODELVIEW_PROJECTION_MATRIX,
        INVERSE_MODELVIEW_MATRIX,
        IDENTITY_MATRIX,
        NORMAL_MATRIX,
        TEXTURE_MATRIX0,
        TEXTURE_MATRIX1,
        TEXTURE_MATRIX2,
        TEXTURE_MATRIX3,
        OBJECT_PLANE_S,
        OBJECT_PLANE_T,
        TEXTURE_BASE_COLOR_TRANSFORM,
        TEXTURE_NORMAL_TRANSFORM,
        TEXTURE_METALLIC_ROUGHNESS_TRANSFORM,
        TEXTURE_OCCLUSION_TRANSFORM,
        TEXTURE_EMISSIVE_TRANSFORM,
        BASE_COLOR_TEXCOORD,
        EMISSIVE_TEXCOORD,
        NORMAL_TEXCOORD,
        METALLIC_ROUGHNESS_TEXCOORD,
        OCCLUSION_TEXCOORD,
        GLTF_NODE_ID,
        GLTF_MATERIAL_ID,
        TERRAIN_TEXTURE_TRANSFORMS,
        VIEWPORT,
        LIGHT_POSITION,
        LIGHT_DIRECTION,
        LIGHT_ATTENUATION,
        LIGHT_DEFERRED_ATTENUATION,
        LIGHT_DIFFUSE,
        LIGHT_AMBIENT,
        MULTI_LIGHT_COUNT,
        MULTI_LIGHT,
        MULTI_LIGHT_COL,
        MULTI_LIGHT_FAR_Z,
        PROJECTOR_MATRIX,
        PROJECTOR_NEAR,
        PROJECTOR_P,
        PROJECTOR_N,
        PROJECTOR_ORIGIN,
        PROJECTOR_RANGE,
        PROJECTOR_AMBIANCE,
        PROJECTOR_SHADOW_INDEX,
        PROJECTOR_SHADOW_FADE,
        PROJECTOR_FOCUS,
        PROJECTOR_LOD,
        PROJECTOR_AMBIENT_LOD,
        DIFFUSE_COLOR,
        EMISSIVE_COLOR,
        METALLIC_FACTOR,
        ROUGHNESS_FACTOR,
        MIRROR_FLAG,
        CLIP_PLANE,
        CLIP_SIGN,
        DIFFUSE_MAP,
        ALTERNATE_DIFFUSE_MAP,
        SPECULAR_MAP,
        METALLIC_ROUGHNESS_MAP,
        NORMAL_MAP,
        OCCLUSION_MAP,
        EMISSIVE_MAP,
        BUMP_MAP,
        BUMP_MAP2,
        ENVIRONMENT_MAP,
        SCENE_MAP,
        SCENE_DEPTH,
        REFLECTION_PROBES,
        IRRADIANCE_PROBES,
        HERO_PROBE,
        CLOUD_NOISE_MAP,
        CLOUD_NOISE_MAP_NEXT,
        LIGHTNORM,
        SUNLIGHT_COLOR,
        AMBIENT,
        SKY_HDR_SCALE,
        SKY_SUNLIGHT_SCALE,
        SKY_AMBIENT_SCALE,
        CLASSIC_MODE,
        BLUE_HORIZON,
        BLUE_DENSITY,
        HAZE_HORIZON,
        HAZE_DENSITY,
        CLOUD_SHADOW,
        DENSITY_MULTIPLIER,
        DISTANCE_MULTIPLIER,
        MAX_Y,
        GLOW,
        CLOUD_COLOR,
        CLOUD_POS_DENSITY1,
        CLOUD_POS_DENSITY2,
        CLOUD_SCALE,
        GAMMA,
        SCENE_LIGHT_STRENGTH,
        LIGHT_CENTER,
        LIGHT_SIZE,
        LIGHT_FALLOFF,
        BOX_CENTER,
        BOX_SIZE,
        GLOW_MIN_LUMINANCE,
        GLOW_MAX_EXTRACT_ALPHA,
        GLOW_LUM_WEIGHTS,
        GLOW_WARMTH_WEIGHTS,
        GLOW_WARMTH_AMOUNT,
        GLOW_STRENGTH,
        GLOW_DELTA,
        GLOW_NOISE_MAP,
        MINIMUM_ALPHA,
        EMISSIVE_BRIGHTNESS,
        DEFERRED_SHADOW_MATRIX,
        DEFERRED_ENV_MAT,
        DEFERRED_SHADOW_CLIP,
        DEFERRED_SUN_WASH,
        DEFERRED_SHADOW_NOISE,
        DEFERRED_BLUR_SIZE,
        DEFERRED_SSAO_RADIUS,
        DEFERRED_SSAO_MAX_RADIUS,
        DEFERRED_SSAO_FACTOR,
        DEFERRED_SSAO_FACTOR_INV,
        DEFERRED_SSAO_EFFECT_MAT,
        DEFERRED_SCREEN_RES,
        DEFERRED_NEAR_CLIP,
        DEFERRED_SHADOW_OFFSET,
        DEFERRED_SHADOW_BIAS,
        DEFERRED_SPOT_SHADOW_BIAS,
        DEFERRED_SPOT_SHADOW_OFFSET,
        DEFERRED_SUN_DIR,
        DEFERRED_MOON_DIR,
        DEFERRED_SHADOW_RES,
        DEFERRED_PROJ_SHADOW_RES,
        DEFERRED_DEPTH_CUTOFF,
        DEFERRED_NORM_CUTOFF,
        DEFERRED_SHADOW_TARGET_WIDTH,
        DEFERRED_SSR_ITR_COUNT,
        DEFERRED_SSR_RAY_STEP,
        DEFERRED_SSR_DIST_BIAS,
        DEFERRED_SSR_REJECT_BIAS,
        DEFERRED_SSR_GLOSSY_SAMPLES,
        DEFERRED_SSR_NOISE_SINE,
        DEFERRED_SSR_ADAPTIVE_STEP_MULT,
        MODELVIEW_DELTA_MATRIX,
        INVERSE_MODELVIEW_DELTA_MATRIX,
        CUBE_SNAPSHOT,
        FXAA_TC_SCALE,
        FXAA_RCP_SCREEN_RES,
        FXAA_RCP_FRAME_OPT,
        FXAA_RCP_FRAME_OPT2,
        DOF_FOCAL_DISTANCE,
        DOF_BLUR_CONSTANT,
        DOF_TAN_PIXEL_ANGLE,
        DOF_MAGNIFICATION,
        DOF_MAX_COF,
        DOF_RES_SCALE,
        DOF_WIDTH,
        DOF_HEIGHT,
        DEFERRED_DEPTH,
        DEFERRED_SHADOW0,
        DEFERRED_SHADOW1,
        DEFERRED_SHADOW2,
        DEFERRED_SHADOW3,
        DEFERRED_SHADOW4,
        DEFERRED_SHADOW5,
        DEFERRED_POSITION,
        DEFERRED_DIFFUSE,
        DEFERRED_SPECULAR,
        DEFERRED_EMISSIVE,
        EXPOSURE_MAP,
        DEFERRED_BRDF_LUT,
        DEFERRED_NOISE,
        DEFERRED_LIGHTFUNC,
        DEFERRED_LIGHT,
        DEFERRED_BLOOM,
        DEFERRED_PROJECTION,
        DEFERRED_NORM_MATRIX,
        SPECULAR_COLOR,
        ENVIRONMENT_INTENSITY,
        AVATAR_MATRIX,
        AVATAR_TRANSLATION,
        RENDER_VIGNETTE,
        WATER_SCREENTEX,
        WATER_SCREENDEPTH,
        WATER_REFTEX,
        WATER_EXCLUSIONTEX,
        WATER_EYEVEC,
        WATER_TIME,
        WATER_WAVE_DIR1,
        WATER_WAVE_DIR2,
        WATER_LIGHT_DIR,
        WATER_SPECULAR,
        WATER_SPECULAR_EXP,
        WATER_FOGCOLOR,
        WATER_FOGCOLOR_LINEAR,
        WATER_FOGDENSITY,
        WATER_FOGKS,
        WATER_REFSCALE,
        WATER_WATERHEIGHT,
        WATER_WATERPLANE,
        WATER_NORM_SCALE,
        WATER_FRESNEL_SCALE,
        WATER_FRESNEL_OFFSET,
        WATER_BLUR_MULTIPLIER,
        WATER_SUN_ANGLE,
        WATER_SCALED_ANGLE,
        WATER_SUN_ANGLE2,
        WL_CAMPOSLOCAL,
        RLV_EFFECT_MODE,
        RLV_EFFECT_PARAM1,
        RLV_EFFECT_PARAM2,
        RLV_EFFECT_PARAM3,
        RLV_EFFECT_PARAM4,
        RLV_EFFECT_PARAM5,
        AVATAR_WIND,
        AVATAR_SINWAVE,
        AVATAR_GRAVITY,
        TERRAIN_DETAIL0,
        TERRAIN_DETAIL1,
        TERRAIN_DETAIL2,
        TERRAIN_DETAIL3,
        TERRAIN_ALPHARAMP,
        TERRAIN_PAINTMAP,
        TERRAIN_DETAIL0_BASE_COLOR,
        TERRAIN_DETAIL1_BASE_COLOR,
        TERRAIN_DETAIL2_BASE_COLOR,
        TERRAIN_DETAIL3_BASE_COLOR,
        TERRAIN_DETAIL0_NORMAL,
        TERRAIN_DETAIL1_NORMAL,
        TERRAIN_DETAIL2_NORMAL,
        TERRAIN_DETAIL3_NORMAL,
        TERRAIN_DETAIL0_METALLIC_ROUGHNESS,
        TERRAIN_DETAIL1_METALLIC_ROUGHNESS,
        TERRAIN_DETAIL2_METALLIC_ROUGHNESS,
        TERRAIN_DETAIL3_METALLIC_ROUGHNESS,
        TERRAIN_DETAIL0_EMISSIVE,
        TERRAIN_DETAIL1_EMISSIVE,
        TERRAIN_DETAIL2_EMISSIVE,
        TERRAIN_DETAIL3_EMISSIVE,
        TERRAIN_BASE_COLOR_FACTORS,
        TERRAIN_METALLIC_FACTORS,
        TERRAIN_ROUGHNESS_FACTORS,
        TERRAIN_EMISSIVE_COLORS,
        TERRAIN_MINIMUM_ALPHAS,
        REGION_SCALE,
        SHINY_ORIGIN,
        DISPLAY_GAMMA,
        INSCATTER_RT,
        SUN_SIZE,
        FOG_COLOR,
        TRANSMITTANCE_TEX,
        SCATTER_TEX,
        SINGLE_MIE_SCATTER_TEX,
        ILLUMINANCE_TEX,
        BLEND_FACTOR,
        MOISTURE_LEVEL,
        DROPLET_RADIUS,
        ICE_LEVEL,
        RAINBOW_MAP,
        HALO_MAP,
        MOON_BRIGHTNESS,
        CLOUD_VARIANCE,
        REFLECTION_PROBE_AMBIANCE,
        REFLECTION_PROBE_MAX_LOD,
        REFLECTION_PROBE_STRENGTH,
        SH_INPUT_L1R,
        SH_INPUT_L1G,
        SH_INPUT_L1B,
        SUN_MOON_GLOW_FACTOR,
        WATER_EDGE_FACTOR,
        SUN_UP_FACTOR,
        MOONLIGHT_COLOR,
        DEBUG_NORMAL_DRAW_LENGTH,
        SMAA_EDGE_TEX,
        SMAA_AREA_TEX,
        SMAA_SEARCH_TEX,
        SMAA_BLEND_TEX,
        SNAPSHOT_BORDER_COLOR,
        SNAPSHOT_BORDER_THICKNESS,
        SNAPSHOT_FRAME_RECT,
        END_RESERVED_UNIFORMS
    }

    data class ProgramBinaryData(
        var binaryLength: Int = 0,
        var binaryFormat: UInt = 0u,
        var lastUsedTime: Float = 0f
    )

    val vertexShaderObjects: MutableMap<String, UInt> = mutableMapOf()
    val fragmentShaderObjects: MutableMap<String, UInt> = mutableMapOf()
    val reservedAttribs: MutableList<String> = mutableListOf()
    val reservedUniforms: MutableList<String> = mutableListOf()
    val shaderBinaryCache: MutableMap<String, ProgramBinaryData> = mutableMapOf()
    var shaderCacheVersion: String = ""
    var shaderCacheEnabled: Boolean = false
    var shaderCacheDir: String = ""

    abstract fun getShaderDirPrefix(): String

    abstract fun updateShaderUniforms(shader: GlslShader)

    open fun initAttribsAndUniforms() {
        reservedAttribs.addAll(listOf(
            "position", "normal", "texcoord0", "texcoord1", "texcoord2", "texcoord3",
            "diffuse_color", "emissive", "tangent", "weight", "weight4", "clothing",
            "joint", "texture_index"
        ))

        reservedUniforms.addAll(listOf(
            "modelview_matrix", "projection_matrix", "inv_proj", "modelview_projection_matrix",
            "inv_modelview", "identity_matrix", "normal_matrix",
            "texture_matrix0", "texture_matrix1", "texture_matrix2", "texture_matrix3",
            "object_plane_s", "object_plane_t",
            "texture_base_color_transform", "texture_normal_transform",
            "texture_metallic_roughness_transform", "texture_occlusion_transform",
            "texture_emissive_transform", "base_color_texcoord", "emissive_texcoord",
            "normal_texcoord", "metallic_roughness_texcoord", "occlusion_texcoord",
            "gltf_node_id", "gltf_material_id", "terrain_texture_transforms",
            "viewport",
            "light_position", "light_direction", "light_attenuation", "light_deferred_attenuation",
            "light_diffuse", "light_ambient", "light_count", "light", "light_col", "far_z",
            "proj_mat", "proj_near", "proj_p", "proj_n", "proj_origin", "proj_range",
            "proj_ambiance", "proj_shadow_idx", "shadow_fade", "proj_focus", "proj_lod",
            "proj_ambient_lod",
            "color", "emissiveColor", "metallicFactor", "roughnessFactor", "mirror_flag",
            "clipPlane", "clipSign",
            "diffuseMap", "altDiffuseMap", "specularMap", "metallicRoughnessMap", "normalMap",
            "occlusionMap", "emissiveMap", "bumpMap", "bumpMap2", "environmentMap",
            "sceneMap", "sceneDepth", "reflectionProbes", "irradianceProbes", "heroProbes",
            "cloud_noise_texture", "cloud_noise_texture_next",
            "lightnorm", "sunlight_color", "ambient_color", "sky_hdr_scale", "sky_sunlight_scale",
            "sky_ambient_scale", "classic_mode", "blue_horizon", "blue_density", "haze_horizon",
            "haze_density", "cloud_shadow", "density_multiplier", "distance_multiplier",
            "max_y", "glow", "cloud_color", "cloud_pos_density1", "cloud_pos_density2",
            "cloud_scale", "gamma", "scene_light_strength",
            "center", "size", "falloff", "box_center", "box_size",
            "minLuminance", "maxExtractAlpha", "lumWeights", "warmthWeights", "warmthAmount",
            "glowStrength", "glowDelta", "glowNoiseMap",
            "minimum_alpha", "emissive_brightness",
            "shadow_matrix", "env_mat", "shadow_clip", "sun_wash", "shadow_noise", "blur_size",
            "ssao_radius", "ssao_max_radius", "ssao_factor", "ssao_factor_inv", "ssao_effect_mat",
            "screen_res", "near_clip", "shadow_offset", "shadow_bias", "spot_shadow_bias",
            "spot_shadow_offset", "sun_dir", "moon_dir", "shadow_res", "proj_shadow_res",
            "depth_cutoff", "norm_cutoff", "shadow_target_width",
            "iterationCount", "rayStep", "distanceBias", "depthRejectBias", "glossySampleCount",
            "noiseSine", "adaptiveStepMultiplier",
            "modelview_delta", "inv_modelview_delta", "cube_snapshot",
            "tc_scale", "rcp_screen_res", "rcp_frame_opt", "rcp_frame_opt2",
            "focal_distance", "blur_constant", "tan_pixel_angle", "magnification",
            "max_cof", "res_scale", "dof_width", "dof_height",
            "depthMap", "shadowMap0", "shadowMap1", "shadowMap2", "shadowMap3", "shadowMap4", "shadowMap5",
            "positionMap", "diffuseRect", "specularRect", "emissiveRect", "exposureMap",
            "brdfLut", "noiseMap", "lightFunc", "lightMap", "bloomMap", "projectionMap", "norm_mat",
            "specular_color", "env_intensity",
            "matrixPalette", "translationPalette",
            "vignette",
            "screenTex", "screenDepth", "refTex", "exclusionTex", "eyeVec", "time",
            "waveDir1", "waveDir2", "lightDir", "specular", "lightExp",
            "waterFogColor", "waterFogColorLinear", "waterFogDensity", "waterFogKS",
            "refScale", "waterHeight", "waterPlane", "normScale", "fresnelScale", "fresnelOffset",
            "blurMultiplier", "sunAngle", "scaledAngle", "sunAngle2",
            "camPosLocal",
            "rlvEffectMode", "rlvEffectParam1", "rlvEffectParam2", "rlvEffectParam3",
            "rlvEffectParam4", "rlvEffectParam5",
            "gWindDir", "gSinWaveParams", "gGravity",
            "detail_0", "detail_1", "detail_2", "detail_3",
            "alpha_ramp", "paint_map",
            "detail_0_base_color", "detail_1_base_color", "detail_2_base_color", "detail_3_base_color",
            "detail_0_normal", "detail_1_normal", "detail_2_normal", "detail_3_normal",
            "detail_0_metallic_roughness", "detail_1_metallic_roughness",
            "detail_2_metallic_roughness", "detail_3_metallic_roughness",
            "detail_0_emissive", "detail_1_emissive", "detail_2_emissive", "detail_3_emissive",
            "baseColorFactors", "metallicFactors", "roughnessFactors", "emissiveColors", "minimum_alphas",
            "region_scale",
            "origin", "display_gamma",
            "inscatter", "sun_size", "fog_color",
            "transmittance_texture", "scattering_texture", "single_mie_scattering_texture",
            "irradiance_texture", "blend_factor",
            "moisture_level", "droplet_radius", "ice_level", "rainbow_map", "halo_map",
            "moon_brightness", "cloud_variance",
            "reflection_probe_ambiance", "max_probe_lod", "probe_strength",
            "sh_input_r", "sh_input_g", "sh_input_b",
            "sun_moon_glow_factor", "water_edge", "sun_up_factor", "moonlight_color",
            "debug_normal_draw_length",
            "edgesTex", "areaTex", "searchTex", "blendTex",
            "border_color", "border_thickness", "frame_rect"
        ))

        require(reservedUniforms.size == GlslReservedUniform.END_RESERVED_UNIFORMS.ordinal) {
            "reservedUniforms size ${reservedUniforms.size} does not match END_RESERVED_UNIFORMS ${GlslReservedUniform.END_RESERVED_UNIFORMS.ordinal}"
        }

        val dupeCheck = mutableSetOf<String>()
        for (uniform in reservedUniforms) {
            require(dupeCheck.add(uniform)) { "Duplicate reserved uniform name: $uniform" }
        }
    }

    fun attachShaderFeatures(shader: GlslShader): Boolean {
        // no-op
        return false
    }

    fun dumpObjectLog(obj: UInt, warns: Boolean = true, filename: String = "") {
        // no-op
    }

    fun dumpShaderSource(shaderCodeCount: UInt, shaderCodeText: Array<String>) {
        // no-op
    }

    fun linkProgramObject(obj: UInt, suppressErrors: Boolean = false): Boolean {
        // no-op
        return false
    }

    fun validateProgramObject(obj: UInt): Boolean {
        // no-op
        return false
    }

    fun loadShaderFile(
        filename: String,
        shaderLevel: Int,
        type: ShaderType,
        defines: MutableMap<String, String>? = null,
        textureIndexChannels: Int = -1
    ): Pair<UInt, Int> {
        // no-op
        return Pair(0u, 0)
    }

    fun initShaderCache(enabled: Boolean, oldCacheVersion: String, currentCacheVersion: String, secondInstance: Boolean) {
        System.err.println("ShaderMgr: initShaderCache not yet implemented")
    }

    fun clearShaderCache() {
        System.err.println("ShaderMgr: clearShaderCache not yet implemented")
    }

    fun persistShaderCacheMetadata() {
        if (!shaderCacheEnabled) return
        System.err.println("ShaderMgr: persistShaderCacheMetadata not yet implemented")
    }

    fun loadCachedProgramBinary(shader: GlslShader): Boolean {
        if (!shaderCacheEnabled) return false
        // no-op
        return false
    }

    fun saveCachedProgramBinary(shader: GlslShader): Boolean {
        if (!shaderCacheEnabled) return true
        // no-op
        return false
    }

    enum class ShaderType { VERTEX, FRAGMENT, GEOMETRY }

    companion object {
        var instance: ShaderMgr? = null
            get() = checkNotNull(field) { "ShaderMgr must be instantiated by the application before use" }
    }
}

class GlslShader {
    var name: String = ""
    var shaderHash: String = ""
    var programObject: UInt = 0u
    var riggedVariant: GlslShader? = null
    val features: ShaderFeatures = ShaderFeatures()
}

class ShaderFeatures {
    var attachNothing: Boolean = false
    var calculatesAtmospherics: Boolean = false
    var hasGamma: Boolean = false
    var isDeferred: Boolean = false
    var calculatesLighting: Boolean = false
    var isSpecular: Boolean = false
    var isAlphaLighting: Boolean = false
    var hasSkinning: Boolean = false
    var hasObjectSkinning: Boolean = false
    var hasSrgb: Boolean = false
    var hasAtmospherics: Boolean = false
    var hasReflectionProbes: Boolean = false
    var hasFullGBuffer: Boolean = false
    var hasScreenSpaceReflections: Boolean = false
    var hasShadows: Boolean = false
    var hasAmbientOcclusion: Boolean = false
    var isPBRTerrain: Boolean = false
    var hasTonemap: Boolean = false
    var hasLighting: Boolean = false
    var hasAlphaMask: Boolean = false
    var mIndexedTextureChannels: Int = 0
}
