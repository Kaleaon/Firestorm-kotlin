package com.firestorm.newview

import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL31.*
import org.lwjgl.opengl.GL32.*
import org.lwjgl.opengl.GL33.*
import org.lwjgl.opengl.GL40.*
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.TreeMap
import java.util.UUID

// ---------------------------------------------------------------------------
// Companion value types (mirrors C++ LLVector4 / LLVector3 usage in shader cache)
// ---------------------------------------------------------------------------

data class Vector4(val x: Float, val y: Float, val z: Float, val w: Float) {
    constructor(v: FloatArray) : this(v[0], v[1], v[2], v[3])
}

data class Vector3(val x: Float, val y: Float, val z: Float)

// ---------------------------------------------------------------------------
// ShaderFeatures
// ---------------------------------------------------------------------------

class ShaderFeatures {
    var indexedTextureChannels: Int = 0
    var calculatesLighting: Boolean = false
    var calculatesAtmospherics: Boolean = false
    var hasLighting: Boolean = false
    var isAlphaLighting: Boolean = false
    var isSpecular: Boolean = false
    var hasTransport: Boolean = false
    var hasSkinning: Boolean = false
    var hasObjectSkinning: Boolean = false
    var isGltf: Boolean = false
    var hasAtmospherics: Boolean = false
    var hasGamma: Boolean = false
    var hasShadows: Boolean = false
    var hasAmbientOcclusion: Boolean = false
    var hasSrgb: Boolean = false
    var isDeferred: Boolean = false
    var hasFullGBuffer: Boolean = false
    var hasScreenSpaceReflections: Boolean = false
    var hasAlphaMask: Boolean = false
    var hasReflectionProbes: Boolean = false
    var attachNothing: Boolean = false
    var hasHeroProbes: Boolean = false
    var isPbrTerrain: Boolean = false
    var hasTonemap: Boolean = false
}

// ---------------------------------------------------------------------------
// ShaderUniforms  (mirrors C++ LLShaderUniforms)
// ---------------------------------------------------------------------------

class ShaderUniforms {
    data class UniformSetting<T>(val uniform: Int, val value: T)

    val integers: MutableList<UniformSetting<Int>> = mutableListOf()
    val floats: MutableList<UniformSetting<Float>> = mutableListOf()
    val vectors: MutableList<UniformSetting<Vector4>> = mutableListOf()
    val vector3s: MutableList<UniformSetting<Vector3>> = mutableListOf()

    fun clear() {
        integers.clear()
        floats.clear()
        vectors.clear()
        vector3s.clear()
    }

    fun uniform1i(index: Int, value: Int) {
        integers.add(UniformSetting(index, value))
    }

    fun uniform1f(index: Int, value: Float) {
        floats.add(UniformSetting(index, value))
    }

    fun uniform4fv(index: Int, value: Vector4) {
        vectors.add(UniformSetting(index, value))
    }

    fun uniform3fv(index: Int, value: Vector3) {
        vector3s.add(UniformSetting(index, value))
    }

    fun apply(shader: GLSLShader) {
        for (u in integers) shader.uniform1i(u.uniform.toUInt(), u.value)
        for (u in floats)   shader.uniform1f(u.uniform.toUInt(), u.value)
        for (u in vectors)  shader.uniform4fv(u.uniform.toUInt(), 1u,
            floatArrayOf(u.value.x, u.value.y, u.value.z, u.value.w))
        for (u in vector3s) shader.uniform3fv(u.uniform.toUInt(), 1u,
            floatArrayOf(u.value.x, u.value.y, u.value.z))
    }
}

// ---------------------------------------------------------------------------
// GLSLShader  (mirrors C++ LLGLSLShader)
// ---------------------------------------------------------------------------

class GLSLShader {

    enum class ShaderConst { CLOUD_MOON_DEPTH, STAR_DEPTH }

    enum class Group(val id: Int) {
        DEFAULT(0), SKY(1), WATER(2), ANY(3)
    }

    enum class UniformBlock(val id: Int) {
        REFLECTION_PROBES(0),
        GLTF_JOINTS(1),
        GLTF_NODES(2),
        GLTF_MATERIALS(3);
    }

    object GLTFVariant {
        const val ALPHA_BLEND: UByte = 1u
        const val RIGGED: UByte = 2u
        const val UNLIT: UByte = 4u
        const val MULTI_UV: UByte = 8u
    }

    companion object {
        const val NUM_GLTF_VARIANTS: UByte = 16u

        val shaderConstKeys = mapOf(
            ShaderConst.CLOUD_MOON_DEPTH to "LL_SHADER_CONST_CLOUD_MOON_DEPTH",
            ShaderConst.STAR_DEPTH       to "LL_SHADER_CONST_STAR_DEPTH"
        )
        val shaderConstVals = mapOf(
            ShaderConst.CLOUD_MOON_DEPTH to "0.99998",
            ShaderConst.STAR_DEPTH       to "0.99999"
        )

        val instances: MutableSet<GLSLShader> = mutableSetOf()
        var profileEnabled: Boolean = false
        var canProfile: Boolean = true

        var curBoundShader: Int = 0          // GL program object id
        var curBoundShaderPtr: GLSLShader? = null
        var indexedTextureChannels: Int = 0

        var maxGltfMaterials: UInt = 0u
        var maxGltfNodes: UInt = 0u

        var totalTimeElapsed: ULong = 0uL
        var totalTrianglesDrawn: UInt = 0u
        var totalSamplesDrawn: ULong = 0uL
        var totalBinds: UInt = 0u

        val globalDefines: TreeMap<String, String> = TreeMap()

        // UBO name table – order must match UniformBlock ordinal
        private val uboNames = arrayOf(
            "ReflectionProbes",  // UB_REFLECTION_PROBES
            "GLTFJoints",        // UB_GLTF_JOINTS
            "GLTFNodes",         // UB_GLTF_NODES
            "GLTFMaterials"      // UB_GLTF_MATERIALS
        )

        fun initProfile() {
            profileEnabled = true
            totalTimeElapsed = 0uL
            totalTrianglesDrawn = 0u
            totalSamplesDrawn = 0uL
            totalBinds = 0u
            for (shader in instances) shader.clearStats()
        }

        fun finishProfile(): Map<String, Any> {
            profileEnabled = false
            val sorted = instances.sortedBy { it.timeElapsed }
            val result = mutableMapOf<String, Any>()
            val shaderList = mutableListOf<Map<String, Any>>()
            val unusedList = mutableListOf<String>()
            for (shader in sorted) {
                if (shader.binds == 0u) unusedList.add(shader.name)
                else shaderList.add(shader.dumpStats())
            }
            result["shaders"] = shaderList
            result["unused"] = unusedList
            val totalTimeMs = totalTimeElapsed.toFloat() / 1_000_000f
            result["totals"] = mapOf(
                "time"      to totalTimeMs / 1000.0,
                "binds"     to totalBinds,
                "samples"   to totalSamplesDrawn,
                "triangles" to totalTrianglesDrawn
            )
            return result
        }

        fun startProfile() {
            if (profileEnabled) curBoundShaderPtr?.placeProfileQuery()
        }

        fun stopProfile() {
            if (profileEnabled) curBoundShaderPtr?.unbind()
        }

        /**
         * Unbinds any currently bound shader program.
         * Mirrors: glUseProgram(0)
         */
        fun unbind() {
            // Flush pending rendering work before changing program state.
            // (In the original C++ this calls gGL.flush() and LLVertexBuffer::unbind().)
            curBoundShaderPtr?.readProfileQuery()
            glUseProgram(0)
            curBoundShader = 0
            curBoundShaderPtr = null
        }
    }

    // -----------------------------------------------------------------------
    // Instance fields
    // -----------------------------------------------------------------------

    /** The OpenGL program object id returned by glCreateProgram(). */
    var programObject: Int = 0
    var attributeMask: UInt = 0u
    val attribute: MutableList<Int> = mutableListOf()
    val uniform: MutableList<Int> = mutableListOf()
    val uniformMap: MutableMap<String, Int> = mutableMapOf()
    val value: MutableMap<Int, Vector4> = mutableMapOf()
    val texture: MutableList<Int> = mutableListOf()
    var totalUniformSize: Int = 0
    var activeTextureChannels: Int = 0
    var shaderLevel: Int = 0
    var shaderGroup: Group = Group.DEFAULT
    var uniformsDirty: Boolean = false
    var features: ShaderFeatures = ShaderFeatures()
    val shaderFiles: MutableList<Pair<String, Int>> = mutableListOf()
    var name: String = ""
    val defines: TreeMap<String, String> = TreeMap()
    var shaderHash: UUID = UUID(0L, 0L)
    var usingBinaryProgram: Boolean = false

    var profilePending: Boolean = false
    var timerQuery: Int = 0
    var samplesQuery: Int = 0
    var primitivesQuery: Int = 0

    var timeElapsed: ULong = 0uL
    var trianglesDrawn: UInt = 0u
    var samplesDrawn: ULong = 0uL
    var binds: UInt = 0u

    var riggedVariant: GLSLShader? = null
    val gltfVariants: MutableList<GLSLShader> = mutableListOf()

    var canBindFast: Boolean = false

    /** Per-matrix-mode dirty hashes, initialised to "dirty" sentinel. */
    val matHash: UIntArray = UIntArray(8) { 0xFFFFFFFFu }
    var lightHash: UInt = 0xFFFFFFFFu

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    fun unload() {
        shaderFiles.clear()
        defines.clear()
        features = ShaderFeatures()
        unloadInternal()
    }

    private fun unloadInternal() {
        instances.remove(this)
        attribute.clear()
        texture.clear()
        uniform.clear()

        if (programObject != 0) {
            // Detach and delete all attached shader objects, then delete the program.
            // glGetAttachedShaders returns the list of currently attached shader objects.
            val maxShaders = 1024
            val countBuf = IntArray(1)
            val shaderObjs = IntArray(maxShaders)
            glGetAttachedShaders(programObject, countBuf, shaderObjs)
            val count = countBuf[0]
            for (i in 0 until count) {
                glDetachShader(programObject, shaderObjs[i])
            }
            for (i in 0 until count) {
                if (glIsShader(shaderObjs[i])) {
                    glDeleteShader(shaderObjs[i])
                }
            }
            glDeleteProgram(programObject)
            programObject = 0
        }

        if (timerQuery != 0) {
            // glDeleteQueries for profiling query objects.
            glDeleteQueries(timerQuery)
            timerQuery = 0
            if (samplesQuery != 0) {
                glDeleteQueries(samplesQuery)
                samplesQuery = 0
            }
            if (primitivesQuery != 0) {
                glDeleteQueries(primitivesQuery)
                primitivesQuery = 0
            }
        }
    }

    /**
     * Creates the GL program object and compiles/links all shader files.
     * Mirrors: LLGLSLShader::createShader()
     */
    fun createShader(): Boolean {
        unloadInternal()
        instances.add(this)

        for (i in matHash.indices) matHash[i] = 0xFFFFFFFFu
        lightHash = 0xFFFFFFFFu

        check(shaderFiles.isNotEmpty()) { "shaderFiles must not be empty before calling createShader()" }

        // Create an empty program object.
        programObject = glCreateProgram()
        if (programObject == 0) {
            System.err.println("GLSLShader: glCreateProgram() failed for shader '$name'")
            unloadInternal()
            return false
        }

        // NOTE: In the full Firestorm engine, shader source loading/compilation
        // is delegated to LLShaderMgr::loadShaderFile() / loadCachedProgramBinary().
        // Here we attach shader objects that are assumed to have been compiled
        // externally (e.g. by a ShaderMgr equivalent) and stored in shaderFiles
        // as (path → glShaderType) pairs. The actual glCreateShader / glShaderSource
        // / glCompileShader cycle belongs in that layer. For direct use without a
        // ShaderMgr, call attachObject() with already-compiled shader handles.

        val success = mapAttributes() && mapUniforms()

        if (!success) {
            if (shaderLevel > 0) {
                shaderLevel--
                return createShader()
            }
            unloadInternal()
        }

        return success
    }

    /**
     * Attaches a compiled vertex shader object to the program.
     * The shader object must already exist (compiled by the shader manager).
     * Mirrors: LLGLSLShader::attachVertexObject()
     *
     * @param shaderHandle  The GL handle of an already-compiled GL_VERTEX_SHADER object.
     */
    fun attachVertexObject(shaderHandle: Int): Boolean {
        if (shaderHandle == 0) {
            System.err.println("GLSLShader: attachVertexObject called with null handle")
            return false
        }
        glAttachShader(programObject, shaderHandle)
        return true
    }

    /**
     * Attaches a compiled fragment shader object to the program.
     * Mirrors: LLGLSLShader::attachFragmentObject()
     *
     * @param shaderHandle  The GL handle of an already-compiled GL_FRAGMENT_SHADER object.
     */
    fun attachFragmentObject(shaderHandle: Int): Boolean {
        if (usingBinaryProgram) return true
        if (shaderHandle == 0) {
            System.err.println("GLSLShader: attachFragmentObject called with null handle")
            return false
        }
        glAttachShader(programObject, shaderHandle)
        return true
    }

    /**
     * Attaches any compiled shader object (vertex, fragment, geometry, etc.).
     * Mirrors: LLGLSLShader::attachObject()
     */
    fun attachObject(objectHandle: Int) {
        if (usingBinaryProgram) return
        if (objectHandle != 0) {
            glAttachShader(programObject, objectHandle)
        } else {
            System.err.println("GLSLShader: attachObject called with null handle")
        }
    }

    fun attachObjects(objects: IntArray) {
        if (usingBinaryProgram) return
        for (obj in objects) attachObject(obj)
    }

    /**
     * Binds reserved attribute locations, links the program, then reads back
     * attribute channel assignments.
     * Mirrors: LLGLSLShader::mapAttributes()
     *
     * Reserved attribute names/indices must be supplied by the caller via
     * reservedAttribs before calling createShader().
     */
    var reservedAttribs: List<String> = emptyList()

    fun mapAttributes(): Boolean {
        if (!usingBinaryProgram) {
            // Bind reserved attribute locations before linking so that every
            // shader program assigns the same channel to each semantic.
            for (i in reservedAttribs.indices) {
                glBindAttribLocation(programObject, i, reservedAttribs[i])
            }
            if (!link()) return false
        }

        attribute.clear()
        repeat(reservedAttribs.size) { attribute.add(-1) }

        attributeMask = 0u
        for (i in reservedAttribs.indices) {
            val idx = glGetAttribLocation(programObject, reservedAttribs[i])
            if (idx != -1) {
                attribute[i] = idx
                attributeMask = attributeMask or (1u shl i)
            }
        }
        return true
    }

    /**
     * Queries the active uniform at [index], accumulates its size in
     * totalUniformSize, and stores the location in uniformMap / uniform list.
     * Mirrors: LLGLSLShader::mapUniform()
     */
    fun mapUniform(index: Int) {
        if (index == -1) return

        val sizeBuf  = IntArray(1)
        val typeBuf  = IntArray(1)
        val nameBuf  = ByteArray(1024)
        val lengthBuf = IntArray(1)
        glGetActiveUniform(programObject, index, lengthBuf, sizeBuf, typeBuf, nameBuf)

        var size = sizeBuf[0]
        val type = typeBuf[0]
        val nameLen = lengthBuf[0]
        val rawName = String(nameBuf, 0, nameLen)

        // Accumulate total uniform data size (in component slots).
        if (size > 0) {
            size *= when (type) {
                GL_FLOAT_VEC2          -> 2
                GL_FLOAT_VEC3          -> 3
                GL_FLOAT_VEC4          -> 4
                GL_DOUBLE              -> 2
                0x8FFC /* GL_DOUBLE_VEC2 */ -> 2
                0x8FFD /* GL_DOUBLE_VEC3 */ -> 6
                0x8FFE /* GL_DOUBLE_VEC4 */ -> 8
                GL_INT_VEC2            -> 2
                GL_INT_VEC3            -> 3
                GL_INT_VEC4            -> 4
                GL_UNSIGNED_INT_VEC2   -> 2
                GL_UNSIGNED_INT_VEC3   -> 3
                GL_UNSIGNED_INT_VEC4   -> 4
                GL_BOOL_VEC2           -> 2
                GL_BOOL_VEC3           -> 3
                GL_BOOL_VEC4           -> 4
                GL_FLOAT_MAT2          -> 4
                GL_FLOAT_MAT3          -> 9
                GL_FLOAT_MAT4          -> 16
                GL_FLOAT_MAT2x3        -> 6
                GL_FLOAT_MAT2x4        -> 8
                GL_FLOAT_MAT3x2        -> 6
                GL_FLOAT_MAT3x4        -> 12
                GL_FLOAT_MAT4x2        -> 8
                GL_FLOAT_MAT4x3        -> 12
                0x8F46 /* GL_DOUBLE_MAT2 */ -> 8
                0x8F47 /* GL_DOUBLE_MAT3 */ -> 18
                0x8F48 /* GL_DOUBLE_MAT4 */ -> 32
                0x8F49 /* GL_DOUBLE_MAT2x3 */ -> 12
                0x8F4A /* GL_DOUBLE_MAT2x4 */ -> 16
                0x8F4B /* GL_DOUBLE_MAT3x2 */ -> 12
                0x8F4C /* GL_DOUBLE_MAT3x4 */ -> 24
                0x8F4D /* GL_DOUBLE_MAT4x2 */ -> 16
                0x8F4E /* GL_DOUBLE_MAT4x3 */ -> 24
                else   -> 1
            }
            totalUniformSize += size
        }

        // Strip the "[0]" suffix so array uniforms are keyed by base name.
        val uniName = if (rawName.endsWith("[0]")) rawName.dropLast(3) else rawName

        val location = glGetUniformLocation(programObject, uniName)
        if (location == -1) return

        uniformMap[uniName] = location

        // Match against the reserved-uniform list (filled in by the caller /
        // shader manager) and record the channel assignment.
        for (i in reservedUniforms.indices) {
            if (uniform[i] == -1 && reservedUniforms[i] == uniName) {
                uniform[i] = location
                texture[i] = mapUniformTextureChannel(location, type, size)
                return
            }
        }
    }

    /**
     * Names of uniforms that the engine reserves and assigns by ordinal index.
     * Must be populated by the caller (mirrors LLShaderMgr::mReservedUniforms).
     */
    var reservedUniforms: List<String> = emptyList()

    /**
     * Queries the number of active uniforms, then maps each one.
     * Also sets up UBO bindings for the known named uniform blocks.
     * Mirrors: LLGLSLShader::mapUniforms()
     */
    fun mapUniforms(): Boolean {
        totalUniformSize = 0
        activeTextureChannels = 0
        uniform.clear()
        uniformMap.clear()
        texture.clear()
        value.clear()

        uniform.addAll(List(reservedUniforms.size) { -1 })
        texture.addAll(List(reservedUniforms.size) { -1 })

        bind()

        val activeCountBuf = IntArray(1)
        glGetProgramiv(programObject, GL_ACTIVE_UNIFORMS, activeCountBuf)
        val activeCount = activeCountBuf[0]

        // ----------------------------------------------------------------
        // Ensure diffuseMap is mapped first so it gets texture channel 0.
        // This preserves the ordering semantics from the original C++ code.
        // ----------------------------------------------------------------
        val sizeBuf   = IntArray(1)
        val typeBuf   = IntArray(1)
        val lenBuf    = IntArray(1)
        val nameBuf   = ByteArray(1024)

        var diffuseMapIdx    = glGetUniformLocation(programObject, "diffuseMap")
        var specularMapIdx   = glGetUniformLocation(programObject, "specularMap")
        var bumpMapIdx       = glGetUniformLocation(programObject, "bumpMap")
        var altDiffuseMapIdx = glGetUniformLocation(programObject, "altDiffuseMap")
        var environmentMapIdx= glGetUniformLocation(programObject, "environmentMap")
        var reflectionMapIdx = glGetUniformLocation(programObject, "reflectionMap")

        val skipIndices = mutableSetOf<Int>()

        if (diffuseMapIdx != -1 && (specularMapIdx != -1 || bumpMapIdx != -1 ||
                    environmentMapIdx != -1 || altDiffuseMapIdx != -1)) {

            // Re-scan by active-uniform index to find the slot indices
            var dIdx = -1; var sIdx = -1; var bIdx = -1
            var aIdx = -1; var eIdx = -1; var rIdx = -1
            diffuseMapIdx = -1; altDiffuseMapIdx = -1; specularMapIdx = -1
            bumpMapIdx = -1; environmentMapIdx = -1; reflectionMapIdx = -1

            for (i in 0 until activeCount) {
                glGetActiveUniform(programObject, i, lenBuf, sizeBuf, typeBuf, nameBuf)
                val nm = String(nameBuf, 0, lenBuf[0])
                when {
                    dIdx == -1 && nm == "diffuseMap"    -> { dIdx = i; diffuseMapIdx    = i }
                    sIdx == -1 && nm == "specularMap"   -> { sIdx = i; specularMapIdx   = i }
                    bIdx == -1 && nm == "bumpMap"       -> { bIdx = i; bumpMapIdx       = i }
                    eIdx == -1 && nm == "environmentMap"-> { eIdx = i; environmentMapIdx= i }
                    rIdx == -1 && nm == "reflectionMap" -> { rIdx = i; reflectionMapIdx = i }
                    aIdx == -1 && nm == "altDiffuseMap" -> { aIdx = i; altDiffuseMapIdx = i }
                }
            }

            val specularBeforeDiff  = specularMapIdx   < diffuseMapIdx && specularMapIdx   != -1
            val bumpBeforeDiff      = bumpMapIdx       < diffuseMapIdx && bumpMapIdx       != -1
            val envBeforeDiff       = environmentMapIdx< diffuseMapIdx && environmentMapIdx!= -1
            val refBeforeDiff       = reflectionMapIdx < diffuseMapIdx && reflectionMapIdx != -1

            if (specularBeforeDiff || bumpBeforeDiff || envBeforeDiff || refBeforeDiff) {
                if (diffuseMapIdx != -1)    { mapUniform(diffuseMapIdx);     skipIndices.add(diffuseMapIdx) }
                if (specularMapIdx != -1)   { mapUniform(specularMapIdx);    skipIndices.add(specularMapIdx) }
                if (bumpMapIdx != -1)       { mapUniform(bumpMapIdx);        skipIndices.add(bumpMapIdx) }
                if (environmentMapIdx != -1){ mapUniform(environmentMapIdx); skipIndices.add(environmentMapIdx) }
                if (reflectionMapIdx != -1) { mapUniform(reflectionMapIdx);  skipIndices.add(reflectionMapIdx) }
            }
        }

        for (i in 0 until activeCount) {
            if (i in skipIndices) continue
            mapUniform(i)
        }

        // ----------------------------------------------------------------
        // Bind named Uniform Buffer Objects to their canonical binding points.
        // Mirrors: glGetUniformBlockIndex / glUniformBlockBinding loop.
        // ----------------------------------------------------------------
        for ((ordinal, uboName) in uboNames.withIndex()) {
            val blockIndex = glGetUniformBlockIndex(programObject, uboName)
            if (blockIndex != GL_INVALID_INDEX) {
                glUniformBlockBinding(programObject, blockIndex, ordinal)
            }
        }

        unbind()
        return true
    }

    /**
     * Links the program object and checks for errors.
     * Mirrors: LLGLSLShader::link() / LLShaderMgr::linkProgramObject()
     */
    fun link(suppressErrors: Boolean = false): Boolean {
        glLinkProgram(programObject)

        val statusBuf = IntArray(1)
        glGetProgramiv(programObject, GL_LINK_STATUS, statusBuf)
        val success = statusBuf[0] == GL_TRUE

        if (!success && !suppressErrors) {
            val logLen = IntArray(1)
            glGetProgramiv(programObject, GL_INFO_LOG_LENGTH, logLen)
            val log = glGetProgramInfoLog(programObject)
            System.err.println("GLSLShader: link failed for '$name':\n$log")
        }
        return success
    }

    /**
     * Binds this shader program for rendering.
     * Mirrors: LLGLSLShader::bind()
     */
    fun bind() {
        check(programObject != 0) { "GLSLShader '$name' is not loaded (programObject == 0)" }

        if (curBoundShader != programObject) {
            curBoundShaderPtr?.readProfileQuery()
            // LLVertexBuffer::unbind() equivalent would go here in the full engine.
            glUseProgram(programObject)
            curBoundShader = programObject
            curBoundShaderPtr = this
            placeProfileQuery()
            // LLVertexBuffer::setupClientArrays(attributeMask) equivalent here.
        }

        if (uniformsDirty) {
            // ShaderMgr::updateShaderUniforms(this) equivalent here.
            uniformsDirty = false
        }
    }

    fun bind(rigged: Boolean) {
        if (rigged) {
            checkNotNull(riggedVariant) { "no riggedVariant set on '$name'" }.bind()
        } else {
            bind()
        }
    }

    fun bind(variant: UByte) {
        check(gltfVariants.size == NUM_GLTF_VARIANTS.toInt()) {
            "GLTF variants not fully populated on '$name'"
        }
        check(variant < NUM_GLTF_VARIANTS)
        gltfVariants[variant.toInt()].bind()
    }

    /**
     * Unbinds this shader program (instance method, for symmetry with bind()).
     * Mirrors: LLGLSLShader::unbind()
     */
    fun unbind() {
        curBoundShaderPtr?.readProfileQuery()
        glUseProgram(0)
        curBoundShader = 0
        curBoundShaderPtr = null
    }

    fun isComplete(): Boolean = programObject != 0

    fun hash(): UUID {
        // The original C++ uses HBXXH128 over name, shaderGroup, shaderLevel,
        // shaderFiles, defines, globalDefines, features, and the GL
        // vendor/renderer/version strings.  Without an xxHash128 binding here we
        // fall back to Java UUID.nameUUIDFromBytes over a canonical byte string.
        val sb = StringBuilder()
        sb.append(name)
        sb.append(shaderGroup.id)
        sb.append(shaderLevel)
        for ((path, glType) in shaderFiles) { sb.append(path); sb.append(glType) }
        for ((k, v) in defines) { sb.append(k); sb.append(v) }
        for ((k, v) in globalDefines) { sb.append(k); sb.append(v) }
        // features bit-field approximation
        sb.append(features.indexedTextureChannels)
            .append(features.hasLighting).append(features.hasSkinning)
            .append(features.isDeferred).append(features.hasReflectionProbes)
        // In the real engine gGLManager.mGLVendor / mGLRenderer / mGLVersionString
        // are also included; omit here as they require a GL context query.
        return UUID.nameUUIDFromBytes(sb.toString().toByteArray(Charsets.UTF_8))
    }

    // -----------------------------------------------------------------------
    // Permutations / constants
    // -----------------------------------------------------------------------

    fun clearPermutations() { defines.clear() }
    fun addPermutation(name: String, value: String) { defines[name] = value }
    fun addPermutations(defs: Map<String, String>) { defines.putAll(defs) }
    fun removePermutation(name: String) { defines.remove(name) }

    fun addConstant(shaderConst: ShaderConst) {
        addPermutation(shaderConstKeys[shaderConst]!!, shaderConstVals[shaderConst]!!)
    }

    // -----------------------------------------------------------------------
    // Uniform location lookups
    // -----------------------------------------------------------------------

    fun getUniformLocation(uniformName: String): Int = uniformMap[uniformName] ?: -1

    fun getUniformLocation(index: UInt): Int {
        if (programObject == 0) return -1
        if (index >= uniform.size.toUInt()) return -1
        return uniform[index.toInt()]
    }

    fun getAttribLocation(attrib: UInt): Int =
        if (attrib < attribute.size.toUInt()) attribute[attrib.toInt()] else -1

    /**
     * If [type] is a sampler type, assigns the next available texture channel
     * via glUniform1i (or glUniform1iv for arrays) and returns the first channel.
     * Returns -1 for non-sampler types.
     * Mirrors: LLGLSLShader::mapUniformTextureChannel()
     */
    fun mapUniformTextureChannel(location: Int, type: Int, size: Int): Int {
        // GL sampler types occupy the range GL_SAMPLER_1D..GL_SAMPLER_2D_RECT_SHADOW,
        // plus GL_SAMPLER_2D_MULTISAMPLE and GL_SAMPLER_CUBE_MAP_ARRAY.
        val isSampler = (type >= GL_SAMPLER_1D && type <= GL_SAMPLER_2D_SHADOW) ||
                        type == GL_SAMPLER_2D_MULTISAMPLE ||
                        type == 0x900C /* GL_SAMPLER_CUBE_MAP_ARRAY */ ||
                        type == GL_SAMPLER_CUBE ||
                        type == GL_SAMPLER_3D ||
                        type == GL_SAMPLER_2D_RECT ||
                        type == GL_SAMPLER_2D_RECT_SHADOW ||
                        type == GL_SAMPLER_1D_ARRAY ||
                        type == GL_SAMPLER_2D_ARRAY ||
                        type == GL_SAMPLER_1D_ARRAY_SHADOW ||
                        type == GL_SAMPLER_2D_ARRAY_SHADOW ||
                        type == GL_SAMPLER_CUBE_SHADOW
        if (!isSampler) return -1

        val firstChannel = activeTextureChannels
        if (size == 1) {
            // Single texture: assign the next channel.
            glUniform1i(location, activeTextureChannels)
            activeTextureChannels++
        } else {
            // Array of textures: assign a contiguous block of channels.
            val clamped = minOf(size, 16)
            val channels = IntArray(clamped) { activeTextureChannels++ }
            glUniform1iv(location, channels)
        }
        return firstChannel
    }

    fun getTextureChannel(uniformIndex: Int): Int = texture[uniformIndex]

    /**
     * Activates the texture unit assigned to [uniformIndex].
     * Actual texture-unit management (gGL.getTexUnit) belongs to the render layer;
     * the GL call here is glActiveTexture.
     */
    fun enableTexture(uniformIndex: Int, mode: Int = GL_TEXTURE_2D): Int {
        if (uniformIndex < 0 || uniformIndex >= texture.size) return -1
        val index = texture[uniformIndex]
        if (index != -1) {
            glActiveTexture(GL_TEXTURE0 + index)
            glEnable(mode)
        }
        return index
    }

    /**
     * Disables the texture unit assigned to [uniformIndex].
     */
    fun disableTexture(uniformIndex: Int, mode: Int = GL_TEXTURE_2D): Int {
        if (uniformIndex < 0 || uniformIndex >= texture.size) return -1
        val index = texture[uniformIndex]
        if (index < 0) return index
        glActiveTexture(GL_TEXTURE0 + index)
        glDisable(mode)
        return index
    }

    /**
     * Binds [textureName] to the channel reserved for [uniformName].
     * The caller is responsible for calling glActiveTexture if needed.
     *
     * @param textureName  GL texture object id.
     * @param target       GL texture target (e.g. GL_TEXTURE_2D).
     */
    fun bindTexture(uniformName: String, textureName: Int,
                    target: Int = GL_TEXTURE_2D): Int {
        val channel = getUniformLocation(uniformName)
        return bindTexture(channel, textureName, target)
    }

    fun bindTexture(uniformIndex: Int, textureName: Int,
                    target: Int = GL_TEXTURE_2D): Int {
        if (uniformIndex < 0 || uniformIndex >= texture.size) return -1
        val channel = texture[uniformIndex]
        if (channel > -1) {
            glActiveTexture(GL_TEXTURE0 + channel)
            glBindTexture(target, textureName)
        }
        return channel
    }

    fun unbindTexture(uniformName: String, target: Int = GL_TEXTURE_2D): Int {
        val channel = getUniformLocation(uniformName)
        return unbindTexture(channel, target)
    }

    fun unbindTexture(uniformIndex: Int, target: Int = GL_TEXTURE_2D): Int {
        if (uniformIndex < 0 || uniformIndex >= texture.size) return -1
        val channel = texture[uniformIndex]
        if (channel > -1) {
            glActiveTexture(GL_TEXTURE0 + channel)
            glBindTexture(target, 0)
        }
        return channel
    }

    /**
     * Sets the minimum alpha discard threshold uniform.
     * Mirrors: LLGLSLShader::setMinimumAlpha()
     *
     * The uniform index for MINIMUM_ALPHA must be known to the caller and
     * passed in; in the full engine it is LLShaderMgr::MINIMUM_ALPHA.
     */
    fun setMinimumAlpha(minimum: Float, minimumAlphaUniformIndex: UInt) {
        // gGL.flush() equivalent goes before changing uniforms in the real engine.
        uniform1f(minimumAlphaUniformIndex, minimum)
    }

    // -----------------------------------------------------------------------
    // Profiling queries
    // -----------------------------------------------------------------------

    /**
     * Places OpenGL timer / sample / primitive query objects.
     * Mirrors: LLGLSLShader::placeProfileQuery()
     */
    fun placeProfileQuery(forRuntime: Boolean = false) {
        if (profileEnabled || forRuntime) {
            // Generate query objects on first use.
            if (timerQuery == 0) {
                timerQuery     = glGenQueries()
                samplesQuery   = glGenQueries()
                primitivesQuery= glGenQueries()
            }
            // GL_TIME_ELAPSED measures GPU time for this shader dispatch.
            glBeginQuery(GL_TIME_ELAPSED, timerQuery)
            if (!forRuntime) {
                glBeginQuery(GL_SAMPLES_PASSED, samplesQuery)
                glBeginQuery(0x8C87 /* GL_PRIMITIVES_GENERATED */, primitivesQuery)
            }
        }
    }

    /**
     * Reads back profiling query results and accumulates statistics.
     * Mirrors: LLGLSLShader::readProfileQuery()
     *
     * Returns false if a query result is not yet available (try again later).
     */
    fun readProfileQuery(forRuntime: Boolean = false, forceRead: Boolean = false): Boolean {
        if (!(profileEnabled || forRuntime) || !canProfile) return true

        if (!profilePending) {
            glEndQuery(GL_TIME_ELAPSED)
            if (!forRuntime) {
                glEndQuery(GL_SAMPLES_PASSED)
                glEndQuery(0x8C87 /* GL_PRIMITIVES_GENERATED */)
            }
            profilePending = forRuntime
        }

        // For runtime queries, poll availability unless forceRead is set.
        if (profilePending && forRuntime && !forceRead) {
            val availableBuf = LongArray(1)
            glGetQueryObjectui64v(timerQuery, GL_QUERY_RESULT_AVAILABLE, availableBuf)
            if (availableBuf[0].toInt() != GL_TRUE) return false
        }

        val timeElapsedBuf = LongArray(1)
        glGetQueryObjectui64v(timerQuery, GL_QUERY_RESULT, timeElapsedBuf)
        timeElapsed += timeElapsedBuf[0].toULong()
        profilePending = false

        if (!forRuntime) {
            val samplesBuf = LongArray(1)
            glGetQueryObjectui64v(samplesQuery, GL_QUERY_RESULT, samplesBuf)

            val primitivesBuf = LongArray(1)
            glGetQueryObjectui64v(primitivesQuery, GL_QUERY_RESULT, primitivesBuf)

            totalTimeElapsed    += timeElapsedBuf[0].toULong()
            totalSamplesDrawn   += samplesBuf[0].toULong()
            samplesDrawn        += samplesBuf[0].toULong()

            val triCount = (primitivesBuf[0] / 3L).toUInt()
            trianglesDrawn      += triCount
            totalTrianglesDrawn += triCount

            totalBinds++
            binds++
        }
        return true
    }

    fun clearStats() {
        trianglesDrawn = 0u
        timeElapsed    = 0uL
        samplesDrawn   = 0uL
        binds          = 0u
    }

    fun dumpStats(): Map<String, Any> {
        val mega    = 1_000_000f
        val ms      = timeElapsed.toFloat() / mega
        val seconds = ms / 1000f
        return mapOf(
            "name"      to name,
            "files"     to shaderFiles.map { it.first },
            "time"      to seconds.toDouble(),
            "binds"     to binds,
            "samples"   to samplesDrawn,
            "triangles" to trianglesDrawn
        )
    }

    // -----------------------------------------------------------------------
    // Internal helper
    // -----------------------------------------------------------------------

    private fun resolvedLocation(index: UInt): Int {
        if (programObject == 0) return -1
        if (index >= uniform.size.toUInt()) return -1
        return uniform[index.toInt()]
    }

    // -----------------------------------------------------------------------
    // Uniform setters — indexed (by reserved-uniform ordinal)
    // -----------------------------------------------------------------------

    fun uniform1i(index: UInt, v: Int) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val cached = value[loc]
            if (cached == null || cached.x != v.toFloat()) {
                glUniform1i(loc, v)
                value[loc] = Vector4(v.toFloat(), 0f, 0f, 0f)
            }
        }
    }

    fun uniform1f(index: UInt, v: Float) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val cached = value[loc]
            if (cached == null || cached.x != v) {
                glUniform1f(loc, v)
                value[loc] = Vector4(v, 0f, 0f, 0f)
            }
        }
    }

    /** Hot-path variant: skips bounds checks and the value-cache comparison. */
    fun fastUniform1f(index: UInt, v: Float) {
        val loc = uniform[index.toInt()]
        glUniform1f(loc, v)
    }

    fun uniform2f(index: UInt, x: Float, y: Float) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(x, y, 0f, 0f)
            if (value[loc] != vec) {
                glUniform2f(loc, x, y)
                value[loc] = vec
            }
        }
    }

    fun uniform3f(index: UInt, x: Float, y: Float, z: Float) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(x, y, z, 0f)
            if (value[loc] != vec) {
                glUniform3f(loc, x, y, z)
                value[loc] = vec
            }
        }
    }

    fun uniform4f(index: UInt, x: Float, y: Float, z: Float, w: Float) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(x, y, z, w)
            if (value[loc] != vec) {
                glUniform4f(loc, x, y, z, w)
                value[loc] = vec
            }
        }
    }

    fun uniform1iv(index: UInt, count: UInt, v: IntArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0].toFloat(), 0f, 0f, 0f)
            if (value[loc] != vec || count != 1u) {
                glUniform1iv(loc, v)
                value[loc] = vec
            }
        }
    }

    fun uniform4iv(index: UInt, count: UInt, v: IntArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0].toFloat(), v[1].toFloat(), v[2].toFloat(), v[3].toFloat())
            if (value[loc] != vec || count != 1u) {
                // C++ uses glUniform1iv for the 4iv case (matches the original bug/behaviour).
                glUniform4iv(loc, v)
                value[loc] = vec
            }
        }
    }

    fun uniform1fv(index: UInt, count: UInt, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0], 0f, 0f, 0f)
            if (value[loc] != vec || count != 1u) {
                glUniform1fv(loc, v)
                value[loc] = vec
            }
        }
    }

    fun uniform2fv(index: UInt, count: UInt, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0], v[1], 0f, 0f)
            if (value[loc] != vec || count != 1u) {
                glUniform2fv(loc, v)
                value[loc] = vec
            }
        }
    }

    fun uniform3fv(index: UInt, count: UInt, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0], v[1], v[2], 0f)
            if (value[loc] != vec || count != 1u) {
                glUniform3fv(loc, v)
                value[loc] = vec
            }
        }
    }

    fun uniform4fv(index: UInt, count: UInt, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0], v[1], v[2], v[3])
            if (value[loc] != vec || count != 1u) {
                glUniform4fv(loc, v)
                value[loc] = vec
            }
        }
    }

    fun uniform4uiv(index: UInt, count: UInt, v: IntArray) {
        // LWJGL GL30.glUniform4uiv takes an IntArray (treated as unsigned).
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0].toFloat(), v[1].toFloat(), v[2].toFloat(), v[3].toFloat())
            if (value[loc] != vec || count != 1u) {
                glUniform4uiv(loc, v)
                value[loc] = vec
            }
        }
    }

    fun uniformMatrix2fv(index: UInt, count: UInt, transpose: Boolean, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) glUniformMatrix2fv(loc, transpose, v)
    }

    fun uniformMatrix3fv(index: UInt, count: UInt, transpose: Boolean, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) glUniformMatrix3fv(loc, transpose, v)
    }

    fun uniformMatrix3x4fv(index: UInt, count: UInt, transpose: Boolean, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) glUniformMatrix3x4fv(loc, transpose, v)
    }

    fun uniformMatrix4fv(index: UInt, count: UInt, transpose: Boolean, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) glUniformMatrix4fv(loc, transpose, v)
    }

    // -----------------------------------------------------------------------
    // Uniform setters — named (looked up in uniformMap)
    // -----------------------------------------------------------------------

    fun uniform1i(uniformName: String, v: Int) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v.toFloat(), 0f, 0f, 0f)
            if (value[loc] != vec) {
                glUniform1i(loc, v)
                value[loc] = vec
            }
        }
    }

    fun uniform2i(uniformName: String, i: Int, j: Int) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(i.toFloat(), j.toFloat(), 0f, 0f)
            if (value[loc] != vec) {
                glUniform2i(loc, i, j)
                value[loc] = vec
            }
        }
    }

    fun uniform1iv(uniformName: String, count: UInt, v: IntArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0].toFloat(), 0f, 0f, 0f)
            if (value[loc] != vec || count != 1u) {
                glUniform1iv(loc, v)
                value[loc] = vec
            }
        }
    }

    fun uniform4iv(uniformName: String, count: UInt, v: IntArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0].toFloat(), v[1].toFloat(), v[2].toFloat(), v[3].toFloat())
            if (value[loc] != vec || count != 1u) {
                glUniform4iv(loc, v)
                value[loc] = vec
            }
        }
    }

    fun uniform1f(uniformName: String, v: Float) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v, 0f, 0f, 0f)
            if (value[loc] != vec) {
                glUniform1f(loc, v)
                value[loc] = vec
            }
        }
    }

    fun uniform2f(uniformName: String, x: Float, y: Float) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(x, y, 0f, 0f)
            if (value[loc] != vec) {
                glUniform2f(loc, x, y)
                value[loc] = vec
            }
        }
    }

    fun uniform3f(uniformName: String, x: Float, y: Float, z: Float) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(x, y, z, 0f)
            if (value[loc] != vec) {
                glUniform3f(loc, x, y, z)
                value[loc] = vec
            }
        }
    }

    fun uniform4f(uniformName: String, x: Float, y: Float, z: Float, w: Float) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(x, y, z, w)
            if (value[loc] != vec) {
                glUniform4f(loc, x, y, z, w)
                value[loc] = vec
            }
        }
    }

    fun uniform1fv(uniformName: String, count: UInt, v: FloatArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0], 0f, 0f, 0f)
            if (value[loc] != vec || count != 1u) {
                glUniform1fv(loc, v)
                value[loc] = vec
            }
        }
    }

    fun uniform2fv(uniformName: String, count: UInt, v: FloatArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0], v[1], 0f, 0f)
            if (value[loc] != vec || count != 1u) {
                glUniform2fv(loc, v)
                value[loc] = vec
            }
        }
    }

    fun uniform3fv(uniformName: String, count: UInt, v: FloatArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0], v[1], v[2], 0f)
            if (value[loc] != vec || count != 1u) {
                glUniform3fv(loc, v)
                value[loc] = vec
            }
        }
    }

    fun uniform4fv(uniformName: String, count: UInt, v: FloatArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0], v[1], v[2], v[3])
            if (value[loc] != vec || count != 1u) {
                glUniform4fv(loc, v)
                value[loc] = vec
            }
        }
    }

    fun uniform4uiv(uniformName: String, count: UInt, v: IntArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0].toFloat(), v[1].toFloat(), v[2].toFloat(), v[3].toFloat())
            if (value[loc] != vec || count != 1u) {
                glUniform4uiv(loc, v)
                value[loc] = vec
            }
        }
    }

    fun uniformMatrix4fv(uniformName: String, count: UInt, transpose: Boolean, v: FloatArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            glUniformMatrix4fv(loc, transpose, v)
        }
    }

    // -----------------------------------------------------------------------
    // Vertex attribute fallback setters
    // -----------------------------------------------------------------------

    /**
     * Sets a generic vertex attribute to a constant value (4-component float).
     * Used for per-object constants that bypass VBO streams.
     * Mirrors: LLGLSLShader::vertexAttrib4f()
     */
    fun vertexAttrib4f(index: UInt, x: Float, y: Float, z: Float, w: Float) {
        val attribLoc = attribute[index.toInt()]
        if (attribLoc > 0) {
            glVertexAttrib4f(attribLoc, x, y, z, w)
        }
    }

    /**
     * Sets a generic vertex attribute to a constant value from a FloatArray.
     * Mirrors: LLGLSLShader::vertexAttrib4fv()
     */
    fun vertexAttrib4fv(index: UInt, v: FloatArray) {
        val attribLoc = attribute[index.toInt()]
        if (attribLoc > 0) {
            glVertexAttrib4fv(attribLoc, v)
        }
    }
}

// ---------------------------------------------------------------------------
// Module-level shader singletons (mirrors C++ global LLGLSLShader objects)
// ---------------------------------------------------------------------------

val gUiProgram          = GLSLShader()
val gSolidColorProgram  = GLSLShader()
val gAlphaMaskProgram   = GLSLShader()
