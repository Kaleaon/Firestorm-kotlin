package com.firestorm.newview

import java.util.UUID

// Mute flag constants that mirror LLMute::flag* values
object MuteFlags {
    const val VOICE_CHAT: UInt    = 0x01u
    const val TEXT_CHAT: UInt     = 0x02u
    const val PARTICLES: UInt     = 0x04u
    const val OBJECT_SOUNDS: UInt = 0x08u
}

enum class BlockListActionType { NONE, ADD, REMOVE }

// ---------------------------------------------------------------------------
// LLBlockList
//
// A scrollable flat list that mirrors LLMuteList contents.
// Implements LLMuteListObserver so it rebuilds on every mute change.
// ---------------------------------------------------------------------------

class LLBlockList {

    private var mContextMenu: Any? = null        // LLHandle<LLToggleableMenu>

    private var mNameFilter: String = ""
    private var mDirty: Boolean = true
    private var mShouldAddAll: Boolean = true
    private var mActionType: BlockListActionType = BlockListActionType.NONE
    var mMuteListSize: UInt = 0u

    private var mCurItemId: UUID = UUID(0, 0)
    private var mCurItemName: String = ""
    private var mCurItemType: MuteType = MuteType.AGENT
    private var mCurItemFlags: UInt = 0u
    private var mPrevNameFilter: String = ""

    init {
        TODO("APR: LLMuteList.getInstance().addObserver(this); cache mMuteListSize; load menu_people_blocked_gear.xml context menu; register Block.Action/Enable/Check/Visible callbacks")
    }

    fun destroy() {
        TODO("APR: if mContextMenu exists, call die() on it; LLMuteList.getInstance().removeObserver(this)")
    }

    // ---- LLMuteListObserver ------------------------------------------------

    fun onChange() {}

    fun onChangeDetailed(mute: LLMute) {
        mActionType = getCurrentMuteListActionType()
        mCurItemId = mute.id
        mCurItemName = mute.name
        mCurItemType = mute.type
        mCurItemFlags = mute.flags
        refresh()
    }

    // ---- UI events ---------------------------------------------------------

    fun handleRightMouseDown(x: Int, y: Int): Boolean {
        TODO("GPU: delegate to LLUICtrl.handleRightMouseDown(); if context menu exists and list is non-empty, show popup at x=$x y=$y")
    }

    fun getContextMenu(): Any? = mContextMenu

    fun getBlockedItem(): LLBlockedListItem? {
        TODO("GPU: return dynamic_cast of LLFlatListView.getSelectedItem() to LLBlockedListItem, or null")
    }

    fun draw() {
        if (mDirty) refresh()
        TODO("GPU: LLFlatListView.draw()")
    }

    // ---- public filter / sort API ------------------------------------------

    fun setNameFilter(filter: String) {
        val upper = filter.uppercase()
        if (mNameFilter != upper) {
            mNameFilter = upper
            setDirty()
        }
    }

    fun sortByName() {
        TODO("GPU: setComparator(NAME_COMPARATOR); sort()")
    }

    fun sortByType() {
        TODO("GPU: setComparator(NAME_TYPE_COMPARATOR); sort()")
    }

    fun getMuteListSize(): UInt = mMuteListSize

    // ---- private helpers ---------------------------------------------------

    private fun addNewItem(mute: LLMute) {
        val item = LLBlockedListItem(mute)
        if (mNameFilter.isNotEmpty()) item.highlightName(mNameFilter)
        if (mute.id != UUID(0, 0)) {
            TODO("GPU: addItem(item, item.getUUID(), ADD_BOTTOM)")
        } else {
            TODO("GPU: addItem(item, item.getName(), ADD_BOTTOM)")
        }
    }

    private fun removeListItem(mute: LLMute) {
        if (mute.id != UUID(0, 0)) {
            TODO("GPU: removeItemByUUID(mute.id)")
        } else {
            TODO("GPU: removeItemByValue(mute.name)")
        }
    }

    private fun hideListItem(item: LLBlockedListItem, show: Boolean) {
        TODO("GPU: item.setVisible($show)")
    }

    private fun setDirty(dirty: Boolean = true) {
        mDirty = dirty
    }

    private fun findInsensitive(haystack: String, needleUpper: String): Boolean =
        haystack.uppercase().contains(needleUpper)

    private fun getCurrentMuteListActionType(): BlockListActionType {
        TODO("APR: compare LLMuteList.getInstance().getMutes().size with mMuteListSize to determine ADD/REMOVE/NONE")
    }

    fun refresh() {
        val haveFilter = mNameFilter.isNotEmpty()

        TODO("GPU: save current selection; rebuild or incrementally update the list; apply name filter visibility; restore selection; update mMuteListSize; sort(); setDirty(false)")

        if (mShouldAddAll) {
            TODO("GPU: clear(); createList(); mShouldAddAll = false")
        } else {
            val mute = LLMute(mCurItemId, mCurItemName, mCurItemType, mCurItemFlags)
            when (mActionType) {
                BlockListActionType.ADD    -> addNewItem(mute)
                BlockListActionType.REMOVE -> {
                    TODO("GPU: handle selection migration before remove; removeListItem(mute)")
                }
                BlockListActionType.NONE   -> {}
            }
            mActionType = BlockListActionType.NONE
        }

        if (haveFilter || mPrevNameFilter.isNotEmpty()) {
            TODO("GPU: iterate all items; call hideListItem(item, findInsensitive(item.getName(), mNameFilter))")
        }
        mPrevNameFilter = mNameFilter

        mMuteListSize = TODO("APR: LLMuteList.getInstance().getMutes().size.toUInt()")
        setDirty(false)
    }

    private fun createList() {
        TODO("APR: iterate LLMuteList.instance.getMutes(); call addNewItem() for each")
    }

    // ---- context-menu callbacks --------------------------------------------

    private fun isActionEnabled(userdata: String): Boolean {
        return when (userdata) {
            "profile_item", "block_voice", "block_text", "block_particles", "block_obj_sounds" -> {
                TODO("GPU: return true only when exactly one selected item and it is of type AGENT")
            }
            "unblock_item" -> {
                TODO("GPU: return getSelectedItem() != null")
            }
            else -> true
        }
    }

    private fun onCustomAction(userdata: String) {
        if (!isActionEnabled(userdata)) return
        when (userdata) {
            "unblock_item" -> {
                TODO("GPU: for each selected item, build LLMute and call LLMuteList.getInstance().remove(mute)")
            }
            "profile_item" -> {
                TODO("GPU: get single blocked item; if AGENT call LLAvatarActions.showProfile(item.getUUID())")
            }
            "block_voice"      -> toggleMute(MuteFlags.VOICE_CHAT)
            "block_text"       -> toggleMute(MuteFlags.TEXT_CHAT)
            "block_particles"  -> toggleMute(MuteFlags.PARTICLES)
            "block_obj_sounds" -> toggleMute(MuteFlags.OBJECT_SOUNDS)
        }
    }

    private fun isMenuItemChecked(userdata: String): Boolean {
        val item = getBlockedItem() ?: return false
        return when (userdata) {
            "block_voice"      -> isMuted(item.getUUID(), MuteFlags.VOICE_CHAT)
            "block_text"       -> isMuted(item.getUUID(), MuteFlags.TEXT_CHAT)
            "block_particles"  -> isMuted(item.getUUID(), MuteFlags.PARTICLES)
            "block_obj_sounds" -> isMuted(item.getUUID(), MuteFlags.OBJECT_SOUNDS)
            else -> false
        }
    }

    private fun isMenuItemVisible(userdata: String): Boolean {
        val item = getBlockedItem()
        return when (userdata) {
            "block_voice", "block_text", "block_particles", "block_obj_sounds" ->
                item != null && item.getType() == MuteType.AGENT
            else -> false
        }
    }

    private fun toggleMute(flags: UInt) {
        val item = getBlockedItem() ?: return
        val mute = LLMute(item.getUUID(), item.getName(), item.getType())
        if (!isMuted(item.getUUID(), flags)) {
            TODO("APR: LLMuteList.getInstance().add(mute, flags)")
        } else {
            TODO("APR: LLMuteList.getInstance().remove(mute, flags)")
        }
    }

    private fun isMuted(id: UUID, flags: UInt): Boolean {
        TODO("APR: return LLMuteList.getInstance().isMuted(id, flags)")
    }
}

// ---------------------------------------------------------------------------
// Comparators
// ---------------------------------------------------------------------------

abstract class LLBlockListItemComparator {
    fun compare(item1: LLBlockedListItem, item2: LLBlockedListItem): Boolean {
        return doCompare(item1, item2)
    }

    protected abstract fun doCompare(
        item1: LLBlockedListItem,
        item2: LLBlockedListItem
    ): Boolean
}

class LLBlockListNameComparator : LLBlockListItemComparator() {
    override fun doCompare(item1: LLBlockedListItem, item2: LLBlockedListItem): Boolean =
        item1.getName().uppercase() < item2.getName().uppercase()
}

// Objects (OBJECT or BY_NAME) sort before avatars; within the same category
// entries are sorted alphabetically.
class LLBlockListNameTypeComparator : LLBlockListItemComparator() {
    private val nameComparator = LLBlockListNameComparator()

    override fun doCompare(item1: LLBlockedListItem, item2: LLBlockedListItem): Boolean {
        val t1 = item1.getType()
        val t2 = item2.getType()
        val t1IsObject = t1 == MuteType.OBJECT || t1 == MuteType.BY_NAME
        val t2IsObject = t2 == MuteType.OBJECT || t2 == MuteType.BY_NAME
        val bothObjects = t1IsObject && t2IsObject

        if (t1 != t2 && !bothObjects) {
            // Non-avatar (object) types sort first
            return t1 != MuteType.AGENT
        }
        return nameComparator.compare(item1, item2)
    }
}
