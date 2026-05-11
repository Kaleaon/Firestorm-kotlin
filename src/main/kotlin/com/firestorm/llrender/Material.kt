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

    fun toSD(): LLSD = LLSD.LLSDMap(mapOf(
        "NormMap" to LLSD.LLSDUUID(normalId),
        "NormOffsetX" to LLSD.LLSDReal(normalOffsetX.toDouble()),
        "NormOffsetY" to LLSD.LLSDReal(normalOffsetY.toDouble()),
        "NormRepeatX" to LLSD.LLSDReal(normalRepeatX.toDouble()),
        "NormRepeatY" to LLSD.LLSDReal(normalRepeatY.toDouble()),
        "NormRotation" to LLSD.LLSDReal(normalRotation.toDouble()),
        "SpecMap" to LLSD.LLSDUUID(specularId),
        "SpecOffsetX" to LLSD.LLSDReal(specularOffsetX.toDouble()),
        "SpecOffsetY" to LLSD.LLSDReal(specularOffsetY.toDouble()),
        "SpecRepeatX" to LLSD.LLSDReal(specularRepeatX.toDouble()),
        "SpecRepeatY" to LLSD.LLSDReal(specularRepeatY.toDouble()),
        "SpecRotation" to LLSD.LLSDReal(specularRotation.toDouble()),
        "SpecColor" to LLSD.LLSDArray(listOf(
            LLSD.LLSDInteger(specularColor.r.toInt()),
            LLSD.LLSDInteger(specularColor.g.toInt()),
            LLSD.LLSDInteger(specularColor.b.toInt()),
            LLSD.LLSDInteger(specularColor.a.toInt())
        )),
        "SpecExp" to LLSD.LLSDInteger(specularExp.toInt()),
        "EnvIntensity" to LLSD.LLSDInteger(environmentIntensity.toInt()),
        "AlphaMode" to LLSD.LLSDInteger(diffuseAlphaMode.toInt()),
        "AlphaMaskCutoff" to LLSD.LLSDInteger(alphaThreshold.toInt())
    ))

    fun fromSD(sd: LLSD) {
        val map = (sd as? LLSD.LLSDMap)?.value ?: return
        normalId = (map["NormMap"]?.asUUID()) ?: LLUUID.NULL
        normalOffsetX = (map["NormOffsetX"]?.asReal() ?: 0.0).toFloat()
        normalOffsetY = (map["NormOffsetY"]?.asReal() ?: 0.0).toFloat()
        normalRepeatX = (map["NormRepeatX"]?.asReal() ?: 1.0).toFloat()
        normalRepeatY = (map["NormRepeatY"]?.asReal() ?: 1.0).toFloat()
        normalRotation = (map["NormRotation"]?.asReal() ?: 0.0).toFloat()
        specularId = (map["SpecMap"]?.asUUID()) ?: LLUUID.NULL
        specularOffsetX = (map["SpecOffsetX"]?.asReal() ?: 0.0).toFloat()
        specularOffsetY = (map["SpecOffsetY"]?.asReal() ?: 0.0).toFloat()
        specularRepeatX = (map["SpecRepeatX"]?.asReal() ?: 1.0).toFloat()
        specularRepeatY = (map["SpecRepeatY"]?.asReal() ?: 1.0).toFloat()
        specularRotation = (map["SpecRotation"]?.asReal() ?: 0.0).toFloat()
        (map["SpecColor"] as? LLSD.LLSDArray)?.value?.let { arr ->
            if (arr.size >= 4) {
                specularColor = Color4u(
                    arr[0].asInt().coerceIn(0, 255).toUByte(),
                    arr[1].asInt().coerceIn(0, 255).toUByte(),
                    arr[2].asInt().coerceIn(0, 255).toUByte(),
                    arr[3].asInt().coerceIn(0, 255).toUByte()
                )
            }
        }
        specularExp = (map["SpecExp"]?.asInt() ?: MaterialConstants.DEFAULT_SPECULAR_EXP.toInt()).coerceIn(0, 255).toUByte()
        environmentIntensity = (map["EnvIntensity"]?.asInt() ?: 0).coerceIn(0, 255).toUByte()
        diffuseAlphaMode = (map["AlphaMode"]?.asInt() ?: 0).coerceIn(0, 255).toUByte()
        alphaThreshold = (map["AlphaMaskCutoff"]?.asInt() ?: 0).coerceIn(0, 255).toUByte()
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
