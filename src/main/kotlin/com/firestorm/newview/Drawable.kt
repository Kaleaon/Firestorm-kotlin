package com.firestorm.newview

import com.firestorm.llmath.Vector3

class Drawable {

    var viewerObject: ViewerObject? = null
    var position: Vector3 = Vector3.ZERO
    var radius: Float = 0f
    var renderType: Int = 0
    var isVisible: Boolean = true
    var isDead: Boolean = false
    var isNew: Boolean = true
    val faces: MutableList<Face> = mutableListOf()
    var spatialGroup: SpatialGroup? = null

    private var state: UInt = 0u
    private var active: Boolean = false

    fun setActive() {
        active = true
    }

    fun makeStatic() {
        active = false
    }

    fun isActive(): Boolean = active

    fun isDynamic(): Boolean = active && !isDead

    data class Face(
        val index: Int,
        var center: Vector3 = Vector3.ZERO,
        var area: Float = 0f
    )

    companion object {
        const val FORCE_INVISIBLE: UInt = 0x4u
        const val SPATIAL_PARTITION_MOVED: UInt = 0x200u
    }
}
