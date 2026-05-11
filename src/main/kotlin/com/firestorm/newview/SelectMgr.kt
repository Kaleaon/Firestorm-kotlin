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

    fun saveColors() { TODO("snapshot current TE colors") }
    fun saveShinyColors() { TODO("snapshot current TE shiny colors") }
    fun saveTextures(textures: List<LLUUID>) { savedTextures.clear(); savedTextures.addAll(textures) }
    fun saveGLTFMaterials(materials: List<LLUUID>, overrides: List<Any>) {
        savedGLTFMaterialIds.clear(); savedGLTFMaterialIds.addAll(materials)
    }
    fun saveTextureScaleRatios(texIndex: Int) { TODO("compute and store texture scale ratios") }

    fun allowOperationOnNode(op: UInt, groupProxyPower: ULong): Boolean {
        TODO("check permissions against op and group proxy power")
    }

    fun renderOneSilhouette(color: FloatArray) { TODO("GPU: render silhouette for this node") }
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

    fun getSelectedObjectRenderCost(): Int { TODO("sum GPU render cost across nodes") }
    fun getSelectedObjectStreamingCost(): Float { TODO("sum streaming costs across nodes") }
    fun getSelectedObjectTriangleCount(): UInt { TODO("sum triangle counts across nodes") }

    fun checkAnimatedObjectEstTris(): Boolean { TODO("check triangle limit for animated object selection") }
    fun checkAnimatedObjectLinkable(): Boolean { TODO("check linkability constraints for animated objects") }

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

    fun updateEffects() { TODO("update HUD effects for this selection") }

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
        if (sendToSim) TODO("APR: use JVM equivalent - send deselect to simulator")
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
        if (sendToSim) TODO("APR: use JVM equivalent - send deselect to simulator")
    }

    fun deselectAll() {
        if (selectedObjects.isEmpty()) return
        pushUndo()
        selectedObjects.allNodes.forEach { it.obj.setSelected(false) }
        selectedObjects.deleteAllNodes()
        notifyChange()
        TODO("APR: use JVM equivalent - send deselect-all to simulator")
    }

    fun deselectAllForStandingUp() {
        selectedObjects.allNodes.forEach { it.obj.setSelected(false) }
        selectedObjects.deleteAllNodes()
        notifyChange()
    }

    fun deselectUnused() {
        if (selectedObjects.getNumNodes() <= 1) deselectAll()
    }

    fun deselectAllIfTooFar() { TODO("check distance and deselect if over threshold") }

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

    fun linkObjects(): Boolean { TODO("APR: use JVM equivalent - send link request") }
    fun unlinkObjects(): Boolean { TODO("APR: use JVM equivalent - send unlink request") }
    fun enableLinkObjects(): Boolean = selectedObjects.getRootObjectCount() > 1
    fun enableUnlinkObjects(): Boolean = selectedObjects.getObjectCount() > 1

    fun update() {
        selectedObjects.cleanupNodes()
        updateSelectionCenter()
        updateSilhouettes()
        updateEffects()
    }

    fun updateEffects() { TODO("update HUD beam and selection effects") }

    fun resetObjectOverrides() { TODO("clear local position/rotation/scale overrides") }
    fun resetObjectOverrides(selectionHandle: ObjectSelection) { TODO("clear overrides for handle") }
    fun overrideObjectUpdates() { TODO("install local-override update interceptors") }
    fun resetAvatarOverrides() { avatarOverridesMap.clear() }
    fun overrideAvatarUpdates() { TODO("install avatar override interceptors") }

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
        TODO("compute grid basis from gridMode and objects")
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

    fun updatePointAt() { TODO("APR: use JVM equivalent - send PointAt effect") }

    fun getBBoxOfSelection(): Any { TODO("compute bounding box of all selected objects") }

    fun getSavedBBoxOfSelection(): Any { TODO("return saved bounding box") }

    fun saveSelectedObjectTransform(actionType: ActionType) { TODO("snapshot positions/rotations/scales") }
    fun saveSelectedObjectColors() { selectedObjects.allNodes.forEach { it.saveColors() } }
    fun saveSelectedShinyColors() { selectedObjects.allNodes.forEach { it.saveShinyColors() } }
    fun saveSelectedObjectTextures() { TODO("snapshot current texture ids for each TE") }

    fun selectionUpdatePhysics(usePhysics: Boolean) { TODO("APR: use JVM equivalent - update physics flags") }
    fun selectionUpdateTemporary(isTemporary: Boolean) { TODO("APR: use JVM equivalent") }
    fun selectionUpdatePhantom(isGhost: Boolean) { TODO("APR: use JVM equivalent") }
    fun selectionDump() { selectedObjects.allNodes.forEach { it.obj.dump() } }

    fun selectionAllPCode(code: UByte): Boolean = selectedObjects.allNodes.all { it.obj.primCode == code }

    fun selectionGetClickAction(outAction: () -> Unit): Boolean { TODO("aggregate click actions across selection") }
    fun selectionGetIncludeInSearch(out: () -> Unit): Boolean { TODO("check if all selected have same include-in-search value") }
    fun selectionGetGlow(glow: () -> Unit): Boolean { TODO("aggregate glow values") }

    fun selectionSetPhysicsType(type: UByte) { selectedObjects.allNodes.forEach { it.obj.setPhysicsShapeType(type) } }
    fun selectionSetGravity(gravity: Float) { selectedObjects.allNodes.forEach { it.obj.setPhysicsGravity(gravity) } }
    fun selectionSetFriction(friction: Float) { selectedObjects.allNodes.forEach { it.obj.setPhysicsFriction(friction) } }
    fun selectionSetDensity(density: Float) { selectedObjects.allNodes.forEach { it.obj.setPhysicsDensity(density) } }
    fun selectionSetRestitution(restitution: Float) { selectedObjects.allNodes.forEach { it.obj.setPhysicsRestitution(restitution) } }
    fun selectionSetMaterial(material: UByte) { selectedObjects.allNodes.forEach { it.obj.material = material } }
    fun selectionSetImage(imageId: LLUUID, isPBR: Boolean = true): Boolean { TODO("APR: use JVM equivalent - send TE texture update") }
    fun selectionSetGLTFMaterial(matId: LLUUID): Boolean { TODO("APR: use JVM equivalent - set GLTF material by id") }
    fun selectionSetColor(color: FloatArray) { TODO("APR: use JVM equivalent - send TE color update") }
    fun selectionSetColorOnly(color: FloatArray) { TODO("APR: use JVM equivalent - send RGB-only color update") }
    fun selectionSetAlphaOnly(alpha: Float) { TODO("APR: use JVM equivalent - send alpha-only update") }
    fun selectionRevertColors() { TODO("restore colors from node.savedColors") }
    fun selectionRevertShinyColors() { TODO("restore shiny colors") }
    fun selectionRevertTextures(): Boolean { TODO("restore textures from node.savedTextures") }
    fun selectionRevertGLTFMaterials() { TODO("restore GLTF materials from node.savedGLTFMaterialIds") }
    fun selectionSetBumpmap(bumpmap: UByte, imageId: LLUUID) { TODO("APR: use JVM equivalent - send bump map update") }
    fun selectionSetTexGen(texGen: UByte) { TODO("APR: use JVM equivalent - send texgen update") }
    fun selectionSetShiny(shiny: UByte, imageId: LLUUID) { TODO("APR: use JVM equivalent - send shiny update") }
    fun selectionSetFullbright(fullbright: UByte) { TODO("APR: use JVM equivalent - send fullbright update") }
    fun selectionSetMedia(mediaType: UByte, mediaData: Map<String, Any>) { TODO("APR: use JVM equivalent - send media update") }
    fun selectionSetClickAction(action: UByte) { selectedObjects.allNodes.forEach { it.obj.setClickAction(action) } }
    fun selectionSetIncludeInSearch(include: Boolean) { selectedObjects.allNodes.forEach { it.obj.setIncludeInSearch(include) } }
    fun selectionSetGlow(glow: Float) { TODO("APR: use JVM equivalent - send glow update") }
    fun selectionSetMaterialParams(func: Any, specificTE: Int = -1) { TODO("apply material params functor") }
    fun selectionRemoveMaterial() { TODO("APR: use JVM equivalent - remove material from TEs") }

    fun selectionSetObjectPermissions(permField: UByte, set: Boolean, permMask: UInt, override: Boolean = false) {
        TODO("APR: use JVM equivalent - send ObjectPermissions message")
    }
    fun selectionSetObjectName(name: String) { TODO("APR: use JVM equivalent - send ObjectName message") }
    fun selectionSetObjectDescription(desc: String) { TODO("APR: use JVM equivalent - send ObjectDescription message") }
    fun selectionSetObjectSaleInfo(saleInfo: Any) { TODO("APR: use JVM equivalent - send ObjectSaleInfo message") }

    fun selectionTexScaleAutofit(repeatsPerMeter: Float) { TODO("APR: use JVM equivalent - auto-fit texture scaling") }
    fun adjustTexturesByScale(sendToSim: Boolean, stretch: Boolean) { TODO("adjust textures proportional to scale change") }

    fun selectionMove(displacement: Vector3, rx: Float, ry: Float, rz: Float, updateType: UInt): Boolean {
        TODO("APR: use JVM equivalent - apply transform to selection")
    }
    fun sendSelectionMove() { TODO("APR: use JVM equivalent - send MultipleObjectUpdate position") }

    fun sendMultipleUpdate(type: UInt) { TODO("APR: use JVM equivalent - send MultipleObjectUpdate") }
    fun sendOwner(ownerId: LLUUID, groupId: LLUUID, override: Boolean = false) { TODO("APR: use JVM equivalent") }
    fun sendGroup(groupId: LLUUID) { TODO("APR: use JVM equivalent") }
    fun sendBuy(buyerId: LLUUID, categoryId: LLUUID, saleInfo: Any) { TODO("APR: use JVM equivalent") }
    fun sendAttach(attachmentPoint: UByte, replace: Boolean) { TODO("APR: use JVM equivalent") }
    fun sendAttach(selectionHandle: ObjectSelection, attachmentPoint: UByte, replace: Boolean) { TODO("APR: use JVM equivalent") }
    fun sendDetach() { TODO("APR: use JVM equivalent") }
    fun sendDropAttachment() { TODO("APR: use JVM equivalent") }
    fun sendLink() { TODO("APR: use JVM equivalent") }
    fun sendDelink() { TODO("APR: use JVM equivalent") }
    fun sendSelect() { TODO("APR: use JVM equivalent - send ObjectSelect message") }
    fun sendGodlikeRequest(request: String, parameter: String) { TODO("APR: use JVM equivalent") }

    fun selectDelete() { TODO("APR: use JVM equivalent - send DeRezObject to trash") }
    fun selectForceDelete() { TODO("APR: use JVM equivalent - force delete without trash") }
    fun selectDuplicate(offset: Vector3, selectCopy: Boolean) { TODO("APR: use JVM equivalent - send ObjectDuplicate") }
    fun repeatDuplicate() { TODO("APR: use JVM equivalent") }
    fun selectDuplicateOnRay(rayStart: Vector3, rayEnd: Vector3, bypassRaycast: Boolean,
                              rayEndIsIntersection: Boolean, rayTargetId: LLUUID,
                              copyCenters: Boolean, copyRotates: Boolean, selectCopy: Boolean) {
        TODO("APR: use JVM equivalent - send ObjectDuplicateOnRay")
    }

    fun requestObjectPropertiesFamily(obj: ViewerObject) { TODO("APR: use JVM equivalent - send RequestObjectPropertiesFamily") }

    fun requestGodInfo() { TODO("APR: use JVM equivalent") }

    fun validateSelection() { TODO("recheck all objects against current canSelectObject criteria") }

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
    fun selectGetEditableLinksets(): Boolean { TODO("check linkset editability") }
    fun selectGetViewableCharacters(): Boolean { TODO("check character viewability") }
    fun selectGetRootsTransfer(): Boolean = selectedObjects.rootNodes.all { it.obj.permTransfer() }
    fun selectGetRootsCopy(): Boolean = selectedObjects.rootNodes.all { it.obj.permCopy() }

    fun selectGetCreator(id: () -> Unit, name: () -> Unit): Boolean { TODO("aggregate creator ids") }
    fun selectGetOwner(id: () -> Unit, name: () -> Unit): Boolean { TODO("aggregate owner ids") }
    fun selectGetLastOwner(id: () -> Unit, name: () -> Unit): Boolean { TODO("aggregate last-owner ids") }
    fun selectGetGroup(id: () -> Unit): Boolean { TODO("aggregate group ids") }
    fun selectGetPerm(whichPerm: UByte, maskOn: () -> Unit, maskOff: () -> Unit): Boolean { TODO("aggregate perm masks") }
    fun selectIsGroupOwned(): Boolean { TODO("check all roots are group owned") }
    fun selectGetPermissions(perm: Any): Boolean { TODO("aggregate permissions across selection") }
    fun selectGetEditMoveLinksetPermissions(move: () -> Unit, modify: () -> Unit): Boolean { TODO("compute move+modify perms") }
    fun selectGetAggregateSaleInfo(numForSale: () -> Unit, isForSaleMixed: () -> Unit,
                                   isSalePriceMixed: () -> Unit, totalSalePrice: () -> Unit,
                                   individualSalePrice: () -> Unit) { TODO("aggregate sale info") }
    fun selectGetCategory(category: Any): Boolean { TODO("aggregate category") }
    fun selectGetSaleInfo(saleInfo: Any): Boolean { TODO("aggregate sale info") }
    fun selectGetAggregatePermissions(agPerm: Any): Boolean { TODO("aggregate permissions") }
    fun selectGetAggregateTexturePermissions(agPerm: Any): Boolean { TODO("aggregate texture permissions") }
    fun findObjectPermissions(obj: ViewerObject): Any? { TODO("find permissions for a specific object node") }

    fun isMovableAvatarSelected(): Boolean = selectedObjects.allNodes.any { it.obj.isAvatar() }

    fun pauseAssociatedAvatars() { TODO("pause animations on avatars associated with selection") }

    fun resetAgentHUDZoom() { TODO("reset HUD zoom to default") }
    fun setAgentHUDZoom(targetZoom: Float, currentZoom: Float) { TODO("set HUD zoom") }
    fun getAgentHUDZoom(targetZoom: () -> Unit, currentZoom: () -> Unit) { TODO("get HUD zoom") }

    fun clearWaterExclusion() { TODO("APR: use JVM equivalent - clear water exclusion volumes") }

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

    fun showGLTFMaterial() { TODO("switch face panel to GLTF/PBR mode") }
    fun hideGLTFMaterial() { TODO("switch face panel to blinn-phong mode") }

    fun updateSilhouettes() { TODO("GPU: regenerate silhouettes for changed objects") }

    fun renderSilhouettes(forHud: Boolean) { TODO("GPU: draw silhouette pass") }

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

fun dialogRefreshAll() { TODO("notify UI that selection has changed") }
