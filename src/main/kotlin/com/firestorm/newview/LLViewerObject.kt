package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Quaternion
import com.firestorm.llmath.Vector3
import com.firestorm.llmath.Vector3d

open class ViewerObject(
    val id: LLUUID,
    val localId: UInt,
    val parentId: UInt,
) {
    enum class UpdateType { TERSE, FULL, COMPRESSED }

    var position: Vector3 = Vector3.ZERO
    var rotation: Quaternion = Quaternion.DEFAULT
    var scale: Vector3 = Vector3.ALL_ONE
    var velocity: Vector3 = Vector3.ZERO
    var angularVelocity: Vector3 = Vector3.ZERO
    var acceleration: Vector3 = Vector3.ZERO
    var region: ViewerRegion? = null
    var primCode: UByte = 0u
    var material: UByte = 0u
    var clickAction: UByte = 0u
    var flags: UInt = 0u

    val children: MutableList<ViewerObject> = mutableListOf()
    var parent: ViewerObject? = null

    private var selected: Boolean = false

    fun getPositionGlobal(): Vector3d {
        val r = region ?: return Vector3d(position.x.toDouble(), position.y.toDouble(), position.z.toDouble())
        return r.localToGlobal(position)
    }

    fun getPositionAgent(): Vector3 = position

    fun getPositionRegion(): Vector3 = position

    open fun isAvatar(): Boolean = false

    fun isAttachment(): Boolean = parentId != 0u && parent?.isAvatar() == true

    fun isPrimitive(): Boolean = !isAvatar()

    fun isSelected(): Boolean = selected

    open fun update(dt: Float) {}
}
