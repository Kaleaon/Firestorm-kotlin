package com.firestorm.newview

import com.firestorm.tool.Tool
import com.firestorm.tool.ToolMgr
import com.firestorm.ui.PickInfo
import com.firestorm.ui.ViewerWindow
import com.firestorm.inventory.ViewerInventoryItem
import com.firestorm.inventory.ViewerInventoryCategory
import com.firestorm.inventory.InventoryItem
import com.firestorm.inventory.InventoryModel
import com.firestorm.agent.Agent
import com.firestorm.object.ViewerObject

typealias Uuid = java.util.UUID

object ToolDragAndDrop : Tool("draganddrop") {

    enum class ESource {
        SOURCE_AGENT,
        SOURCE_WORLD,
        SOURCE_NOTECARD,
        SOURCE_LIBRARY,
        SOURCE_VIEWER,
        SOURCE_PEOPLE
    }

    enum class EDropTarget(val value: Int) {
        DT_NONE(0),
        DT_SELF(1),
        DT_AVATAR(2),
        DT_OBJECT(3),
        DT_LAND(4),
        DT_COUNT(5)
    }

    enum class EDragAndDropType {
        DAD_NONE,
        DAD_TEXTURE,
        DAD_MATERIAL,
        DAD_SOUND,
        DAD_CALLINGCARD,
        DAD_LANDMARK,
        DAD_SCRIPT,
        DAD_CLOTHING,
        DAD_OBJECT,
        DAD_NOTECARD,
        DAD_CATEGORY,
        DAD_ROOT_CATEGORY,
        DAD_BODYPART,
        DAD_ANIMATION,
        DAD_GESTURE,
        DAD_LINK,
        DAD_MESH,
        DAD_SETTINGS
    }

    enum class EAcceptance {
        ACCEPT_NO,
        ACCEPT_NO_LOCKED,
        ACCEPT_NO_CUSTOM,
        ACCEPT_POSTPONED,
        ACCEPT_YES_COPY_SINGLE,
        ACCEPT_YES_COPY_MULTI,
        ACCEPT_YES_SINGLE,
        ACCEPT_YES_MULTI
    }

    enum class ECursorType {
        UI_CURSOR_NO,
        UI_CURSOR_NOLOCKED,
        UI_CURSOR_ARROWDRAG,
        UI_CURSOR_ARROWDRAGMULTI,
        UI_CURSOR_ARROWCOPY,
        UI_CURSOR_ARROWCOPYMULTI
    }

    private typealias DragOrDrop3dImpl = (ViewerObject?, Int, MASK, Boolean) -> EAcceptance

    var cargoCount: UInt = 0u
    private var dragStartX: Int = 0
    private var dragStartY: Int = 0
    private val cargoTypes: MutableList<EDragAndDropType> = mutableListOf()
    private val cargoIDs: MutableList<Uuid> = mutableListOf()
    private var source: ESource = ESource.SOURCE_AGENT
    private var sourceID: Uuid = Uuid(0, 0)
    private var objectID: Uuid = Uuid(0, 0)
    private var cursor: ECursorType = ECursorType.UI_CURSOR_NO
    private var lastAccept: EAcceptance = EAcceptance.ACCEPT_NO
    private var drop: Boolean = false
    private var curItemIndex: Int = 0
    private var toolTipMsg: String = ""
    private var customMsg: String = ""
    private val endDragListeners: MutableList<() -> Unit> = mutableListOf()

    private val DRAG_N_DROP_DISTANCE_THRESHOLD = 8

    companion object {
        var operationId: Int = 0
            private set
    }

    private val dragDropDictionary: MutableMap<EDragAndDropType, Array<DragOrDrop3dImpl>> = buildDictionary()

    private fun buildDictionary(): MutableMap<EDragAndDropType, Array<DragOrDrop3dImpl>> {
        val n = ::dad3dNull
        return mutableMapOf(
            EDragAndDropType.DAD_NONE         to arrayOf(n, n, n, n, n),
            EDragAndDropType.DAD_TEXTURE      to arrayOf(n, n, ::dad3dGiveInventory,         ::dad3dTextureObject,           n),
            EDragAndDropType.DAD_MATERIAL     to arrayOf(n, n, ::dad3dGiveInventory,         ::dad3dMaterialObject,          n),
            EDragAndDropType.DAD_SOUND        to arrayOf(n, n, ::dad3dGiveInventory,         ::dad3dUpdateInventory,         n),
            EDragAndDropType.DAD_CALLINGCARD  to arrayOf(n, n, ::dad3dGiveInventory,         ::dad3dUpdateInventory,         n),
            EDragAndDropType.DAD_LANDMARK     to arrayOf(n, n, ::dad3dGiveInventory,         ::dad3dUpdateInventory,         n),
            EDragAndDropType.DAD_SCRIPT       to arrayOf(n, n, ::dad3dGiveInventory,         ::dad3dRezScript,               n),
            EDragAndDropType.DAD_CLOTHING     to arrayOf(n, ::dad3dWearItem,                 ::dad3dGiveInventory,           ::dad3dUpdateInventory,         n),
            EDragAndDropType.DAD_OBJECT       to arrayOf(n, ::dad3dRezAttachmentFromInv,     ::dad3dGiveInventoryObject,     ::dad3dRezObjectOnObject,       ::dad3dRezObjectOnLand),
            EDragAndDropType.DAD_NOTECARD     to arrayOf(n, n, ::dad3dGiveInventory,         ::dad3dUpdateInventory,         n),
            EDragAndDropType.DAD_CATEGORY     to arrayOf(n, ::dad3dWearCategory,             ::dad3dGiveInventoryCategory,   ::dad3dRezCategoryOnObject,     n),
            EDragAndDropType.DAD_ROOT_CATEGORY to arrayOf(n, n, n, n, n),
            EDragAndDropType.DAD_BODYPART     to arrayOf(n, ::dad3dWearItem,                 ::dad3dGiveInventory,           ::dad3dUpdateInventory,         n),
            EDragAndDropType.DAD_ANIMATION    to arrayOf(n, n, ::dad3dGiveInventory,         ::dad3dUpdateInventory,         n),
            EDragAndDropType.DAD_GESTURE      to arrayOf(n, ::dad3dActivateGesture,          ::dad3dGiveInventory,           ::dad3dUpdateInventory,         n),
            EDragAndDropType.DAD_LINK         to arrayOf(n, n, n, n, n),
            EDragAndDropType.DAD_MESH         to arrayOf(n, n, ::dad3dGiveInventory,         ::dad3dMeshObject,              n),
            EDragAndDropType.DAD_SETTINGS     to arrayOf(n, n, ::dad3dGiveInventory,         ::dad3dUpdateInventory,         n)
        )
    }

    fun setEndDragCallback(cb: () -> Unit) = endDragListeners.add(cb)

    fun getSource(): ESource = source
    fun getSourceID(): Uuid = sourceID
    fun getObjectID(): Uuid = objectID
    fun getLastAccept(): EAcceptance = lastAccept
    fun getCargoIndex(): Int = curItemIndex
    fun getCargoCount(): UInt = if (cargoCount > 0u) cargoCount else cargoIDs.size.toUInt()
    fun resetCargoCount() { cargoCount = 0u }

    fun setDragStart(x: Int, y: Int) {
        dragStartX = x
        dragStartY = y
    }

    fun isOverThreshold(x: Int, y: Int): Boolean {
        val dx = x - dragStartX
        val dy = y - dragStartY
        return dx * dx + dy * dy > DRAG_N_DROP_DISTANCE_THRESHOLD * DRAG_N_DROP_DISTANCE_THRESHOLD
    }

    fun beginDrag(
        type: EDragAndDropType,
        cargoId: Uuid,
        source: ESource,
        sourceId: Uuid = Uuid(0, 0),
        objectId: Uuid = Uuid(0, 0)
    ) {
        if (type == EDragAndDropType.DAD_NONE) return
        cargoTypes.clear()
        cargoTypes.add(type)
        cargoIDs.clear()
        cargoIDs.add(cargoId)
        this.source = source
        this.sourceID = sourceId
        this.objectID = objectId
        setMouseCapture(true)
        ToolMgr.instance.setTransientTool(this)
        cursor = ECursorType.UI_CURSOR_NO
        if (type == EDragAndDropType.DAD_CATEGORY &&
            (source == ESource.SOURCE_AGENT || source == ESource.SOURCE_LIBRARY)) {
            System.err.println("APR: use JVM equivalent - prefetch category descendants")
        }
    }

    fun beginMultiDrag(
        types: List<EDragAndDropType>,
        cargoIds: List<Uuid>,
        source: ESource,
        sourceId: Uuid = Uuid(0, 0)
    ) {
        if (types.any { it == EDragAndDropType.DAD_NONE }) return
        cargoTypes.clear()
        cargoTypes.addAll(types)
        cargoIDs.clear()
        cargoIDs.addAll(cargoIds)
        this.source = source
        this.sourceID = sourceId
        setMouseCapture(true)
        ToolMgr.instance.setTransientTool(this)
        cursor = ECursorType.UI_CURSOR_NO
        if (source == ESource.SOURCE_AGENT || source == ESource.SOURCE_LIBRARY) {
            System.err.println("APR: use JVM equivalent - prefetch category descendants for multi-drag")
        }
    }

    fun endDrag() {
        endDragListeners.forEach { it() }
        SelectMgr.instance.unhighlightAll()
        setMouseCapture(false)
    }

    override fun onMouseCaptureLost() {
        ToolMgr.instance.clearTransientTool()
        cargoTypes.clear()
        cargoIDs.clear()
        source = ESource.SOURCE_AGENT
        sourceID = Uuid(0, 0)
        objectID = Uuid(0, 0)
        customMsg = ""
    }

    override fun handleMouseUp(x: Int, y: Int, mask: MASK): Boolean {
        if (hasMouseCapture()) {
            var acceptance = EAcceptance.ACCEPT_NO
            dragOrDrop(x, y, mask, true) { acceptance = it }
            endDrag()
        }
        return true
    }

    override fun handleHover(x: Int, y: Int, mask: MASK): Boolean {
        var acceptance = EAcceptance.ACCEPT_NO
        dragOrDrop(x, y, mask, false) { acceptance = it }
        val cur = acceptanceToCursor(acceptance)
        ViewerWindow.instance.getWindow().setCursor(cur)
        return true
    }

    override fun handleKey(key: KEY, mask: MASK): Boolean {
        if (key == KEY_ESCAPE) {
            endDrag()
            return true
        }
        return false
    }

    override fun handleToolTip(x: Int, y: Int, mask: MASK): Boolean {
        if (toolTipMsg.isNotEmpty()) {
            System.err.println("APR: use JVM equivalent - show tooltip: $toolTipMsg")
        }
        return true
    }

    override fun handleDeselect() {
        endDrag()
    }

    private fun acceptanceToCursor(acceptance: EAcceptance): ECursorType {
        cursor = when (acceptance) {
            EAcceptance.ACCEPT_YES_MULTI -> {
                if (cargoIDs.size > 1) ECursorType.UI_CURSOR_ARROWDRAGMULTI
                else ECursorType.UI_CURSOR_ARROWDRAG
            }
            EAcceptance.ACCEPT_YES_SINGLE -> {
                if (cargoIDs.size > 1) {
                    toolTipMsg = Trans.getString("TooltipMustSingleDrop")
                    ECursorType.UI_CURSOR_NO
                } else ECursorType.UI_CURSOR_ARROWDRAG
            }
            EAcceptance.ACCEPT_NO_LOCKED  -> ECursorType.UI_CURSOR_NOLOCKED
            EAcceptance.ACCEPT_NO_CUSTOM  -> { toolTipMsg = customMsg; ECursorType.UI_CURSOR_NO }
            EAcceptance.ACCEPT_NO         -> ECursorType.UI_CURSOR_NO
            EAcceptance.ACCEPT_YES_COPY_MULTI -> {
                if (cargoIDs.size > 1) ECursorType.UI_CURSOR_ARROWCOPYMULTI
                else ECursorType.UI_CURSOR_ARROWCOPY
            }
            EAcceptance.ACCEPT_YES_COPY_SINGLE -> {
                if (cargoIDs.size > 1) {
                    toolTipMsg = Trans.getString("TooltipMustSingleDrop")
                    ECursorType.UI_CURSOR_NO
                } else ECursorType.UI_CURSOR_ARROWCOPY
            }
            EAcceptance.ACCEPT_POSTPONED  -> cursor
        }
        return cursor
    }

    private fun dragOrDrop(x: Int, y: Int, mask: MASK, drop: Boolean, acceptanceOut: (EAcceptance) -> Unit) {
        TODO("APR: use JVM equivalent - perform 2D/3D drag-or-drop routing")
    }

    private fun dragOrDrop3D(x: Int, y: Int, mask: MASK, drop: Boolean, acceptanceOut: (EAcceptance) -> Unit) {
        TODO("APR: use JVM equivalent - perform 3D scene drop test")
    }

    private fun pick(pickInfo: PickInfo) {
        TODO("APR: use JVM equivalent - route pick result to appropriate 3D drop handler")
    }

    private fun locateInventory(
        itemOut: (ViewerInventoryItem?) -> Unit,
        catOut: (ViewerInventoryCategory?) -> Unit
    ) {
        TODO("APR: use JVM equivalent - locate inventory item or category by cargoID")
    }

    fun dadUpdateInventory(obj: ViewerObject?, drop: Boolean): Boolean {
        TODO("APR: use JVM equivalent - update object inventory with current cargo")
    }

    fun dadUpdateInventoryCategory(obj: ViewerObject?, drop: Boolean): Boolean {
        TODO("APR: use JVM equivalent - update object inventory with current cargo category")
    }

    private fun dad3dNull(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance =
        EAcceptance.ACCEPT_NO

    private fun dad3dRezObjectOnLand(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - rez object on land")
    }

    private fun dad3dRezObjectOnObject(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - rez object on object")
    }

    private fun dad3dRezCategoryOnObject(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - rez category on object")
    }

    private fun dad3dRezScript(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - rez script into object")
    }

    private fun dad3dTextureObject(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - apply texture to object face")
    }

    private fun dad3dMaterialObject(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - apply PBR material to object face")
    }

    private fun dad3dMeshObject(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - apply mesh to object")
    }

    private fun dad3dWearItem(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - wear wearable item on avatar")
    }

    private fun dad3dWearCategory(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - wear category (outfit) on avatar")
    }

    private fun dad3dUpdateInventory(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - add item to object inventory")
    }

    private fun dad3dUpdateInventoryCategory(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - add category to object inventory")
    }

    private fun dad3dGiveInventoryObject(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - give inventory item to object owner")
    }

    private fun dad3dGiveInventory(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - give inventory item to avatar")
    }

    private fun dad3dGiveInventoryCategory(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - give inventory category to avatar")
    }

    private fun dad3dRezFromObjectOnLand(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - rez object from task inventory onto land")
    }

    private fun dad3dRezFromObjectOnObject(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - rez object from task inventory onto object")
    }

    private fun dad3dRezAttachmentFromInv(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - attach object from inventory to avatar")
    }

    private fun dad3dCategoryOnLand(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - rez category contents on land")
    }

    private fun dad3dAssetOnLand(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - rez asset on land")
    }

    private fun dad3dActivateGesture(obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean): EAcceptance {
        TODO("APR: use JVM equivalent - activate gesture")
    }

    private fun dad3dApplyToObject(
        obj: ViewerObject?, face: Int, mask: MASK, drop: Boolean, cargoType: EDragAndDropType
    ): EAcceptance {
        TODO("APR: use JVM equivalent - apply item (texture/mesh/material) to object face")
    }

    companion object {
        fun isInventoryDropAcceptable(obj: ViewerObject?, item: InventoryItem?): Boolean {
            TODO("APR: use JVM equivalent - check willObjectAcceptInventory")
        }

        fun willObjectAcceptInventory(
            obj: ViewerObject?,
            item: InventoryItem?,
            type: ToolDragAndDrop.EDragAndDropType = ToolDragAndDrop.EDragAndDropType.DAD_NONE
        ): EAcceptance {
            TODO("APR: use JVM equivalent - check permissions/copyability for drop")
        }

        fun handleDropMaterialProtections(
            hitObj: ViewerObject?,
            item: InventoryItem?,
            source: ESource,
            srcId: Uuid
        ): Boolean {
            TODO("APR: use JVM equivalent - validate material drop permissions")
        }

        fun dropScript(hitObj: ViewerObject?, item: InventoryItem?, active: Boolean, source: ESource, srcId: Uuid) {
            TODO("APR: use JVM equivalent - send script drop to sim")
        }

        fun dropTexture(
            hitObj: ViewerObject?, hitFace: Int, item: InventoryItem?,
            source: ESource, srcId: Uuid, allFaces: Boolean, replacePbr: Boolean, texChannel: Int = -1
        ) {
            TODO("GPU: apply texture drop to object face")
        }

        fun dropTextureOneFace(
            hitObj: ViewerObject?, hitFace: Int, item: InventoryItem?,
            source: ESource, srcId: Uuid, removePbr: Boolean, texChannel: Int = -1
        ) {
            TODO("GPU: apply texture to one face")
        }

        fun dropTextureAllFaces(
            hitObj: ViewerObject?, item: InventoryItem?,
            source: ESource, srcId: Uuid, removePbr: Boolean
        ) {
            TODO("GPU: apply texture to all faces")
        }

        fun dropMaterial(
            hitObj: ViewerObject?, hitFace: Int, item: InventoryItem?,
            source: ESource, srcId: Uuid, allFaces: Boolean
        ) {
            TODO("GPU: apply PBR material to face(s)")
        }

        fun dropMaterialOneFace(hitObj: ViewerObject?, hitFace: Int, item: InventoryItem?, source: ESource, srcId: Uuid) {
            TODO("GPU: apply PBR material to one face")
        }

        fun dropMaterialAllFaces(hitObj: ViewerObject?, item: InventoryItem?, source: ESource, srcId: Uuid) {
            TODO("GPU: apply PBR material to all faces")
        }

        fun dropMesh(hitObj: ViewerObject?, item: InventoryItem?, source: ESource, srcId: Uuid) {
            TODO("APR: use JVM equivalent - send mesh drop to sim")
        }

        fun dropInventory(hitObj: ViewerObject?, item: InventoryItem?, source: ESource, srcId: Uuid) {
            TODO("APR: use JVM equivalent - send inventory item drop to sim")
        }

        fun handleGiveDragAndDrop(
            agent: Uuid,
            session: Uuid,
            drop: Boolean,
            cargoType: EDragAndDropType,
            cargoData: Any?,
            acceptOut: (EAcceptance) -> Unit
        ): Boolean {
            TODO("APR: use JVM equivalent - process give-inventory drag-drop")
        }

        @JvmStatic fun pickCallback(pickInfo: PickInfo) {
            ToolDragAndDrop.pick(pickInfo)
        }
    }

    fun packPermissionsSlam(flags: UInt, perms: Any) {
        TODO("APR: use JVM equivalent - pack permission slam into message")
    }
}
