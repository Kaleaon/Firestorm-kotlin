package com.firestorm.newview

import java.util.UUID

class LLPickData
class LLAvatarName(private val completeName: String) {
    fun getCompleteName(): String = completeName
}

open class LLPanelProfile {
    open fun onOpen(key: Map<String, Any?>) { System.err.println("LLPanelProfile: onOpen not yet implemented") }
    open fun hasUnpublishedClassifieds(): Boolean = false
    open fun hasUnsavedChanges(): Boolean = false
    open fun commitUnsavedChanges() { System.err.println("LLPanelProfile: commitUnsavedChanges not yet implemented") }
    open fun createPick(data: LLPickData) { System.err.println("LLPanelProfile: createPick not yet implemented") }
    open fun showPick(pickId: UUID) { System.err.println("LLPanelProfile: showPick not yet implemented") }
    open fun isPickTabSelected(): Boolean = false
    open fun showClassified(classifiedId: UUID, edit: Boolean) { System.err.println("LLPanelProfile: showClassified not yet implemented") }
    open fun createClassified() { System.err.println("LLPanelProfile: createClassified not yet implemented") }
}

open class LLPanelProfileSecondLife {
    open fun refreshName() { System.err.println("LLPanelProfileSecondLife: refreshName not yet implemented") }
}

private const val PANEL_PROFILE_VIEW = "panel_profile_view"

class LLFloaterProfile(key: Map<String, Any?>) : LLFloater(key) {

    private var avatarId: UUID = (key["id"] as? String)?.let { UUID.fromString(it) } ?: UUID(0, 0)
    private var nameCallbackConnection: (() -> Unit)? = null
    private var panelProfile: LLPanelProfile? = null

    init {
        System.err.println("LLFloaterProfile: set mDefaultRectForGroup = false not yet implemented")
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

    private fun closeFloater() { System.err.println("LLFloaterProfile: closeFloater not yet implemented") }
    private fun setTitle(title: String) { System.err.println("LLFloaterProfile: setTitle not yet implemented") }
    private fun findChildPanel(name: String): LLPanelProfile? = null
    private fun findChildPanelSecondLife(name: String): LLPanelProfileSecondLife? = null
    private fun getSelectedOption(notification: Map<String, Any>, response: Map<String, Any>): Int = 0
    private fun showNotification(
        name: String,
        callback: (Map<String, Any>, Map<String, Any>) -> Unit
    ) { System.err.println("LLFloaterProfile: showNotification not yet implemented") }
    private fun fetchAvatarName(
        avatarId: UUID,
        callback: (UUID, LLAvatarName) -> Unit
    ): () -> Unit = {}
}
