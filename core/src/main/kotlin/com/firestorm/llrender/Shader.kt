package com.firestorm.llrender

import com.firestorm.llmath.Vector3
import com.firestorm.llmath.Vector4

class Shader {
    var name: String = ""
    var shaderLevel: Int = 0

    enum class ShaderGroup {
        SHADERS_WINDLIGHT,
        SHADERS_WATER,
        SHADERS_AVATAR,
        SHADERS_ENVIRONMENT,
        SHADERS_DEFERRED,
        SHADERS_COUNT,
    }

    var shaderGroup: ShaderGroup = ShaderGroup.SHADERS_ENVIRONMENT

    fun uniform1i(name: String, i: Int) { /* GPU call */ }
    fun uniform1i(index: Int, i: Int) { /* GPU call */ }

    fun uniform1f(name: String, v: Float) { /* GPU call */ }
    fun uniform1f(index: Int, v: Float) { /* GPU call */ }

    fun uniform2f(name: String, x: Float, y: Float) { /* GPU call */ }
    fun uniform2f(index: Int, x: Float, y: Float) { /* GPU call */ }

    fun uniform3f(name: String, x: Float, y: Float, z: Float) { /* GPU call */ }
    fun uniform3f(index: Int, x: Float, y: Float, z: Float) { /* GPU call */ }

    fun uniform4f(name: String, x: Float, y: Float, z: Float, w: Float) { /* GPU call */ }
    fun uniform4f(index: Int, x: Float, y: Float, z: Float, w: Float) { /* GPU call */ }

    fun uniformMatrix3fv(index: Int, count: Int, transpose: Boolean, v: FloatArray) { /* GPU call */ }
    fun uniformMatrix3fv(name: String, count: Int, transpose: Boolean, v: FloatArray) { /* GPU call */ }

    fun uniformMatrix4fv(index: Int, count: Int, transpose: Boolean, v: FloatArray) { /* GPU call */ }
    fun uniformMatrix4fv(name: String, count: Int, transpose: Boolean, v: FloatArray) { /* GPU call */ }

    fun uniform3fv(name: String, vec: Vector3) { /* GPU call */ }
    fun uniform3fv(index: Int, count: Int, v: FloatArray) { /* GPU call */ }

    fun uniform4fv(name: String, vec: Vector4) { /* GPU call */ }
    fun uniform4fv(index: Int, count: Int, v: FloatArray) { /* GPU call */ }

    fun bind() { /* GPU call */ }
    fun unbind() { /* GPU call */ }

    fun link(): Boolean = true

    companion object {
        val shaders: MutableMap<String, Shader> = mutableMapOf()
    }
}
