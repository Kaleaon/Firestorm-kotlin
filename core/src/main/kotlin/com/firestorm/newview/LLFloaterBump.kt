package com.firestorm.newview

import java.util.UUID

class LLFloaterBump private constructor(key: LLSD) : LLFloater(key) {

    private var mList: FSScrollListCtrl? = null
    private var mDirty: Boolean = false

    override fun postBuild(): Boolean {
        mList = getChild<FSScrollListCtrl>("bump_list")
        mList!!.setContextMenu(gFSBumpListMenu)
        return super.postBuild()
    }

    override fun onOpen(key: LLSD) {
        updateList()
    }

    override fun draw() {
        if (mDirty) {
            updateList()
            mDirty = false
        }
        super.draw()
    }

    fun setDirty() {
        mDirty = true
    }

    protected fun add(list: LLScrollListCtrl, mcd: LLMeanCollisionData) {
        if (mcd.mFullName.isEmpty() || list.getItemCount() >= 20) return

        var timeStr = getString("timeStr")
        val substitution = LLSD()
        substitution["datetime"] = mcd.mTime.toInt()
        LLStringUtil.format(timeStr, substitution)

        val action = when (mcd.mType) {
            MeanCollisionType.MEAN_BUMP -> "bump"
            MeanCollisionType.MEAN_LLPUSHOBJECT -> "llpushobject"
            MeanCollisionType.MEAN_SELECTED_OBJECT_COLLIDE -> "selected_object_collide"
            MeanCollisionType.MEAN_SCRIPTED_OBJECT_COLLIDE -> "scripted_object_collide"
            MeanCollisionType.MEAN_PHYSICAL_OBJECT_COLLIDE -> "physical_object_collide"
            else -> return
        }

        val text = getString(action)
            .setArg("[TIME]", timeStr)
            .setArg("[NAME]", mcd.mFullName)

        val row = LLSD()
        row["id"] = mcd.mPerp
        row["columns"][0]["value"] = text
        row["columns"][0]["font"] = "SansSerifBold"
        list.addElement(row)
    }

    protected fun onScrollListRightClicked(ctrl: LLUICtrl, x: Int, y: Int) {
        System.err.println("LLFloaterBump: onScrollListRightClicked not yet implemented")
    }

    private fun updateList() {
        val list = mList ?: return
        list.deleteAllItems()

        if (gMeanCollisionList.isEmpty()) {
            val noneDetected = getString("none_detected")
            val row = LLSD()
            row["columns"][0]["value"] = noneDetected
            row["columns"][0]["font"] = "SansSerifBold"
            list.addElement(row)
        } else {
            for (mcd in gMeanCollisionList) {
                add(list, mcd)
            }
        }
    }

    fun startIM() { LLAvatarActions.startIM(getSelectedUUID()) }
    fun startCall() { LLAvatarActions.startCall(getSelectedUUID()) }
    fun reportAbuse() { LLAvatarActions.report(getSelectedUUID()) }
    fun showProfile() { LLAvatarActions.showProfile(getSelectedUUID()) }
    fun addFriend() { LLAvatarActions.requestFriendshipDialog(getSelectedUUID()) }
    fun inviteToGroup() { LLAvatarActions.inviteToGroup(getSelectedUUID()) }
    fun enableAddFriend(): Boolean = !LLAvatarActions.isFriend(getSelectedUUID())
    fun muteAvatar() { LLAvatarActions.toggleBlock(getSelectedUUID()) }
    fun payAvatar() { LLAvatarActions.pay(getSelectedUUID()) }
    fun zoomInAvatar() { LLAvatarActions.zoomIn(getSelectedUUID()) }
    fun enableMute(): Boolean = LLAvatarActions.canBlock(getSelectedUUID())

    private fun getSelectedUUID(): UUID = mList?.getSelectedUUID() ?: UUID.randomUUID()

    companion object {
        fun getInstance(): LLFloaterBump? =
            LLFloaterReg.getTypedInstance<LLFloaterBump>("bumps")
    }
}

class FSBumpListMenu : LLListContextMenu() {

    override fun createMenu(): LLContextMenu {
        return LLContextMenu()
    }

    private fun onContextMenuItemClick(userdata: LLSD) {
        val item = userdata.asString()
        when (item) {
            "show_profile" -> LLAvatarActions.showProfile(mUUIDs.first())
            "sendim" -> {
                if (mUUIDs.size == 1) {
                    LLAvatarActions.startIM(mUUIDs.first())
                } else {
                    LLAvatarActions.startConference(mUUIDs)
                }
            }
            "zoom" -> LLAvatarActions.zoomIn(mUUIDs.first())
            "teleportto" -> LLAvatarActions.teleportTo(mUUIDs.first())
            "block" -> LLAvatarActions.toggleBlock(mUUIDs.first())
            "report" -> LLAvatarActions.report(mUUIDs.first())
            "copy" -> {
                val floater = LLFloaterReg.findTypedInstance<LLFloaterBump>("bumps")
                if (floater != null && gMeanCollisionList.isNotEmpty() && mUUIDs.isNotEmpty()) {
                    val list = floater.getChild<FSScrollListCtrl>("bump_list")
                    val selected = list.getAllSelected()
                    val bumpsText = selected.joinToString("\n") { it.getColumn(0).getValue().asString() }
                    if (bumpsText.isNotEmpty()) {
                        LLClipboard.instance().copyToClipboard(bumpsText)
                    }
                }
            }
        }
    }

    private fun onContextMenuItemEnable(userdata: LLSD): Boolean {
        val floater = LLFloaterReg.findTypedInstance<LLFloaterBump>("bumps") ?: return false
        val item = userdata.asString()
        return when (item) {
            "can_show_profile" -> gMeanCollisionList.isNotEmpty() && mUUIDs.size == 1
            "can_sendim" -> gMeanCollisionList.isNotEmpty() && mUUIDs.isNotEmpty()
            "can_zoom" -> gMeanCollisionList.isNotEmpty() && mUUIDs.size == 1 && LLAvatarActions.canZoomIn(mUUIDs.first())
            "can_teleportto" -> gMeanCollisionList.isNotEmpty() && mUUIDs.size == 1 && FSRadar.getInstance().getEntry(mUUIDs.first()) != null
            "can_block" -> gMeanCollisionList.isNotEmpty() && mUUIDs.size == 1
            "is_blocked" -> {
                if (gMeanCollisionList.isNotEmpty() && mUUIDs.size == 1) {
                    val avName = LLAvatarNameCache.get(mUUIDs.first())
                    LLMuteList.getInstance().isMuted(mUUIDs.first(), avName.getUserName())
                } else {
                    false
                }
            }
            "can_report" -> gMeanCollisionList.isNotEmpty() && mUUIDs.size == 1
            "can_copy" -> gMeanCollisionList.isNotEmpty() && mUUIDs.isNotEmpty()
            else -> false
        }
    }
}

val gFSBumpListMenu: FSBumpListMenu = FSBumpListMenu()
