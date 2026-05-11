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
        TODO("APR: send GroupNoticesListRequest message via gMessageSystem")
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
        TODO("APR: send GroupNoticeRequest message for noticeId=${item.id}")
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
    fun forceResponse(response: InventoryOfferResponse) = TODO("APR: respond to inventory offer")
}

enum class InventoryOfferResponse { ACCEPT, DECLINE }

object Inventory {
    fun getItem(id: UUID): ViewerInventoryItem? = TODO("APR: inventory item lookup")
}

object GiveInventory {
    fun isInventoryGroupGiveAcceptable(item: ViewerInventoryItem): Boolean = TODO("APR: check give acceptability")
}

object InventoryIcon {
    fun getIconName(type: Any, inventoryType: Any, flags: Int = 0, multi: Boolean = false): String =
        TODO("APR: icon name for inventory type")
    fun getIconName(assetType: UByte): String = TODO("APR: icon name for asset type")
}

object CacheName {
    fun buildUsername(legacyName: String): String = TODO("APR: convert legacy name to username")
    fun buildLegacyName(legacyName: String): String = TODO("APR: return legacy name as-is")
}

object AgentUi {
    fun buildFullName(): String = TODO("APR: build agent full name string")
}

fun sendGroupNotice(groupId: UUID, subject: String, message: String, inventoryItem: ViewerInventoryItem?) {
    TODO("APR: send group notice message via gMessageSystem")
}

const val II_FLAGS_OBJECT_HAS_MULTIPLE_ITEMS = 0x01

fun ScrollListCtrl.addRow(id: UUID, columns: Map<String, String>) = TODO("GPU: add list row with ID and column data")
fun ScrollListCtrl.deleteAllItems() = TODO("GPU: clear list")
fun ScrollListCtrl.setEnabled(v: Boolean) = TODO("GPU: set list enabled state")
fun ScrollListCtrl.setNeedsSort(v: Boolean) = TODO("GPU: deferred sort flag")
fun ScrollListCtrl.setCommentText(text: String) = TODO("GPU: set placeholder text")
fun ScrollListCtrl.getFirstSelected(): ScrollListItem? = TODO("GPU: first selected item")
fun ScrollListCtrl.getSelectedId(): UUID? = TODO("GPU: selected item ID")
fun ScrollListCtrl.selectById(id: UUID): Boolean = TODO("GPU: select by ID")
fun ScrollListCtrl.selectFirstItem() = TODO("GPU: select first item")
fun ScrollListCtrl.deselectAllItems() = TODO("GPU: deselect all")
fun ScrollListCtrl.setCommitOnSelectionChange(v: Boolean) = TODO("GPU: config")
fun ScrollListCtrl.setCommitCallback(fn: () -> Unit) = TODO("GPU: commit callback")
var ScrollListCtrl.lastUpdateFrame: Int
    get() = TODO("GPU: last frame counter")
    set(value) { TODO("GPU: set frame counter") }

fun LineEditor.setTabStop(v: Boolean) = TODO("GPU: tab stop config")
fun LineEditor.clear() = TODO("GPU: clear text")
fun LineEditor.getText(): String = TODO("GPU: get text")
fun LineEditor.setText(s: String) = TODO("GPU: set text")

fun Panel.setEnabled(v: Boolean) = TODO("GPU: enable panel")
fun Panel.setVisible(v: Boolean) = TODO("GPU: show/hide panel")
fun Panel.isVisible(): Boolean = TODO("GPU: visibility query")

fun Button.setClickedCallback(fn: () -> Unit) = TODO("GPU: click callback")

fun <T> PanelGroupTab.findChild(name: String): T? = TODO("GPU: find child widget by name")

fun GroupDropTarget.setPanel(panel: PanelGroupNotices) = TODO("GPU: set panel reference")
fun GroupDropTarget.setGroup(id: UUID) = TODO("GPU: set group ID")

fun MessageSystem.getNumberOfBlocks(block: String): Int = TODO("APR: message block count")
fun MessageSystem.getUUID(block: String, field: String, index: Int = 0): UUID = TODO("APR: read UUID field")
fun MessageSystem.getString(block: String, field: String, index: Int = 0): String = TODO("APR: read string field")
fun MessageSystem.getBool(block: String, field: String, index: Int = 0): Boolean = TODO("APR: read bool field")
fun MessageSystem.getUByte(block: String, field: String, index: Int = 0): UByte = TODO("APR: read ubyte field")
fun MessageSystem.getUInt(block: String, field: String, index: Int = 0): UInt = TODO("APR: read uint field")

class MessageSystem

const val GP_NOTICES_SEND_OR_RECEIVE: Long = GP_NOTICES_SEND or GP_NOTICES_RECEIVE
