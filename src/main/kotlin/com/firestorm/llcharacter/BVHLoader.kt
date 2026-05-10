// Converted from llbvhloader.h / llbvhloader.cpp — Linden Research, Inc.
// LGPL 2.1; see original source for full license text.
package com.firestorm.llcharacter

import com.firestorm.llmath.Matrix3
import com.firestorm.llmath.Vector3

// ---------------------------------------------------------------------------
// Load-status codes (mirror ELoadStatus)
// ---------------------------------------------------------------------------

enum class LoadStatus {
    OK,
    EOF,
    NO_CONSTRAINT,
    NO_FILE,
    NO_HIER,
    NO_JOINT,
    NO_NAME,
    NO_OFFSET,
    NO_CHANNELS,
    NO_ROTATION,
    NO_AXIS,
    NO_MOTION,
    NO_FRAMES,
    NO_FRAME_TIME,
    NO_POS,
    NO_ROT,
    NO_XLT_FILE,
    NO_XLT_HEADER,
    NO_XLT_NAME,
    NO_XLT_IGNORE,
    NO_XLT_RELATIVE,
    NO_XLT_OUTNAME,
    NO_XLT_MATRIX,
    NO_XLT_MERGECHILD,
    NO_XLT_MERGEPARENT,
    NO_XLT_PRIORITY,
    NO_XLT_LOOP,
    NO_XLT_EASEIN,
    NO_XLT_EASEOUT,
    NO_XLT_HAND,
    NO_XLT_EMOTE,
    BAD_ROOT;
}

// ---------------------------------------------------------------------------
// Constraint types (mirror EConstraintType / EConstraintTargetType)
// ---------------------------------------------------------------------------

enum class ConstraintType       { POINT, PLANE }
enum class ConstraintTargetType { BODY, GROUND }

// ---------------------------------------------------------------------------
// Raw BVH data types
// ---------------------------------------------------------------------------

/** A single frame's raw position + rotation sample for one joint. */
data class BvhKey(
    var pos: FloatArray = FloatArray(3),
    var rot: FloatArray = FloatArray(3),
    var ignorePos: Boolean = false,
    var ignoreRot: Boolean = false
)

/** One joint node extracted from the BVH HIERARCHY section. */
data class BvhJoint(
    val name: String,
    var ignore: Boolean = false,
    var ignorePositions: Boolean = false,
    var relativePositionKey: Boolean = false,
    var relativeRotationKey: Boolean = false,
    var outName: String = name,
    var mergeParentName: String = "",
    var mergeChildName: String = "",
    /** Rotation channel order, e.g. "ZXY". */
    var order: String = "XYZ",
    val keys: MutableList<BvhKey> = mutableListOf(),
    var numPosKeys: Int = 0,
    var numRotKeys: Int = 0,
    var childTreeMaxDepth: Int = 0,
    var priority: Int = 0,
    var numChannels: Int = 3,
    var frameMatrix: Matrix3 = Matrix3(),
    var offsetMatrix: Matrix3 = Matrix3(),
    var relativePosition: Vector3 = Vector3()
)

/** An IK constraint read from a translation / config table. */
data class BvhConstraint(
    val sourceJointName: String,
    val targetJointName: String,
    val chainLength: Int,
    val sourceOffset: Vector3,
    val targetOffset: Vector3,
    val targetDir: Vector3,
    val easeInStart: Float,
    val easeInStop: Float,
    val easeOutStart: Float,
    val easeOutStop: Float,
    val constraintType: ConstraintType
)

/** Per-joint renaming / transform overrides loaded from a translation table. */
class Translation {
    var outName: String = ""
    var ignore: Boolean = false
    var ignorePositions: Boolean = false
    var relativePositionKey: Boolean = false
    var relativeRotationKey: Boolean = false
    var frameMatrix: Matrix3  = Matrix3()
    var offsetMatrix: Matrix3 = Matrix3()
    var relativePosition: Vector3 = Vector3()
    var mergeParentName: String = ""
    var mergeChildName: String  = ""
    var priorityModifier: Int = 0
}

// ---------------------------------------------------------------------------
// BVHLoader — stateful parser (mirrors C++ LLBVHLoader)
// ---------------------------------------------------------------------------

/**
 * Parses a BVH file from an in-memory [buffer] string.
 *
 * After construction, check [isInitialized] / [status].  Call
 * [applyTranslations] to remap joint names, [optimize] to flag redundant
 * keyframes, and [serialize] to produce a compact byte payload.
 */
class BVHLoader(
    buffer: String,
    jointAliasMap: Map<String, String> = emptyMap()
) {

    // ---- parsed state -------------------------------------------------------

    val joints: MutableList<BvhJoint>       = mutableListOf()
    val constraints: MutableList<BvhConstraint> = mutableListOf()
    val translations: MutableMap<String, Translation> = mutableMapOf()

    var priority: Int     = 0
    var loop: Boolean     = false
    var loopInPoint: Float  = 0f
    var loopOutPoint: Float = 0f
    var easeIn: Float     = 0f
    var easeOut: Float    = 0f
    var hand: Int         = 0
    var emoteName: String = ""

    var numFrames: Int   = 0
    var frameTime: Float = 0f
    var duration: Float  = 0f

    var isInitialized: Boolean = false
    var status: LoadStatus = LoadStatus.NO_FILE

    // ---- parser internals --------------------------------------------------

    private val lines: List<String> = buffer.lines()
    private var linePos: Int = 0
    private var lineNumber: Int = 0

    init {
        status = parseBVH(jointAliasMap)
        isInitialized = (status == LoadStatus.OK)
        if (isInitialized) duration = numFrames.toFloat() * frameTime
    }

    // ---- public API --------------------------------------------------------

    fun getLineNumber(): Int = lineNumber
    fun getDuration(): Float = duration
    fun getNumFrames(): Int  = numFrames
    fun getStatus(): LoadStatus = status

    /** Apply any loaded [translations] to the parsed [joints] list. */
    fun applyTranslations() {
        for (joint in joints) {
            val t = translations[joint.name] ?: continue
            if (t.ignore) { joint.ignore = true; continue }
            if (t.outName.isNotEmpty()) joint.outName = t.outName
            joint.ignorePositions    = t.ignorePositions
            joint.relativePositionKey = t.relativePositionKey
            joint.relativeRotationKey = t.relativeRotationKey
            joint.mergeParentName    = t.mergeParentName
            joint.mergeChildName     = t.mergeChildName
            joint.priority          += t.priorityModifier
            joint.frameMatrix        = t.frameMatrix
            joint.offsetMatrix       = t.offsetMatrix
            joint.relativePosition   = t.relativePosition
        }
    }

    /**
     * Flag redundant keyframes (identical across all frames) so that they can
     * be skipped during serialisation.
     */
    fun optimize() {
        for (joint in joints) {
            if (joint.keys.size < 2) continue
            val allRotSame = joint.keys.zipWithNext().all { (a, b) ->
                a.rot.indices.all { i -> kotlin.math.abs(a.rot[i] - b.rot[i]) < 1e-6f }
            }
            if (allRotSame) joint.keys.forEach { it.ignoreRot = true }

            val allPosSame = joint.keys.zipWithNext().all { (a, b) ->
                a.pos.indices.all { i -> kotlin.math.abs(a.pos[i] - b.pos[i]) < 1e-6f }
            }
            if (allPosSame) joint.keys.forEach { it.ignorePos = true }
        }
    }

    /** Serialise the parsed/optimised data to a compact UTF-8 byte array. */
    fun serialize(): ByteArray {
        val sb = StringBuilder()
        sb.appendLine("frames\t$numFrames")
        sb.appendLine("frameTime\t$frameTime")
        sb.appendLine("duration\t$duration")
        sb.appendLine("loop\t$loop")
        sb.appendLine("loopIn\t$loopInPoint")
        sb.appendLine("loopOut\t$loopOutPoint")
        sb.appendLine("easeIn\t$easeIn")
        sb.appendLine("easeOut\t$easeOut")
        sb.appendLine("emote\t$emoteName")
        sb.appendLine("joints\t${joints.count { !it.ignore }}")
        for (joint in joints) {
            if (joint.ignore) continue
            sb.appendLine("joint\t${joint.outName}\t${joint.priority}")
            for (key in joint.keys) {
                if (!key.ignorePos) sb.appendLine("pos\t${key.pos[0]}\t${key.pos[1]}\t${key.pos[2]}")
                if (!key.ignoreRot) sb.appendLine("rot\t${key.rot[0]}\t${key.rot[1]}\t${key.rot[2]}")
            }
        }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    /** Reset all parsed state so the loader can be reused. */
    fun reset() {
        joints.clear(); constraints.clear()
        numFrames = 0; frameTime = 0f; duration = 0f
        loop = false; loopInPoint = 0f; loopOutPoint = 0f
        easeIn = 0f; easeOut = 0f; emoteName = ""
        isInitialized = false; status = LoadStatus.NO_FILE
        linePos = 0; lineNumber = 0
    }

    // ---- private parsing helpers ------------------------------------------

    private fun nextLine(): String? {
        while (linePos < lines.size) {
            val raw = lines[linePos++].trim()
            lineNumber++
            if (raw.isNotEmpty()) return raw
        }
        return null
    }

    private fun parseBVH(aliasMap: Map<String, String>): LoadStatus {
        val header = nextLine() ?: return LoadStatus.EOF
        if (!header.startsWith("HIERARCHY", ignoreCase = true)) return LoadStatus.NO_HIER

        val rootLine = nextLine() ?: return LoadStatus.EOF
        if (!rootLine.startsWith("ROOT", ignoreCase = true)) return LoadStatus.BAD_ROOT

        val rootName = rootLine.substringAfter("ROOT").trim()
        val rootJoint = BvhJoint(name = rootName, outName = aliasMap[rootName] ?: rootName)
        val hierResult = parseJointBlock(rootJoint, aliasMap)
        if (hierResult != LoadStatus.OK) return hierResult
        joints.add(rootJoint)

        val motionMarker = nextLine() ?: return LoadStatus.EOF
        if (!motionMarker.startsWith("MOTION", ignoreCase = true)) return LoadStatus.NO_MOTION

        val framesLine = nextLine() ?: return LoadStatus.NO_FRAMES
        numFrames = framesLine.substringAfterLast(":").trim().toIntOrNull()
            ?: return LoadStatus.NO_FRAMES

        val ftLine = nextLine() ?: return LoadStatus.NO_FRAME_TIME
        frameTime = ftLine.substringAfterLast(":").trim().toFloatOrNull()
            ?: return LoadStatus.NO_FRAME_TIME

        repeat(numFrames) {
            val dataLine = nextLine() ?: return LoadStatus.NO_POS
            val vals = dataLine.split(Regex("\\s+")).mapNotNull { it.toFloatOrNull() }
            distributeFrameValues(vals)
        }

        return LoadStatus.OK
    }

    /**
     * Parse the brace-delimited block for one joint, recursing into JOINT children.
     * Child joints are appended to [joints] in depth-first order, matching C++ behaviour.
     */
    private fun parseJointBlock(joint: BvhJoint, aliasMap: Map<String, String>): LoadStatus {
        val open = nextLine() ?: return LoadStatus.NO_JOINT
        if (open != "{") return LoadStatus.NO_JOINT

        var line = nextLine() ?: return LoadStatus.EOF
        while (line != "}") {
            when {
                line.startsWith("OFFSET", ignoreCase = true) -> {
                    val p = line.split(Regex("\\s+"))
                    if (p.size < 4) return LoadStatus.NO_OFFSET
                    joint.relativePosition = Vector3(
                        p[1].toFloatOrNull() ?: 0f,
                        p[2].toFloatOrNull() ?: 0f,
                        p[3].toFloatOrNull() ?: 0f
                    )
                }
                line.startsWith("CHANNELS", ignoreCase = true) -> {
                    val p = line.split(Regex("\\s+"))
                    joint.numChannels = p.getOrNull(1)?.toIntOrNull() ?: return LoadStatus.NO_CHANNELS
                    joint.order = p.drop(2).take(3)
                        .mapNotNull { s -> s.firstOrNull { it in "XYZxyz" }?.uppercaseChar() }
                        .joinToString("")
                }
                line.startsWith("JOINT", ignoreCase = true) -> {
                    val childName = line.substringAfter("JOINT").trim()
                    val child = BvhJoint(name = childName, outName = aliasMap[childName] ?: childName)
                    val result = parseJointBlock(child, aliasMap)
                    if (result != LoadStatus.OK) return result
                    joints.add(child)
                }
                line.startsWith("End Site", ignoreCase = true) -> skipBlock()
            }
            line = nextLine() ?: return LoadStatus.EOF
        }
        return LoadStatus.OK
    }

    private fun skipBlock() {
        var depth = 0
        while (true) {
            val line = nextLine() ?: return
            when (line) {
                "{"  -> depth++
                "}"  -> { if (depth == 0) return else depth-- }
            }
        }
    }

    /** Distribute one frame's flat value array across all joints in parse order. */
    private fun distributeFrameValues(values: List<Float>) {
        var idx = 0
        for (joint in joints) {
            val key = BvhKey()
            if (joint.numChannels >= 6) {
                if (!joint.ignorePositions) {
                    key.pos[0] = values.getOrElse(idx++) { 0f }
                    key.pos[1] = values.getOrElse(idx++) { 0f }
                    key.pos[2] = values.getOrElse(idx++) { 0f }
                } else {
                    idx += 3
                    key.ignorePos = true
                }
            }
            key.rot[0] = values.getOrElse(idx++) { 0f }
            key.rot[1] = values.getOrElse(idx++) { 0f }
            key.rot[2] = values.getOrElse(idx++) { 0f }
            joint.keys.add(key)
        }
    }
}
