package com.firestorm.llrender

import com.firestorm.llcommon.LLUUID
import com.firestorm.llcommon.LLSD
import com.firestorm.llmath.Color4

class Material() {

    object MaterialConstants {
        const val ALPHA_MODE_NONE: UByte     = 0u
        const val ALPHA_MODE_BLEND: UByte    = 1u
        const val ALPHA_MODE_MASK: UByte     = 2u
        const val ALPHA_MODE_EMISSIVE: UByte = 3u
        const val ALPHA_MODE_DEFAULT: UByte  = 4u

        const val DIFFUSE_ALPHA_NONE: UByte     = 0u
        const val DIFFUSE_ALPHA_BLEND: UByte    = 1u
        const val DIFFUSE_ALPHA_MASK: UByte     = 2u
        const val DIFFUSE_ALPHA_EMISSIVE: UByte = 3u
        const val DIFFUSE_ALPHA_DEFAULT: UByte  = 4u

        val DEFAULT_SPECULAR_COLOR: Color4u = Color4u(255u, 255u, 255u, 255u)
        const val DEFAULT_SPECULAR_EXP: UByte = 51u  // (U8)(0.2f * 255)
        const val DEFAULT_ENV_INTENSITY: UByte = 0u
    }

    var normalId: LLUUID = LLUUID.NULL
    var specularId: LLUUID = LLUUID.NULL

    var normalOffsetX: Float = 0f
    var normalOffsetY: Float = 0f
    var normalRepeatX: Float = 1f
    var normalRepeatY: Float = 1f
    var normalRotation: Float = 0f

    var specularOffsetX: Float = 0f
    var specularOffsetY: Float = 0f
    var specularRepeatX: Float = 1f
    var specularRepeatY: Float = 1f
    var specularRotation: Float = 0f

    var specularColor: Color4u = MaterialConstants.DEFAULT_SPECULAR_COLOR
    var specularExp: UByte = MaterialConstants.DEFAULT_SPECULAR_EXP
    var environmentIntensity: UByte = MaterialConstants.DEFAULT_ENV_INTENSITY
    var diffuseAlphaMode: UByte = MaterialConstants.ALPHA_MODE_NONE
    var alphaThreshold: UByte = 0u

    constructor(sd: LLSD) : this() {
        fromSD(sd)
    }

    fun toSD(): LLSD {
        return LLSD.Undefined
    }

    fun fromSD(sd: LLSD) {
        System.err.println("Material: fromSD not yet implemented")
    }

    fun isEmpty(): Boolean = normalId == LLUUID.NULL && specularId == LLUUID.NULL

    companion object {
        fun fromSD(sd: LLSD): Material = Material(sd)
    }
}

data class Color4u(val r: UByte, val g: UByte, val b: UByte, val a: UByte) {
    constructor(r: UInt, g: UInt, b: UInt, a: UInt) :
        this(r.toUByte(), g.toUByte(), b.toUByte(), a.toUByte())

    fun toColor4(): Color4 = Color4(r.toInt() / 255f, g.toInt() / 255f, b.toInt() / 255f, a.toInt() / 255f)
}
