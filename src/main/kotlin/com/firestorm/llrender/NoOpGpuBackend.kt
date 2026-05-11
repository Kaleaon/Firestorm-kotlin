package com.firestorm.llrender

import java.util.concurrent.atomic.AtomicInteger

/**
 * Default `GpuBackend` for unit tests and pre-context-init code.
 *
 * Mutating calls are recorded just enough to keep the higher-level state
 * machine self-consistent: handles are minted from a monotonic counter, state
 * is cached so `isEnabled`/`getInteger` return what was last set, and the
 * shader-compile / program-link paths always succeed.
 */
object NoOpGpuBackend : GpuBackend {

    private val nextHandle = AtomicInteger(1)
    private val enabledCaps: MutableSet<Int> = mutableSetOf()
    private val intState: MutableMap<Int, Int> = mutableMapOf()
    private val shaderSources: MutableMap<Int, String> = mutableMapOf()
    private val programs: MutableMap<Int, MutableSet<Int>> = mutableMapOf()
    private val uniformLocations: MutableMap<Pair<Int, String>, Int> = mutableMapOf()
    private val attribLocations: MutableMap<Pair<Int, String>, Int> = mutableMapOf()
    private val activeUniforms: MutableMap<Int, MutableList<ActiveUniform>> = mutableMapOf()

    private fun fresh(): Int = nextHandle.getAndIncrement()

    override fun majorVersion(): Int = 3
    override fun minorVersion(): Int = 2
    override fun glslVersion(): String = "OpenGL ES GLSL ES 3.20 (NoOp)"
    override fun vendor(): String = "Firestorm"
    override fun renderer(): String = "NoOpGpuBackend"
    override fun versionString(): String = "OpenGL ES 3.2 (NoOp)"
    override fun getInteger(name: Int): Int = intState[name] ?: 0
    override fun getError(): Int = 0

    override fun enable(cap: Int) { enabledCaps.add(cap) }
    override fun disable(cap: Int) { enabledCaps.remove(cap) }
    override fun isEnabled(cap: Int): Boolean = cap in enabledCaps
    override fun depthFunc(func: Int) { intState[GL_DEPTH_FUNC] = func }
    override fun depthMask(flag: Boolean) { intState[GL_DEPTH_WRITEMASK] = if (flag) 1 else 0 }
    override fun depthRangef(near: Float, far: Float) {}
    override fun cullFace(mode: Int) { intState[GL_CULL_FACE_MODE] = mode }
    override fun frontFace(mode: Int) { intState[GL_FRONT_FACE] = mode }
    override fun blendFunc(sFactor: Int, dFactor: Int) {}
    override fun blendFuncSeparate(srcRgb: Int, dstRgb: Int, srcAlpha: Int, dstAlpha: Int) {}
    override fun viewport(x: Int, y: Int, width: Int, height: Int) {}
    override fun scissor(x: Int, y: Int, width: Int, height: Int) {}
    override fun clearColor(r: Float, g: Float, b: Float, a: Float) {}
    override fun clearDepthf(depth: Float) {}
    override fun clearStencil(s: Int) {}
    override fun clear(mask: Int) {}
    override fun pixelStorei(pname: Int, param: Int) {}

    override fun createShader(type: Int): Int = fresh()
    override fun shaderSource(shader: Int, source: String) { shaderSources[shader] = source }
    override fun compileShader(shader: Int): Boolean = (shaderSources[shader]?.isNotBlank() == true)
    override fun getShaderInfoLog(shader: Int): String =
        if (shaderSources[shader].isNullOrBlank()) "shader source is empty" else ""
    override fun deleteShader(shader: Int) { shaderSources.remove(shader) }

    override fun createProgram(): Int {
        val p = fresh()
        programs[p] = mutableSetOf()
        return p
    }
    override fun attachShader(program: Int, shader: Int) { programs[program]?.add(shader) }
    override fun detachShader(program: Int, shader: Int) { programs[program]?.remove(shader) }
    override fun bindAttribLocation(program: Int, index: Int, name: String) {
        attribLocations[program to name] = index
    }
    override fun linkProgram(program: Int): Boolean = programs[program]?.isNotEmpty() == true
    override fun validateProgram(program: Int): Boolean = programs[program] != null
    override fun getProgramInfoLog(program: Int): String = ""
    override fun useProgram(program: Int) {}
    override fun deleteProgram(program: Int) {
        programs.remove(program)
        uniformLocations.keys.removeAll { it.first == program }
        attribLocations.keys.removeAll { it.first == program }
        activeUniforms.remove(program)
    }
    override fun getActiveUniformCount(program: Int): Int = activeUniforms[program]?.size ?: 0
    override fun getActiveUniform(program: Int, index: Int): ActiveUniform =
        activeUniforms[program]?.getOrNull(index)
            ?: ActiveUniform("uniform_$index", 0, 1, index)
    override fun getUniformLocation(program: Int, name: String): Int =
        uniformLocations.getOrPut(program to name) { uniformLocations.size + 1 }
    override fun getAttribLocation(program: Int, name: String): Int =
        attribLocations.getOrPut(program to name) { attribLocations.size }
    override fun uniformBlockBinding(program: Int, blockIndex: Int, binding: Int) {}
    override fun getUniformBlockIndex(program: Int, name: String): Int = -1

    override fun uniform1i(location: Int, x: Int) {}
    override fun uniform1f(location: Int, x: Float) {}
    override fun uniform2f(location: Int, x: Float, y: Float) {}
    override fun uniform3f(location: Int, x: Float, y: Float, z: Float) {}
    override fun uniform4f(location: Int, x: Float, y: Float, z: Float, w: Float) {}
    override fun uniform1iv(location: Int, count: Int, value: IntArray) {}
    override fun uniform4iv(location: Int, count: Int, value: IntArray) {}
    override fun uniform1fv(location: Int, count: Int, value: FloatArray) {}
    override fun uniform2fv(location: Int, count: Int, value: FloatArray) {}
    override fun uniform3fv(location: Int, count: Int, value: FloatArray) {}
    override fun uniform4fv(location: Int, count: Int, value: FloatArray) {}
    override fun uniform4uiv(location: Int, count: Int, value: IntArray) {}
    override fun uniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatArray) {}
    override fun uniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatArray) {}
    override fun uniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray) {}
    override fun uniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray) {}
    override fun vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float) {}
    override fun vertexAttrib4fv(index: Int, value: FloatArray) {}

    override fun genTextures(n: Int): IntArray = IntArray(n) { fresh() }
    override fun deleteTextures(textures: IntArray) {}
    override fun bindTexture(target: Int, texture: Int) {}
    override fun activeTexture(unit: Int) {}
    override fun texParameteri(target: Int, pname: Int, param: Int) {}
    override fun texParameterf(target: Int, pname: Int, param: Float) {}
    override fun texImage2D(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, format: Int, type: Int, data: ByteArray?) {}
    override fun texSubImage2D(target: Int, level: Int, xOffset: Int, yOffset: Int, width: Int, height: Int, format: Int, type: Int, data: ByteArray) {}
    override fun copyTexImage2D(target: Int, level: Int, internalFormat: Int, x: Int, y: Int, width: Int, height: Int, border: Int) {}
    override fun generateMipmap(target: Int) {}

    override fun genFramebuffers(n: Int): IntArray = IntArray(n) { fresh() }
    override fun deleteFramebuffers(fbs: IntArray) {}
    override fun bindFramebuffer(target: Int, fb: Int) {}
    override fun framebufferTexture2D(target: Int, attachment: Int, texTarget: Int, texture: Int, level: Int) {}
    override fun drawBuffers(buffers: IntArray) {}
    override fun readBuffer(src: Int) {}
    override fun checkFramebufferStatus(target: Int): Int = GL_FRAMEBUFFER_COMPLETE

    override fun debugMessageInsert(source: Int, type: Int, id: Int, severity: Int, message: String) {}

    override fun genQueries(n: Int): IntArray = IntArray(n) { fresh() }
    override fun deleteQueries(queries: IntArray) {}
    override fun beginQuery(target: Int, id: Int) {}
    override fun endQuery(target: Int) {}
    override fun getQueryObjectui64(id: Int): Long = 0L

    private const val GL_DEPTH_FUNC = 0x0B74
    private const val GL_DEPTH_WRITEMASK = 0x0B72
    private const val GL_CULL_FACE_MODE = 0x0B45
    private const val GL_FRONT_FACE = 0x0B46
    private const val GL_FRAMEBUFFER_COMPLETE = 0x8CD5
}
