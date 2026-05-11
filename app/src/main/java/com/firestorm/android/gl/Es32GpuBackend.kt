package com.firestorm.android.gl

import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLES31
import android.opengl.GLES32
import com.firestorm.llrender.ActiveUniform
import com.firestorm.llrender.GpuBackend

/**
 * `GpuBackend` implementation that delegates each call to the Android
 * `android.opengl.GLES3x` surface. Only safe to use after an EGL 3.x context
 * is current on the calling thread.
 *
 * Behavioural quirks:
 *  - `getShaderInfoLog` / `getProgramInfoLog` return the empty string when
 *    the underlying log is `null` so callers don't need to null-check.
 *  - The optional ES 3.2 entry points (debug callback, geometry shader,
 *    `glGetQueryObjectui64v`) are guarded; the constructor records whether
 *    the runtime context advertised ES 3.2 and falls back to ES 3.0/3.1
 *    equivalents otherwise.
 */
class Es32GpuBackend : GpuBackend {

    private val cachedVersionMajor: Int
    private val cachedVersionMinor: Int
    val supportsEs32: Boolean
        get() = cachedVersionMajor > 3 || (cachedVersionMajor == 3 && cachedVersionMinor >= 2)

    init {
        val tmp = IntArray(1)
        GLES30.glGetIntegerv(GLES30.GL_MAJOR_VERSION, tmp, 0)
        cachedVersionMajor = tmp[0]
        GLES30.glGetIntegerv(GLES30.GL_MINOR_VERSION, tmp, 0)
        cachedVersionMinor = tmp[0]
    }

    override fun majorVersion(): Int = cachedVersionMajor
    override fun minorVersion(): Int = cachedVersionMinor
    override fun glslVersion(): String = GLES20.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION) ?: ""
    override fun vendor(): String = GLES20.glGetString(GLES20.GL_VENDOR) ?: ""
    override fun renderer(): String = GLES20.glGetString(GLES20.GL_RENDERER) ?: ""
    override fun versionString(): String = GLES20.glGetString(GLES20.GL_VERSION) ?: ""

    override fun getInteger(name: Int): Int {
        val tmp = IntArray(1)
        GLES20.glGetIntegerv(name, tmp, 0)
        return tmp[0]
    }

    override fun getError(): Int = GLES20.glGetError()

    override fun enable(cap: Int) = GLES20.glEnable(cap)
    override fun disable(cap: Int) = GLES20.glDisable(cap)
    override fun isEnabled(cap: Int): Boolean = GLES20.glIsEnabled(cap)
    override fun depthFunc(func: Int) = GLES20.glDepthFunc(func)
    override fun depthMask(flag: Boolean) = GLES20.glDepthMask(flag)
    override fun depthRangef(near: Float, far: Float) = GLES20.glDepthRangef(near, far)
    override fun cullFace(mode: Int) = GLES20.glCullFace(mode)
    override fun frontFace(mode: Int) = GLES20.glFrontFace(mode)
    override fun blendFunc(sFactor: Int, dFactor: Int) = GLES20.glBlendFunc(sFactor, dFactor)
    override fun blendFuncSeparate(srcRgb: Int, dstRgb: Int, srcAlpha: Int, dstAlpha: Int) =
        GLES20.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha)
    override fun viewport(x: Int, y: Int, width: Int, height: Int) =
        GLES20.glViewport(x, y, width, height)
    override fun scissor(x: Int, y: Int, width: Int, height: Int) =
        GLES20.glScissor(x, y, width, height)
    override fun clearColor(r: Float, g: Float, b: Float, a: Float) = GLES20.glClearColor(r, g, b, a)
    override fun clearDepthf(depth: Float) = GLES20.glClearDepthf(depth)
    override fun clearStencil(s: Int) = GLES20.glClearStencil(s)
    override fun clear(mask: Int) = GLES20.glClear(mask)
    override fun pixelStorei(pname: Int, param: Int) = GLES20.glPixelStorei(pname, param)

    override fun createShader(type: Int): Int = GLES20.glCreateShader(type)
    override fun shaderSource(shader: Int, source: String) = GLES20.glShaderSource(shader, source)
    override fun compileShader(shader: Int): Boolean {
        GLES20.glCompileShader(shader)
        val tmp = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, tmp, 0)
        return tmp[0] != 0
    }
    override fun getShaderInfoLog(shader: Int): String =
        GLES20.glGetShaderInfoLog(shader).orEmpty()
    override fun deleteShader(shader: Int) = GLES20.glDeleteShader(shader)

    override fun createProgram(): Int = GLES20.glCreateProgram()
    override fun attachShader(program: Int, shader: Int) = GLES20.glAttachShader(program, shader)
    override fun detachShader(program: Int, shader: Int) = GLES20.glDetachShader(program, shader)
    override fun bindAttribLocation(program: Int, index: Int, name: String) =
        GLES20.glBindAttribLocation(program, index, name)
    override fun linkProgram(program: Int): Boolean {
        GLES20.glLinkProgram(program)
        val tmp = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, tmp, 0)
        return tmp[0] != 0
    }
    override fun validateProgram(program: Int): Boolean {
        GLES20.glValidateProgram(program)
        val tmp = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, tmp, 0)
        return tmp[0] != 0
    }
    override fun getProgramInfoLog(program: Int): String =
        GLES20.glGetProgramInfoLog(program).orEmpty()
    override fun useProgram(program: Int) = GLES20.glUseProgram(program)
    override fun deleteProgram(program: Int) = GLES20.glDeleteProgram(program)
    override fun getActiveUniformCount(program: Int): Int {
        val tmp = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_ACTIVE_UNIFORMS, tmp, 0)
        return tmp[0]
    }
    override fun getActiveUniform(program: Int, index: Int): ActiveUniform {
        val size = IntArray(1)
        val type = IntArray(1)
        val length = IntArray(1)
        val nameBuf = ByteArray(256)
        GLES20.glGetActiveUniform(program, index, nameBuf.size, length, 0, size, 0, type, 0, nameBuf, 0)
        val name = String(nameBuf, 0, length[0])
        val loc = GLES20.glGetUniformLocation(program, name)
        return ActiveUniform(name = name, type = type[0], size = size[0], location = loc)
    }
    override fun getUniformLocation(program: Int, name: String): Int =
        GLES20.glGetUniformLocation(program, name)
    override fun getAttribLocation(program: Int, name: String): Int =
        GLES20.glGetAttribLocation(program, name)
    override fun uniformBlockBinding(program: Int, blockIndex: Int, binding: Int) =
        GLES30.glUniformBlockBinding(program, blockIndex, binding)
    override fun getUniformBlockIndex(program: Int, name: String): Int =
        GLES30.glGetUniformBlockIndex(program, name)

    override fun uniform1i(location: Int, x: Int) = GLES20.glUniform1i(location, x)
    override fun uniform1f(location: Int, x: Float) = GLES20.glUniform1f(location, x)
    override fun uniform2f(location: Int, x: Float, y: Float) = GLES20.glUniform2f(location, x, y)
    override fun uniform3f(location: Int, x: Float, y: Float, z: Float) =
        GLES20.glUniform3f(location, x, y, z)
    override fun uniform4f(location: Int, x: Float, y: Float, z: Float, w: Float) =
        GLES20.glUniform4f(location, x, y, z, w)
    override fun uniform1iv(location: Int, count: Int, value: IntArray) =
        GLES20.glUniform1iv(location, count, value, 0)
    override fun uniform4iv(location: Int, count: Int, value: IntArray) =
        GLES20.glUniform4iv(location, count, value, 0)
    override fun uniform1fv(location: Int, count: Int, value: FloatArray) =
        GLES20.glUniform1fv(location, count, value, 0)
    override fun uniform2fv(location: Int, count: Int, value: FloatArray) =
        GLES20.glUniform2fv(location, count, value, 0)
    override fun uniform3fv(location: Int, count: Int, value: FloatArray) =
        GLES20.glUniform3fv(location, count, value, 0)
    override fun uniform4fv(location: Int, count: Int, value: FloatArray) =
        GLES20.glUniform4fv(location, count, value, 0)
    override fun uniform4uiv(location: Int, count: Int, value: IntArray) =
        GLES30.glUniform4uiv(location, count, value, 0)
    override fun uniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatArray) =
        GLES20.glUniformMatrix2fv(location, count, transpose, value, 0)
    override fun uniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatArray) =
        GLES20.glUniformMatrix3fv(location, count, transpose, value, 0)
    override fun uniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray) =
        GLES30.glUniformMatrix3x4fv(location, count, transpose, value, 0)
    override fun uniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray) =
        GLES20.glUniformMatrix4fv(location, count, transpose, value, 0)
    override fun vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float) =
        GLES20.glVertexAttrib4f(index, x, y, z, w)
    override fun vertexAttrib4fv(index: Int, value: FloatArray) =
        GLES20.glVertexAttrib4fv(index, value, 0)

    override fun genTextures(n: Int): IntArray {
        val out = IntArray(n)
        GLES20.glGenTextures(n, out, 0)
        return out
    }
    override fun deleteTextures(textures: IntArray) =
        GLES20.glDeleteTextures(textures.size, textures, 0)
    override fun bindTexture(target: Int, texture: Int) = GLES20.glBindTexture(target, texture)
    override fun activeTexture(unit: Int) = GLES20.glActiveTexture(unit)
    override fun texParameteri(target: Int, pname: Int, param: Int) =
        GLES20.glTexParameteri(target, pname, param)
    override fun texParameterf(target: Int, pname: Int, param: Float) =
        GLES20.glTexParameterf(target, pname, param)
    override fun texImage2D(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, format: Int, type: Int, data: ByteArray?) {
        val buf = data?.let { java.nio.ByteBuffer.wrap(it) }
        GLES20.glTexImage2D(target, level, internalFormat, width, height, 0, format, type, buf)
    }
    override fun texSubImage2D(target: Int, level: Int, xOffset: Int, yOffset: Int, width: Int, height: Int, format: Int, type: Int, data: ByteArray) {
        val buf = java.nio.ByteBuffer.wrap(data)
        GLES20.glTexSubImage2D(target, level, xOffset, yOffset, width, height, format, type, buf)
    }
    override fun copyTexImage2D(target: Int, level: Int, internalFormat: Int, x: Int, y: Int, width: Int, height: Int, border: Int) =
        GLES20.glCopyTexImage2D(target, level, internalFormat, x, y, width, height, border)
    override fun generateMipmap(target: Int) = GLES20.glGenerateMipmap(target)

    override fun genFramebuffers(n: Int): IntArray {
        val out = IntArray(n)
        GLES20.glGenFramebuffers(n, out, 0)
        return out
    }
    override fun deleteFramebuffers(fbs: IntArray) =
        GLES20.glDeleteFramebuffers(fbs.size, fbs, 0)
    override fun bindFramebuffer(target: Int, fb: Int) = GLES20.glBindFramebuffer(target, fb)
    override fun framebufferTexture2D(target: Int, attachment: Int, texTarget: Int, texture: Int, level: Int) =
        GLES20.glFramebufferTexture2D(target, attachment, texTarget, texture, level)
    override fun drawBuffers(buffers: IntArray) =
        GLES30.glDrawBuffers(buffers.size, buffers, 0)
    override fun readBuffer(src: Int) = GLES30.glReadBuffer(src)
    override fun checkFramebufferStatus(target: Int): Int =
        GLES20.glCheckFramebufferStatus(target)

    override fun debugMessageInsert(source: Int, type: Int, id: Int, severity: Int, message: String) {
        if (!supportsEs32) return
        GLES32.glDebugMessageInsert(source, type, id, severity, message.length, message)
    }

    override fun genQueries(n: Int): IntArray {
        val out = IntArray(n)
        GLES30.glGenQueries(n, out, 0)
        return out
    }
    override fun deleteQueries(queries: IntArray) =
        GLES30.glDeleteQueries(queries.size, queries, 0)
    override fun beginQuery(target: Int, id: Int) = GLES30.glBeginQuery(target, id)
    override fun endQuery(target: Int) = GLES30.glEndQuery(target)
    override fun getQueryObjectui64(id: Int): Long {
        // GLES30 returns the 32-bit "available" result. The 64-bit timer
        // query value is only exposed via the EXT_disjoint_timer_query GLES
        // extension; if we don't have it we fall back to the 32-bit result.
        val tmp = IntArray(1)
        GLES30.glGetQueryObjectuiv(id, GLES30.GL_QUERY_RESULT, tmp, 0)
        return tmp[0].toLong() and 0xFFFFFFFFL
    }
}
