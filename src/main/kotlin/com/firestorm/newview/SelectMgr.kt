package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import com.firestorm.llinventory.*

const val UPD_NONE: UByte        = 0x00u
const val UPD_POSITION: UByte    = 0x01u
const val UPD_ROTATION: UByte    = 0x02u
const val UPD_SCALE: UByte       = 0x04u
const val UPD_LINKED_SETS: UByte = 0x08u
const val UPD_UNIFORM: UByte     = 0x10u

const val MAX_CHILDREN_PER_TASK: Int          = 255
const val MAX_CHILDREN_PER_PHYSICAL_TASK: Int = 32
const val SELECT_ALL_TES: Int                 = -1
const val SELECT_MAX_TES: Int                 = 32
const val TE_SELECT_MASK_ALL: Int             = 0xFFFFFFFF.toInt()

const val SILHOUETTE_UPDATE_THRESHOLD_SQUARED: Float = 0.02f
const val MAX_SILS_PER_FRAME: Int    = 50
const val MAX_OBJECTS_PER_PACKET: Int = 254

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
    RETURN_TO_LAST_OWNER,
}

enum class SendType { ONLY_ROOTS, INDIVIDUALS, ROOTS_FIRST, CHILDREN_FIRST }
enum class GridMode { WORLD, LOCAL, REF_OBJECT }
enum class ActionType { BEGIN, PICK, MOVE, ROTATE, SCALE }
enum class SelectType { WORLD, ATTACHMENT, HUD }

enum class ShowHideHighlight { NORMAL, SHOW, HIDE }

fun interface SelectedObjectFunctor { fun apply(obj: ViewerObject): Boolean }
fun interface SelectedNodeFunctor  { fun apply(node: SelectNode): Boolean }
fun interface SelectedTEFunctor    { fun apply(obj: ViewerObject, face: Int): Boolean }

data class DeRezInfo(val destination: DeRezDestination, val destinationId: LLUUID)

data class AvatarPositionOverride(
    val lastPositionLocal: Vector3,
    val lastRotation: Quaternion,
    val obj: ViewerObject,
)

class SelectNode(val obj: ViewerObject, doGlow: Boolean) {
    var individualSelection: Boolean = false
    var transient: Boolean = false
    var valid: Boolean = false
    var name: String = ""
    var description: String = ""
    var savedPositionLocal: Vector3 = Vector3.ZERO
    var lastPositionLocal: Vector3 = Vector3.ZERO
    var lastMoveLocal: Vector3 = Vector3.ZERO
    var savedPositionGlobal: Vector3d = Vector3d.ZERO
    var savedScale: Vector3 = Vector3.ZERO
    var lastScale: Vector3 = Vector3.ZERO
    var savedRotation: Quaternion = Quaternion.IDENTITY
    var lastRotation: Quaternion = Quaternion.IDENTITY
    var duplicated: Boolean = false
    var duplicatePos: Vector3d = Vector3d.ZERO
    var duplicateRot: Quaternion = Quaternion.IDENTITY
    var itemId: LLUUID = LLUUID.NULL
    var folderId: LLUUID = LLUUID.NULL
    var fromTaskId: LLUUID = LLUUID.NULL
    var touchName: String = ""
    var sitName: String = ""
    var creationDate: ULong = 0uL
    var inventorySerial: Short = 0

    var selectedGLTFNode: Int = -1
    var selectedGLTFPrimitive: Int = -1

    val savedColors: MutableList<FloatArray> = mutableListOf()
    val savedShinyColors: MutableList<FloatArray> = mutableListOf()
    val savedTextures: MutableList<LLUUID> = mutableListOf()
    val savedGLTFMaterialIds: MutableList<LLUUID> = mutableListOf()
    val textureScaleRatios: MutableList<Vector3> = mutableListOf()
    val silhouetteVertices: MutableList<Vector3> = mutableListOf()
    val silhouetteNormals: MutableList<Vector3> = mutableListOf()
    var silhouetteExists: Boolean = false

    private var teSelectMask: Int = 0
    private var lastTESelected: Int = -1

    fun selectAllTEs(selected: Boolean) {
        teSelectMask = if (selected) TE_SELECT_MASK_ALL else 0
    }

    fun selectTE(teIndex: Int, selected: Boolean) {
        teSelectMask = if (selected) teSelectMask or (1 shl teIndex)
                       else teSelectMask and (1 shl teIndex).inv()
        if (selected) lastTESelected = teIndex
    }

    fun selectGLTFNode(nodeIndex: Int, primitiveIndex: Int, selected: Boolean) {
        if (selected) {
            selectedGLTFNode = nodeIndex
            selectedGLTFPrimitive = primitiveIndex
        } else if (selectedGLTFNode == nodeIndex) {
            selectedGLTFNode = -1
            selectedGLTFPrimitive = -1
        }
    }

    fun isTESelected(teIndex: Int): Boolean = (teSelectMask and (1 shl teIndex)) != 0

    fun hasSelectedTE(): Boolean = teSelectMask != 0

    fun getLastSelectedTE(): Int = lastTESelected
    fun getLastOperatedTE(): Int = lastTESelected
    fun getTESelectMask(): Int = teSelectMask

    fun setTransient(t: Boolean) { transient = t }
    fun isTransient(): Boolean = transient

    fun getObject(): ViewerObject = obj

    fun saveColors() { System.err.println("SelectNode: snapshot current TE colors not yet implemented") }
    fun saveShinyColors() { System.err.println("SelectNode: snapshot current TE shiny colors not yet implemented") }
    fun saveTextures(textures: List<LLUUID>) { savedTextures.clear(); savedTextures.addAll(textures) }
    fun saveGLTFMaterials(materials: List<LLUUID>, overrides: List<Any>) {
        savedGLTFMaterialIds.clear(); savedGLTFMaterialIds.addAll(materials)
    }
    fun saveTextureScaleRatios(texIndex: Int) { System.err.println("SelectNode: compute and store texture scale ratios not yet implemented") }

    fun allowOperationOnNode(op: UInt, groupProxyPower: ULong): Boolean {
        System.err.println("SelectNode: check permissions against op and group proxy power not yet implemented")
        return false
    }

    fun renderOneSilhouette(color: FloatArray) {
        // GPU: render silhouette for this node
    }
}

class ObjectSelection {
    private var selectType: SelectType = SelectType.WORLD
    private val list: ArrayDeque<SelectNode> = ArrayDeque()
    var primaryObject: ViewerObject? = null
    private val selectNodeMap: MutableMap<ViewerObject, SelectNode> = mutableMapOf()

    fun getSelectType(): SelectType = selectType
    internal fun setSelectType(t: SelectType) { selectType = t }

    val allNodes: List<SelectNode> get() = list.filter { !it.obj.isDead() }
    val rootNodes: List<SelectNode> get() = allNodes.filter { it.obj.parentId == 0u }
    val validNodes: List<SelectNode> get() = allNodes.filter { it.valid }
    val rootValidNodes: List<SelectNode> get() = validNodes.filter { it.obj.parentId == 0u }

    fun isEmpty(): Boolean = list.isEmpty()

    fun getObjectCount(): Int = allNodes.size
    fun getRootObjectCount(): Int = rootNodes.size
    fun getNumNodes(): Int = list.size
    fun getTECount(): Int = allNodes.sumOf { node ->
        (0 until SELECT_MAX_TES).count { node.isTESelected(it) }
    }

    fun getFirstObject(): ViewerObject? = allNodes.firstOrNull()?.obj
    fun getFirstRootObject(nonRootOk: Boolean = false): ViewerObject? {
        val rn = rootNodes.firstOrNull()
        return rn?.obj ?: if (nonRootOk) getFirstObject() else null
    }
    fun getPrimaryObject(): ViewerObject? = primaryObject

    fun getFirstNode(func: SelectedNodeFunctor? = null): SelectNode? =
        if (func == null) allNodes.firstOrNull()
        else allNodes.firstOrNull { func.apply(it) }

    fun getFirstRootNode(func: SelectedNodeFunctor? = null, nonRootOk: Boolean = false): SelectNode? {
        val candidates = if (func == null) rootNodes else rootNodes.filter { func.apply(it) }
        return candidates.firstOrNull() ?: if (nonRootOk) getFirstNode(func) else null
    }

    fun findNode(obj: ViewerObject): SelectNode? = selectNodeMap[obj]

    fun contains(obj: ViewerObject): Boolean = selectNodeMap.containsKey(obj)

    fun contains(obj: ViewerObject, te: Int): Boolean {
        val node = findNode(obj) ?: return false
        return if (te == SELECT_ALL_TES) node.hasSelectedTE()
               else node.isTESelected(te)
    }

    fun isAttachment(): Boolean = allNodes.any { it.obj.isAttachment() }

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

    fun applyToRootNodes(func: SelectedNodeFunctor, firstOnly: Boolean = false): Boolean {
        for (n in rootNodes) {
            val r = func.apply(n)
            if (firstOnly && r) return true
        }
        return !firstOnly
    }

    fun applyToTEs(func: SelectedTEFunctor, firstOnly: Boolean = false): Boolean {
        for (node in allNodes) {
            for (te in 0 until SELECT_MAX_TES) {
                if (!node.isTESelected(te)) continue
                val r = func.apply(node.obj, te)
                if (firstOnly && r) return true
            }
        }
        return !firstOnly
    }

    fun getSelectedObjectCost(): Float = allNodes.map { it.obj.objectCost }.filter { it >= 0f }.sum()
    fun getSelectedLinksetCost(): Float = rootNodes.map { it.obj.linksetCost }.filter { it >= 0f }.sum()
    fun getSelectedPhysicsCost(): Float = allNodes.map { it.obj.physicsCost }.filter { it >= 0f }.sum()
    fun getSelectedLinksetPhysicsCost(): Float = rootNodes.map { it.obj.linksetPhysicsCost }.filter { it >= 0f }.sum()

    fun getSelectedObjectRenderCost(): Int {
        // GPU: sum render cost across nodes
        return 0
    }
    fun getSelectedObjectStreamingCost(): Float {
        // GPU: sum streaming costs across nodes
        return 0f
    }
    fun getSelectedObjectTriangleCount(): UInt {
        // GPU: sum triangle counts across nodes
        return 0u
    }

    fun checkAnimatedObjectEstTris(): Boolean {
        System.err.println("ObjectSelection: check triangle limit for animated object selection not yet implemented")
        return false
    }
    fun checkAnimatedObjectLinkable(): Boolean {
        System.err.println("ObjectSelection: check linkability constraints for animated objects not yet implemented")
        return false
    }

    fun isMultipleTESelected(): Boolean {
        var count = 0
        for (n in allNodes) {
            for (te in 0 until SELECT_MAX_TES) {
                if (n.isTESelected(te)) {
                    count++
                    if (count > 1) return true
                }
            }
        }
        return false
    }

    fun updateEffects() { System.err.println("ObjectSelection: update HUD effects for this selection not yet implemented") }

    internal fun addNode(node: SelectNode) {
        list.addFirst(node)
        selectNodeMap[node.obj] = node
    }
    internal fun addNodeAtEnd(node: SelectNode) {
        list.addLast(node)
        selectNodeMap[node.obj] = node
    }
    internal fun moveNodeToFront(node: SelectNode) {
        list.remove(node)
        list.addFirst(node)
    }
    internal fun removeNode(node: SelectNode) {
        list.remove(node)
        selectNodeMap.remove(node.obj)
    }
    internal fun deleteAllNodes() {
        list.clear()
        selectNodeMap.clear()
        primaryObject = null
    }
    internal fun cleanupNodes() {
        val dead = list.filter { it.obj.isDead() }
        dead.forEach { removeNode(it) }
    }
    internal fun setPrimary(obj: ViewerObject?) { primaryObject = obj }
}

typealias ObjectSelectionHandle = ObjectSelection?
typealias SelectionChangeListener = () -> Unit

private data class SelectionUndoState(val objectIds: List<LLUUID>)

class SelectionCallbackData {
    val selection: ObjectSelection = ObjectSelection()

    init {
        val src = SelectMgr.getSelection()
        for (node in src.allNodes) {
            val copy = SelectNode(node.obj, false)
            copy.valid = node.valid
            copy.name = node.name
            selection.addNodeAtEnd(copy)
            when {
                node.obj.isHUDAttachment()  -> selection.setSelectType(SelectType.HUD)
                node.obj.isAttachment()     -> selection.setSelectType(SelectType.ATTACHMENT)
                else                        -> selection.setSelectType(SelectType.WORLD)
            }
        }
    }
}

object SelectMgr {
    var sRectSelectInclusive: Boolean    = true
    var sRenderHiddenSelections: Boolean = true
    var sRenderLightRadius: Boolean      = false

    var sHighlightThickness: Float  = 0f
    var sHighlightUScale: Float     = 0f
    var sHighlightVScale: Float     = 0f
    var sHighlightAlpha: Float      = 0f
    var sHighlightAlphaTest: Float  = 0f
    var sHighlightUAnim: Float      = 0f
    var sHighlightVAnim: Float      = 0f

    var sSilhouetteParentColor: FloatArray = floatArrayOf(1f, 0f, 0f, 1f)
    var sSilhouetteChildColor: FloatArray  = floatArrayOf(0f, 1f, 0f, 1f)
    var sHighlightParentColor: FloatArray  = floatArrayOf(1f, 0.5f, 0f, 1f)
    var sHighlightChildColor: FloatArray   = floatArrayOf(1f, 1f, 0f, 1f)
    var sHighlightInspectColor: FloatArray = floatArrayOf(0f, 1f, 1f, 1f)
    var sContextSilhouetteColor: FloatArray = floatArrayOf(0.5f, 0.5f, 0.5f, 1f)

    var hideSelectedObjects: Boolean        = false
    var renderHighlightSelections: Boolean  = true
    var allowSelectAvatar: Boolean          = false
    var debugSelectMgr: Boolean             = false

    private val selectedObjects: ObjectSelection    = ObjectSelection()
    private val highlightedObjects: ObjectSelection = ObjectSelection()
    private val hoverObjects: ObjectSelection       = ObjectSelection()
    private val rectSelectedObjects: MutableSet<ViewerObject> = mutableSetOf()

    private val gridObjects: ObjectSelection = ObjectSelection()
    private var gridRotation: Quaternion = Quaternion.IDENTITY
    private var gridOrigin: Vector3 = Vector3.ZERO
    private var gridScale: Vector3 = Vector3.ALL_ONE
    private var gridMode: GridMode = GridMode.WORLD

    private val undoStack: ArrayDeque<SelectionUndoState> = ArrayDeque()
    private val redoStack: ArrayDeque<SelectionUndoState> = ArrayDeque()

    val updateSignal: MutableList<SelectionChangeListener> = mutableListOf()

    var teMode: Boolean = false
    var showSelection: Boolean = true
    private var renderSilhouettes: Boolean = true
    private var forceSelection: Boolean = false
    private var fsShowHideHighlight: ShowHideHighlight = ShowHideHighlight.NORMAL

    private var selectionCenterGlobal: Vector3d = Vector3d.ZERO
    private var lastSentSelectionCenterGlobal: Vector3d = Vector3d.ZERO
    private var lastCameraPos: Vector3d = Vector3d.ZERO

    val avatarOverridesMap: MutableMap<LLUUID, AvatarPositionOverride> = mutableMapOf()

    fun getSelection(): ObjectSelection       = selectedObjects
    fun getEditSelection(): ObjectSelection   { convertTransient(); return selectedObjects }
    fun getHighlightedObjects(): ObjectSelection = highlightedObjects
    fun getHoverObjects(): ObjectSelection    = hoverObjects

    fun addUpdateListener(l: SelectionChangeListener): SelectionChangeListener {
        updateSignal.add(l); return l
    }
    fun removeUpdateListener(l: SelectionChangeListener) { updateSignal.remove(l) }

    private fun notifyChange() { updateSignal.forEach { it() } }

    private fun pushUndo() {
        undoStack.addLast(SelectionUndoState(selectedObjects.allNodes.map { it.obj.id }))
        redoStack.clear()
    }

    fun setForceSelection(force: Boolean): Boolean {
        val prev = forceSelection
        forceSelection = force
        return prev
    }

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

    fun canDoDelete(): Boolean = selectedObjects.getObjectCount() > 0
    fun doDelete() = selectDelete()

    fun deselect() = deselectAll()
    fun canDeselect(): Boolean = !selectedObjects.isEmpty()

    fun duplicate() = selectDuplicate(Vector3.ZERO, true)
    fun canDuplicate(): Boolean = selectedObjects.getObjectCount() > 0

    fun clearSelections() {
        selectedObjects.deleteAllNodes()
        highlightedObjects.deleteAllNodes()
        hoverObjects.deleteAllNodes()
        notifyChange()
    }

    fun selectObjectAndFamily(obj: ViewerObject, addToEnd: Boolean = false,
                               ignoreSelectOwned: Boolean = false): ObjectSelection {
        if (!canSelectObject(obj, ignoreSelectOwned)) return selectedObjects
        pushUndo()
        val node = SelectNode(obj, true)
        node.selectAllTEs(true)
        if (addToEnd) selectedObjects.addNodeAtEnd(node)
        else          selectedObjects.addNode(node)
        obj.children.forEach { child ->
            val childNode = SelectNode(child, true)
            childNode.selectAllTEs(true)
            selectedObjects.addNodeAtEnd(childNode)
        }
        selectedObjects.setPrimary(obj)
        obj.setSelected(true)
        notifyChange()
        sendSelect()
        return selectedObjects
    }

    fun selectObjectOnly(obj: ViewerObject, face: Int = SELECT_ALL_TES,
                         gltfNode: Int = -1, gltfPrimitive: Int = -1): ObjectSelection {
        pushUndo()
        val node = SelectNode(obj, true)
        if (face == SELECT_ALL_TES) node.selectAllTEs(true)
        else node.selectTE(face, true)
        if (gltfNode >= 0) node.selectGLTFNode(gltfNode, gltfPrimitive, true)
        selectedObjects.addNode(node)
        selectedObjects.setPrimary(obj)
        obj.setSelected(true)
        notifyChange()
        sendSelect()
        return selectedObjects
    }

    fun selectObjectAndFamily(objects: List<ViewerObject>, sendToSim: Boolean = true): ObjectSelection {
        if (objects.isEmpty()) return selectedObjects
        pushUndo()
        for (obj in objects) {
            if (!canSelectObject(obj)) continue
            val node = SelectNode(obj, true)
            node.selectAllTEs(true)
            selectedObjects.addNodeAtEnd(node)
            obj.setSelected(true)
        }
        selectedObjects.setPrimary(objects.first())
        notifyChange()
        if (sendToSim) sendSelect()
        return selectedObjects
    }

    fun selectHighlightedObjects(): ObjectSelection {
        pushUndo()
        for (node in highlightedObjects.allNodes) {
            val newNode = SelectNode(node.obj, true)
            newNode.selectAllTEs(true)
            selectedObjects.addNodeAtEnd(newNode)
            node.obj.setSelected(true)
        }
        highlightedObjects.deleteAllNodes()
        notifyChange()
        sendSelect()
        return selectedObjects
    }

    fun setHoverObject(obj: ViewerObject?, face: Int = -1): ObjectSelection {
        hoverObjects.deleteAllNodes()
        if (obj != null) {
            val node = SelectNode(obj, false)
            if (face >= 0) node.selectTE(face, true) else node.selectAllTEs(true)
            hoverObjects.addNode(node)
        }
        return hoverObjects
    }

    fun getHoverNode(): SelectNode? = hoverObjects.getFirstNode()

    fun getPrimaryHoverNode(): SelectNode? = hoverObjects.allNodes.firstOrNull { it.obj === hoverObjects.primaryObject }
        ?: hoverObjects.getFirstNode()

    fun highlightObjectOnly(obj: ViewerObject) {
        val node = SelectNode(obj, false)
        node.selectAllTEs(true)
        highlightedObjects.addNode(node)
    }

    fun highlightObjectAndFamily(obj: ViewerObject) {
        highlightObjectOnly(obj)
        obj.children.forEach { highlightObjectOnly(it) }
    }

    fun highlightObjectAndFamily(objects: List<ViewerObject>) {
        objects.forEach { highlightObjectAndFamily(it) }
    }

    fun deselectObjectOnly(obj: ViewerObject, sendToSim: Boolean = true) {
        val node = selectedObjects.findNode(obj) ?: return
        pushUndo()
        selectedObjects.removeNode(node)
        obj.setSelected(false)
        notifyChange()
        if (sendToSim) System.err.println("SelectMgr: send deselect to simulator not yet implemented")
    }

    fun deselectObjectAndFamily(obj: ViewerObject, sendToSim: Boolean = true,
                                includeEntireObject: Boolean = false) {
        val node = selectedObjects.findNode(obj) ?: return
        pushUndo()
        selectedObjects.removeNode(node)
        if (includeEntireObject) {
            obj.children.forEach { child ->
                selectedObjects.findNode(child)?.let { selectedObjects.removeNode(it) }
                child.setSelected(false)
            }
        }
        obj.setSelected(false)
        notifyChange()
        if (sendToSim) System.err.println("SelectMgr: send deselect to simulator not yet implemented")
    }

    fun deselectAll() {
        if (selectedObjects.isEmpty()) return
        pushUndo()
        selectedObjects.allNodes.forEach { it.obj.setSelected(false) }
        selectedObjects.deleteAllNodes()
        notifyChange()
        System.err.println("SelectMgr: send deselect-all to simulator not yet implemented")
    }

    fun deselectAllForStandingUp() {
        selectedObjects.allNodes.forEach { it.obj.setSelected(false) }
        selectedObjects.deleteAllNodes()
        notifyChange()
    }

    fun deselectUnused() {
        if (selectedObjects.getNumNodes() <= 1) deselectAll()
    }

    fun deselectAllIfTooFar() { System.err.println("SelectMgr: check distance and deselect if over threshold not yet implemented") }

    fun deselectHighlightedObjects() {
        for (node in highlightedObjects.allNodes.toList()) {
            selectedObjects.findNode(node.obj)?.let { selectedObjects.removeNode(it) }
        }
        highlightedObjects.deleteAllNodes()
        notifyChange()
    }

    fun unhighlightObjectOnly(obj: ViewerObject) {
        highlightedObjects.findNode(obj)?.let { highlightedObjects.removeNode(it) }
    }

    fun unhighlightObjectAndFamily(obj: ViewerObject) {
        unhighlightObjectOnly(obj)
        obj.children.forEach { unhighlightObjectOnly(it) }
    }

    fun unhighlightAll() { highlightedObjects.deleteAllNodes() }

    fun removeObjectFromSelections(id: LLUUID): Boolean {
        val node = selectedObjects.allNodes.firstOrNull { it.obj.id == id } ?: return false
        selectedObjects.removeNode(node)
        notifyChange()
        return true
    }

    fun canSelectObject(obj: ViewerObject, ignoreSelectOwned: Boolean = false): Boolean {
        if (obj.isDead()) return false
        if (!obj.canSelect) return false
        return true
    }

    fun linkObjects(): Boolean {
        System.err.println("SelectMgr: send link request not yet implemented")
        return false
    }
    fun unlinkObjects(): Boolean {
        System.err.println("SelectMgr: send unlink request not yet implemented")
        return false
    }
    fun enableLinkObjects(): Boolean = selectedObjects.getRootObjectCount() > 1
    fun enableUnlinkObjects(): Boolean = selectedObjects.getObjectCount() > 1

    fun update() {
        selectedObjects.cleanupNodes()
        updateSelectionCenter()
        updateSilhouettes()
        updateEffects()
    }

    fun updateEffects() { System.err.println("SelectMgr: update HUD beam and selection effects not yet implemented") }

    fun resetObjectOverrides() { System.err.println("SelectMgr: clear local position/rotation/scale overrides not yet implemented") }
    fun resetObjectOverrides(selectionHandle: ObjectSelection) { System.err.println("SelectMgr: clear overrides for handle not yet implemented") }
    fun overrideObjectUpdates() { System.err.println("SelectMgr: install local-override update interceptors not yet implemented") }
    fun resetAvatarOverrides() { avatarOverridesMap.clear() }
    fun overrideAvatarUpdates() { System.err.println("SelectMgr: install avatar override interceptors not yet implemented") }

    fun setTEMode(b: Boolean) { teMode = b }
    fun getTEMode(): Boolean = teMode

    fun shouldShowSelection(): Boolean = showSelection

    fun enableSilhouette(enable: Boolean) { renderSilhouettes = enable }

    fun setFSShowHideHighlight(state: ShowHideHighlight) { fsShowHideHighlight = state }

    fun getGridMode(): GridMode = gridMode

    fun setGridMode(mode: GridMode) { gridMode = mode }

    fun addGridObject(obj: ViewerObject) {
        val node = SelectNode(obj, false)
        gridObjects.addNodeAtEnd(node)
    }

    fun clearGridObjects() { gridObjects.deleteAllNodes() }

    fun getGrid(origin: Vector3, rotation: Quaternion, scale: Vector3, forSnapGuides: Boolean = false) {
        System.err.println("SelectMgr: compute grid basis from gridMode and objects not yet implemented")
    }

    fun getSelectionCenterGlobal(): Vector3d = selectionCenterGlobal

    fun updateSelectionCenter() {
        if (selectedObjects.isEmpty()) {
            selectionCenterGlobal = Vector3d.ZERO
            return
        }
        var sumX = 0.0; var sumY = 0.0; var sumZ = 0.0
        val nodes = selectedObjects.allNodes
        nodes.forEach { n ->
            val p = n.obj.getPositionGlobal()
            sumX += p.x; sumY += p.y; sumZ += p.z
        }
        val count = nodes.size.toDouble()
        selectionCenterGlobal = Vector3d(sumX / count, sumY / count, sumZ / count)
    }

    fun updatePointAt() { System.err.println("SelectMgr: send PointAt effect not yet implemented") }

    fun getBBoxOfSelection(): Any {
        System.err.println("SelectMgr: compute bounding box of all selected objects not yet implemented")
        return Unit
    }

    fun getSavedBBoxOfSelection(): Any {
        System.err.println("SelectMgr: return saved bounding box not yet implemented")
        return Unit
    }

    fun saveSelectedObjectTransform(actionType: ActionType) { System.err.println("SelectMgr: snapshot positions/rotations/scales not yet implemented") }
    fun saveSelectedObjectColors() { selectedObjects.allNodes.forEach { it.saveColors() } }
    fun saveSelectedShinyColors() { selectedObjects.allNodes.forEach { it.saveShinyColors() } }
    fun saveSelectedObjectTextures() { System.err.println("SelectMgr: snapshot current texture ids for each TE not yet implemented") }

    fun selectionUpdatePhysics(usePhysics: Boolean) { System.err.println("SelectMgr: update physics flags not yet implemented") }
    fun selectionUpdateTemporary(isTemporary: Boolean) { System.err.println("SelectMgr: selectionUpdateTemporary not yet implemented") }
    fun selectionUpdatePhantom(isGhost: Boolean) { System.err.println("SelectMgr: selectionUpdatePhantom not yet implemented") }
    fun selectionDump() { selectedObjects.allNodes.forEach { it.obj.dump() } }

    fun selectionAllPCode(code: UByte): Boolean = selectedObjects.allNodes.all { it.obj.primCode == code }

    fun selectionGetClickAction(outAction: () -> Unit): Boolean {
        System.err.println("SelectMgr: aggregate click actions across selection not yet implemented")
        return false
    }
    fun selectionGetIncludeInSearch(out: () -> Unit): Boolean {
        System.err.println("SelectMgr: check if all selected have same include-in-search value not yet implemented")
        return false
    }
    fun selectionGetGlow(glow: () -> Unit): Boolean {
        System.err.println("SelectMgr: aggregate glow values not yet implemented")
        return false
    }

    fun selectionSetPhysicsType(type: UByte) { selectedObjects.allNodes.forEach { it.obj.setPhysicsShapeType(type) } }
    fun selectionSetGravity(gravity: Float) { selectedObjects.allNodes.forEach { it.obj.setPhysicsGravity(gravity) } }
    fun selectionSetFriction(friction: Float) { selectedObjects.allNodes.forEach { it.obj.setPhysicsFriction(friction) } }
    fun selectionSetDensity(density: Float) { selectedObjects.allNodes.forEach { it.obj.setPhysicsDensity(density) } }
    fun selectionSetRestitution(restitution: Float) { selectedObjects.allNodes.forEach { it.obj.setPhysicsRestitution(restitution) } }
    fun selectionSetMaterial(material: UByte) { selectedObjects.allNodes.forEach { it.obj.material = material } }
    fun selectionSetImage(imageId: LLUUID, isPBR: Boolean = true): Boolean {
        System.err.println("SelectMgr: send TE texture update not yet implemented")
        return false
    }
    fun selectionSetGLTFMaterial(matId: LLUUID): Boolean {
        System.err.println("SelectMgr: set GLTF material by id not yet implemented")
        return false
    }
    fun selectionSetColor(color: FloatArray) { System.err.println("SelectMgr: send TE color update not yet implemented") }
    fun selectionSetColorOnly(color: FloatArray) { System.err.println("SelectMgr: send RGB-only color update not yet implemented") }
    fun selectionSetAlphaOnly(alpha: Float) { System.err.println("SelectMgr: send alpha-only update not yet implemented") }
    fun selectionRevertColors() { System.err.println("SelectMgr: restore colors from node.savedColors not yet implemented") }
    fun selectionRevertShinyColors() { System.err.println("SelectMgr: restore shiny colors not yet implemented") }
    fun selectionRevertTextures(): Boolean {
        System.err.println("SelectMgr: restore textures from node.savedTextures not yet implemented")
        return false
    }
    fun selectionRevertGLTFMaterials() { System.err.println("SelectMgr: restore GLTF materials from node.savedGLTFMaterialIds not yet implemented") }
    fun selectionSetBumpmap(bumpmap: UByte, imageId: LLUUID) { System.err.println("SelectMgr: send bump map update not yet implemented") }
    fun selectionSetTexGen(texGen: UByte) { System.err.println("SelectMgr: send texgen update not yet implemented") }
    fun selectionSetShiny(shiny: UByte, imageId: LLUUID) { System.err.println("SelectMgr: send shiny update not yet implemented") }
    fun selectionSetFullbright(fullbright: UByte) { System.err.println("SelectMgr: send fullbright update not yet implemented") }
    fun selectionSetMedia(mediaType: UByte, mediaData: Map<String, Any>) { System.err.println("SelectMgr: send media update not yet implemented") }
    fun selectionSetClickAction(action: UByte) { selectedObjects.allNodes.forEach { it.obj.setClickAction(action) } }
    fun selectionSetIncludeInSearch(include: Boolean) { selectedObjects.allNodes.forEach { it.obj.setIncludeInSearch(include) } }
    fun selectionSetGlow(glow: Float) { System.err.println("SelectMgr: send glow update not yet implemented") }
    fun selectionSetMaterialParams(func: Any, specificTE: Int = -1) { System.err.println("SelectMgr: apply material params functor not yet implemented") }
    fun selectionRemoveMaterial() { System.err.println("SelectMgr: remove material from TEs not yet implemented") }

    fun selectionSetObjectPermissions(permField: UByte, set: Boolean, permMask: UInt, override: Boolean = false) {
        System.err.println("SelectMgr: send ObjectPermissions message not yet implemented")
    }
    fun selectionSetObjectName(name: String) { System.err.println("SelectMgr: send ObjectName message not yet implemented") }
    fun selectionSetObjectDescription(desc: String) { System.err.println("SelectMgr: send ObjectDescription message not yet implemented") }
    fun selectionSetObjectSaleInfo(saleInfo: Any) { System.err.println("SelectMgr: send ObjectSaleInfo message not yet implemented") }

    fun selectionTexScaleAutofit(repeatsPerMeter: Float) { System.err.println("SelectMgr: auto-fit texture scaling not yet implemented") }
    fun adjustTexturesByScale(sendToSim: Boolean, stretch: Boolean) { System.err.println("SelectMgr: adjust textures proportional to scale change not yet implemented") }

    fun selectionMove(displacement: Vector3, rx: Float, ry: Float, rz: Float, updateType: UInt): Boolean {
        System.err.println("SelectMgr: apply transform to selection not yet implemented")
        return false
    }
    fun sendSelectionMove() { System.err.println("SelectMgr: send MultipleObjectUpdate position not yet implemented") }

    fun sendMultipleUpdate(type: UInt) { System.err.println("SelectMgr: send MultipleObjectUpdate not yet implemented") }
    fun sendOwner(ownerId: LLUUID, groupId: LLUUID, override: Boolean = false) { System.err.println("SelectMgr: sendOwner not yet implemented") }
    fun sendGroup(groupId: LLUUID) { System.err.println("SelectMgr: sendGroup not yet implemented") }
    fun sendBuy(buyerId: LLUUID, categoryId: LLUUID, saleInfo: Any) { System.err.println("SelectMgr: sendBuy not yet implemented") }
    fun sendAttach(attachmentPoint: UByte, replace: Boolean) { System.err.println("SelectMgr: sendAttach not yet implemented") }
    fun sendAttach(selectionHandle: ObjectSelection, attachmentPoint: UByte, replace: Boolean) { System.err.println("SelectMgr: sendAttach(handle) not yet implemented") }
    fun sendDetach() { System.err.println("SelectMgr: sendDetach not yet implemented") }
    fun sendDropAttachment() { System.err.println("SelectMgr: sendDropAttachment not yet implemented") }
    fun sendLink() { System.err.println("SelectMgr: sendLink not yet implemented") }
    fun sendDelink() { System.err.println("SelectMgr: sendDelink not yet implemented") }
    fun sendSelect() { System.err.println("SelectMgr: send ObjectSelect message not yet implemented") }
    fun sendGodlikeRequest(request: String, parameter: String) { System.err.println("SelectMgr: sendGodlikeRequest not yet implemented") }

    fun selectDelete() { System.err.println("SelectMgr: send DeRezObject to trash not yet implemented") }
    fun selectForceDelete() { System.err.println("SelectMgr: force delete without trash not yet implemented") }
    fun selectDuplicate(offset: Vector3, selectCopy: Boolean) { System.err.println("SelectMgr: send ObjectDuplicate not yet implemented") }
    fun repeatDuplicate() { System.err.println("SelectMgr: repeatDuplicate not yet implemented") }
    fun selectDuplicateOnRay(rayStart: Vector3, rayEnd: Vector3, bypassRaycast: Boolean,
                              rayEndIsIntersection: Boolean, rayTargetId: LLUUID,
                              copyCenters: Boolean, copyRotates: Boolean, selectCopy: Boolean) {
        System.err.println("SelectMgr: send ObjectDuplicateOnRay not yet implemented")
    }

    fun requestObjectPropertiesFamily(obj: ViewerObject) { System.err.println("SelectMgr: send RequestObjectPropertiesFamily not yet implemented") }

    fun requestGodInfo() { System.err.println("SelectMgr: requestGodInfo not yet implemented") }

    fun validateSelection() { System.err.println("SelectMgr: recheck all objects against current canSelectObject criteria not yet implemented") }

    fun selectGetAllRootsValid(): Boolean = selectedObjects.rootValidNodes.size == selectedObjects.getRootObjectCount()
    fun selectGetAllValid(): Boolean = selectedObjects.validNodes.size == selectedObjects.getObjectCount()
    fun selectGetAllValidAndObjectsFound(): Boolean = selectGetAllValid()

    fun selectGetRootsModify(): Boolean = selectedObjects.rootNodes.all { it.obj.permModify() }
    fun selectGetModify(): Boolean = selectedObjects.allNodes.all { it.obj.permModify() }
    fun selectGetSameRegion(): Boolean {
        val first = selectedObjects.allNodes.firstOrNull()?.obj?.region ?: return true
        return selectedObjects.allNodes.all { it.obj.region === first }
    }

    fun selectGetRootsNonPermanentEnforced(): Boolean = selectedObjects.rootNodes.all { !it.obj.isPermanentEnforced() }
    fun selectGetNonPermanentEnforced(): Boolean = selectedObjects.allNodes.all { !it.obj.isPermanentEnforced() }
    fun selectGetRootsPermanent(): Boolean = selectedObjects.rootNodes.all { it.obj.flagObjectPermanent() }
    fun selectGetPermanent(): Boolean = selectedObjects.allNodes.all { it.obj.flagObjectPermanent() }
    fun selectGetRootsCharacter(): Boolean = selectedObjects.rootNodes.all { it.obj.flagCharacter() }
    fun selectGetCharacter(): Boolean = selectedObjects.allNodes.all { it.obj.flagCharacter() }
    fun selectGetRootsNonPathfinding(): Boolean = !selectGetRootsPermanent() && !selectGetRootsCharacter()
    fun selectGetNonPathfinding(): Boolean = !selectGetPermanent() && !selectGetCharacter()
    fun selectGetRootsNonPermanent(): Boolean = selectedObjects.rootNodes.all { !it.obj.flagObjectPermanent() }
    fun selectGetNonPermanent(): Boolean = selectedObjects.allNodes.all { !it.obj.flagObjectPermanent() }
    fun selectGetRootsNonCharacter(): Boolean = selectedObjects.rootNodes.all { !it.obj.flagCharacter() }
    fun selectGetNonCharacter(): Boolean = selectedObjects.allNodes.all { !it.obj.flagCharacter() }
    fun selectGetEditableLinksets(): Boolean {
        System.err.println("SelectMgr: check linkset editability not yet implemented")
        return false
    }
    fun selectGetViewableCharacters(): Boolean {
        System.err.println("SelectMgr: check character viewability not yet implemented")
        return false
    }
    fun selectGetRootsTransfer(): Boolean = selectedObjects.rootNodes.all { it.obj.permTransfer() }
    fun selectGetRootsCopy(): Boolean = selectedObjects.rootNodes.all { it.obj.permCopy() }

    fun selectGetCreator(id: () -> Unit, name: () -> Unit): Boolean {
        System.err.println("SelectMgr: aggregate creator ids not yet implemented")
        return false
    }
    fun selectGetOwner(id: () -> Unit, name: () -> Unit): Boolean {
        System.err.println("SelectMgr: aggregate owner ids not yet implemented")
        return false
    }
    fun selectGetLastOwner(id: () -> Unit, name: () -> Unit): Boolean {
        System.err.println("SelectMgr: aggregate last-owner ids not yet implemented")
        return false
    }
    fun selectGetGroup(id: () -> Unit): Boolean {
        System.err.println("SelectMgr: aggregate group ids not yet implemented")
        return false
    }
    fun selectGetPerm(whichPerm: UByte, maskOn: () -> Unit, maskOff: () -> Unit): Boolean {
        System.err.println("SelectMgr: aggregate perm masks not yet implemented")
        return false
    }
    fun selectIsGroupOwned(): Boolean {
        System.err.println("SelectMgr: check all roots are group owned not yet implemented")
        return false
    }
    fun selectGetPermissions(perm: Any): Boolean {
        System.err.println("SelectMgr: aggregate permissions across selection not yet implemented")
        return false
    }
    fun selectGetEditMoveLinksetPermissions(move: () -> Unit, modify: () -> Unit): Boolean {
        System.err.println("SelectMgr: compute move+modify perms not yet implemented")
        return false
    }
    fun selectGetAggregateSaleInfo(numForSale: () -> Unit, isForSaleMixed: () -> Unit,
                                   isSalePriceMixed: () -> Unit, totalSalePrice: () -> Unit,
                                   individualSalePrice: () -> Unit) {
        System.err.println("SelectMgr: aggregate sale info not yet implemented")
    }
    fun selectGetCategory(category: Any): Boolean {
        System.err.println("SelectMgr: aggregate category not yet implemented")
        return false
    }
    fun selectGetSaleInfo(saleInfo: Any): Boolean {
        System.err.println("SelectMgr: aggregate sale info not yet implemented")
        return false
    }
    fun selectGetAggregatePermissions(agPerm: Any): Boolean {
        System.err.println("SelectMgr: aggregate permissions not yet implemented")
        return false
    }
    fun selectGetAggregateTexturePermissions(agPerm: Any): Boolean {
        System.err.println("SelectMgr: aggregate texture permissions not yet implemented")
        return false
    }
    fun findObjectPermissions(obj: ViewerObject): Any? {
        System.err.println("SelectMgr: find permissions for a specific object node not yet implemented")
        return null
    }

    fun isMovableAvatarSelected(): Boolean = selectedObjects.allNodes.any { it.obj.isAvatar() }

    fun pauseAssociatedAvatars() { System.err.println("SelectMgr: pause animations on avatars associated with selection not yet implemented") }

    fun resetAgentHUDZoom() { System.err.println("SelectMgr: reset HUD zoom to default not yet implemented") }
    fun setAgentHUDZoom(targetZoom: Float, currentZoom: Float) { System.err.println("SelectMgr: set HUD zoom not yet implemented") }
    fun getAgentHUDZoom(targetZoom: () -> Unit, currentZoom: () -> Unit) { System.err.println("SelectMgr: get HUD zoom not yet implemented") }

    fun clearWaterExclusion() { System.err.println("SelectMgr: clear water exclusion volumes not yet implemented") }

    fun remove(objects: List<ViewerObject>) { objects.forEach { deselectObjectOnly(it) } }
    fun remove(obj: ViewerObject, te: Int = SELECT_ALL_TES, undoable: Boolean = true) {
        if (undoable) pushUndo()
        val node = selectedObjects.findNode(obj) ?: return
        if (te == SELECT_ALL_TES) {
            selectedObjects.removeNode(node)
            obj.setSelected(false)
        } else {
            node.selectTE(te, false)
        }
        notifyChange()
    }
    fun removeAll() = deselectAll()

    fun addAsIndividual(obj: ViewerObject, te: Int = SELECT_ALL_TES,
                        undoable: Boolean = true, gltfNode: Int = -1, gltfPrimitive: Int = -1) {
        if (undoable) pushUndo()
        val node = SelectNode(obj, true)
        node.individualSelection = true
        if (te == SELECT_ALL_TES) node.selectAllTEs(true)
        else node.selectTE(te, true)
        if (gltfNode >= 0) node.selectGLTFNode(gltfNode, gltfPrimitive, true)
        selectedObjects.addNode(node)
        obj.setSelected(true)
        notifyChange()
    }

    fun promoteSelectionToRoot() {
        val roots = selectedObjects.allNodes.map { it.obj.getRootEdit() }.toSet()
        selectedObjects.deleteAllNodes()
        roots.forEach { root ->
            val n = SelectNode(root, true); n.selectAllTEs(true)
            selectedObjects.addNodeAtEnd(n)
        }
        notifyChange()
    }

    fun demoteSelectionToIndividuals() {
        val individuals = selectedObjects.allNodes.toList()
        individuals.forEach { it.individualSelection = true }
        notifyChange()
    }

    fun selectGetNoIndividual(): Boolean = selectedObjects.allNodes.none { it.individualSelection }

    fun showGLTFMaterial() { System.err.println("SelectMgr: switch face panel to GLTF/PBR mode not yet implemented") }
    fun hideGLTFMaterial() { System.err.println("SelectMgr: switch face panel to blinn-phong mode not yet implemented") }

    fun updateSilhouettes() {
        // GPU: regenerate silhouettes for changed objects
    }

    fun renderSilhouettes(forHud: Boolean) {
        // GPU: draw silhouette pass
    }

    private fun convertTransient() {
        selectedObjects.allNodes.filter { it.transient }.forEach { it.transient = false }
    }

    fun cleanup() {
        clearSelections()
        gridObjects.deleteAllNodes()
    }

    fun dump() {
        println("SelectMgr: ${selectedObjects.getObjectCount()} selected, ${highlightedObjects.getObjectCount()} highlighted")
        selectedObjects.allNodes.forEach { n ->
            println("  obj=${n.obj.id} teMask=${n.getTESelectMask()}")
        }
    }
}

fun dialogRefreshAll() { System.err.println("dialogRefreshAll: notify UI that selection has changed not yet implemented") }
