package com.firestorm.newview

import java.util.UUID

// This file is guarded in the original C++ with `#if 0` (FS:Ansariel persisted render settings),
// meaning the feature was intentionally disabled in the Firestorm fork.
// The Kotlin port preserves the full class so the feature can be re-enabled when needed.

enum class AvatarVisualMuteSetting {
    AV_RENDER_NORMALLY,
    AV_DO_NOT_RENDER,
    AV_ALWAYS_RENDER
}

class FloaterAvatarRenderSettings(key: Any) : Floater(key) {

    private var avatarSettingsList: NameListCtrl? = null
    private var needsUpdate: Boolean = false
    private var contextMenu: SettingsContextMenu? = null

    init {
        contextMenu = SettingsContextMenu(this)
        RenderMuteList.getInstance().addObserver(muteListObserver)
    }

    override fun postBuild(): Boolean {
        super.postBuild()
        avatarSettingsList = getChild("render_settings_list")
        avatarSettingsList!!.setRightMouseDownCallback { ctrl, x, y -> onAvatarListRightClick(ctrl, x, y) }
        return true
    }

    override fun onOpen(key: Any) {
        updateList()
    }

    override fun draw() {
        if (needsUpdate) {
            updateList()
            needsUpdate = false
        }
        super.draw()
    }

    override fun handleKeyHere(key: Int, mask: Int): Boolean {
        if (key == KeyCode.DELETE) {
            val currentId = avatarSettingsList?.getCurrentID() ?: return false
            setAvatarRenderSetting(currentId, AvatarVisualMuteSetting.AV_RENDER_NORMALLY)
            return true
        }
        return false
    }

    fun onAvatarListRightClick(ctrl: UICtrl?, x: Int, y: Int) {
        val list = ctrl as? NameListCtrl ?: return
        list.selectItemAt(x, y)
        val currentId = list.getCurrentID()
        if (currentId != null) {
            contextMenu!!.show(ctrl, listOf(currentId), x, y)
        }
    }

    fun updateList() {
        avatarSettingsList!!.deleteAllItems()
        for ((avId, setting) in RenderMuteList.getInstance().visualMuteSettingsMap) {
            val avName = AvatarNameCache.get(avId)
            val settingStr = getString(
                if (setting == AvatarVisualMuteSetting.AV_DO_NOT_RENDER) "av_never_render" else "av_always_render"
            )
            avatarSettingsList!!.addNameItem(
                value = avId,
                name = avName.getCompleteName(),
                setting = settingStr
            )
        }
    }

    fun onCustomAction(userdata: Any, avId: UUID) {
        val commandName = userdata.toString()
        val newSetting = when (commandName) {
            "default" -> AvatarVisualMuteSetting.AV_RENDER_NORMALLY
            "never"   -> AvatarVisualMuteSetting.AV_DO_NOT_RENDER
            "always"  -> AvatarVisualMuteSetting.AV_ALWAYS_RENDER
            else      -> return
        }
        setAvatarRenderSetting(avId, newSetting)
    }

    fun isActionChecked(userdata: Any, avId: UUID): Boolean {
        val commandName = userdata.toString()
        val visualSetting = RenderMuteList.getInstance().getSavedVisualMuteSetting(avId)
        return when (commandName) {
            "default"     -> visualSetting == AvatarVisualMuteSetting.AV_RENDER_NORMALLY
            "non_default" -> visualSetting != AvatarVisualMuteSetting.AV_RENDER_NORMALLY
            "never"       -> visualSetting == AvatarVisualMuteSetting.AV_DO_NOT_RENDER
            "always"      -> visualSetting == AvatarVisualMuteSetting.AV_ALWAYS_RENDER
            else          -> false
        }
    }

    fun onClickAdd(userdata: Any) {
        val commandName = userdata.toString()
        val visualSetting = when (commandName) {
            "never"  -> AvatarVisualMuteSetting.AV_DO_NOT_RENDER
            "always" -> AvatarVisualMuteSetting.AV_ALWAYS_RENDER
            else     -> AvatarVisualMuteSetting.AV_RENDER_NORMALLY
        }
        FloaterAvatarPicker.show(
            callback = { ids, _ -> callbackAvatarPicked(ids, visualSetting) },
            allowMultiple = false,
            closeOnSelect = true,
            skipAgent = false,
            name = getName()
        )
    }

    fun setAvatarRenderSetting(avId: UUID, newSetting: AvatarVisualMuteSetting) {
        val avatar = findAvatar(avId)
        if (avatar != null) {
            avatar.setVisualMuteSettings(newSetting)
        } else {
            RenderMuteList.getInstance().saveVisualMuteSetting(avId, newSetting)
        }
    }

    fun createTimestamp(datetime: Int): String {
        return ""
    }

    private fun callbackAvatarPicked(ids: List<UUID>, visualSetting: AvatarVisualMuteSetting) {
        if (ids.isEmpty()) return
        if (ids[0] == Agent.id) {
            NotificationsUtil.add("AddSelfRenderExceptions")
            return
        }
        setAvatarRenderSetting(ids[0], visualSetting)
    }

    companion object {
        fun setNeedsUpdate() {
            val instance = FloaterReg.getTypedInstance<FloaterAvatarRenderSettings>("avatar_render_settings")
                ?: return
            instance.needsUpdate = true
        }
    }

    private val muteListObserver = object : MuteListObserver {
        override fun onChange() {
            setNeedsUpdate()
        }
    }
}

private class SettingsContextMenu(private val floaterSettings: FloaterAvatarRenderSettings) : ListContextMenu() {
    override fun createMenu(uuids: List<UUID>): ContextMenu {
        val avId = uuids.first()
        return ContextMenu.fromFile("menu_avatar_rendering_settings.xml").apply {
            registerAction("Settings.SetRendering") { userdata -> floaterSettings.onCustomAction(userdata, avId) }
            registerEnableCheck("Settings.IsSelected") { userdata -> floaterSettings.isActionChecked(userdata, avId) }
        }
    }
}

private fun findAvatar(id: UUID): VoAvatar? {
    var obj: ViewerObject? = ObjectList.findObject(id)
    while (obj != null && obj.isAttachment()) {
        obj = obj.getParent() as? ViewerObject
    }
    return if (obj != null && obj.isAvatar()) obj as? VoAvatar else null
}
