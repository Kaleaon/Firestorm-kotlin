package com.firestorm.newview

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

fun buildNoticeDate(timestamp: UInt): String {
    val epochSecond = if (timestamp == 0u) Instant.now().epochSecond else timestamp.toLong()
    val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochSecond(epochSecond))
}

class GroupDropTarget(
    private var groupNoticesPanel: PanelGroupNotices?,
    private var groupId: UUID
) {

    fun setPanel(panel: PanelGroupNotices) { groupNoticesPanel = panel }
    fun setGroup(group: UUID) { groupId = group }

    fun handleDragAndDrop(
        x: Int, y: Int, drop: Boolean,
        cargoType: DragAndDropType, cargoData: Any?
    ): Boolean {
        if (!Agent.hasPowerInGroup(groupId, GP_NOTICES_SEND)) return true

        val invItem = cargoData as? ViewerInventoryItem ?: run {
            return true
        }

        return when (cargoType) {
            DragAndDropType.TEXTURE,
            DragAndDropType.SOUND,
            DragAndDropType.LANDMARK,
            DragAndDropType.SCRIPT,
            DragAndDropType.OBJECT,
            DragAndDropType.NOTECARD,
            DragAndDropType.CLOTHING,
            DragAndDropType.BODYPART,
            DragAndDropType.ANIMATION,
            DragAndDropType.GESTURE,
            DragAndDropType.CALLINGCARD,
            DragAndDropType.MESH,
            DragAndDropType.SETTINGS,
            DragAndDropType.MATERIAL -> {
                val inInventory = Inventory.getItem(invItem.id) != null
                val groupGiveOk = GiveInventory.isInventoryGroupGiveAcceptable(invItem)
                if (inInventory && groupGiveOk) {
                    if (drop) groupNoticesPanel?.setItem(invItem)
                    true
                } else true
            }
            else -> true
        }
    }
}

class PanelGroupNotices : PanelGroupTab() {

    private var inventoryItem: ViewerInventoryItem? = null
    private var inventoryOffer: OfferInfo? = null

    private var createSubject: LineEditor? = null
    private var createInventoryName: LineEditor? = null
    private var createMessage: TextEditor? = null

    private var viewSubject: LineEditor? = null
    private var viewInventoryName: LineEditor? = null
    private var viewMessage: TextEditor? = null

    private var btnSendMessage: Button? = null
    private var btnNewMessage: Button? = null
    private var btnRemoveAttachment: Button? = null
    private var btnOpenAttachment: Button? = null
    private var btnGetPastNotices: Button? = null

    private var panelCreateNotice: Panel? = null
    private var panelViewNotice: Panel? = null

    private var noticesList: ScrollListCtrl? = null
    private val knownNoticeIds: MutableSet<UUID> = mutableSetOf()
    private var noNoticesStr: String = ""
    private var prevSelectedNotice: UUID = UUID(0, 0)

    companion object {
        val instances: MutableMap<UUID, PanelGroupNotices> = mutableMapOf()

        fun processGroupNoticesListReply(msg: MessageSystem) {
            val groupId = msg.getUUID("AgentData", "GroupID")
            val self = instances[groupId] ?: return
            self.processNotices(msg)
        }
    }

    fun isVisibleByAgent(): Boolean =
        allowEdit && Agent.hasPowerInGroup(groupId, GP_NOTICES_SEND or GP_NOTICES_RECEIVE)

    override fun postBuild(): Boolean {
        noticesList = findChild<ScrollListCtrl>("notice_list")
        noticesList?.let {
            it.setCommitOnSelectionChange(true)
            it.setCommitCallback { onSelectNotice() }
        }

        btnNewMessage = findChild<Button>("create_new_notice")
        btnNewMessage?.let {
            it.setClickedCallback { onClickNewMessage() }
            it.setEnabled(Agent.hasPowerInGroup(groupId, GP_NOTICES_SEND))
        }

        btnGetPastNotices = findChild<Button>("refresh_notices")
        btnGetPastNotices?.setClickedCallback { onClickRefreshNotices() }

        createSubject = findChild<LineEditor>("create_subject")
        createMessage = findChild<TextEditor>("create_message")

        createInventoryName = findChild<LineEditor>("create_inventory_name")
        createInventoryName?.let { it.setTabStop(false); it.setEnabled(false) }

        btnSendMessage = findChild<Button>("send_notice")
        btnSendMessage?.setClickedCallback { onClickSendMessage() }

        btnRemoveAttachment = findChild<Button>("remove_attachment")
        btnRemoveAttachment?.let { it.setClickedCallback { onClickRemoveAttachment() }; it.setEnabled(false) }

        viewSubject = findChild<LineEditor>("view_subject")
        viewMessage = findChild<TextEditor>("view_message")

        viewInventoryName = findChild<LineEditor>("view_inventory_name")
        viewInventoryName?.let { it.setTabStop(false); it.setEnabled(false) }

        btnOpenAttachment = findChild<Button>("open_attachment")
        btnOpenAttachment?.setClickedCallback { onClickOpenAttachment() }

        noNoticesStr = getString("no_notices_text")

        panelCreateNotice = findChild<Panel>("panel_create_new_notice")
        panelViewNotice = findChild<Panel>("panel_view_past_notice")

        val target = findChild<GroupDropTarget>("drop_target")
        target?.setPanel(this)
        target?.setGroup(groupId)

        arrangeNoticeView(NoticeView.VIEW_PAST)
        return super.postBuild()
    }

    override fun activate() {
        noticesList?.deleteAllItems()
        knownNoticeIds.clear()
        prevSelectedNotice = UUID(0, 0)

        val canSend = Agent.hasPowerInGroup(groupId, GP_NOTICES_SEND)
        val canReceive = Agent.hasPowerInGroup(groupId, GP_NOTICES_RECEIVE)

        panelViewNotice?.setEnabled(canReceive)
        panelCreateNotice?.setEnabled(canSend)

        createInventoryName?.setEnabled(false)
        viewInventoryName?.setEnabled(false)

        if (canReceive) onClickRefreshNotices()
    }

    fun setItem(invItem: ViewerInventoryItem) {
        inventoryItem = invItem

        val itemIsMulti = (invItem.flags and II_FLAGS_OBJECT_HAS_MULTIPLE_ITEMS) != 0
        val iconName = InventoryIcon.getIconName(invItem.type, invItem.inventoryType, invItem.flags, itemIsMulti)

        createInventoryName?.setText("        ${invItem.name}")
        btnRemoveAttachment?.setEnabled(true)
    }

    private fun onClickRemoveAttachment() {
        inventoryItem = null
        createInventoryName?.clear()
        btnRemoveAttachment?.setEnabled(false)
    }

    private fun onClickOpenAttachment() {
        inventoryOffer?.forceResponse(InventoryOfferResponse.ACCEPT)
        inventoryOffer = null
        btnOpenAttachment?.setEnabled(false)
    }

    private fun onClickSendMessage() {
        val subject = createSubject?.getText() ?: ""
        if (subject.isEmpty()) {
            NotificationsUtil.add("MustSpecifyGroupNoticeSubject")
            return
        }
        sendGroupNotice(
            groupId = groupId,
            subject = subject,
            message = createMessage?.getText() ?: "",
            inventoryItem = inventoryItem
        )

        val id = UUID.randomUUID()
        val senderName = AgentUi.buildFullName()
        val timestamp = 0u

        noticesList?.addRow(
            id = id,
            columns = mapOf(
                "icon" to "",
                "subject" to subject,
                "from" to senderName,
                "date" to buildNoticeDate(timestamp),
                "sort" to timestamp.toString()
            )
        )
        knownNoticeIds.add(id)

        createMessage?.clear()
        createSubject?.clear()
        onClickRemoveAttachment()
        arrangeNoticeView(NoticeView.VIEW_PAST)
    }

    private fun onClickNewMessage() {
        arrangeNoticeView(NoticeView.CREATE_NEW)
        inventoryOffer?.forceResponse(InventoryOfferResponse.DECLINE)
        inventoryOffer = null
        createSubject?.clear()
        createMessage?.clear()
        inventoryItem?.let { onClickRemoveAttachment() }
        noticesList?.deselectAllItems()
    }

    fun refreshNotices() { onClickRefreshNotices() }

    fun clearNoticeList() {
        prevSelectedNotice = noticesList?.getSelectedId() ?: UUID(0, 0)
        noticesList?.deleteAllItems()
        knownNoticeIds.clear()
    }

    private fun onClickRefreshNotices() {
        clearNoticeList()
        System.err.println("APR: send GroupNoticesListRequest message via gMessageSystem")
    }

    fun processNotices(msg: MessageSystem) {
        val count = msg.getNumberOfBlocks("Data")
        noticesList?.setEnabled(true)
        noticesList?.setNeedsSort(false)

        for (i in 0 until count) {
            val id = msg.getUUID("Data", "NoticeID", i)
            if (count == 1 && id == UUID(0, 0)) {
                noticesList?.setCommentText(noNoticesStr)
                noticesList?.setEnabled(false)
                return
            }
            if (id in knownNoticeIds) continue

            val subj = msg.getString("Data", "Subject", i)
            val name = msg.getString("Data", "FromName", i)
            val hasAttachment = msg.getBool("Data", "HasAttachment", i)
            val assetType = msg.getUByte("Data", "AssetType", i)
            val timestamp = msg.getUInt("Data", "Timestamp", i)

            val displayName = CacheName.buildUsername(name)
            val iconValue = if (hasAttachment) InventoryIcon.getIconName(assetType) else ""

            noticesList?.addRow(
                id = id,
                columns = mapOf(
                    "icon" to iconValue,
                    "subject" to subj,
                    "from" to displayName,
                    "date" to buildNoticeDate(timestamp),
                    "sort" to timestamp.toString()
                )
            )
            knownNoticeIds.add(id)
        }

        noticesList?.setNeedsSort(true)
    }

    fun updateSelected() {
        if (noticesList?.lastUpdateFrame == 0) {
            if (panelViewNotice?.isVisible() == true) {
                if (!noticesList!!.selectById(prevSelectedNotice)) {
                    noticesList!!.selectFirstItem()
                }
            }
            noticesList?.lastUpdateFrame = 1
        }
    }

    private fun onSelectNotice() {
        val item = noticesList?.getFirstSelected() ?: return
        System.err.println("APR: send GroupNoticeRequest message for noticeId=${item.id}")
    }

    fun showNotice(
        subject: String,
        message: String,
        hasInventory: Boolean,
        inventoryName: String,
        offer: OfferInfo?
    ) {
        arrangeNoticeView(NoticeView.VIEW_PAST)
        viewSubject?.setText(subject)
        viewMessage?.setText(message)

        inventoryOffer?.forceResponse(InventoryOfferResponse.DECLINE)
        inventoryOffer = null

        if (offer != null) {
            inventoryOffer = offer
            viewInventoryName?.setText("        $inventoryName")
            btnOpenAttachment?.setEnabled(true)
        } else {
            viewInventoryName?.clear()
            btnOpenAttachment?.setEnabled(false)
        }
    }

    private fun arrangeNoticeView(viewType: NoticeView) {
        when (viewType) {
            NoticeView.CREATE_NEW -> {
                panelCreateNotice?.setVisible(true)
                panelViewNotice?.setVisible(false)
            }
            NoticeView.VIEW_PAST -> {
                panelCreateNotice?.setVisible(false)
                panelViewNotice?.setVisible(true)
                btnOpenAttachment?.setEnabled(false)
            }
        }
    }

    override fun setGroupId(id: UUID) {
        instances.remove(groupId)
        super.setGroupId(id)
        instances[groupId] = this

        btnNewMessage?.setEnabled(Agent.hasPowerInGroup(groupId, GP_NOTICES_SEND))

        findChild<GroupDropTarget>("drop_target")?.let {
            it.setPanel(this)
            it.setGroup(groupId)
        }

        viewMessage?.clear()
        viewInventoryName?.clear()

        activate()
    }

    override fun needsApply(mesg: StringBuilder): Boolean = false
    override fun apply(mesg: StringBuilder): Boolean = true
    override fun cancel() {}
    override fun update(gc: GroupChange) {}
    override fun draw() {}
}

enum class NoticeView { CREATE_NEW, VIEW_PAST }

class ViewerInventoryItem {
    val id: UUID = UUID(0, 0)
    val name: String = ""
    val type: Any = Unit
    val inventoryType: Any = Unit
    val flags: Int = 0
}

class OfferInfo {
    val type: Any = Unit
    fun forceResponse(response: InventoryOfferResponse) {
        System.err.println("APR: respond to inventory offer")
    }
}

enum class InventoryOfferResponse { ACCEPT, DECLINE }

object Inventory {
    fun getItem(id: UUID): ViewerInventoryItem? {
        System.err.println("APR: inventory item lookup")
        return null
    }
}

object GiveInventory {
    fun isInventoryGroupGiveAcceptable(item: ViewerInventoryItem): Boolean {
        System.err.println("APR: check give acceptability")
        return false
    }
}

object InventoryIcon {
    fun getIconName(type: Any, inventoryType: Any, flags: Int = 0, multi: Boolean = false): String {
        System.err.println("APR: icon name for inventory type")
        return ""
    }
    fun getIconName(assetType: UByte): String {
        System.err.println("APR: icon name for asset type")
        return ""
    }
}

object CacheName {
    fun buildUsername(legacyName: String): String {
        System.err.println("APR: convert legacy name to username")
        return ""
    }
    fun buildLegacyName(legacyName: String): String {
        System.err.println("APR: return legacy name as-is")
        return ""
    }
}

object AgentUi {
    fun buildFullName(): String {
        System.err.println("APR: build agent full name string")
        return ""
    }
}

fun sendGroupNotice(groupId: UUID, subject: String, message: String, inventoryItem: ViewerInventoryItem?) {
    System.err.println("APR: send group notice message via gMessageSystem")
}

const val II_FLAGS_OBJECT_HAS_MULTIPLE_ITEMS = 0x01

fun ScrollListCtrl.addRow(id: UUID, columns: Map<String, String>) {
    System.err.println("GPU: add list row with ID and column data")
}
fun ScrollListCtrl.deleteAllItems() {
    System.err.println("GPU: clear list")
}
fun ScrollListCtrl.setEnabled(v: Boolean) {
    System.err.println("GPU: set list enabled state")
}
fun ScrollListCtrl.setNeedsSort(v: Boolean) {
    System.err.println("GPU: deferred sort flag")
}
fun ScrollListCtrl.setCommentText(text: String) {
    System.err.println("GPU: set placeholder text")
}
fun ScrollListCtrl.getFirstSelected(): ScrollListItem? {
    System.err.println("GPU: first selected item")
    return null
}
fun ScrollListCtrl.getSelectedId(): UUID? {
    System.err.println("GPU: selected item ID")
    return null
}
fun ScrollListCtrl.selectById(id: UUID): Boolean {
    System.err.println("GPU: select by ID")
    return false
}
fun ScrollListCtrl.selectFirstItem() {
    System.err.println("GPU: select first item")
}
fun ScrollListCtrl.deselectAllItems() {
    System.err.println("GPU: deselect all")
}
fun ScrollListCtrl.setCommitOnSelectionChange(v: Boolean) {
    System.err.println("GPU: config")
}
fun ScrollListCtrl.setCommitCallback(fn: () -> Unit) {
    System.err.println("GPU: commit callback")
}
var ScrollListCtrl.lastUpdateFrame: Int
    get() { System.err.println("GPU: last frame counter"); return 0 }
    set(value) { System.err.println("GPU: set frame counter") }

fun LineEditor.setTabStop(v: Boolean) {
    System.err.println("GPU: tab stop config")
}
fun LineEditor.clear() {
    System.err.println("GPU: clear text")
}
fun LineEditor.getText(): String {
    System.err.println("GPU: get text")
    return ""
}
fun LineEditor.setText(s: String) {
    System.err.println("GPU: set text")
}

fun Panel.setEnabled(v: Boolean) {
    System.err.println("GPU: enable panel")
}
fun Panel.setVisible(v: Boolean) {
    System.err.println("GPU: show/hide panel")
}
fun Panel.isVisible(): Boolean {
    System.err.println("GPU: visibility query")
    return false
}

fun Button.setClickedCallback(fn: () -> Unit) {
    System.err.println("GPU: click callback")
}

fun <T> PanelGroupTab.findChild(name: String): T? {
    System.err.println("GPU: find child widget by name")
    return null
}

fun GroupDropTarget.setPanel(panel: PanelGroupNotices) {
    System.err.println("GPU: set panel reference")
}
fun GroupDropTarget.setGroup(id: UUID) {
    System.err.println("GPU: set group ID")
}

fun MessageSystem.getNumberOfBlocks(block: String): Int {
    System.err.println("APR: message block count")
    return 0
}
fun MessageSystem.getUUID(block: String, field: String, index: Int = 0): UUID {
    System.err.println("APR: read UUID field")
    return UUID(0, 0)
}
fun MessageSystem.getString(block: String, field: String, index: Int = 0): String {
    System.err.println("APR: read string field")
    return ""
}
fun MessageSystem.getBool(block: String, field: String, index: Int = 0): Boolean {
    System.err.println("APR: read bool field")
    return false
}
fun MessageSystem.getUByte(block: String, field: String, index: Int = 0): UByte {
    System.err.println("APR: read ubyte field")
    return 0u
}
fun MessageSystem.getUInt(block: String, field: String, index: Int = 0): UInt {
    System.err.println("APR: read uint field")
    return 0u
}

class MessageSystem

const val GP_NOTICES_SEND_OR_RECEIVE: Long = GP_NOTICES_SEND or GP_NOTICES_RECEIVE
