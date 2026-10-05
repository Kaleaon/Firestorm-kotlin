package com.firestorm.llcharacter

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Vector3

// Constants from lljoint.h
const val LL_CHARACTER_MAX_JOINTS_PER_MESH = 15
const val LL_CHARACTER_MAX_ANIMATED_JOINTS = 216u    // must be divisible by 4
const val LL_MAX_JOINTS_PER_MESH_OBJECT = 110u
const val LL_HAND_JOINT_NUM = LL_CHARACTER_MAX_ANIMATED_JOINTS - 1u
const val LL_FACE_JOINT_NUM = LL_CHARACTER_MAX_ANIMATED_JOINTS - 2u
const val LL_CHARACTER_MAX_PRIORITY = 7
const val LL_MAX_PELVIS_OFFSET = 5f
const val LL_JOINT_THRESHOLD_POS_OFFSET = 0.0001f   // 0.1 mm

enum class SupportCategory { BASE, EXTENDED }

/**
 * Per-mesh attachment override map, keyed by mesh UUID.
 * The "active" override is chosen by max-key ordering (matching C++ max_element on LLUUID).
 */
class Vector3OverrideMap {
    private val map: MutableMap<LLUUID, Vector3> = mutableMapOf()

    fun add(meshId: LLUUID, pos: Vector3) { map[meshId] = pos }

    fun remove(meshId: LLUUID): Boolean = map.remove(meshId) != null

    fun clear() { map.clear() }

    fun count(): Int = map.size

    /** Returns the active (max-key) override if any, null otherwise. */
    fun findActiveOverride(): Pair<LLUUID, Vector3>? {
        return map.maxByOrNull { it.key.toString() }
            ?.let { it.key to it.value }
    }

    fun getMap(): Map<LLUUID, Vector3> = map

    override fun equals(other: Any?): Boolean =
        other is Vector3OverrideMap && map == other.map

    override fun hashCode(): Int = map.hashCode()
}

/**
 * Dirty-flag bits used to track which transforms need recalculating.
 */
object DirtyFlags {
    const val MATRIX_DIRTY: UInt = 0x1u
    const val ROTATION_DIRTY: UInt = 0x2u
    const val POSITION_DIRTY: UInt = 0x4u
    const val ALL_DIRTY: UInt = 0x7u
}

// Extensions on Joint (defined in JointState.kt) for attachment overrides and threshold checks.

fun Joint.aboveJointPosThreshold(pos: Vector3): Boolean {
    val diff = pos - defaultPosition
    return diff.lengthSquared() > LL_JOINT_THRESHOLD_POS_OFFSET * LL_JOINT_THRESHOLD_POS_OFFSET
}

fun Joint.aboveJointScaleThreshold(scale: Vector3): Boolean {
    val threshold = 0.0001f
    val diff = scale - defaultScale
    return diff.lengthSquared() > threshold * threshold
}

fun Joint.setSupport(supportString: String) {
    // support = "base" | "extended"; unknown defaults to BASE
    support = when (supportString) {
        "extended" -> SupportCategory.EXTENDED
        else       -> SupportCategory.BASE
    }
}

// Expose support category on Joint via an extension property backed by a companion store.
// Because Joint is defined elsewhere we use a WeakHashMap surrogate pattern via a simple map.
private val supportMap = HashMap<Joint, SupportCategory>()

var Joint.support: SupportCategory
    get() = supportMap[this] ?: SupportCategory.BASE
    set(v) { supportMap[this] = v }
