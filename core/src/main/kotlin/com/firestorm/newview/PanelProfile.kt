package com.firestorm.newview

import com.firestorm.ui.Panel
import com.firestorm.ui.Button
import com.firestorm.ui.MenuButton
import com.firestorm.ui.CheckBoxCtrl
import com.firestorm.ui.ComboBox
import com.firestorm.ui.IconCtrl
import com.firestorm.ui.TabContainer
import com.firestorm.ui.TextBox
import com.firestorm.ui.TextEditor
import com.firestorm.ui.GroupList
import com.firestorm.ui.TextBase
import com.firestorm.ui.LineEditor
import com.firestorm.ui.Floater
import com.firestorm.ui.FloaterHandle
import com.firestorm.ui.View
import com.firestorm.avatar.AvatarName
import com.firestorm.avatar.AvatarData
import com.firestorm.avatar.AvatarGroups
import com.firestorm.avatar.AvatarPropertiesObserver
import com.firestorm.avatar.EAvatarProcessorType
import com.firestorm.avatar.AvatarPropertiesProcessor
import com.firestorm.people.FriendObserver
import com.firestorm.voice.VoiceClientStatusObserver
import com.firestorm.voice.StatusType
import com.firestorm.media.MediaCtrl
import com.firestorm.media.PluginClassMedia
import com.firestorm.media.EMediaEvent
import com.firestorm.media.ViewerMediaObserver
import com.firestorm.render.ViewerFetchedTexture
import com.firestorm.render.ImageRaw
import com.firestorm.types.UUID
import com.firestorm.types.LLSD
import com.firestorm.contactsets.LGGContactSets

class PanelPropertiesObserver : AvatarPropertiesObserver() {
    var requester: UUID = UUID.NULL
    var panelProfile: PanelProfileSecondLife? = null

    override fun processProperties(data: Any?, type: EAvatarProcessorType) {
        if (type == EAvatarProcessorType.APT_PROPERTIES) {
            panelProfile?.onAvatarProperties(data as? AvatarData)
        }
    }
}

open class PanelProfileTab : Panel() {
    open fun onOpen(key: LLSD) {}
    open fun resetData() {}
    open fun updateData() {}
    open fun hasUnsavedChanges(): Boolean = false
    open fun commitUnsavedChanges() {}
    open fun setAvatarId(avatarId: UUID) {}
    protected open fun setLoaded() {}
}

abstract class PanelProfilePropertiesProcessorTab : PanelProfileTab() {
    abstract fun processProperties(data: Any?, type: EAvatarProcessorType)
}

class PanelProfileSecondLife :
    PanelProfilePropertiesProcessorTab(),
    FriendObserver,
    VoiceClientStatusObserver {

    private val groups: MutableMap<String, UUID> = mutableMapOf()
    private val profileContactSets: MutableSet<String> = mutableSetOf()

    private var statusText: TextBox? = null
    private var groupList: GroupList? = null
    private var showInSearchCheckbox: CheckBoxCtrl? = null
    private var hideAgeCheckbox: CheckBoxCtrl? = null
    private var secondLifePic: TextureCtrl? = null
    private var secondLifePicLayout: Panel? = null
    private var descriptionEdit: TextEditor? = null
    private var saveDescriptionChanges: Button? = null
    private var discardDescriptionChanges: Button? = null
    private var canSeeOnlineIcon: IconCtrl? = null
    private var cantSeeOnlineIcon: IconCtrl? = null
    private var canSeeOnMapIcon: IconCtrl? = null
    private var cantSeeOnMapIcon: IconCtrl? = null
    private var canEditObjectsIcon: IconCtrl? = null
    private var cantEditObjectsIcon: IconCtrl? = null
    private var copyMenuButton: MenuButton? = null
    private var groupInviteButton: Button? = null
    private var displayNameButton: Button? = null
    private var imageActionMenuButton: MenuButton? = null
    private var teleportButton: Button? = null
    private var showOnMapButton: Button? = null
    private var blockButton: Button? = null
    private var unblockButton: Button? = null
    private var addFriendButton: Button? = null
    private var removeFriendButton: Button? = null
    private var payButton: Button? = null
    private var imButton: Button? = null
    private var overflowButton: MenuButton? = null
    private var previewButton: Button? = null

    private var floaterPermissionsHandle: FloaterHandle = FloaterHandle.DEAD
    private var floaterProfileTextureHandle: FloaterHandle = FloaterHandle.DEAD
    private var floaterTexturePickerHandle: FloaterHandle = FloaterHandle.DEAD

    private var hasUnsavedDescriptionChanges: Boolean = false
    private var voiceStatus: Boolean = false
    private var waitingForImageUpload: Boolean = false
    private var allowPublish: Boolean = false
    private var preview: Boolean = false
    private var hideAge: Boolean = false
    var allowEdit: Boolean = false
    private var descriptionText: String = ""
    private var originalDescriptionText: String = ""
    private var imageId: UUID = UUID.NULL

    private var avatarNameCacheConnection: Any? = null
    private var menuNameCacheConnection: Any? = null
    private var rlvBehaviorCallbackConnection: Any? = null

    private val propertiesObserver = PanelPropertiesObserver().also { it.panelProfile = this }

    override fun onOpen(key: LLSD) {
        super.onOpen(key)
        updateData()
    }

    override fun postBuild(): Boolean {
        statusText = getChild("status_text")
        groupList = getChild("group_list")
        showInSearchCheckbox = getChild("show_in_search_chk")
        hideAgeCheckbox = getChild("hide_age_chk")
        secondLifePic = getChild("sl_profile_pic")
        secondLifePicLayout = getChild("sl_pic_layout")
        descriptionEdit = getChild("sl_description_edit")
        saveDescriptionChanges = getChild<Button>("save_description_btn").also { btn ->
            btn.setClickedCallback { onSaveDescriptionChanges() }
        }
        discardDescriptionChanges = getChild<Button>("discard_description_btn").also { btn ->
            btn.setClickedCallback { onDiscardDescriptionChanges() }
        }
        canSeeOnlineIcon = getChild("can_see_online_icon")
        cantSeeOnlineIcon = getChild("cant_see_online_icon")
        canSeeOnMapIcon = getChild("can_see_on_map_icon")
        cantSeeOnMapIcon = getChild("cant_see_on_map_icon")
        canEditObjectsIcon = getChild("can_edit_objects_icon")
        cantEditObjectsIcon = getChild("cant_edit_objects_icon")
        copyMenuButton = getChild("copy_menu_btn")
        groupInviteButton = getChild("group_invite_btn")
        displayNameButton = getChild("display_name_btn")
        imageActionMenuButton = getChild("image_action_menu_btn")
        teleportButton = getChild("teleport_btn")
        showOnMapButton = getChild("show_on_map_btn")
        blockButton = getChild("block_btn")
        unblockButton = getChild("unblock_btn")
        addFriendButton = getChild("add_friend_btn")
        removeFriendButton = getChild("remove_friend_btn")
        payButton = getChild("pay_btn")
        imButton = getChild("im_btn")
        overflowButton = getChild("overflow_btn")
        previewButton = getChild("preview_btn")

        showInSearchCheckbox?.setCommitCallback { onShowInSearchCallback() }
        hideAgeCheckbox?.setCommitCallback { onHideAgeCallback() }
        secondLifePic?.let { pic ->
            pic.setCommitCallback { onSecondLifePicChanged() }
        }
        descriptionEdit?.setKeystrokeCallback { onSetDescriptionDirty() }
        previewButton?.setClickedCallback { System.err.println("PanelProfileSecondLife: toggle description preview not yet implemented") }

        System.err.println("PanelProfileSecondLife: postBuild remaining callbacks not yet implemented")
        return true
    }

    override fun resetData() {
        groups.clear()
        profileContactSets.clear()
        descriptionText = ""
        imageId = UUID.NULL
        groupList?.clear()
        System.err.println("PanelProfileSecondLife: resetData clear UI fields not yet implemented")
    }

    override fun setAvatarId(avatarId: UUID) {
        super.setAvatarId(avatarId)
        propertiesObserver.requester = avatarId
        System.err.println("PanelProfileSecondLife: setAvatarId observer registration not yet implemented")
    }

    override fun updateData() {
        System.err.println("PanelProfileSecondLife: updateData not yet implemented")
    }

    fun refreshName() {
        System.err.println("PanelProfileSecondLife: refreshName not yet implemented")
    }

    fun apply(data: AvatarData) {
        System.err.println("PanelProfileSecondLife: apply not yet implemented")
    }

    fun onAvatarNameCache(agentId: UUID, avName: AvatarName) {
        System.err.println("PanelProfileSecondLife: onAvatarNameCache not yet implemented")
    }

    fun setProfileImageUploading(loading: Boolean) {
        waitingForImageUpload = loading
        System.err.println("PanelProfileSecondLife: setProfileImageUploading not yet implemented")
    }

    fun setProfileImageUploaded(imageAssetId: UUID) {
        imageId = imageAssetId
        setProfileImageUploading(false)
        System.err.println("PanelProfileSecondLife: setProfileImageUploaded not yet implemented")
    }

    override fun hasUnsavedChanges(): Boolean = hasUnsavedDescriptionChanges

    override fun commitUnsavedChanges() {
        if (hasUnsavedDescriptionChanges) onSaveDescriptionChanges()
    }

    override fun processProperties(data: Any?, type: EAvatarProcessorType) {
        if (type == EAvatarProcessorType.APT_PROPERTIES) {
            processProfileProperties(data as? AvatarData ?: return)
        } else if (type == EAvatarProcessorType.APT_GROUPS) {
            processGroupProperties(data as? AvatarGroups ?: return)
        }
    }

    fun onAvatarProperties(data: AvatarData?) {
        data ?: return
        processProfileProperties(data)
    }

    protected fun processProfileProperties(avatarData: AvatarData) {
        fillCommonData(avatarData)
        fillPartnerData(avatarData)
        fillAccountStatus(avatarData)
        fillRightsData()
        fillAgeData(avatarData)
        updateOnlineStatus()
        setLoaded()
    }

    protected fun processGroupProperties(avatarGroups: AvatarGroups) {
        groups.clear()
        avatarGroups.groups.forEach { g -> groups[g.name] = g.id }
        refreshGroupAndContactSetList()
    }

    protected fun refreshGroupAndContactSetList() {
        System.err.println("PanelProfileSecondLife: refreshGroupAndContactSetList not yet implemented")
    }

    protected fun fillCommonData(avatarData: AvatarData) {
        descriptionText = avatarData.aboutText
        setDescriptionText(descriptionText)
        imageId = avatarData.imageId
        secondLifePic?.setImageAssetID(imageId)
        showInSearchCheckbox?.setValue(avatarData.allowPublish)
        allowPublish = avatarData.allowPublish
    }

    protected fun fillPartnerData(avatarData: AvatarData) {
        System.err.println("PanelProfileSecondLife: fillPartnerData not yet implemented")
    }

    protected fun fillAccountStatus(avatarData: AvatarData) {
        System.err.println("PanelProfileSecondLife: fillAccountStatus not yet implemented")
    }

    protected fun fillRightsData() {
        System.err.println("PanelProfileSecondLife: fillRightsData not yet implemented")
    }

    protected fun fillAgeData(avatarData: AvatarData) {
        System.err.println("PanelProfileSecondLife: fillAgeData not yet implemented")
    }

    protected fun onImageLoaded(success: Boolean, imagep: ViewerFetchedTexture?) {
        System.err.println("PanelProfileSecondLife: onImageLoaded not yet implemented")
    }

    protected fun updateOnlineStatus() {
        System.err.println("PanelProfileSecondLife: updateOnlineStatus not yet implemented")
    }

    protected fun processOnlineStatus(isFriend: Boolean, showOnline: Boolean, online: Boolean) {
        System.err.println("PanelProfileSecondLife: processOnlineStatus not yet implemented")
    }

    override fun setLoaded() {
        System.err.println("PanelProfileSecondLife: setLoaded not yet implemented")
    }

    override fun changed(mask: UInt) {
        if ((mask and (FriendObserver.ADD or FriendObserver.REMOVE or FriendObserver.POWERS)) != 0u) {
            fillRightsData()
            updateButtons()
        }
    }

    override fun onChange(status: StatusType, channelInfo: LLSD, proximal: Boolean) {
        voiceStatus = status != StatusType.STATUS_JOINING && status != StatusType.STATUS_LEFT_CHANNEL
        updateButtons()
    }

    private fun setDescriptionText(text: String) {
        descriptionText = text
        descriptionEdit?.setText(text)
    }

    private fun reparseDescriptionText(text: String) {
        System.err.println("PanelProfileSecondLife: reparseDescriptionText not yet implemented")
    }

    private fun onSetDescriptionDirty() {
        hasUnsavedDescriptionChanges = true
        saveDescriptionChanges?.setEnabled(true)
        discardDescriptionChanges?.setEnabled(true)
    }

    private fun onShowInSearchCallback() {
        allowPublish = showInSearchCheckbox?.getValue()?.asBoolean() ?: false
    }

    private fun onHideAgeCallback() {
        hideAge = hideAgeCheckbox?.getValue()?.asBoolean() ?: false
    }

    private fun onSaveDescriptionChanges() {
        hasUnsavedDescriptionChanges = false
        apply(buildAvatarDataFromUI())
        saveDescriptionChanges?.setEnabled(false)
        discardDescriptionChanges?.setEnabled(false)
    }

    private fun onDiscardDescriptionChanges() {
        hasUnsavedDescriptionChanges = false
        setDescriptionText(descriptionText)
        saveDescriptionChanges?.setEnabled(false)
        discardDescriptionChanges?.setEnabled(false)
    }

    private fun onShowAgentPermissionsDialog() {
        System.err.println("PanelProfileSecondLife: onShowAgentPermissionsDialog not yet implemented")
    }

    private fun onShowAgentProfileTexture() {
        System.err.println("PanelProfileSecondLife: onShowAgentProfileTexture not yet implemented")
    }

    private fun onShowTexturePicker() {
        System.err.println("PanelProfileSecondLife: onShowTexturePicker not yet implemented")
    }

    private fun onSecondLifePicChanged() {
        val newId = secondLifePic?.imageAssetID ?: return
        onCommitProfileImage(newId)
    }

    private fun onCommitProfileImage(id: UUID) {
        imageId = id
        System.err.println("PanelProfileSecondLife: onCommitProfileImage not yet implemented")
    }

    private fun updateButtons() {
        System.err.println("PanelProfileSecondLife: updateButtons not yet implemented")
    }

    private fun setBadge(iconName: String, tooltip: String, location: BadgeLocation) {
        System.err.println("PanelProfileSecondLife: setBadge not yet implemented")
    }

    private fun buildAvatarDataFromUI(): AvatarData {
        System.err.println("PanelProfileSecondLife: buildAvatarDataFromUI not yet implemented")
        return AvatarData()
    }

    private fun updateRlvRestrictions(behavior: Any) {
        System.err.println("PanelProfileSecondLife: updateRlvRestrictions not yet implemented")
    }

    private fun openGroupProfile() {
        System.err.println("PanelProfileSecondLife: openGroupProfile not yet implemented")
    }

    private fun onCommitMenu(userdata: LLSD) {
        System.err.println("PanelProfileSecondLife: onCommitMenu not yet implemented")
    }

    private fun onEnableMenu(userdata: LLSD): Boolean {
        return false
    }

    private fun onCheckMenu(userdata: LLSD): Boolean {
        return false
    }

    private fun onAvatarNameCacheSetName(id: UUID, avName: AvatarName) {
        System.err.println("PanelProfileSecondLife: onAvatarNameCacheSetName not yet implemented")
    }

    private enum class BadgeLocation { TOP, BOTTOM }
}

class PanelProfileWeb : PanelProfileTab(), ViewerMediaObserver {

    private var urlHome: String = ""
    private var urlWebProfile: String = ""
    private var webBrowser: MediaCtrl? = null
    private var firstNavigate: Boolean = true
    private var avatarNameCacheConnection: Any? = null

    override fun onOpen(key: LLSD) {
        updateData()
    }

    override fun postBuild(): Boolean {
        webBrowser = getChild("profile_html")
        webBrowser?.addObserver(this)
        webBrowser?.setCommitCallback { ctrl -> onCommitLoad(ctrl) }
        return true
    }

    override fun resetData() {
        urlHome = ""
        urlWebProfile = ""
        webBrowser?.navigateTo("about:blank")
    }

    override fun updateData() {
        System.err.println("PanelProfileWeb: updateData not yet implemented")
    }

    fun apply(data: AvatarData) {
        System.err.println("PanelProfileWeb: apply not yet implemented")
    }

    override fun handleMediaEvent(self: PluginClassMedia, event: EMediaEvent) {
        System.err.println("PanelProfileWeb: handleMediaEvent not yet implemented")
    }

    fun onAvatarNameCache(agentId: UUID, avName: AvatarName) {
        System.err.println("PanelProfileWeb: onAvatarNameCache not yet implemented")
    }

    private fun onCommitLoad(ctrl: Any) {
        System.err.println("PanelProfileWeb: onCommitLoad not yet implemented")
    }
}

class PanelProfileFirstLife : PanelProfilePropertiesProcessorTab() {

    protected var descriptionEdit: TextEditor? = null
    protected var picture: TextureCtrl? = null
    protected var uploadPhoto: Button? = null
    protected var changePhoto: Button? = null
    protected var removePhoto: Button? = null
    protected var saveChanges: Button? = null
    protected var discardChanges: Button? = null
    protected var previewButton: Button? = null
    protected var floaterTexturePickerHandle: FloaterHandle = FloaterHandle.DEAD

    protected var currentDescription: String = ""
    protected var imageId: UUID = UUID.NULL
    protected var hasUnsavedChanges: Boolean = false
    private var preview: Boolean = false
    private var originalDescription: String = ""

    override fun onOpen(key: LLSD) {}

    override fun postBuild(): Boolean {
        descriptionEdit = getChild("fl_description_edit")
        picture = getChild("fl_profile_pic")
        uploadPhoto = getChild<Button>("upload_photo_btn").also { it.setClickedCallback { onUploadPhoto() } }
        changePhoto = getChild<Button>("change_photo_btn").also { it.setClickedCallback { onChangePhoto() } }
        removePhoto = getChild<Button>("remove_photo_btn").also { it.setClickedCallback { onRemovePhoto() } }
        saveChanges = getChild<Button>("save_changes_btn").also { it.setClickedCallback { onSaveDescriptionChanges() } }
        discardChanges = getChild<Button>("discard_changes_btn").also { it.setClickedCallback { onDiscardDescriptionChanges() } }
        previewButton = getChild<Button>("preview_btn").also { it.setClickedCallback { onClickPreview() } }
        descriptionEdit?.setKeystrokeCallback { onSetDescriptionDirty() }
        picture?.setCommitCallback { onFirstLifePicChanged() }
        return true
    }

    override fun processProperties(data: Any?, type: EAvatarProcessorType) {
        if (type == EAvatarProcessorType.APT_PROPERTIES) {
            processProperties(data as? AvatarData ?: return)
        }
    }

    fun processProperties(avatarData: AvatarData) {
        currentDescription = avatarData.flAboutText
        setDescriptionText(currentDescription)
        imageId = avatarData.flImageId
        picture?.setImageAssetID(imageId)
        setLoaded()
    }

    fun apply(data: AvatarData) {
        System.err.println("PanelProfileFirstLife: apply not yet implemented")
    }

    override fun resetData() {
        currentDescription = ""
        imageId = UUID.NULL
        descriptionEdit?.setText("")
        picture?.setImageAssetID(UUID.NULL)
    }

    fun setProfileImageUploading(loading: Boolean) {
        System.err.println("PanelProfileFirstLife: setProfileImageUploading not yet implemented")
    }

    fun setProfileImageUploaded(imageAssetId: UUID) {
        imageId = imageAssetId
        setProfileImageUploading(false)
        picture?.setImageAssetID(imageId)
    }

    override fun hasUnsavedChanges(): Boolean = hasUnsavedChanges

    override fun commitUnsavedChanges() {
        if (hasUnsavedChanges) onSaveDescriptionChanges()
    }

    override fun setLoaded() {
        System.err.println("PanelProfileFirstLife: setLoaded not yet implemented")
    }

    protected fun onUploadPhoto() {
        System.err.println("PanelProfileFirstLife: onUploadPhoto not yet implemented")
    }

    protected fun onChangePhoto() {
        System.err.println("PanelProfileFirstLife: onChangePhoto not yet implemented")
    }

    protected fun onRemovePhoto() {
        imageId = UUID.NULL
        picture?.setImageAssetID(UUID.NULL)
        apply(buildAvatarDataFromUI())
    }

    protected fun onFirstLifePicChanged() {
        imageId = picture?.imageAssetID ?: UUID.NULL
        onCommitPhoto(imageId)
    }

    protected fun onCommitPhoto(id: UUID) {
        System.err.println("PanelProfileFirstLife: onCommitPhoto not yet implemented")
    }

    protected fun setDescriptionText(text: String) {
        currentDescription = text
        descriptionEdit?.setText(text)
    }

    protected fun reparseDescriptionText(text: String) {
        System.err.println("PanelProfileFirstLife: reparseDescriptionText not yet implemented")
    }

    protected fun onSetDescriptionDirty() {
        hasUnsavedChanges = true
        saveChanges?.setEnabled(true)
        discardChanges?.setEnabled(true)
    }

    protected fun onSaveDescriptionChanges() {
        hasUnsavedChanges = false
        apply(buildAvatarDataFromUI())
        saveChanges?.setEnabled(false)
        discardChanges?.setEnabled(false)
    }

    protected fun onDiscardDescriptionChanges() {
        hasUnsavedChanges = false
        setDescriptionText(currentDescription)
        saveChanges?.setEnabled(false)
        discardChanges?.setEnabled(false)
    }

    protected fun onClickPreview() {
        preview = !preview
        System.err.println("PanelProfileFirstLife: onClickPreview toggle preview not yet implemented")
    }

    private fun buildAvatarDataFromUI(): AvatarData {
        System.err.println("PanelProfileFirstLife: buildAvatarDataFromUI not yet implemented")
        return AvatarData()
    }
}

class PanelProfileNotes : PanelProfilePropertiesProcessorTab() {

    private var notesEditor: TextEditor? = null
    private var saveChanges: Button? = null
    private var discardChanges: Button? = null
    private var currentNotes: String = ""
    private var hasUnsavedChanges: Boolean = false

    override fun onOpen(key: LLSD) {}

    override fun postBuild(): Boolean {
        notesEditor = getChild("notes_edit")
        saveChanges = getChild<Button>("save_notes_btn").also { it.setClickedCallback { onSaveNotesChanges() } }
        discardChanges = getChild<Button>("discard_notes_btn").also { it.setClickedCallback { onDiscardNotesChanges() } }
        notesEditor?.setKeystrokeCallback { onSetNotesDirty() }
        return true
    }

    override fun processProperties(data: Any?, type: EAvatarProcessorType) {
        if (type == EAvatarProcessorType.APT_PROPERTIES) {
            processProperties(data as? AvatarData ?: return)
        }
    }

    fun processProperties(avatarData: AvatarData) {
        currentNotes = avatarData.notes
        setNotesText(currentNotes)
    }

    override fun resetData() {
        currentNotes = ""
        notesEditor?.setText("")
    }

    override fun updateData() {
        System.err.println("PanelProfileNotes: updateData not yet implemented")
    }

    override fun hasUnsavedChanges(): Boolean = hasUnsavedChanges

    override fun commitUnsavedChanges() {
        if (hasUnsavedChanges) onSaveNotesChanges()
    }

    protected fun setNotesText(text: String) {
        currentNotes = text
        notesEditor?.setText(text)
    }

    protected fun onSetNotesDirty() {
        hasUnsavedChanges = true
        saveChanges?.setEnabled(true)
        discardChanges?.setEnabled(true)
    }

    protected fun onSaveNotesChanges() {
        hasUnsavedChanges = false
        System.err.println("PanelProfileNotes: onSaveNotesChanges send to server not yet implemented")
        saveChanges?.setEnabled(false)
        discardChanges?.setEnabled(false)
    }

    protected fun onDiscardNotesChanges() {
        hasUnsavedChanges = false
        setNotesText(currentNotes)
        saveChanges?.setEnabled(false)
        discardChanges?.setEnabled(false)
    }
}

class PanelProfile : PanelProfileTab() {

    private var panelSecondlife: PanelProfileSecondLife? = null
    private var panelWeb: PanelProfileWeb? = null
    private var panelPicks: Panel? = null
    private var panelClassifieds: Panel? = null
    private var panelFirstlife: PanelProfileFirstLife? = null
    private var panelNotes: PanelProfileNotes? = null
    private var tabContainer: TabContainer? = null

    private var avatarData: AvatarData = AvatarData()

    override fun postBuild(): Boolean {
        panelSecondlife = findChild("panel_profile_secondlife")
        panelWeb = findChild("panel_profile_web")
        panelPicks = findChild("panel_profile_picks")
        panelClassifieds = findChild("panel_profile_classifieds")
        panelFirstlife = findChild("panel_profile_firstlife")
        panelNotes = findChild("panel_profile_notes")
        tabContainer = getChild<TabContainer>("profile_tabs").also { tabs ->
            tabs.setCommitCallback { onTabChange() }
        }
        return true
    }

    override fun updateData() {
        panelSecondlife?.updateData()
        panelWeb?.updateData()
    }

    fun refreshName() {
        panelSecondlife?.refreshName()
    }

    override fun onOpen(key: LLSD) {
        panelSecondlife?.onOpen(key)
        panelWeb?.onOpen(key)
        panelPicks?.let { System.err.println("PanelProfile: open picks panel not yet implemented") }
        panelClassifieds?.let { System.err.println("PanelProfile: open classifieds panel not yet implemented") }
        panelFirstlife?.onOpen(key)
        panelNotes?.onOpen(key)
    }

    fun createPick(data: Any) {
        System.err.println("PanelProfile: createPick not yet implemented")
    }

    fun showPick(pickId: UUID = UUID.NULL) {
        System.err.println("PanelProfile: showPick not yet implemented")
    }

    fun isPickTabSelected(): Boolean {
        return false
    }

    fun isNotesTabSelected(): Boolean {
        return false
    }

    override fun hasUnsavedChanges(): Boolean {
        return panelSecondlife?.hasUnsavedChanges() == true
            || panelFirstlife?.hasUnsavedChanges() == true
            || panelNotes?.hasUnsavedChanges() == true
    }

    fun hasUnpublishedClassifieds(): Boolean {
        return false
    }

    override fun commitUnsavedChanges() {
        panelSecondlife?.commitUnsavedChanges()
        panelFirstlife?.commitUnsavedChanges()
        panelNotes?.commitUnsavedChanges()
    }

    fun showClassified(classifiedId: UUID = UUID.NULL, edit: Boolean = false) {
        System.err.println("PanelProfile: showClassified not yet implemented")
    }

    fun createClassified() {
        System.err.println("PanelProfile: createClassified not yet implemented")
    }

    fun getAvatarData(): AvatarData = avatarData
    fun setAvatarData(avatarData: AvatarData) { this.avatarData = avatarData }

    private fun onTabChange() {
        System.err.println("PanelProfile: onTabChange not yet implemented")
    }
}

fun postProfileImage(
    capUrl: String,
    firstData: LLSD,
    pathToImage: String,
    handle: Any,
): UUID {
    System.err.println("postProfileImage: not yet implemented")
    return UUID.NULL
}

enum class ProfileImageType { PROFILE_IMAGE_SL, PROFILE_IMAGE_FL }

fun postProfileImageCoro(
    capUrl: String,
    type: ProfileImageType,
    pathToImage: String,
    handle: Any,
) {
    System.err.println("postProfileImageCoro: not yet implemented")
}
