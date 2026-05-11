package com.firestorm.llrender

/**
 * Platform-neutral surface for the OpenGL ES 3.2 calls used by the renderer.
 *
 * The pure-Kotlin `llrender` package cannot link against `android.opengl.*`, so
 * GL invocations are routed through this interface. The Android app module
 * installs `Es32GpuBackend` once the EGL 3.2 context is current; in unit tests
 * the default `NoOpGpuBackend` lets the renderer's bookkeeping run without a
 * live GPU.
 *
 * Handles (programs, shaders, textures, framebuffers, queries) are passed
 * around as `Int` to mirror the GLES native type. The "0" value always means
 * "unallocated" / "default framebuffer" as on the wire.
 */
interface GpuBackend {

    // -------- extension query ------------------------------------------------

    fun extensionSupported(name: String): Boolean

    // -------- context info --------------------------------------------------

    fun majorVersion(): Int
    fun minorVersion(): Int
    fun glslVersion(): String
    fun vendor(): String
    fun renderer(): String
    fun versionString(): String
    fun getInteger(name: Int): Int
    fun getError(): Int

    // -------- pipeline state ------------------------------------------------

    fun enable(cap: Int)
    fun disable(cap: Int)
    fun isEnabled(cap: Int): Boolean
    fun depthFunc(func: Int)
    fun depthMask(flag: Boolean)
    fun depthRangef(near: Float, far: Float)
    fun cullFace(mode: Int)
    fun frontFace(mode: Int)
    fun blendFunc(sFactor: Int, dFactor: Int)
    fun blendFuncSeparate(srcRgb: Int, dstRgb: Int, srcAlpha: Int, dstAlpha: Int)
    fun viewport(x: Int, y: Int, width: Int, height: Int)
    fun scissor(x: Int, y: Int, width: Int, height: Int)
    fun clearColor(r: Float, g: Float, b: Float, a: Float)
    fun clearDepthf(depth: Float)
    fun clearStencil(s: Int)
    fun clear(mask: Int)
    fun pixelStorei(pname: Int, param: Int)

    // -------- shader / program ---------------------------------------------

    fun createShader(type: Int): Int
    fun shaderSource(shader: Int, source: String)
    fun compileShader(shader: Int): Boolean
    fun getShaderInfoLog(shader: Int): String
    fun deleteShader(shader: Int)

    fun createProgram(): Int
    fun attachShader(program: Int, shader: Int)
    fun detachShader(program: Int, shader: Int)
    fun bindAttribLocation(program: Int, index: Int, name: String)
    fun linkProgram(program: Int): Boolean
    fun validateProgram(program: Int): Boolean
    fun getProgramInfoLog(program: Int): String
    fun useProgram(program: Int)
    fun deleteProgram(program: Int)
    fun getActiveUniformCount(program: Int): Int
    fun getActiveUniform(program: Int, index: Int): ActiveUniform
    fun getUniformLocation(program: Int, name: String): Int
    fun getAttribLocation(program: Int, name: String): Int
    fun uniformBlockBinding(program: Int, blockIndex: Int, binding: Int)
    fun getUniformBlockIndex(program: Int, name: String): Int

    // -------- uniforms ------------------------------------------------------

    fun uniform1i(location: Int, x: Int)
    fun uniform1f(location: Int, x: Float)
    fun uniform2f(location: Int, x: Float, y: Float)
    fun uniform3f(location: Int, x: Float, y: Float, z: Float)
    fun uniform4f(location: Int, x: Float, y: Float, z: Float, w: Float)
    fun uniform1iv(location: Int, count: Int, value: IntArray)
    fun uniform4iv(location: Int, count: Int, value: IntArray)
    fun uniform1fv(location: Int, count: Int, value: FloatArray)
    fun uniform2fv(location: Int, count: Int, value: FloatArray)
    fun uniform3fv(location: Int, count: Int, value: FloatArray)
    fun uniform4fv(location: Int, count: Int, value: FloatArray)
    fun uniform4uiv(location: Int, count: Int, value: IntArray)
    fun uniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatArray)
    fun uniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatArray)
    fun uniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray)
    fun uniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray)
    fun vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float)
    fun vertexAttrib4fv(index: Int, value: FloatArray)

    // -------- textures ------------------------------------------------------

    fun genTextures(n: Int): IntArray
    fun deleteTextures(textures: IntArray)
    fun bindTexture(target: Int, texture: Int)
    fun activeTexture(unit: Int)
    fun texParameteri(target: Int, pname: Int, param: Int)
    fun texParameterf(target: Int, pname: Int, param: Float)
    fun texImage2D(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, format: Int, type: Int, data: ByteArray?)
    fun texSubImage2D(target: Int, level: Int, xOffset: Int, yOffset: Int, width: Int, height: Int, format: Int, type: Int, data: ByteArray)
    fun copyTexImage2D(target: Int, level: Int, internalFormat: Int, x: Int, y: Int, width: Int, height: Int, border: Int)
    fun generateMipmap(target: Int)

    // -------- framebuffers --------------------------------------------------

    fun genFramebuffers(n: Int): IntArray
    fun deleteFramebuffers(fbs: IntArray)
    fun bindFramebuffer(target: Int, fb: Int)
    fun framebufferTexture2D(target: Int, attachment: Int, texTarget: Int, texture: Int, level: Int)
    fun drawBuffers(buffers: IntArray)
    fun readBuffer(src: Int)
    fun checkFramebufferStatus(target: Int): Int

    // -------- debug ---------------------------------------------------------

    fun debugMessageInsert(source: Int, type: Int, id: Int, severity: Int, message: String)

    // -------- queries (timing / occlusion) ---------------------------------

    fun genQueries(n: Int): IntArray
    fun deleteQueries(queries: IntArray)
    fun beginQuery(target: Int, id: Int)
    fun endQuery(target: Int)
    fun getQueryObjectui64(id: Int): Long

    companion object {
        @Volatile
        var current: GpuBackend = NoOpGpuBackend
            private set

        fun install(backend: GpuBackend) {
            current = backend
        }

        fun uninstall() {
            current = NoOpGpuBackend
        }
    }
}

/** Reflection metadata for an active uniform. */
data class ActiveUniform(
    val name: String,
    val type: Int,
    val size: Int,
    val location: Int
)
