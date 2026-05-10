package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import com.firestorm.llinventory.*

// Update flags for object transforms
const val UPD_NONE: UByte        = 0x00u
const val UPD_POSITION: UByte    = 0x01u
const val UPD_ROTATION: UByte    = 0x02u
const val UPD_SCALE: UByte       = 0x04u
const val UPD_LINKED_SETS: UByte = 0x08u
const val UPD_UNIFORM: UByte     = 0x10u

const val MAX_CHILDREN_PER_TASK: Int          = 255
const val MAX_CHILDREN_PER_PHYSICAL_TASK: Int = 32
const val SELECT_ALL_TES: Int = -1
const val SELECT_MAX_TES: Int = 32

enum class DeRezDestination {
    SAVE_INTO_AGENT_INVENTORY,
    ACQUIRE_TO_AGENT_INVENTORY,
    SAVE_INTO_TASK_INVENTORY,
    ATTACHMENT,
    TAKE_INTO_AGENT_INVENTORY,
    FORCE_TO_GOD_INVENTORY,
    TRASH,
    ATTACHMENT_TO_INV,
    ATTACHMENT_EXISTS,
    RETURN_TO_OWNER,
    RETURN_TO_LAST_OWNER
}

enum class SendType { ONLY_ROOTS, INDIVIDUALS, ROOTS_FIRST, CHILDREN_FIRST }
enum class GridMode { WORLD, LOCAL, REF_OBJECT }
enum class ActionType { BEGIN, PICK, MOVE, ROTATE, SCALE }
enum class SelectType { WORLD, ATTACHMENT, HUD }

fun interface SelectedObjectFunctor { fun apply(obj: ViewerObject): Boolean }
fun interface SelectedNodeFunctor  { fun apply(node: SelectNode): Boolean }
fun interface SelectedTEFunctor    { fun apply(obj: ViewerObject, face: Int): Boolean }

class SelectNode(val obj: ViewerObject, doGlow: Boolean) {
    var individualSelection: Boolean = false
    var transient: Boolean = false
    var valid: Boolean = false
    var name: String = ""
    var description: String = ""
    var savedPositionLocal: Vector3 = Vector3.ZERO
    var lastPositionLocal: Vector3 = Vector3.ZERO
    var savedScale: Vector3 = Vector3.ZERO
    var savedRotation: Quaternion = Quaternion.IDENTITY
    var itemId: LLUUID = LLUUID.NULL
    var folderId: LLUUID = LLUUID.NULL
    var teSelectMask: Int = 0

    fun selectAllTEs(selected: Boolean) {
        teSelectMask = if (selected) 0xFFFFFFFF.toInt() else 0
    }

    fun selectTE(teIndex: Int, selected: Boolean) {
        teSelectMask = if (selected) teSelectMask or (1 shl teIndex)
                       else teSelectMask and (1 shl teIndex).inv()
    }

    fun isTESelected(teIndex: Int): Boolean = (teSelectMask and (1 shl teIndex)) != 0
    fun hasSelectedTE(): Boolean = teSelectMask != 0
}

class ObjectSelection {
    val selectType: SelectType get() = _selectType
    private var _selectType: SelectType = SelectType.WORLD
    private val nodes: MutableList<SelectNode> = mutableListOf()
    private var primaryObject: ViewerObject? = null

    val allNodes: List<SelectNode> get() = nodes.filter { !it.obj.isDead() }
    val rootNodes: List<SelectNode> get() = allNodes.filter { it.obj.parentId == 0u }
    val validNodes: List<SelectNode> get() = allNodes.filter { it.valid }

    fun isEmpty(): Boolean = nodes.isEmpty()
    fun getObjectCount(): Int = allNodes.size
    fun getRootObjectCount(): Int = rootNodes.size
    fun getNumNodes(): Int = nodes.size

    fun getFirstObject(): ViewerObject? = allNodes.firstOrNull()?.obj
    fun getFirstRootObject(): ViewerObject? = rootNodes.firstOrNull()?.obj
    fun getPrimaryObject(): ViewerObject? = primaryObject

    fun contains(obj: ViewerObject): Boolean = allNodes.any { it.obj === obj }

    fun applyToObjects(func: SelectedObjectFunctor): Boolean =
        allNodes.fold(true) { acc, n -> acc && func.apply(n.obj) }

    fun applyToRootObjects(func: SelectedObjectFunctor, firstOnly: Boolean = false): Boolean {
        for (n in rootNodes) {
            val r = func.apply(n.obj)
            if (firstOnly && r) return true
        }
        return !firstOnly
    }

    fun applyToNodes(func: SelectedNodeFunctor, firstOnly: Boolean = false): Boolean {
        for (n in allNodes) {
            val r = func.apply(n)
            if (firstOnly && r) return true
        }
        return !firstOnly
    }

    internal fun addNode(node: SelectNode) { nodes.add(0, node) }
    internal fun addNodeAtEnd(node: SelectNode) { nodes.add(node) }
    internal fun removeNode(node: SelectNode) { nodes.remove(node) }
    internal fun deleteAllNodes() { nodes.clear() }
    internal fun findNode(obj: ViewerObject): SelectNode? = nodes.find { it.obj === obj }
    internal fun setPrimary(obj: ViewerObject?) { primaryObject = obj }
    internal fun setSelectType(t: SelectType) { _selectType = t }
}

typealias SelectionChangeListener = () -> Unit

private data class SelectionUndoState(val objectIds: List<LLUUID>)

object SelectMgr {
    var sRectSelectInclusive: Boolean    = true
    var sRenderHiddenSelections: Boolean = true
    var sRenderLightRadius: Boolean      = false

    var sHighlightThickness: Float  = 0.01f
    var sHighlightAlpha: Float      = 0.4f
    var sHighlightUScale: Float     = 1.0f
    var sHighlightVScale: Float     = 1.0f

    private val selectedObjects: ObjectSelection    = ObjectSelection()
    private val highlightedObjects: ObjectSelection = ObjectSelection()
    private val hoverObjects: ObjectSelection       = ObjectSelection()

    private val undoStack: ArrayDeque<SelectionUndoState> = ArrayDeque()
    private val redoStack: ArrayDeque<SelectionUndoState> = ArrayDeque()

    private val changeListeners: MutableSet<SelectionChangeListener> = mutableSetOf()

    var teMode: Boolean = false
    var showSelection: Boolean = true

    fun getSelection(): ObjectSelection       = selectedObjects
    fun getHighlightedObjects(): ObjectSelection = highlightedObjects
    fun getHoverObjects(): ObjectSelection    = hoverObjects

    fun addSelectionChangeListener(l: SelectionChangeListener): SelectionChangeListener {
        changeListeners.add(l); return l
    }
    fun removeSelectionChangeListener(l: SelectionChangeListener) { changeListeners.remove(l) }

    private fun notifyChange() { changeListeners.forEach { it() } }

    private fun pushUndo() {
        val ids = selectedObjects.allNodes.map { it.obj.id }
        undoStack.addLast(SelectionUndoState(ids))
        redoStack.clear()
    }

    fun selectObjectAndFamily(obj: ViewerObject, addToEnd: Boolean = false): ObjectSelection {
        pushUndo()
        val node = SelectNode(obj, true)
        node.selectAllTEs(true)
        if (addToEnd) selectedObjects.addNodeAtEnd(node)
        else          selectedObjects.addNode(node)
        selectedObjects.setPrimary(obj)
        notifyChange()
        TODO("Send select message to simulator")
    }

    fun selectObjectOnly(obj: ViewerObject, face: Int = SELECT_ALL_TES): ObjectSelection {
        pushUndo()
        val node = SelectNode(obj, true)
        if (face == SELECT_ALL_TES) node.selectAllTEs(true)
        else node.selectTE(face, true)
        selectedObjects.addNode(node)
        selectedObjects.setPrimary(obj)
        notifyChange()
        TODO("Send select message to simulator")
    }

    fun deselectObjectAndFamily(obj: ViewerObject, sendToSim: Boolean = true) {
        pushUndo()
        val node = selectedObjects.findNode(obj) ?: return
        selectedObjects.removeNode(node)
        notifyChange()
        if (sendToSim) TODO("Send deselect message to simulator")
    }

    fun deselectAll() {
        if (selectedObjects.isEmpty()) return
        pushUndo()
        selectedObjects.deleteAllNodes()
        notifyChange()
        TODO("Send deselect-all message to simulator")
    }

    fun getSelectedObject(id: LLUUID): ViewerObject? =
        selectedObjects.allNodes.firstOrNull { it.obj.id == id }?.obj

    fun highlightObjectAndFamily(obj: ViewerObject) {
        val node = SelectNode(obj, false)
        node.selectAllTEs(true)
        highlightedObjects.addNode(node)
    }

    fun unhighlightAll() { highlightedObjects.deleteAllNodes() }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo() {
        if (!canUndo()) return
        redoStack.addLast(SelectionUndoState(selectedObjects.allNodes.map { it.obj.id }))
        val prev = undoStack.removeLast()
        selectedObjects.deleteAllNodes()
        for (id in prev.objectIds) {
            val obj = ViewerObjectList.findObject(id) ?: continue
            selectedObjects.addNodeAtEnd(SelectNode(obj, true))
        }
        notifyChange()
    }

    fun redo() {
        if (!canRedo()) return
        undoStack.addLast(SelectionUndoState(selectedObjects.allNodes.map { it.obj.id }))
        val next = redoStack.removeLast()
        selectedObjects.deleteAllNodes()
        for (id in next.objectIds) {
            val obj = ViewerObjectList.findObject(id) ?: continue
            selectedObjects.addNodeAtEnd(SelectNode(obj, true))
        }
        notifyChange()
    }

    fun removeObjectFromSelections(id: LLUUID): Boolean {
        val node = selectedObjects.allNodes.firstOrNull { it.obj.id == id } ?: return false
        selectedObjects.removeNode(node)
        notifyChange()
        return true
    }

    fun update() { TODO("Update silhouettes and HUD effects") }

    fun updateSilhouettes() { TODO("GPU: render silhouettes for selected objects") }

    fun renderSilhouettes(forHud: Boolean) { TODO("GPU: draw silhouette pass") }

    fun dump() { println("SelectMgr: ${selectedObjects.getObjectCount()} selected") }
}
