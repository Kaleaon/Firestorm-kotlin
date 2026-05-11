package com.firestorm.newview

import java.util.UUID

const val MANIPULATOR_SIZE = 5.0f
const val MANIPULATOR_SELECT_SIZE = 20.0f

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    operator fun div(s: Float) = Vec3(x / s, y / s, z / s)
    fun get(axis: Int) = when (axis) { 0 -> x; 1 -> y; else -> z }
    fun set(axis: Int, v: Float) = when (axis) { 0 -> Vec3(v, y, z); 1 -> Vec3(x, v, z); else -> Vec3(x, y, v) }
    fun magSq() = x * x + y * y + z * z
}

data class BBox(
    val centerAgent: Vec3,
    val rotation: FloatArray,
    val minLocal: Vec3,
    val maxLocal: Vec3
) {
    val extentLocal: Vec3 get() = maxLocal - minLocal
}

interface PickInfo {
    val keyMask: Int
    fun getObject(): ViewerObjectRef?
}

interface ViewerObjectRef {
    val id: UUID
    fun isAvatar(): Boolean
    fun isSelected(): Boolean
    fun getPositionAgent(): Vec3
    fun getBoundingBoxAgent(): BBox
    val children: List<ViewerObjectRef>
}

interface SelectionManager {
    fun promoteSelectionToRoot()
    fun deselectAll()
    fun selectObjectAndFamily(obj: ViewerObjectRef)
    fun deselectObjectAndFamily(obj: ViewerObjectRef)
    fun getBBoxOfSelection(): BBox
    fun getSelection(): SelectionSet
    fun sendMultipleUpdate(flags: Int)
}

interface SelectionSet {
    fun getObjectCount(): Int
    fun getSelectType(): Int
    fun iterator(): Iterator<SelectNodeRef?>
    fun rootIterator(): Iterator<SelectNodeRef?>
    fun applyToObjects(func: (ViewerObjectRef) -> Boolean): Boolean
}

interface SelectNodeRef {
    fun getObject(): ViewerObjectRef?
}

const val SELECT_TYPE_HUD = 1
const val MASK_SHIFT = 0x1
const val UPD_POSITION = 0x1

object QToolAlign {

    private var bbox = BBox(Vec3(0f, 0f, 0f), FloatArray(4), Vec3(0f, 0f, 0f), Vec3(0f, 0f, 0f))
    private var manipulatorSize = 0f
    private var highlightedAxis = -1
    private var highlightedDirection = 0f
    private var force = false

    lateinit var selectionManager: SelectionManager

    fun handleSelect() {
        selectionManager.promoteSelectionToRoot()
    }

    fun handleDeselect() {}

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (highlightedAxis != -1) {
            align()
        } else {
            TODO("GPU: pickAsync(x, y, mask, ::pickCallback)")
        }
        return true
    }

    fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        force = (mask and MASK_SHIFT) == 0
        TODO("GPU: setCursor(UI_CURSOR_ARROW)")
        return findSelectedManipulator(x, y)
    }

    fun pickCallback(pickInfo: PickInfo) {
        val obj = pickInfo.getObject()
        if (obj != null) {
            if (obj.isAvatar()) return
            if (pickInfo.keyMask and MASK_SHIFT != 0) {
                if (!obj.isSelected()) selectionManager.selectObjectAndFamily(obj)
                else selectionManager.deselectObjectAndFamily(obj)
            } else {
                selectionManager.deselectAll()
                selectionManager.selectObjectAndFamily(obj)
            }
        } else {
            if (pickInfo.keyMask != MASK_SHIFT) {
                selectionManager.deselectAll()
            }
        }
        selectionManager.promoteSelectionToRoot()
    }

    private fun findSelectedManipulator(x: Int, y: Int): Boolean {
        highlightedAxis = -1
        highlightedDirection = 0f

        TODO("GPU: compute screen-space manipulator hit test using camera projection matrices")

        return false
    }

    fun canAffectSelection(): Boolean {
        val sel = selectionManager.getSelection()
        if (sel.getObjectCount() == 0) return false
        return sel.applyToObjects { obj -> obj.isSelected() }
    }

    fun render() {
        TODO("GPU: render bounding box and cone manipulators via OpenGL")
    }

    private fun computeManipulatorSize() {
        TODO("GPU: compute manipulator size from camera FOV and distance")
    }

    private fun renderManipulators() {
        TODO("GPU: render cone arrow manipulators for each axis")
    }

    private fun bboxOverlap(b1: BBox, b2: BBox): Boolean {
        val delta = b1.centerAgent - b2.centerAgent
        val halfExtent = (b1.extentLocal + b2.extentLocal) / 2.0f
        return (Math.abs(delta.x) < halfExtent.x - Float.MIN_VALUE &&
                Math.abs(delta.y) < halfExtent.y - Float.MIN_VALUE &&
                Math.abs(delta.z) < halfExtent.z - Float.MIN_VALUE)
    }

    private fun getSelectionAxisAlignedBbox(): BBox {
        val selBbox = selectionManager.getBBoxOfSelection()
        val position = selBbox.centerAgent
        var result = BBox(position, FloatArray(4), Vec3(0f, 0f, 0f), Vec3(0f, 0f, 0f))

        val iter = selectionManager.getSelection().iterator()
        while (iter.hasNext()) {
            val node = iter.next()
            val obj = node?.getObject() ?: continue
            val objBbox = obj.getBoundingBoxAgent()
            result = expandBbox(result, objBbox)
        }
        return result
    }

    private fun expandBbox(base: BBox, addition: BBox): BBox {
        TODO("APR: use JVM equivalent for BBox expansion logic")
    }

    private fun align() {
        selectionManager.promoteSelectionToRoot()

        val objects = mutableListOf<ViewerObjectRef>()
        val originalBboxes = mutableMapOf<UUID, BBox>()

        val rootIter = selectionManager.getSelection().rootIterator()
        while (rootIter.hasNext()) {
            val node = rootIter.next()
            val obj = node?.getObject() ?: continue
            val position = obj.getPositionAgent()
            var objBbox = BBox(position, FloatArray(4), Vec3(0f, 0f, 0f), Vec3(0f, 0f, 0f))
            objBbox = expandBbox(objBbox, obj.getBoundingBoxAgent())
            for (child in obj.children) {
                objBbox = expandBbox(objBbox, child.getBoundingBoxAgent())
            }
            objects.add(obj)
            originalBboxes[obj.id] = objBbox
        }

        val axis = highlightedAxis
        val direction = highlightedDirection

        objects.sortWith(Comparator { o1, o2 ->
            val b1 = originalBboxes[o1.id]!!
            val b2 = originalBboxes[o2.id]!!
            val c1 = b1.centerAgent.get(axis) - direction * b1.extentLocal.get(axis) / 2.0f
            val c2 = b2.centerAgent.get(axis) - direction * b2.extentLocal.get(axis) / 2.0f
            (direction * c1).compareTo(direction * c2)
        })

        val newBboxes = originalBboxes.toMutableMap()

        for (i in objects.indices) {
            val targetBbox = bbox
            val targetCorner = targetBbox.centerAgent.get(axis) - direction * targetBbox.extentLocal.get(axis) / 2.0f

            val obj = objects[i]
            val thisBbox = originalBboxes[obj.id]!!
            val thisCorner = thisBbox.centerAgent.get(axis) - direction * thisBbox.extentLocal.get(axis) / 2.0f

            var smallest = direction * 9999999f
            var bestBbox: BBox? = null

            var currentTarget = targetCorner
            for (j in 0..i) {
                val deltaAxis = currentTarget - thisCorner
                val newCenter = thisBbox.centerAgent.set(axis, thisBbox.centerAgent.get(axis) + deltaAxis)
                val newBbox = BBox(newCenter, FloatArray(4),
                    thisBbox.minLocal,
                    thisBbox.maxLocal)

                var overlap = false
                if (!force) {
                    for (k in 0 until i) {
                        val otherBbox = newBboxes[objects[k].id]!!
                        if (bboxOverlap(otherBbox, newBbox)) {
                            overlap = true
                            break
                        }
                    }
                }

                if (!overlap) {
                    val thisValue = newBbox.centerAgent.get(axis) - direction * newBbox.extentLocal.get(axis) / 2.0f
                    if (direction * thisValue < direction * smallest) {
                        smallest = thisValue
                        bestBbox = newBbox
                    }
                }

                if (j < objects.size) {
                    val nextBbox = newBboxes[objects[j].id]!!
                    currentTarget = nextBbox.centerAgent.get(axis) + direction * nextBbox.extentLocal.get(axis) / 2.0f
                }
            }
            if (bestBbox != null) {
                newBboxes[obj.id] = bestBbox
            }
        }

        for (obj in objects) {
            val originalBbox = originalBboxes[obj.id]!!
            val newBbox = newBboxes[obj.id]!!
            val delta = newBbox.centerAgent - originalBbox.centerAgent
            TODO("GPU: obj.setPosition(obj.getPositionAgent() + delta)")
        }

        selectionManager.sendMultipleUpdate(UPD_POSITION)
    }
}
