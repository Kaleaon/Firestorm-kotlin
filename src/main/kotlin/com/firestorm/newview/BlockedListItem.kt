package com.firestorm.newview

import com.firestorm.ui.Panel
import com.firestorm.ui.TextBox
import com.firestorm.mutelist.Mute
import java.util.UUID

class BlockedListItem(item: Mute) : Panel() {

    private var titleCtrl: TextBox? = null
    val itemId: UUID = item.id
    val name: String = item.name
    val type: Mute.EType = item.type

    init {
        buildFromFile("panel_blocked_list_item.xml")
    }

    fun postBuild(): Boolean {
        titleCtrl = getChild("item_name")
        titleCtrl?.setValue(name)

        when (type) {
            Mute.EType.AGENT, Mute.EType.EXTERNAL -> {
                val avatarIcon = getChild<AvatarIconCtrl>("avatar_icon")
                avatarIcon.isVisible = true
                avatarIcon.setValue(itemId)
            }
            Mute.EType.GROUP -> {
                val groupIcon = getChild<GroupIconCtrl>("group_icon")
                groupIcon.isVisible = true
                groupIcon.setValue(itemId)
            }
            Mute.EType.OBJECT, Mute.EType.BY_NAME -> {
                getChild<UICtrl>("object_icon").isVisible = true
            }
            else -> Unit
        }
        return true
    }

    fun onMouseEnter(x: Int, y: Int, mask: Int) {
        getChildView("hovered_icon").isVisible = true
        super.onMouseEnter(x, y, mask)
    }

    fun onMouseLeave(x: Int, y: Int, mask: Int) {
        getChildView("hovered_icon").isVisible = false
        super.onMouseLeave(x, y, mask)
    }

    fun setValue(value: LLSD) {
        if (!value.isMap || !value.has("selected")) return
        getChildView("selected_icon").isVisible = value["selected"].asBoolean()
    }

    fun highlightName(highlightedText: String) {
        val params = Style.Params()
        TextUtil.textboxSetHighlightedVal(titleCtrl, params, name, highlightedText)
    }
}
