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
        TODO("GPU: buildFromFile(\"panel_blocked_list_item.xml\")")
    }

    fun postBuild(): Boolean {
        TODO("GPU: bind mTitleCtrl to child 'item_name'; set its text to mItemName; show the appropriate icon child (avatar_icon / group_icon / object_icon) based on mMuteType; return true")
    }

    fun onMouseEnter(x: Int, y: Int) {
        TODO("GPU: set child 'hovered_icon' visible=true; delegate to LLPanel.onMouseEnter()")
    }

    fun onMouseLeave(x: Int, y: Int) {
        TODO("GPU: set child 'hovered_icon' visible=false; delegate to LLPanel.onMouseLeave()")
    }

    open fun setValue(value: Any?) {
        if (value !is Map<*, *> || !value.containsKey("selected")) return
        val selected = value["selected"] as? Boolean ?: return
        TODO("GPU: set child 'selected_icon' visible=$selected")
    }

    fun highlightName(highlightedText: String) {
        TODO("GPU: call LLTextUtil.textboxSetHighlightedVal(mTitleCtrl, params, mItemName, highlightedText)")
    }

    fun getName(): String = mItemName
    fun getType(): MuteType = mMuteType
    fun getUUID(): UUID = mItemID
}
