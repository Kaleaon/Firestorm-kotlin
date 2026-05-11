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

    /**
     * Attach feature-derived `#define`s. The actual stages were already
     * compiled and attached by `GLSLShader.createShader`; this hook just
     * surfaces shader-feature flags as `LL_FEATURE_…` preprocessor symbols so
     * future re-compiles can branch on them.
     */
    fun attachShaderFeatures(shader: GlslShader): Boolean {
        val f = shader.features
        fun set(name: String, on: Boolean) {
            if (on) shader.defines[name] = "1" else shader.defines.remove(name)
        }
        set("LL_FEATURE_LIGHTING", f.calculatesLighting || f.hasLighting)
        set("LL_FEATURE_ATMOSPHERICS", f.calculatesAtmospherics || f.hasAtmospherics)
        set("LL_FEATURE_SHADOWS", f.hasShadows)
        set("LL_FEATURE_SPECULAR", f.isSpecular)
        set("LL_FEATURE_SKINNING", f.hasSkinning)
        set("LL_FEATURE_OBJECT_SKINNING", f.hasObjectSkinning)
        set("LL_FEATURE_GAMMA", f.hasGamma)
        set("LL_FEATURE_SRGB", f.hasSrgb)
        set("LL_FEATURE_DEFERRED", f.isDeferred)
        set("LL_FEATURE_FULL_GBUFFER", f.hasFullGBuffer)
        set("LL_FEATURE_SSR", f.hasScreenSpaceReflections)
        set("LL_FEATURE_REFLECTION_PROBES", f.hasReflectionProbes)
        set("LL_FEATURE_ALPHA_MASK", f.hasAlphaMask)
        set("LL_FEATURE_ALPHA_LIGHTING", f.isAlphaLighting)
        set("LL_FEATURE_AO", f.hasAmbientOcclusion)
        set("LL_FEATURE_PBR_TERRAIN", f.isPbrTerrain)
        set("LL_FEATURE_TONEMAP", f.hasTonemap)
        return true
    }

    fun dumpObjectLog(obj: UInt, warns: Boolean = true, filename: String = "") {
        val gl = GpuBackend.current
        val programLog = gl.getProgramInfoLog(obj.toInt())
        val shaderLog = gl.getShaderInfoLog(obj.toInt())
        val log = if (programLog.isNotBlank()) programLog else shaderLog
        if (log.isBlank()) return
        val tag = if (filename.isNotEmpty()) " ($filename)" else ""
        if (warns) System.err.println("GL object $obj$tag:\n$log") else println("GL object $obj$tag:\n$log")
    }

    fun dumpShaderSource(shaderCodeCount: UInt, shaderCodeText: Array<String>) {
        var line = 1
        for (chunk in shaderCodeText) {
            for (lineText in chunk.lines()) {
                System.err.println("${"%4d".format(line)}: $lineText")
                line++
            }
        }
    }

    fun linkProgramObject(obj: UInt, suppressErrors: Boolean = false): Boolean {
        val gl = GpuBackend.current
        val ok = gl.linkProgram(obj.toInt())
        if (!ok && !suppressErrors) {
            val log = gl.getProgramInfoLog(obj.toInt())
            System.err.println("glLinkProgram($obj) failed: $log")
            val lower = log.lowercase()
            if ("software" in lower || "swrast" in lower || "llvmpipe" in lower) {
                System.err.println("WARNING: software renderer detected — performance will be poor")
            }
        }
        return ok
    }

    fun validateProgramObject(obj: UInt): Boolean {
        val gl = GpuBackend.current
        val ok = gl.validateProgram(obj.toInt())
        if (!ok) dumpObjectLog(obj, warns = true)
        return ok
    }

    fun loadShaderFile(
        filename: String,
        shaderLevel: Int,
        type: ShaderType,
        defines: MutableMap<String, String>? = null,
        textureIndexChannels: Int = -1
    ): Pair<UInt, Int> {
        var level = shaderLevel
        var source: String? = null
        // Locate the file under `shaderDir/class<N>/`, descending `gpu_class`
        // until a file is found. Falls back to the bare filename if no class
        // directory exists.
        while (level >= 0 && source == null) {
            val candidate = if (getShaderDirPrefix().isNotEmpty())
                "${getShaderDirPrefix()}/class$level/$filename"
            else
                filename
            source = try {
                java.io.File(candidate).takeIf { it.exists() }?.readText()
            } catch (e: Exception) { null }
            if (source == null) level--
        }
        if (source == null) {
            source = try { java.io.File(filename).readText() } catch (e: Exception) { null }
            if (source == null) {
                System.err.println("loadShaderFile: $filename not found")
                return 0u to 0
            }
            level = shaderLevel
        }

        // Inject extra texture-index defines so a single shader can be reused
        // for different texture-array sizes.
        val effective = defines?.toMutableMap() ?: mutableMapOf()
        if (textureIndexChannels > 0) {
            effective["NUM_TEX_UNITS"] = textureIndexChannels.toString()
        }

        val gl = GpuBackend.current
        val handle = gl.createShader(type.toGl()).toUInt()
        if (handle == 0u) return 0u to level
        val preamble = buildShaderPreamble(type, effective)
        val finalSource = preamble + source
        gl.shaderSource(handle.toInt(), finalSource)
        val compiled = gl.compileShader(handle.toInt())
        if (!compiled) {
            val log = gl.getShaderInfoLog(handle.toInt())
            System.err.println("Shader compile failed for $filename:\n$log")
            dumpShaderSource(1u, arrayOf(finalSource))
            gl.deleteShader(handle.toInt())
            return 0u to level
        }
        when (type) {
            ShaderType.VERTEX -> vertexShaderObjects[filename] = handle
            ShaderType.FRAGMENT -> fragmentShaderObjects[filename] = handle
            ShaderType.GEOMETRY -> Unit
        }
        return handle to level
    }

    fun initShaderCache(enabled: Boolean, oldCacheVersion: String, currentCacheVersion: String, secondInstance: Boolean) {
        shaderCacheEnabled = enabled
        shaderCacheVersion = currentCacheVersion
        if (!enabled) return
        if (oldCacheVersion != currentCacheVersion) {
            clearShaderCache()
            return
        }
        if (secondInstance || shaderCacheDir.isBlank()) return
        // Best-effort load of cache metadata from `shaderdata.llsd`. Failures
        // are non-fatal; the cache simply rebuilds.
        val metadata = java.io.File(shaderCacheDir, "shaderdata.llsd")
        if (!metadata.exists()) return
        try {
            metadata.readLines().forEach { line ->
                val parts = line.split('\t')
                if (parts.size >= 3) {
                    shaderBinaryCache[parts[0]] = ProgramBinaryData(
                        binaryLength = parts[1].toIntOrNull() ?: 0,
                        binaryFormat = parts[2].toUIntOrNull() ?: 0u,
                        lastUsedTime = parts.getOrNull(3)?.toFloatOrNull() ?: 0f
                    )
                }
            }
        } catch (e: Exception) {
            System.err.println("Failed to parse shader cache metadata: ${e.message}")
        }
    }

    fun clearShaderCache() {
        shaderBinaryCache.clear()
        if (shaderCacheDir.isBlank()) return
        val dir = java.io.File(shaderCacheDir)
        if (!dir.exists()) return
        dir.listFiles()?.forEach { it.delete() }
    }

    fun persistShaderCacheMetadata() {
        if (!shaderCacheEnabled || shaderCacheDir.isBlank()) return
        val dir = java.io.File(shaderCacheDir)
        if (!dir.exists()) dir.mkdirs()
        val pruneOlderThan = System.currentTimeMillis() / 1000f - PRUNE_AGE_SECONDS
        val metadata = java.io.File(dir, "shaderdata.llsd")
        try {
            metadata.printWriter().use { out ->
                shaderBinaryCache.entries
                    .filter { it.value.lastUsedTime >= pruneOlderThan }
                    .forEach { (k, v) ->
                        out.println("$k\t${v.binaryLength}\t${v.binaryFormat}\t${v.lastUsedTime}")
                    }
            }
        } catch (e: Exception) {
            System.err.println("Failed to write shader cache metadata: ${e.message}")
        }
    }

    fun loadCachedProgramBinary(shader: GlslShader): Boolean {
        if (!shaderCacheEnabled) return false
        // Program binary I/O requires `glProgramBinary` which the GpuBackend
        // does not expose: this returns false so callers fall back to source
        // compilation.
        return false
    }

    fun saveCachedProgramBinary(shader: GlslShader): Boolean {
        if (!shaderCacheEnabled) return true
        // See `loadCachedProgramBinary`: persisting program binaries is a
        // pure-write operation that the current backend doesn't support; we
        // skip it without surfacing an error.
        return true
    }

    /**
     * Compile a single shader stage and return its handle, dropping the
     * resolved class level. Convenience wrapper for callers that don't care
     * about LOD-based selection.
     */
    fun compileShaderStage(
        filename: String,
        type: ShaderType,
        defines: MutableMap<String, String>? = null
    ): UInt = loadShaderFile(filename, shaderLevelDefault, type, defines).first

    enum class ShaderType { VERTEX, FRAGMENT, GEOMETRY }

    private fun ShaderType.toGl(): Int = when (this) {
        ShaderType.VERTEX -> GL.VERTEX_SHADER
        ShaderType.FRAGMENT -> GL.FRAGMENT_SHADER
        ShaderType.GEOMETRY -> GL.GEOMETRY_SHADER
    }

    var shaderLevelDefault: Int = 3
    private val PRUNE_AGE_SECONDS: Float = 14f * 24f * 60f * 60f

    companion object {
        /**
         * Singleton instance. Most call sites read it via `instanceOrNull` so
         * pure-Kotlin tests can run without registering a manager; the
         * `instance` accessor throws so old code that requires it surfaces
         * the missing init.
         */
        var instance: ShaderMgr? = null
        val instanceOrThrow: ShaderMgr
            get() = checkNotNull(instance) { "ShaderMgr must be instantiated by the application before use" }

        /**
         * Compile a single shader stage by file name, using whichever
         * `ShaderMgr` is currently installed (or the default loader if none
         * is). Returns 0 on failure.
         */
        fun compileShaderFile(
            filename: String,
            type: Int,
            defines: Map<String, String>? = null
        ): UInt {
            val st = when (type) {
                GL.VERTEX_SHADER -> ShaderType.VERTEX
                GL.FRAGMENT_SHADER -> ShaderType.FRAGMENT
                GL.GEOMETRY_SHADER -> ShaderType.GEOMETRY
                else -> return 0u
            }
            val mgr = instance
            val effective = defines?.toMutableMap()
            return if (mgr != null) {
                mgr.loadShaderFile(filename, mgr.shaderLevelDefault, st, effective).first
            } else {
                inlineCompileFromFile(filename, st, effective)
            }
        }

        /** Compile + link directly via [GpuBackend], without the manager. */
        fun linkProgramObject(obj: UInt, suppressErrors: Boolean = false): Boolean {
            val mgr = instance
            if (mgr != null) return mgr.linkProgramObject(obj, suppressErrors)
            val gl = GpuBackend.current
            val ok = gl.linkProgram(obj.toInt())
            if (!ok && !suppressErrors) {
                System.err.println("glLinkProgram($obj) failed: ${gl.getProgramInfoLog(obj.toInt())}")
            }
            return ok
        }

        private fun inlineCompileFromFile(
            filename: String,
            type: ShaderType,
            defines: MutableMap<String, String>?
        ): UInt {
            val source = try { java.io.File(filename).readText() } catch (e: Exception) { return 0u }
            val gl = GpuBackend.current
            val handle = gl.createShader(
                when (type) {
                    ShaderType.VERTEX -> GL.VERTEX_SHADER
                    ShaderType.FRAGMENT -> GL.FRAGMENT_SHADER
                    ShaderType.GEOMETRY -> GL.GEOMETRY_SHADER
                }
            ).toUInt()
            if (handle == 0u) return 0u
            gl.shaderSource(handle.toInt(), buildShaderPreamble(type, defines) + source)
            if (!gl.compileShader(handle.toInt())) {
                System.err.println("Inline shader compile failed: ${gl.getShaderInfoLog(handle.toInt())}")
                gl.deleteShader(handle.toInt())
                return 0u
            }
            return handle
        }

        // GLSL ES 3.20 matches the OpenGL ES 3.2 context the Android renderer requests.
        const val GLSL_VERSION_DIRECTIVE = "#version 320 es"

        /**
         * Build the preamble prepended to a shader's source: the GLSL version
         * directive, default precision qualifiers, and `#define` lines for the
         * provided defines map. Callers concatenate this with the raw shader body.
         */
        fun buildShaderPreamble(
            type: ShaderType,
            defines: Map<String, String>? = null
        ): String = buildString {
            append(GLSL_VERSION_DIRECTIVE).append('\n')
            when (type) {
                ShaderType.VERTEX -> {
                    append("precision highp float;\n")
                    append("precision highp int;\n")
                    append("precision highp sampler2DArray;\n")
                }
                ShaderType.FRAGMENT -> {
                    append("precision highp float;\n")
                    append("precision highp int;\n")
                    append("precision highp sampler2D;\n")
                    append("precision highp sampler2DArray;\n")
                    append("precision highp samplerCube;\n")
                    append("precision highp samplerCubeArray;\n")
                }
                ShaderType.GEOMETRY -> {
                    append("precision highp float;\n")
                    append("precision highp int;\n")
                }
            }
            defines?.forEach { (k, v) ->
                if (v.isEmpty()) append("#define ").append(k).append('\n')
                else append("#define ").append(k).append(' ').append(v).append('\n')
            }
        }
    }
}

class GlslShader {
    var name: String = ""
    var shaderHash: String = ""
    var programObject: UInt = 0u
    var riggedVariant: GlslShader? = null
    var features: ShaderFeatures = ShaderFeatures()
    var defines: MutableMap<String, String> = mutableMapOf()
}
