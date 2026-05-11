package com.firestorm.llrender

import java.util.TreeMap
import java.util.UUID

data class Vector4(val x: Float, val y: Float, val z: Float, val w: Float) {
    constructor(v: FloatArray) : this(v[0], v[1], v[2], v[3])
}

data class Vector3(val x: Float, val y: Float, val z: Float)

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
        for (u in floats) shader.uniform1f(u.uniform.toUInt(), u.value)
        for (u in vectors) shader.uniform4fv(u.uniform.toUInt(), 1u, floatArrayOf(u.value.x, u.value.y, u.value.z, u.value.w))
        for (u in vector3s) shader.uniform3fv(u.uniform.toUInt(), 1u, floatArrayOf(u.value.x, u.value.y, u.value.z))
    }
}

class GLSLShader {

    enum class ShaderConst {
        CLOUD_MOON_DEPTH,
        STAR_DEPTH;
    }

    enum class Group(val id: Int) {
        DEFAULT(0),
        SKY(1),
        WATER(2),
        ANY(3);
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
            ShaderConst.STAR_DEPTH to "LL_SHADER_CONST_STAR_DEPTH"
        )
        val shaderConstVals = mapOf(
            ShaderConst.CLOUD_MOON_DEPTH to "0.99998",
            ShaderConst.STAR_DEPTH to "0.99999"
        )

        val instances: MutableSet<GLSLShader> = mutableSetOf()
        var profileEnabled: Boolean = false
        var canProfile: Boolean = true

        var curBoundShader: UInt = 0u
        var curBoundShaderPtr: GLSLShader? = null
        var indexedTextureChannels: Int = 0

        var maxGltfMaterials: UInt = 0u
        var maxGltfNodes: UInt = 0u

        var totalTimeElapsed: ULong = 0uL
        var totalTrianglesDrawn: UInt = 0u
        var totalSamplesDrawn: ULong = 0uL
        var totalBinds: UInt = 0u

        val globalDefines: TreeMap<String, String> = TreeMap()

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
                if (shader.binds == 0u) {
                    unusedList.add(shader.name)
                } else {
                    shaderList.add(shader.dumpStats())
                }
            }

            result["shaders"] = shaderList
            result["unused"] = unusedList
            val totalTimeMs = totalTimeElapsed.toFloat() / 1_000_000f
            result["totals"] = mapOf(
                "time" to totalTimeMs / 1000.0,
                "binds" to totalBinds,
                "samples" to totalSamplesDrawn,
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

        fun unbind() {
            TODO("GPU: glUseProgram(0); clear curBoundShader and curBoundShaderPtr")
        }
    }

    var programObject: UInt = 0u
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
    var timerQuery: UInt = 0u
    var samplesQuery: UInt = 0u
    var primitivesQuery: UInt = 0u

    var timeElapsed: ULong = 0uL
    var trianglesDrawn: UInt = 0u
    var samplesDrawn: ULong = 0uL
    var binds: UInt = 0u

    var riggedVariant: GLSLShader? = null
    val gltfVariants: MutableList<GLSLShader> = mutableListOf()

    var canBindFast: Boolean = false

    val matHash: UIntArray = UIntArray(8) { 0xFFFFFFFFu }
    var lightHash: UInt = 0xFFFFFFFFu

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
        if (programObject != 0u) {
            TODO("GPU: detach and delete all attached shader objects, then glDeleteProgram(programObject)")
            programObject = 0u
        }
        if (timerQuery != 0u) {
            TODO("GPU: glDeleteQueries for timerQuery, samplesQuery")
            timerQuery = 0u
            samplesQuery = 0u
        }
    }

    fun createShader(): Boolean {
        unloadInternal()
        instances.add(this)
        for (i in matHash.indices) matHash[i] = 0xFFFFFFFFu
        lightHash = 0xFFFFFFFFu

        check(shaderFiles.isNotEmpty()) { "shaderFiles must not be empty" }

        TODO("GPU: glCreateProgram; load/compile shader files; attachShaderFeatures; mapAttributes; mapUniforms; handle indexed texture channels")
    }

    fun attachVertexObject(objectPath: String): Boolean {
        TODO("GPU: glAttachShader for vertex shader object at $objectPath")
    }

    fun attachFragmentObject(objectPath: String): Boolean {
        if (usingBinaryProgram) return true
        TODO("GPU: glAttachShader for fragment shader object at $objectPath")
    }

    fun attachObject(objectHandle: UInt) {
        if (usingBinaryProgram) return
        if (objectHandle != 0u) {
            TODO("GPU: glAttachShader(programObject, $objectHandle)")
        }
    }

    fun attachObjects(objects: UIntArray) {
        if (usingBinaryProgram) return
        for (obj in objects) attachObject(obj)
    }

    fun mapAttributes(): Boolean {
        TODO("GPU: glBindAttribLocation for reserved attribs; link(); glGetAttribLocation readback")
    }

    fun mapUniform(index: Int) {
        TODO("GPU: glGetActiveUniform; accumulate totalUniformSize; glGetUniformLocation; map into uniformMap and uniform list")
    }

    fun mapUniforms(): Boolean {
        TODO("GPU: glGetProgramiv GL_ACTIVE_UNIFORMS; handle diffuseMap ordering; call mapUniform for each; set up UBO bindings via glUniformBlockBinding")
    }

    fun link(suppressErrors: Boolean = false): Boolean {
        TODO("GPU: ShaderMgr.linkProgramObject(programObject, suppressErrors)")
    }

    fun bind() {
        check(programObject != 0u) { "shader not loaded" }
        if (curBoundShader != programObject) {
            curBoundShaderPtr?.readProfileQuery()
            TODO("GPU: LLVertexBuffer.unbind(); glUseProgram(programObject)")
            curBoundShader = programObject
            curBoundShaderPtr = this
            placeProfileQuery()
            TODO("GPU: LLVertexBuffer.setupClientArrays(attributeMask)")
        }
        if (uniformsDirty) {
            TODO("GPU: ShaderMgr.updateShaderUniforms(this)")
            uniformsDirty = false
        }
    }

    fun bind(rigged: Boolean) {
        if (rigged) {
            checkNotNull(riggedVariant) { "no riggedVariant set" }.bind()
        } else {
            bind()
        }
    }

    fun bind(variant: UByte) {
        check(gltfVariants.size == NUM_GLTF_VARIANTS.toInt())
        check(variant < NUM_GLTF_VARIANTS)
        gltfVariants[variant.toInt()].bind()
    }

    fun unbind() {
        TODO("GPU: flush; LLVertexBuffer.unbind(); readProfileQuery; glUseProgram(0); clear curBoundShader/Ptr")
    }

    fun isComplete(): Boolean = programObject != 0u

    fun hash(): UUID {
        TODO("GPU: compute xxHash128 over name, shaderGroup, shaderLevel, shaderFiles, defines, globalDefines, features, GL vendor/renderer/version strings")
    }

    fun clearPermutations() {
        defines.clear()
    }

    fun addPermutation(name: String, value: String) {
        defines[name] = value
    }

    fun addPermutations(defs: Map<String, String>) {
        defines.putAll(defs)
    }

    fun removePermutation(name: String) {
        defines.remove(name)
    }

    fun addConstant(shaderConst: ShaderConst) {
        addPermutation(shaderConstKeys[shaderConst]!!, shaderConstVals[shaderConst]!!)
    }

    fun getUniformLocation(uniformName: String): Int = uniformMap[uniformName] ?: -1

    fun getUniformLocation(index: UInt): Int {
        if (programObject == 0u) return -1
        if (index >= uniform.size.toUInt()) return -1
        return uniform[index.toInt()]
    }

    fun getAttribLocation(attrib: UInt): Int =
        if (attrib < attribute.size.toUInt()) attribute[attrib.toInt()] else -1

    fun mapUniformTextureChannel(location: Int, type: Int, size: Int): Int {
        TODO("GPU: check if type is a sampler type; assign texture channels via glUniform1i/glUniform1iv")
    }

    fun getTextureChannel(uniformIndex: Int): Int = texture[uniformIndex]

    fun enableTexture(uniformIndex: Int, mode: Int = 0): Int {
        if (uniformIndex < 0 || uniformIndex >= texture.size) return -1
        val index = texture[uniformIndex]
        if (index != -1) {
            TODO("GPU: gGL.getTexUnit(index).activate(); enable(mode)")
        }
        return index
    }

    fun disableTexture(uniformIndex: Int, mode: Int = 0): Int {
        if (uniformIndex < 0 || uniformIndex >= texture.size) return -1
        val index = texture[uniformIndex]
        if (index < 0) return index
        TODO("GPU: gGL.getTexUnit(index) disable if active and type matches mode")
    }

    fun bindTexture(uniformName: String, textureName: UInt, mode: Int = 0): Int {
        val channel = getUniformLocation(uniformName)
        return bindTexture(channel, textureName, mode)
    }

    fun bindTexture(uniformIndex: Int, textureName: UInt, mode: Int = 0): Int {
        if (uniformIndex < 0 || uniformIndex >= texture.size) return -1
        val channel = texture[uniformIndex]
        if (channel > -1) {
            TODO("GPU: gGL.getTexUnit(channel).bindFast(textureName)")
        }
        return channel
    }

    fun unbindTexture(uniformName: String, mode: Int = 0): Int {
        val channel = getUniformLocation(uniformName)
        return unbindTexture(channel, mode)
    }

    fun unbindTexture(uniformIndex: Int, mode: Int = 0): Int {
        if (uniformIndex < 0 || uniformIndex >= texture.size) return -1
        val channel = texture[uniformIndex]
        if (channel > -1) {
            TODO("GPU: gGL.getTexUnit(channel).unbindFast(mode)")
        }
        return channel
    }

    fun setMinimumAlpha(minimum: Float) {
        TODO("GPU: flush gGL; uniform1f(MINIMUM_ALPHA index, minimum)")
    }

    fun placeProfileQuery(forRuntime: Boolean = false) {
        if (profileEnabled || forRuntime) {
            TODO("GPU: glGenQueries if needed; glBeginQuery(GL_TIME_ELAPSED, timerQuery); optionally GL_SAMPLES_PASSED, GL_PRIMITIVES_GENERATED")
        }
    }

    fun readProfileQuery(forRuntime: Boolean = false, forceRead: Boolean = false): Boolean {
        if ((profileEnabled || forRuntime) && canProfile) {
            TODO("GPU: glEndQuery; glGetQueryObjectui64v; accumulate timeElapsed, samplesDrawn, trianglesDrawn; update totals")
        }
        return true
    }

    fun clearStats() {
        trianglesDrawn = 0u
        timeElapsed = 0uL
        samplesDrawn = 0uL
        binds = 0u
    }

    fun dumpStats(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        result["name"] = name
        result["files"] = shaderFiles.map { it.first }
        val mega = 1_000_000f
        val giga = 1_000_000_000.0
        val ms = timeElapsed.toFloat() / mega
        val seconds = ms / 1000f
        result["time"] = seconds.toDouble()
        result["binds"] = binds
        result["samples"] = samplesDrawn
        result["triangles"] = trianglesDrawn
        return result
    }

    // --- Uniform setters by index ---

    private fun resolvedLocation(index: UInt): Int {
        if (programObject == 0u) return -1
        if (index >= uniform.size.toUInt()) return -1
        return uniform[index.toInt()]
    }

    fun uniform1i(index: UInt, v: Int) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val cached = value[loc]
            if (cached == null || cached.x != v.toFloat()) {
                TODO("GPU: glUniform1i($loc, $v)")
                value[loc] = Vector4(v.toFloat(), 0f, 0f, 0f)
            }
        }
    }

    fun uniform1f(index: UInt, v: Float) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val cached = value[loc]
            if (cached == null || cached.x != v) {
                TODO("GPU: glUniform1f($loc, $v)")
                value[loc] = Vector4(v, 0f, 0f, 0f)
            }
        }
    }

    fun fastUniform1f(index: UInt, v: Float) {
        val loc = uniform[index.toInt()]
        TODO("GPU: glUniform1f($loc, $v) — no caching check, hot path")
    }

    fun uniform2f(index: UInt, x: Float, y: Float) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(x, y, 0f, 0f)
            if (value[loc] != vec) {
                TODO("GPU: glUniform2f($loc, $x, $y)")
                value[loc] = vec
            }
        }
    }

    fun uniform3f(index: UInt, x: Float, y: Float, z: Float) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(x, y, z, 0f)
            if (value[loc] != vec) {
                TODO("GPU: glUniform3f($loc, $x, $y, $z)")
                value[loc] = vec
            }
        }
    }

    fun uniform4f(index: UInt, x: Float, y: Float, z: Float, w: Float) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(x, y, z, w)
            if (value[loc] != vec) {
                TODO("GPU: glUniform4f($loc, $x, $y, $z, $w)")
                value[loc] = vec
            }
        }
    }

    fun uniform1iv(index: UInt, count: UInt, v: IntArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0].toFloat(), 0f, 0f, 0f)
            if (value[loc] != vec || count != 1u) {
                TODO("GPU: glUniform1iv($loc, $count, v)")
                value[loc] = vec
            }
        }
    }

    fun uniform4iv(index: UInt, count: UInt, v: IntArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0].toFloat(), v[1].toFloat(), v[2].toFloat(), v[3].toFloat())
            if (value[loc] != vec || count != 1u) {
                TODO("GPU: glUniform4iv($loc, $count, v)")
                value[loc] = vec
            }
        }
    }

    fun uniform1fv(index: UInt, count: UInt, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0], 0f, 0f, 0f)
            if (value[loc] != vec || count != 1u) {
                TODO("GPU: glUniform1fv($loc, $count, v)")
                value[loc] = vec
            }
        }
    }

    fun uniform2fv(index: UInt, count: UInt, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0], v[1], 0f, 0f)
            if (value[loc] != vec || count != 1u) {
                TODO("GPU: glUniform2fv($loc, $count, v)")
                value[loc] = vec
            }
        }
    }

    fun uniform3fv(index: UInt, count: UInt, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0], v[1], v[2], 0f)
            if (value[loc] != vec || count != 1u) {
                TODO("GPU: glUniform3fv($loc, $count, v)")
                value[loc] = vec
            }
        }
    }

    fun uniform4fv(index: UInt, count: UInt, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0], v[1], v[2], v[3])
            if (value[loc] != vec || count != 1u) {
                TODO("GPU: glUniform4fv($loc, $count, v)")
                value[loc] = vec
            }
        }
    }

    fun uniform4uiv(index: UInt, count: UInt, v: UIntArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0].toFloat(), v[1].toFloat(), v[2].toFloat(), v[3].toFloat())
            if (value[loc] != vec || count != 1u) {
                TODO("GPU: glUniform4uiv($loc, $count, v)")
                value[loc] = vec
            }
        }
    }

    fun uniformMatrix2fv(index: UInt, count: UInt, transpose: Boolean, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) TODO("GPU: glUniformMatrix2fv($loc, $count, $transpose, v)")
    }

    fun uniformMatrix3fv(index: UInt, count: UInt, transpose: Boolean, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) TODO("GPU: glUniformMatrix3fv($loc, $count, $transpose, v)")
    }

    fun uniformMatrix3x4fv(index: UInt, count: UInt, transpose: Boolean, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) TODO("GPU: glUniformMatrix3x4fv($loc, $count, $transpose, v)")
    }

    fun uniformMatrix4fv(index: UInt, count: UInt, transpose: Boolean, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) TODO("GPU: glUniformMatrix4fv($loc, $count, $transpose, v)")
    }

    // --- Named-uniform overloads ---

    fun uniform1i(uniformName: String, v: Int) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v.toFloat(), 0f, 0f, 0f)
            if (value[loc] != vec) {
                TODO("GPU: glUniform1i($loc, $v)")
                value[loc] = vec
            }
        }
    }

    fun uniform2i(uniformName: String, i: Int, j: Int) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(i.toFloat(), j.toFloat(), 0f, 0f)
            if (value[loc] != vec) {
                TODO("GPU: glUniform2i($loc, $i, $j)")
                value[loc] = vec
            }
        }
    }

    fun uniform1iv(uniformName: String, count: UInt, v: IntArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0].toFloat(), 0f, 0f, 0f)
            if (value[loc] != vec || count != 1u) {
                TODO("GPU: glUniform1iv($loc, $count, v)")
                value[loc] = vec
            }
        }
    }

    fun uniform4iv(uniformName: String, count: UInt, v: IntArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0].toFloat(), v[1].toFloat(), v[2].toFloat(), v[3].toFloat())
            if (value[loc] != vec || count != 1u) {
                TODO("GPU: glUniform4iv($loc, $count, v)")
                value[loc] = vec
            }
        }
    }

    fun uniform1f(uniformName: String, v: Float) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v, 0f, 0f, 0f)
            if (value[loc] != vec) {
                TODO("GPU: glUniform1f($loc, $v)")
                value[loc] = vec
            }
        }
    }

    fun uniform2f(uniformName: String, x: Float, y: Float) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(x, y, 0f, 0f)
            if (value[loc] != vec) {
                TODO("GPU: glUniform2f($loc, $x, $y)")
                value[loc] = vec
            }
        }
    }

    fun uniform3f(uniformName: String, x: Float, y: Float, z: Float) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(x, y, z, 0f)
            if (value[loc] != vec) {
                TODO("GPU: glUniform3f($loc, $x, $y, $z)")
                value[loc] = vec
            }
        }
    }

    fun uniform4f(uniformName: String, x: Float, y: Float, z: Float, w: Float) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(x, y, z, w)
            if (value[loc] != vec) {
                TODO("GPU: glUniform4f($loc, $x, $y, $z, $w)")
                value[loc] = vec
            }
        }
    }

    fun uniform1fv(uniformName: String, count: UInt, v: FloatArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0], 0f, 0f, 0f)
            if (value[loc] != vec || count != 1u) {
                TODO("GPU: glUniform1fv($loc, $count, v)")
                value[loc] = vec
            }
        }
    }

    fun uniform2fv(uniformName: String, count: UInt, v: FloatArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0], v[1], 0f, 0f)
            if (value[loc] != vec || count != 1u) {
                TODO("GPU: glUniform2fv($loc, $count, v)")
                value[loc] = vec
            }
        }
    }

    fun uniform3fv(uniformName: String, count: UInt, v: FloatArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0], v[1], v[2], 0f)
            if (value[loc] != vec || count != 1u) {
                TODO("GPU: glUniform3fv($loc, $count, v)")
                value[loc] = vec
            }
        }
    }

    fun uniform4fv(uniformName: String, count: UInt, v: FloatArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0], v[1], v[2], v[3])
            if (value[loc] != vec || count != 1u) {
                TODO("GPU: glUniform4fv($loc, $count, v)")
                value[loc] = vec
            }
        }
    }

    fun uniform4uiv(uniformName: String, count: UInt, v: UIntArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0].toFloat(), v[1].toFloat(), v[2].toFloat(), v[3].toFloat())
            if (value[loc] != vec || count != 1u) {
                TODO("GPU: glUniform4uiv($loc, $count, v)")
                value[loc] = vec
            }
        }
    }

    fun uniformMatrix4fv(uniformName: String, count: UInt, transpose: Boolean, v: FloatArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            TODO("GPU: glUniformMatrix4fv($loc, $count, $transpose, v)")
        }
    }

    fun vertexAttrib4f(index: UInt, x: Float, y: Float, z: Float, w: Float) {
        if (attribute[index.toInt()] > 0) {
            TODO("GPU: glVertexAttrib4f(${attribute[index.toInt()]}, $x, $y, $z, $w)")
        }
    }

    fun vertexAttrib4fv(index: UInt, v: FloatArray) {
        if (attribute[index.toInt()] > 0) {
            TODO("GPU: glVertexAttrib4fv(${attribute[index.toInt()]}, v)")
        }
    }
}

val gUiProgram = GLSLShader()
val gSolidColorProgram = GLSLShader()
val gAlphaMaskProgram = GLSLShader()
