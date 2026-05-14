package com.firestorm.newview

import java.util.UUID

class FSFloaterAvatarRenderSettings(key: LLSD) : LLFloater(key) {

    private var avatarList: LLNameListCtrl? = null
    private var picker: LLHandle<LLFloater>? = null

    private var renderSettingChangedCallbackConnection: ((UUID, LLVOAvatar.VisualMuteSettings) -> Unit)? = null

    private var filterSubString: String = ""
    private var filterSubStringOrig: String = ""

    init {
        registerCommitCallback("Settings.AddNewEntry") { _, userdata -> onClickAdd(userdata) }
    }

    override fun postBuild(): Boolean {
        avatarList = getChild<LLNameListCtrl>("avatar_list")
        avatarList?.setContextMenu(FSFloaterAvatarRenderPersistenceMenu.gFSAvatarRenderPersistenceMenu)
        avatarList?.setFilterColumn(0)

        childSetAction("close_btn") { onCloseBtn() }
        getChild<LLFilterEditor>("filter_input").setCommitCallback { _, value -> onFilterEdit(value.asString()) }

        renderSettingChangedCallbackConnection = { avatarId, renderSetting ->
            onAvatarRenderSettingChanged(avatarId, renderSetting)
        }
        FSAvatarRenderPersistence.instance().setAvatarRenderSettingChangedCallback(renderSettingChangedCallbackConnection!!)

        setVisibleCallback { removePicker() }

        loadInitialList()
        return true
    }

    private fun removePicker() {
        picker?.get()?.closeFloater()
    }

    private fun onCloseBtn() {
        closeFloater()
    }

    private fun loadInitialList() {
        val avatarRenderMap = FSAvatarRenderPersistence.instance().getAvatarRenderMap()
        for ((avatarId, renderSetting) in avatarRenderMap) {
            addElementToList(avatarId, renderSetting)
        }
    }

    private fun addElementToList(avatarId: UUID, renderSetting: LLVOAvatar.VisualMuteSettings) {
        val avRenderNever = getString("av_render_never")
        val avRenderAlways = getString("av_render_always")
        val avNameWaiting = LLTrans.getString("AvatarNameWaiting")

        val renderValue = if (renderSetting == LLVOAvatar.VisualMuteSettings.AV_DO_NOT_RENDER) avRenderNever else avRenderAlways

        avatarList?.addNameItemRow(
            value = avatarId,
            target = LLNameListCtrl.Target.INDIVIDUAL,
            name = avNameWaiting,
            columns = mapOf("name" to "", "render_setting" to renderValue)
        )
    }

    private fun onAvatarRenderSettingChanged(avatarId: UUID, renderSetting: LLVOAvatar.VisualMuteSettings) {
        avatarList?.removeNameItem(avatarId)
        if (renderSetting != LLVOAvatar.VisualMuteSettings.AV_RENDER_NORMALLY) {
            addElementToList(avatarId, renderSetting)
        }
    }

    private fun onFilterEdit(searchString: String) {
        filterSubStringOrig = searchString.trimStart()
        val searchUpper = filterSubStringOrig.uppercase()
        if (filterSubString == searchUpper) return
        filterSubString = searchUpper
        avatarList?.setFilterString(filterSubStringOrig)
    }

    override fun handleKeyHere(key: Int, mask: Int): Boolean {
        if (FSCommon.isFilterEditorKeyCombo(key, mask)) {
            getChild<LLFilterEditor>("filter_input").setFocus(true)
            return true
        }
        return super.handleKeyHere(key, mask)
    }

    override fun hasAccelerators(): Boolean = true

    private fun onClickAdd(userdata: LLSD) {
        val commandName = userdata.asString()
        val renderSetting = when (commandName) {
            "never"  -> LLVOAvatar.VisualMuteSettings.AV_DO_NOT_RENDER
            "always" -> LLVOAvatar.VisualMuteSettings.AV_ALWAYS_RENDER
            else     -> LLVOAvatar.VisualMuteSettings.AV_RENDER_NORMALLY
        }

        val button = findChild<LLButton>("plus_btn")
        val rootFloater = gFloaterView.getParentFloater(this)
        val pickerFloater = LLFloaterAvatarPicker.show(
            callback = { ids -> callbackAvatarPicked(ids, renderSetting) },
            allowMultiple = true,
            closeOnSelect = true,
            skipFriends = true,
            name = rootFloater?.getName() ?: "",
            view = button
        )

        rootFloater?.addDependentFloater(pickerFloater)
        picker = pickerFloater.getHandle()
    }

    private fun callbackAvatarPicked(ids: List<UUID>, renderSetting: LLVOAvatar.VisualMuteSettings) {
        for (avatarId in ids) {
            val avatarp = gObjectList.findObject(avatarId) as? LLVOAvatar
            if (avatarp != null) {
                avatarp.setVisualMuteSettings(renderSetting)
            } else {
                FSAvatarRenderPersistence.instance().setAvatarRenderSettings(avatarId, renderSetting)
            }
        }
    }
}

object FSFloaterAvatarRenderPersistenceMenu {

    val gFSAvatarRenderPersistenceMenu = FSAvatarRenderPersistenceMenu()

    class FSAvatarRenderPersistenceMenu : LLListContextMenu() {

        override fun createMenu(): LLContextMenu {
            val registrar = LLUICtrl.CommitCallbackRegistry.ScopedRegistrar()
            registrar.add("Avatar.ChangeRenderSetting") { _, param -> changeRenderSetting(param) }
            return createFromFile("menu_fs_avatar_render_setting.xml")
        }

        private fun changeRenderSetting(param: LLSD) {
            val renderSetting = LLVOAvatar.VisualMuteSettings.fromInt(param.asInt())

            for (avatarId in mUUIDs) {
                val avatar = gObjectList.findObject(avatarId) as? LLVOAvatar
                if (avatar != null) {
                    avatar.setVisualMuteSettings(renderSetting)
                } else {
                    FSAvatarRenderPersistence.instance().setAvatarRenderSettings(avatarId, renderSetting)
                }
            }

            // no-op
        }
    }
}
