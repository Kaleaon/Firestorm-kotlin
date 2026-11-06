package com.firestorm.newview

import com.firestorm.llmath.Vector3
import kotlin.math.sqrt

class SpatialGroup {

    var center: Vector3 = Vector3.ZERO
    var radius: Float = 0f
    var bounds: Vector3 = Vector3.ZERO
    val drawables: MutableSet<Drawable> = mutableSetOf()
    val occlusionVerts: FloatArray = FloatArray(24)
    var isDirty: Boolean = true
    var isVisible: Boolean = false

    fun addDrawable(d: Drawable) {
        drawables.add(d)
        d.spatialGroup = this
        isDirty = true
    }

    fun removeDrawable(d: Drawable) {
        drawables.remove(d)
        if (d.spatialGroup === this) d.spatialGroup = null
        isDirty = true
    }

    fun rebound() {
        if (drawables.isEmpty()) {
            center = Vector3.ZERO
            radius = 0f
            isDirty = false
            return
        }
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        for (d in drawables) {
            val p = d.position
            if (p.x < minX) minX = p.x; if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y; if (p.y > maxY) maxY = p.y
            if (p.z < minZ) minZ = p.z; if (p.z > maxZ) maxZ = p.z
        }
        val cx = (minX + maxX) * 0.5f
        val cy = (minY + maxY) * 0.5f
        val cz = (minZ + maxZ) * 0.5f
        center = Vector3(cx, cy, cz)
        val dx = (maxX - minX) * 0.5f
        val dy = (maxY - minY) * 0.5f
        val dz = (maxZ - minZ) * 0.5f
        bounds = Vector3(dx, dy, dz)
        radius = sqrt(dx * dx + dy * dy + dz * dz)
        isDirty = false
    }

    fun isEmpty(): Boolean = drawables.isEmpty()
}
