package com.firestorm.llrender

import java.util.TreeMap
import java.util.UUID
import java.security.MessageDigest
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
            GpuBackend.current.useProgram(0)
            curBoundShader = 0u
            curBoundShaderPtr = null
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

    private val attachedObjects: MutableSet<UInt> = mutableSetOf()

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
        val gl = GpuBackend.current
        if (programObject != 0u) {
            for (obj in attachedObjects.toList()) {
                gl.detachShader(programObject.toInt(), obj.toInt())
                gl.deleteShader(obj.toInt())
            }
            attachedObjects.clear()
            gl.deleteProgram(programObject.toInt())
            programObject = 0u
        }
        if (timerQuery != 0u) {
            gl.deleteQueries(intArrayOf(timerQuery.toInt(), samplesQuery.toInt(), primitivesQuery.toInt()))
            timerQuery = 0u
            samplesQuery = 0u
            primitivesQuery = 0u
        }
    }

    fun createShader(): Boolean {
        unloadInternal()
        instances.add(this)
        for (i in matHash.indices) matHash[i] = 0xFFFFFFFFu
        lightHash = 0xFFFFFFFFu

        check(shaderFiles.isNotEmpty()) { "shaderFiles must not be empty" }

        val gl = GpuBackend.current
        programObject = gl.createProgram().toUInt()
        check(programObject != 0u) { "glCreateProgram failed" }

        for ((file, type) in shaderFiles) {
            val ok = when (type) {
                GL.VERTEX_SHADER -> attachVertexObject(file)
                GL.FRAGMENT_SHADER -> attachFragmentObject(file)
                GL.GEOMETRY_SHADER -> attachGeometryObject(file)
                else -> false
            }
            if (!ok) return false
        }

        ShaderMgr.instance?.attachShaderFeatures(toGlslShader())

        if (!mapAttributes()) return false
        if (!link()) return false
        if (!mapUniforms()) return false

        return true
    }

    private fun toGlslShader(): GlslShader {
        val s = GlslShader()
        s.name = name
        s.programObject = programObject
        s.riggedVariant = null
        return s
    }

    fun attachVertexObject(objectPath: String): Boolean {
        val handle = ShaderMgr.compileShaderFile(objectPath, GL.VERTEX_SHADER, defines)
        if (handle == 0u) return false
        attachObject(handle)
        return true
    }

    fun attachFragmentObject(objectPath: String): Boolean {
        if (usingBinaryProgram) return true
        val handle = ShaderMgr.compileShaderFile(objectPath, GL.FRAGMENT_SHADER, defines)
        if (handle == 0u) return false
        attachObject(handle)
        return true
    }

    fun attachGeometryObject(objectPath: String): Boolean {
        if (usingBinaryProgram) return true
        val handle = ShaderMgr.compileShaderFile(objectPath, GL.GEOMETRY_SHADER, defines)
        if (handle == 0u) return false
        attachObject(handle)
        return true
    }

    fun attachObject(objectHandle: UInt) {
        if (usingBinaryProgram) return
        if (objectHandle != 0u) {
            GpuBackend.current.attachShader(programObject.toInt(), objectHandle.toInt())
            attachedObjects.add(objectHandle)
        }
    }

    fun attachObjects(objects: UIntArray) {
        if (usingBinaryProgram) return
        for (obj in objects) attachObject(obj)
    }

    fun mapAttributes(): Boolean {
        val gl = GpuBackend.current
        val mgr = ShaderMgr.instance ?: return true
        attribute.clear()
        for ((idx, attrName) in mgr.reservedAttribs.withIndex()) {
            gl.bindAttribLocation(programObject.toInt(), idx, attrName)
        }
        if (!link()) return false
        for (attrName in mgr.reservedAttribs) {
            attribute.add(gl.getAttribLocation(programObject.toInt(), attrName))
        }
        return true
    }

    fun mapUniform(index: Int) {
        val gl = GpuBackend.current
        val au = gl.getActiveUniform(programObject.toInt(), index)
        totalUniformSize += au.size
        val location = gl.getUniformLocation(programObject.toInt(), au.name)
        uniformMap[au.name] = location
        uniform.add(location)
        // Treat sampler-typed uniforms as texture channels.
        mapUniformTextureChannel(location, au.type, au.size)
    }

    fun mapUniforms(): Boolean {
        val gl = GpuBackend.current
        uniform.clear()
        uniformMap.clear()
        texture.clear()
        val count = gl.getActiveUniformCount(programObject.toInt())
        repeat(count) { mapUniform(it) }
        // Bind UBOs that are present in the program.
        UniformBlock.values().forEach { block ->
            val idx = gl.getUniformBlockIndex(programObject.toInt(), block.name.lowercase())
            if (idx >= 0) gl.uniformBlockBinding(programObject.toInt(), idx, block.id)
        }
        return true
    }

    fun link(suppressErrors: Boolean = false): Boolean {
        return ShaderMgr.linkProgramObject(programObject, suppressErrors)
    }

    fun bind() {
        check(programObject != 0u) { "shader not loaded" }
        val gl = GpuBackend.current
        if (curBoundShader != programObject) {
            curBoundShaderPtr?.readProfileQuery()
            gl.useProgram(programObject.toInt())
            curBoundShader = programObject
            curBoundShaderPtr = this
            placeProfileQuery()
            binds++
        }
        if (uniformsDirty) {
            ShaderMgr.instance?.updateShaderUniforms(toGlslShader())
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
        readProfileQuery()
        GpuBackend.current.useProgram(0)
        curBoundShader = 0u
        curBoundShaderPtr = null
    }

    fun isComplete(): Boolean = programObject != 0u

    fun hash(): UUID {
        // SHA-256 over the salient inputs, truncated to 128 bits. Identical
        // inputs always yield the same UUID, so callers can use this for cache
        // keys (binary shader cache, debug log correlation, etc.).
        val md = MessageDigest.getInstance("SHA-256")
        fun feed(s: String) {
            val b = s.toByteArray(Charsets.UTF_8)
            md.update(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(b.size).array())
            md.update(b)
        }
        feed(name)
        feed(shaderGroup.name)
        feed(shaderLevel.toString())
        for ((file, type) in shaderFiles) { feed(file); feed(type.toString()) }
        defines.forEach { (k, v) -> feed(k); feed(v) }
        globalDefines.forEach { (k, v) -> feed(k); feed(v) }
        feed(features.toString())
        feed(GpuBackend.current.vendor())
        feed(GpuBackend.current.renderer())
        feed(GpuBackend.current.versionString())
        val digest = md.digest()
        val msb = ByteBuffer.wrap(digest, 0, 8).order(ByteOrder.BIG_ENDIAN).long
        val lsb = ByteBuffer.wrap(digest, 8, 8).order(ByteOrder.BIG_ENDIAN).long
        shaderHash = UUID(msb, lsb)
        return shaderHash
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
        val isSampler = type == GL.SAMPLER_2D || type == GL.SAMPLER_3D ||
            type == GL.SAMPLER_CUBE || type == GL.SAMPLER_2D_ARRAY ||
            type == GL.SAMPLER_CUBE_MAP_ARRAY || type == GL.SAMPLER_2D_SHADOW
        if (!isSampler) return -1
        val channel = activeTextureChannels
        if (size > 1) {
            val channels = IntArray(size) { channel + it }
            GpuBackend.current.uniform1iv(location, size, channels)
        } else {
            GpuBackend.current.uniform1i(location, channel)
        }
        texture.add(channel)
        activeTextureChannels += size
        return channel
    }

    fun getTextureChannel(uniformIndex: Int): Int = texture[uniformIndex]

    fun enableTexture(uniformIndex: Int, mode: Int = 0): Int {
        if (uniformIndex < 0 || uniformIndex >= texture.size) return -1
        val index = texture[uniformIndex]
        if (index != -1) {
            GpuBackend.current.activeTexture(GL.TEXTURE0 + index)
        }
        return index
    }

    fun disableTexture(uniformIndex: Int, mode: Int = 0): Int {
        if (uniformIndex < 0 || uniformIndex >= texture.size) return -1
        val index = texture[uniformIndex]
        if (index < 0) return index
        val gl = GpuBackend.current
        gl.activeTexture(GL.TEXTURE0 + index)
        gl.bindTexture(GL.TEXTURE_2D, 0)
        return index
    }

    fun bindTexture(uniformName: String, textureName: UInt, mode: Int = 0): Int {
        val channel = getUniformLocation(uniformName)
        return bindTexture(channel, textureName, mode)
    }

    fun bindTexture(uniformIndex: Int, textureName: UInt, mode: Int = 0): Int {
        if (uniformIndex < 0 || uniformIndex >= texture.size) return -1
        val channel = texture[uniformIndex]
        if (channel > -1) {
            val gl = GpuBackend.current
            gl.activeTexture(GL.TEXTURE0 + channel)
            gl.bindTexture(GL.TEXTURE_2D, textureName.toInt())
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
            val gl = GpuBackend.current
            gl.activeTexture(GL.TEXTURE0 + channel)
            gl.bindTexture(GL.TEXTURE_2D, 0)
        }
        return channel
    }

    fun setMinimumAlpha(minimum: Float) {
        val loc = uniformMap["minimum_alpha"] ?: return
        GpuBackend.current.uniform1f(loc, minimum)
    }

    fun placeProfileQuery(forRuntime: Boolean = false) {
        if (!(profileEnabled || forRuntime)) return
        val gl = GpuBackend.current
        if (timerQuery == 0u) {
            val handles = gl.genQueries(3)
            timerQuery = handles[0].toUInt()
            samplesQuery = handles[1].toUInt()
            primitivesQuery = handles[2].toUInt()
        }
        gl.beginQuery(GL.TIME_ELAPSED, timerQuery.toInt())
        gl.beginQuery(GL.SAMPLES_PASSED, samplesQuery.toInt())
        gl.beginQuery(GL.PRIMITIVES_GENERATED, primitivesQuery.toInt())
        profilePending = true
    }

    fun readProfileQuery(forRuntime: Boolean = false, forceRead: Boolean = false): Boolean {
        if (!profilePending) return true
        if ((profileEnabled || forRuntime) && canProfile) {
            val gl = GpuBackend.current
            gl.endQuery(GL.TIME_ELAPSED)
            gl.endQuery(GL.SAMPLES_PASSED)
            gl.endQuery(GL.PRIMITIVES_GENERATED)
            val time = gl.getQueryObjectui64(timerQuery.toInt())
            val samples = gl.getQueryObjectui64(samplesQuery.toInt())
            val prims = gl.getQueryObjectui64(primitivesQuery.toInt())
            timeElapsed += time.toULong()
            samplesDrawn += samples.toULong()
            trianglesDrawn += prims.toUInt()
            totalTimeElapsed += time.toULong()
            totalSamplesDrawn += samples.toULong()
            totalTrianglesDrawn += prims.toUInt()
            totalBinds++
            profilePending = false
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
                GpuBackend.current.uniform1i(loc, v)
                value[loc] = Vector4(v.toFloat(), 0f, 0f, 0f)
            }
        }
    }

    fun uniform1f(index: UInt, v: Float) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val cached = value[loc]
            if (cached == null || cached.x != v) {
                GpuBackend.current.uniform1f(loc, v)
                value[loc] = Vector4(v, 0f, 0f, 0f)
            }
        }
    }

    fun fastUniform1f(index: UInt, v: Float) {
        val loc = uniform[index.toInt()]
        GpuBackend.current.uniform1f(loc, v)
    }

    fun uniform2f(index: UInt, x: Float, y: Float) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(x, y, 0f, 0f)
            if (value[loc] != vec) {
                GpuBackend.current.uniform2f(loc, x, y)
                value[loc] = vec
            }
        }
    }

    fun uniform3f(index: UInt, x: Float, y: Float, z: Float) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(x, y, z, 0f)
            if (value[loc] != vec) {
                GpuBackend.current.uniform3f(loc, x, y, z)
                value[loc] = vec
            }
        }
    }

    fun uniform4f(index: UInt, x: Float, y: Float, z: Float, w: Float) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(x, y, z, w)
            if (value[loc] != vec) {
                GpuBackend.current.uniform4f(loc, x, y, z, w)
                value[loc] = vec
            }
        }
    }

    fun uniform1iv(index: UInt, count: UInt, v: IntArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0].toFloat(), 0f, 0f, 0f)
            if (value[loc] != vec || count != 1u) {
                GpuBackend.current.uniform1iv(loc, count.toInt(), v)
                value[loc] = vec
            }
        }
    }

    fun uniform4iv(index: UInt, count: UInt, v: IntArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0].toFloat(), v[1].toFloat(), v[2].toFloat(), v[3].toFloat())
            if (value[loc] != vec || count != 1u) {
                GpuBackend.current.uniform4iv(loc, count.toInt(), v)
                value[loc] = vec
            }
        }
    }

    fun uniform1fv(index: UInt, count: UInt, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0], 0f, 0f, 0f)
            if (value[loc] != vec || count != 1u) {
                GpuBackend.current.uniform1fv(loc, count.toInt(), v)
                value[loc] = vec
            }
        }
    }

    fun uniform2fv(index: UInt, count: UInt, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0], v[1], 0f, 0f)
            if (value[loc] != vec || count != 1u) {
                GpuBackend.current.uniform2fv(loc, count.toInt(), v)
                value[loc] = vec
            }
        }
    }

    fun uniform3fv(index: UInt, count: UInt, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0], v[1], v[2], 0f)
            if (value[loc] != vec || count != 1u) {
                GpuBackend.current.uniform3fv(loc, count.toInt(), v)
                value[loc] = vec
            }
        }
    }

    fun uniform4fv(index: UInt, count: UInt, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0], v[1], v[2], v[3])
            if (value[loc] != vec || count != 1u) {
                GpuBackend.current.uniform4fv(loc, count.toInt(), v)
                value[loc] = vec
            }
        }
    }

    fun uniform4uiv(index: UInt, count: UInt, v: UIntArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) {
            val vec = Vector4(v[0].toFloat(), v[1].toFloat(), v[2].toFloat(), v[3].toFloat())
            if (value[loc] != vec || count != 1u) {
                GpuBackend.current.uniform4uiv(loc, count.toInt(), v.toIntArrayUnsigned())
                value[loc] = vec
            }
        }
    }

    fun uniformMatrix2fv(index: UInt, count: UInt, transpose: Boolean, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) GpuBackend.current.uniformMatrix2fv(loc, count.toInt(), transpose, v)
    }

    fun uniformMatrix3fv(index: UInt, count: UInt, transpose: Boolean, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) GpuBackend.current.uniformMatrix3fv(loc, count.toInt(), transpose, v)
    }

    fun uniformMatrix3x4fv(index: UInt, count: UInt, transpose: Boolean, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) GpuBackend.current.uniformMatrix3x4fv(loc, count.toInt(), transpose, v)
    }

    fun uniformMatrix4fv(index: UInt, count: UInt, transpose: Boolean, v: FloatArray) {
        val loc = resolvedLocation(index)
        if (loc >= 0) GpuBackend.current.uniformMatrix4fv(loc, count.toInt(), transpose, v)
    }

    // --- Named-uniform overloads ---

    fun uniform1i(uniformName: String, v: Int) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v.toFloat(), 0f, 0f, 0f)
            if (value[loc] != vec) {
                GpuBackend.current.uniform1i(loc, v)
                value[loc] = vec
            }
        }
    }

    fun uniform2i(uniformName: String, i: Int, j: Int) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(i.toFloat(), j.toFloat(), 0f, 0f)
            if (value[loc] != vec) {
                GpuBackend.current.uniform4iv(loc, 1, intArrayOf(i, j, 0, 0))
                value[loc] = vec
            }
        }
    }

    fun uniform1iv(uniformName: String, count: UInt, v: IntArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0].toFloat(), 0f, 0f, 0f)
            if (value[loc] != vec || count != 1u) {
                GpuBackend.current.uniform1iv(loc, count.toInt(), v)
                value[loc] = vec
            }
        }
    }

    fun uniform4iv(uniformName: String, count: UInt, v: IntArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0].toFloat(), v[1].toFloat(), v[2].toFloat(), v[3].toFloat())
            if (value[loc] != vec || count != 1u) {
                GpuBackend.current.uniform4iv(loc, count.toInt(), v)
                value[loc] = vec
            }
        }
    }

    fun uniform1f(uniformName: String, v: Float) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v, 0f, 0f, 0f)
            if (value[loc] != vec) {
                GpuBackend.current.uniform1f(loc, v)
                value[loc] = vec
            }
        }
    }

    fun uniform2f(uniformName: String, x: Float, y: Float) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(x, y, 0f, 0f)
            if (value[loc] != vec) {
                GpuBackend.current.uniform2f(loc, x, y)
                value[loc] = vec
            }
        }
    }

    fun uniform3f(uniformName: String, x: Float, y: Float, z: Float) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(x, y, z, 0f)
            if (value[loc] != vec) {
                GpuBackend.current.uniform3f(loc, x, y, z)
                value[loc] = vec
            }
        }
    }

    fun uniform4f(uniformName: String, x: Float, y: Float, z: Float, w: Float) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(x, y, z, w)
            if (value[loc] != vec) {
                GpuBackend.current.uniform4f(loc, x, y, z, w)
                value[loc] = vec
            }
        }
    }

    fun uniform1fv(uniformName: String, count: UInt, v: FloatArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0], 0f, 0f, 0f)
            if (value[loc] != vec || count != 1u) {
                GpuBackend.current.uniform1fv(loc, count.toInt(), v)
                value[loc] = vec
            }
        }
    }

    fun uniform2fv(uniformName: String, count: UInt, v: FloatArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0], v[1], 0f, 0f)
            if (value[loc] != vec || count != 1u) {
                GpuBackend.current.uniform2fv(loc, count.toInt(), v)
                value[loc] = vec
            }
        }
    }

    fun uniform3fv(uniformName: String, count: UInt, v: FloatArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0], v[1], v[2], 0f)
            if (value[loc] != vec || count != 1u) {
                GpuBackend.current.uniform3fv(loc, count.toInt(), v)
                value[loc] = vec
            }
        }
    }

    fun uniform4fv(uniformName: String, count: UInt, v: FloatArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0], v[1], v[2], v[3])
            if (value[loc] != vec || count != 1u) {
                GpuBackend.current.uniform4fv(loc, count.toInt(), v)
                value[loc] = vec
            }
        }
    }

    fun uniform4uiv(uniformName: String, count: UInt, v: UIntArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            val vec = Vector4(v[0].toFloat(), v[1].toFloat(), v[2].toFloat(), v[3].toFloat())
            if (value[loc] != vec || count != 1u) {
                GpuBackend.current.uniform4uiv(loc, count.toInt(), v.toIntArrayUnsigned())
                value[loc] = vec
            }
        }
    }

    fun uniformMatrix4fv(uniformName: String, count: UInt, transpose: Boolean, v: FloatArray) {
        val loc = getUniformLocation(uniformName)
        if (loc >= 0) {
            GpuBackend.current.uniformMatrix4fv(loc, count.toInt(), transpose, v)
        }
    }

    fun vertexAttrib4f(index: UInt, x: Float, y: Float, z: Float, w: Float) {
        if (attribute[index.toInt()] > 0) {
            GpuBackend.current.vertexAttrib4f(attribute[index.toInt()], x, y, z, w)
        }
    }

    fun vertexAttrib4fv(index: UInt, v: FloatArray) {
        if (attribute[index.toInt()] > 0) {
            GpuBackend.current.vertexAttrib4fv(attribute[index.toInt()], v)
        }
    }
}

private fun UIntArray.toIntArrayUnsigned(): IntArray = IntArray(size) { this[it].toInt() }

val gUiProgram = GLSLShader()
val gSolidColorProgram = GLSLShader()
val gAlphaMaskProgram = GLSLShader()
