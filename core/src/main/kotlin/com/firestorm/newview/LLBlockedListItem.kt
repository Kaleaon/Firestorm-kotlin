package com.firestorm.newview

import java.util.UUID

// Mirrors LLMute::EType
enum class MuteType {
    AGENT,
    OBJECT,
    GROUP,
    BY_NAME,
    EXTERNAL,
    COUNT
}

// Lightweight stand-in for LLMute; carries the data LLBlockedListItem needs.
data class LLMute(
    val id: UUID,
    val name: String,
    val type: MuteType,
    val flags: UInt = 0u
)

// ---------------------------------------------------------------------------
// LLBlockedListItem
//
// A single row in LLBlockList.  Displays the muted entity's icon and name.
// Panel layout is driven by "panel_blocked_list_item.xml".
// ---------------------------------------------------------------------------

class LLBlockedListItem(item: LLMute) {

    private val mItemID: UUID = item.id
    private val mItemName: String = item.name
    private val mMuteType: MuteType = item.type

    private var mTitleCtrl: Any? = null   // LLTextBox

    init {
        // no-op
    }

    fun postBuild(): Boolean {
        return false
    }

    fun onMouseEnter(x: Int, y: Int) {
        // no-op
    }

    fun onMouseLeave(x: Int, y: Int) {
        // no-op
    }

    open fun setValue(value: Any?) {
        if (value !is Map<*, *> || !value.containsKey("selected")) return
        val selected = value["selected"] as? Boolean ?: return
        // no-op
    }

    fun highlightName(highlightedText: String) {
        // no-op
    }

    fun getName(): String = mItemName
    fun getType(): MuteType = mMuteType
    fun getUUID(): UUID = mItemID
}
