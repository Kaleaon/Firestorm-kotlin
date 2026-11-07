package com.firestorm.newview

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Colour math helpers (ported 1-to-1 from lggbeammaps.cpp)
// ---------------------------------------------------------------------------

private const val F_ALMOST_ZERO = 0.00001f

/**
 * Maps a hue value in [0,1] to an RGB component using the standard HSL
 * piecewise interpolation.
 */
fun hueToRgb(val1: Float, val2: Float, hue: Float): Float {
    var h = hue
    while (h < 0f) h += 1f
    while (h > 1f) h -= 1f
    return when {
        6f * h < 1f -> val1 + (val2 - val1) * 6f * h
        2f * h < 1f -> val2
        3f * h < 2f -> val1 + (val2 - val1) * (2f / 3f - h) * 6f
        else -> val1
    }
}

/** Converts HSL (all in [0,1]) to RGB components (each in [0,1]). */
fun hslToRgb(h: Float, s: Float, l: Float): Triple<Float, Float, Float> {
    if (s < F_ALMOST_ZERO) return Triple(l, l, l)
    val v2 = if (l < 0.5f) l * (1f + s) else (l + s) - (s * l)
    val v1 = 2f * l - v2
    return Triple(
        hueToRgb(v1, v2, h + 1f / 3f),
        hueToRgb(v1, v2, h),
        hueToRgb(v1, v2, h - 1f / 3f)
    )
}

// ---------------------------------------------------------------------------
// Data types
// ---------------------------------------------------------------------------

/** An RGBA colour packed as four unsigned bytes. */
data class Color4U(val r: UByte, val g: UByte, val b: UByte, val a: UByte = 255u) {
    companion object {
        fun fromFloats(r: Float, g: Float, b: Float, a: Float = 1f) = Color4U(
            (r.coerceIn(0f, 1f) * 255f).toInt().toUByte(),
            (g.coerceIn(0f, 1f) * 255f).toInt().toUByte(),
            (b.coerceIn(0f, 1f) * 255f).toInt().toUByte(),
            (a.coerceIn(0f, 1f) * 255f).toInt().toUByte()
        )
    }
}

/** Position + colour for a single beam-dot offset. */
data class LGGBeamData(
    val px: Double,
    val py: Double,
    val pz: Double,
    val color: Color4U
)

// Stub HUD types — real implementations issue OpenGL / sim-protocol calls
class HUDEffectSpiral {
    fun getPositionGlobal(): Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0)
    fun getTargetObject(): Any? = null
    fun getSourceObject(): Any? = null
    fun getNeedsSendToSim(): Boolean = false
    fun setPositionGlobal(p: Triple<Double, Double, Double>): Unit { /* no-op */ }
    fun setColor(c: Color4U): Unit { /* no-op */ }
    fun setTargetObject(o: Any?): Unit { /* no-op */ }
    fun setSourceObject(o: Any?): Unit { /* no-op */ }
    fun setNeedsSendToSim(v: Boolean): Unit { /* no-op */ }
    fun setDuration(d: Float): Unit { /* no-op */ }
}

object HUDManagerStub {
    fun createBeamEffect(): HUDEffectSpiral = HUDEffectSpiral()
}

object GAgentStub {
    fun getPositionGlobal(): Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0)
    fun getAgentId(): String = ""
    fun getSessionId(): String = ""
    fun sendReliableMessage(): Unit { System.err.println("GAgentStub: sendReliableMessage not yet implemented") }
    val regionName: String get() = ""
}

object SavedSettings {
    fun getString(key: String): String = ""
    fun getBool(key: String): Boolean = false
    fun getFloat(key: String): Float = 0f
}

object MessageSystem {
    fun sendChatFromViewer(message: String, channel: Int = 9000): Unit {
        System.err.println("MessageSystem: sendChatFromViewer not yet implemented")
    }
}

object DirUtil {
    fun getExpandedFilename(pathType: String, subDir: String): String = ""
    fun getNextFileInDir(path: String, glob: String): Sequence<String> = emptySequence()
}

object FSCommonUtil {
    fun unescapeName(name: String): String = ""
}

// ---------------------------------------------------------------------------
// lggBeamMaps → LGGBeamMaps (singleton, mirrors the C++ global gLggBeamMaps)
// ---------------------------------------------------------------------------

/**
 * Manages beam-shape presets and per-frame particle emission.
 *
 * The C++ translation unit declared a file-scope global `gLggBeamMaps`; the
 * idiomatic Kotlin equivalent is an `object`.  Callers that referenced
 * `gLggBeamMaps` should reference `LGGBeamMaps` directly.
 */
object LGGBeamMaps {

    private var lastFileName: String = ""
    private var lastColorFileName: String = ""
    private var lastColorsData: LGGBeamColors = LGGBeamColors()
    private var duration: Float = 0.25f
    private var scale: Float = 0f
    private var partsNow: Boolean = false
    private var beamLastAt: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0)
    private val dots: MutableList<LGGBeamData> = mutableListOf()

    // Elapsed-time reference point for colour cycling; wall-clock origin is fine
    private val colorTimerStart = System.currentTimeMillis()
    private fun colorElapsedSeconds(): Float =
        (System.currentTimeMillis() - colorTimerStart) / 1000f

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    fun setUpAndGetDuration(): Float {
        val settingName = SavedSettings.getString("FSBeamShape")
        if (settingName == lastFileName) return duration

        lastFileName = settingName
        if (settingName.isNotEmpty()) {
            val pathApp = DirUtil.getExpandedFilename("LL_PATH_APP_SETTINGS", "beams/")
            val pathUser = DirUtil.getExpandedFilename("LL_PATH_USER_SETTINGS", "beams/")
            var filename = "$pathApp$settingName.xml"
            if (!java.io.File(filename).exists()) filename = "$pathUser$settingName.xml"

            val mydata = readLLSD(filename)
            scale = ((mydata["scale"] as? Number)?.toDouble() ?: 0.0).toFloat() / 10f

            @Suppress("UNCHECKED_CAST")
            val picture = mydata["data"] as? List<Map<String, Any?>> ?: emptyList()
            dots.clear()
            val shapeScale = SavedSettings.getFloat("FSBeamShapeScale")
            for (entry in picture) {
                @Suppress("UNCHECKED_CAST")
                val offset = entry["offset"] as? Map<String, Any?> ?: continue
                val px = ((offset["x"] as? Number)?.toDouble() ?: 0.0) * shapeScale * 2.0
                val py = ((offset["y"] as? Number)?.toDouble() ?: 0.0) * shapeScale * 2.0
                val pz = ((offset["z"] as? Number)?.toDouble() ?: 0.0) * shapeScale * 2.0
                @Suppress("UNCHECKED_CAST")
                val colorMap = entry["color"] as? Map<String, Any?> ?: emptyMap()
                val r = (colorMap["x"] as? Number)?.toFloat() ?: 0f
                val g = (colorMap["y"] as? Number)?.toFloat() ?: 0f
                val b = (colorMap["z"] as? Number)?.toFloat() ?: 0f
                dots.add(LGGBeamData(px, py, pz, Color4U.fromFloats(r, g, b)))
            }

            val maxPerQS = SavedSettings.getFloat("FSMaxBeamsPerSecond") / 4f
            duration = ceil(picture.size.toFloat() / maxPerQS) * 0.25f
        } else {
            dots.clear()
            scale = 0f
            duration = 0.25f
        }
        return duration
    }

    fun fireCurrentBeams(beam: HUDEffectSpiral, rgb: Color4U) {
        if (scale == 0f) return

        val colorsDisabled = SavedSettings.getString("FSBeamColorFile").isEmpty()
        val beamPos = beam.getPositionGlobal()
        val agentPos = GAgentStub.getPositionGlobal()

        val frameTime = System.currentTimeMillis() / 1000.0
        val distanceAdjust = dist3d(beamPos, agentPos).toFloat()
        val pulse = 0.75f + sin(frameTime * 1.0).toFloat() * 0.25f

        // Compute beam direction for rotating offsets into world space
        val beamLine = subtract3d(beamPos, agentPos)
        val beamLineFlat = beamLine.copy(third = 0.0)

        for (dot in dots) {
            val myColor = if (colorsDisabled) dot.color else rgb

            var ox = dot.px
            var oy = -dot.py  // mirror Y as in C++
            var oz = dot.pz
            val factor = pulse * scale * distanceAdjust * 0.1f
            ox *= factor; oy *= factor; oz *= factor

            // Rotate offset to align with beam direction (stubbed quaternion math)
            val rotatedOffset = rotateOffsetToBeam(ox, oy, oz, beamLine, beamLineFlat)

            val wobble = sin(frameTime * 2.0).toFloat() * 0.2f
            val newPos = Triple(
                beamPos.first  + rotatedOffset.first  + beamLine.first  * wobble,
                beamPos.second + rotatedOffset.second + beamLine.second * wobble,
                beamPos.third  + rotatedOffset.third  + beamLine.third  * wobble
            )

            val myBeam = HUDManagerStub.createBeamEffect()
            myBeam.setPositionGlobal(newPos)
            myBeam.setColor(myColor)
            myBeam.setTargetObject(beam.getTargetObject())
            myBeam.setSourceObject(beam.getSourceObject())
            myBeam.setNeedsSendToSim(beam.getNeedsSendToSim())
            myBeam.setDuration(duration * 1.2f)
        }
    }

    fun forceUpdate() {
        dots.clear()
        scale = 0f
        lastFileName = ""
    }

    fun stopBeamChat() {
        if (!SavedSettings.getBool("FSParticleChat")) return
        if (!partsNow) return
        partsNow = false
        MessageSystem.sendChatFromViewer("stop")
        beamLastAt = Triple(0.0, 0.0, 0.0)
    }

    fun updateBeamChat(currentPos: Triple<Double, Double, Double>) {
        if (!SavedSettings.getBool("FSParticleChat")) return
        if (!partsNow) {
            partsNow = true
            MessageSystem.sendChatFromViewer("start")
        }
        val dx = beamLastAt.first - currentPos.first
        val dy = beamLastAt.second - currentPos.second
        val dz = beamLastAt.third - currentPos.third
        if (Math.sqrt(dx * dx + dy * dy + dz * dz) > 0.2) {
            beamLastAt = currentPos
            val msg = "<%.6f, %.6f, %.6f>".format(currentPos.first, currentPos.second, currentPos.third)
            MessageSystem.sendChatFromViewer(msg)
        }
    }

    fun beamColorFromData(data: LGGBeamColors): Color4U {
        val difference = data.endHue - data.startHue
        val timeinc = if (difference != 0f)
            colorElapsedSeconds() * 0.3f * (data.rotateSpeed + 0.01f) * (360f / difference)
        else 0f

        val roundedDiff = Math.round(difference)
        val (r, g, b) = if (roundedDiff == 360 || roundedDiff == 720) {
            hslToRgb(timeinc % 1f, 1f, 0.5f)
        } else {
            val variance = difference / 360f / 2f
            hslToRgb(data.startHue / 360f + variance + sin(timeinc.toDouble()).toFloat() * variance, 1f, 0.5f)
        }
        return Color4U.fromFloats(r, g, b)
    }

    fun getCurrentColor(agentColor: Color4U): Color4U {
        val settingName = SavedSettings.getString("FSBeamColorFile")
        if (settingName.isEmpty()) return agentColor

        if (settingName != lastColorFileName) {
            lastColorFileName = settingName
            val pathApp = DirUtil.getExpandedFilename("LL_PATH_APP_SETTINGS", "beamsColors/")
            val pathUser = DirUtil.getExpandedFilename("LL_PATH_USER_SETTINGS", "beamsColors/")
            var filename = "$pathApp$settingName.xml"
            if (!java.io.File(filename).exists()) {
                filename = "$pathUser$settingName.xml"
                if (!java.io.File(filename).exists()) return agentColor
            }
            lastColorsData = LGGBeamColors.fromLLSD(readLLSD(filename))
        }
        return beamColorFromData(lastColorsData)
    }

    fun getFileNames(): MutableList<String> = collectXmlNames("beams")

    fun getColorsFileNames(): MutableList<String> = collectXmlNames("beamsColors")

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    private fun readLLSD(filename: String): Map<String, Any?> {
        System.err.println("LGGBeamMaps: readLLSD not yet implemented")
        return emptyMap()
    }

    private fun collectXmlNames(subDir: String): MutableList<String> {
        val names = mutableListOf<String>()
        val pathApp = DirUtil.getExpandedFilename("LL_PATH_APP_SETTINGS", "$subDir/")
        val pathUser = DirUtil.getExpandedFilename("LL_PATH_USER_SETTINGS", "$subDir/")
        for (path in listOf(pathApp, pathUser)) {
            for (name in DirUtil.getNextFileInDir(path, "*.xml")) {
                names.add(FSCommonUtil.unescapeName(name.removeSuffix(".xml")))
            }
        }
        return names
    }

    private fun dist3d(a: Triple<Double, Double, Double>, b: Triple<Double, Double, Double>): Double {
        val dx = a.first - b.first
        val dy = a.second - b.second
        val dz = a.third - b.third
        return Math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun subtract3d(
        a: Triple<Double, Double, Double>,
        b: Triple<Double, Double, Double>
    ) = Triple(a.first - b.first, a.second - b.second, a.third - b.third)

    /** Rotates an offset vector to align with the beam direction. Full quaternion math is GPU-side. */
    private fun rotateOffsetToBeam(
        ox: Float, oy: Float, oz: Float,
        beamLine: Triple<Double, Double, Double>,
        beamLineFlat: Triple<Double, Double, Double>
    ): Triple<Float, Float, Float> {
        return Triple(ox, oy, oz)
    }
}
