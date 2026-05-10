package com.firestorm.newview

import java.util.UUID

class LLPickData
class LLAvatarName(private val completeName: String) {
    fun getCompleteName(): String = completeName
}

open class LLPanelProfile {
    open fun onOpen(key: Map<String, Any?>) = TODO("stub")
    open fun hasUnpublishedClassifieds(): Boolean = TODO("stub")
    open fun hasUnsavedChanges(): Boolean = TODO("stub")
    open fun commitUnsavedChanges() = TODO("stub")
    open fun createPick(data: LLPickData) = TODO("stub")
    open fun showPick(pickId: UUID) = TODO("stub")
    open fun isPickTabSelected(): Boolean = TODO("stub")
    open fun showClassified(classifiedId: UUID, edit: Boolean) = TODO("stub")
    open fun createClassified() = TODO("stub")
}

open class LLPanelProfileSecondLife {
    open fun refreshName() = TODO("stub")
}

private const val PANEL_PROFILE_VIEW = "panel_profile_view"

class LLFloaterProfile(key: Map<String, Any?>) : LLFloater(key) {

    private var avatarId: UUID = (key["id"] as? String)?.let { UUID.fromString(it) } ?: UUID(0, 0)
    private var nameCallbackConnection: (() -> Unit)? = null
    private var panelProfile: LLPanelProfile? = null

    init {
        TODO("stub: set mDefaultRectForGroup = false")
    }

    override fun finalize() {
        nameCallbackConnection?.invoke()
        nameCallbackConnection = null
    }

    open fun onOpen(key: Map<String, Any?>) {
        panelProfile?.onOpen(key)
        nameCallbackConnection = fetchAvatarName(avatarId) { agentId, avName ->
            onAvatarNameCache(agentId, avName)
        }
    }

    open fun postBuild(): Boolean {
        panelProfile = findChildPanel(PANEL_PROFILE_VIEW)
        return true
    }

    open fun onClickCloseBtn(appQuitting: Boolean = false) {
        if (!appQuitting) {
            val profile = panelProfile ?: run { closeFloater(); return }
            when {
                profile.hasUnpublishedClassifieds() ->
                    showNotification("ProfileUnpublishedClassified") { notification, response ->
                        onUnsavedChangesCallback(notification, response, canSave = false)
                    }
                profile.hasUnsavedChanges() ->
                    showNotification("ProfileUnsavedChanges") { notification, response ->
                        onUnsavedChangesCallback(notification, response, canSave = true)
                    }
                else -> closeFloater()
            }
        } else {
            closeFloater()
        }
    }

    fun onUnsavedChangesCallback(
        notification: Map<String, Any>,
        response: Map<String, Any>,
        canSave: Boolean
    ) {
        val option = getSelectedOption(notification, response)
        if (canSave) {
            when (option) {
                0 -> { panelProfile?.commitUnsavedChanges(); closeFloater() }
                1 -> closeFloater()
            }
        } else {
            if (option == 0) closeFloater()
        }
    }

    fun createPick(data: LLPickData) {
        panelProfile?.createPick(data)
    }

    fun showPick(pickId: UUID = UUID(0, 0)) {
        panelProfile?.showPick(pickId)
    }

    fun isPickTabSelected(): Boolean = panelProfile?.isPickTabSelected() ?: false

    fun refreshName() {
        if (nameCallbackConnection == null) {
            nameCallbackConnection = fetchAvatarName(avatarId) { agentId, avName ->
                onAvatarNameCache(agentId, avName)
            }
        }
        findChildPanelSecondLife("panel_profile_secondlife")?.refreshName()
    }

    fun showClassified(classifiedId: UUID = UUID(0, 0), edit: Boolean = false) {
        panelProfile?.showClassified(classifiedId, edit)
    }

    fun createClassified() {
        panelProfile?.createClassified()
    }

    private fun onAvatarNameCache(agentId: UUID, avName: LLAvatarName) {
        nameCallbackConnection = null
        setTitle(avName.getCompleteName())
    }

    private fun closeFloater() = TODO("stub")
    private fun setTitle(title: String) = TODO("stub")
    private fun findChildPanel(name: String): LLPanelProfile? = TODO("stub")
    private fun findChildPanelSecondLife(name: String): LLPanelProfileSecondLife? = TODO("stub")
    private fun getSelectedOption(notification: Map<String, Any>, response: Map<String, Any>): Int = TODO("stub")
    private fun showNotification(
        name: String,
        callback: (Map<String, Any>, Map<String, Any>) -> Unit
    ) = TODO("stub")
    private fun fetchAvatarName(
        avatarId: UUID,
        callback: (UUID, LLAvatarName) -> Unit
    ): () -> Unit = TODO("stub")
}
