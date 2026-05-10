package com.firestorm.newview

import com.firestorm.ui.FriendObserver
import com.firestorm.ui.LLAvatarName
import com.firestorm.ui.LLAvatarNameCache
import com.firestorm.ui.LLAvatarPropertiesProcessor
import com.firestorm.ui.LLAvatarTracker
import com.firestorm.ui.LLColor4
import com.firestorm.ui.LLOutputMonitorCtrl
import com.firestorm.ui.LLPanel
import com.firestorm.ui.LLRelationship
import com.firestorm.ui.LLResMgr
import com.firestorm.ui.LLSD
import com.firestorm.ui.LLTextBox
import com.firestorm.ui.LLUIColorTable
import com.firestorm.ui.LLVoiceClient
import com.firestorm.ui.RlvActions
import com.firestorm.ui.gSavedSettings
import com.firestorm.lggcontactsets.LGGContactSets
import com.firestorm.lggcontactsets.ContactSetType
import java.util.UUID

enum class ItemState {
    IS_DEFAULT, IS_VOICE_INVITED, IS_VOICE_JOINED, IS_VOICE_LEFT,
    IS_ONLINE, IS_OFFLINE, IS_GROUPMOD
}

private enum class OnlineStatus { OFFLINE, ONLINE, UNKNOWN }

enum class AvatarListItemChildIndex {
    ALIC_SPEAKER_INDICATOR,
    ALIC_PROFILE_BUTTON,
    ALIC_INFO_BUTTON,
    ALIC_VOLUME_SLIDER,
    ALIC_PERMISSION_ONLINE,
    ALIC_PERMISSION_MAP,
    ALIC_PERMISSION_EDIT_MINE,
    ALIC_PERMISSION_EDIT_THEIRS,
    ALIC_INTERACTION_TIME,
    ALIC_NAME,
    ALIC_ICON;
}

open class AvatarListItem(notFromUiFactory: Boolean = true) : LLPanel(), FriendObserver {

    // UI children – resolved after postBuild
    protected var speakingIndicator: LLOutputMonitorCtrl? = null
    protected var avatarIcon: AvatarIconCtrl? = null
    private var avatarNameBox: LLTextBox? = null
    private var lastInteractionTimeBox: LLTextBox? = null
    protected var btnPermissionOnline: com.firestorm.ui.LLButton? = null
    protected var btnPermissionMap: com.firestorm.ui.LLButton? = null
    protected var btnPermissionEditMine: com.firestorm.ui.LLButton? = null
    protected var iconPermissionEditTheirs: com.firestorm.ui.LLIconCtrl? = null
    private var infoBtn: com.firestorm.ui.LLButton? = null
    private var profileBtn: com.firestorm.ui.LLButton? = null
    private var voiceSlider: com.firestorm.ui.LLUICtrl? = null

    protected var showDisplayName: Boolean = true
    protected var showUsername: Boolean = true

    private var avatarId: UUID = NULL_UUID
    private var highlightSubstring: String = ""
    private var onlineStatus: OnlineStatus = OnlineStatus.UNKNOWN
    private var showInfoBtn: Boolean = true
    private var showVoiceVolume: Boolean = false
    private var showProfileBtn: Boolean = true
    private var rlvCheckShowNames: Boolean = false
    var userName: String = ""
        private set
    private var showPermissions: Boolean = false
    private var hovered: Boolean = false
    var showCompleteName: Boolean = false
        private set
    var forceCompleteName: Boolean = false
        private set
    private var useContactSetColors: Boolean = false
    private var useContactSetListStyle: Boolean = false
    private var greyOutUsername: String = ""

    private var avatarNameCacheConnection: AutoCloseable? = null
    private var voiceLevelChangeCallbackConnection: AutoCloseable? = null
    private var avatarNameStyle: Any? = null  // LLStyle.Params equivalent

    companion object {
        private val NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

        var staticInitialized: Boolean = false
            private set
        var leftPadding: Int = 0
            private set
        var nameRightPadding: Int = 0
            private set
        val childrenWidths: IntArray = IntArray(AvatarListItemChildIndex.values().size)

        private val itemIconColorMap: MutableMap<ItemState, LLColor4> by lazy {
            mutableMapOf(
                ItemState.IS_DEFAULT to LLUIColorTable.instance().getColor("AvatarListItemIconDefaultColor", LLColor4.white),
                ItemState.IS_VOICE_INVITED to LLUIColorTable.instance().getColor("AvatarListItemIconVoiceInvitedColor", LLColor4.white),
                ItemState.IS_VOICE_JOINED to LLUIColorTable.instance().getColor("AvatarListItemIconVoiceJoinedColor", LLColor4.white),
                ItemState.IS_VOICE_LEFT to LLUIColorTable.instance().getColor("AvatarListItemIconVoiceLeftColor", LLColor4.white),
                ItemState.IS_ONLINE to LLUIColorTable.instance().getColor("AvatarListItemIconOnlineColor", LLColor4.white),
                ItemState.IS_OFFLINE to LLUIColorTable.instance().getColor("AvatarListItemIconOfflineColor", LLColor4.white),
                ItemState.IS_GROUPMOD to LLUIColorTable.instance().getColor("AvatarListItemIconOnlineColor", LLColor4.white),
            )
        }

        private fun initChildrenWidths(self: AvatarListItem) {
            TODO("GPU: compute child widget widths from layout rects")
        }
    }

    init {
        voiceLevelChangeCallbackConnection = LLVoiceClient.setUserVolumeUpdateCallback { id ->
            onUserVoiceLevelChange(id)
        }
        if (notFromUiFactory) {
            buildFromFile("panel_avatar_list_item.xml")
        }
    }

    open fun postBuild(): Boolean {
        avatarIcon = getChild("avatar_icon")
        avatarNameBox = getChild("avatar_name")
        lastInteractionTimeBox = getChild("last_interaction")
        btnPermissionOnline = getChild("permission_online_btn")
        btnPermissionMap = getChild("permission_map_btn")
        btnPermissionEditMine = getChild("permission_edit_mine_btn")
        iconPermissionEditTheirs = getChild("permission_edit_theirs_icon")
        btnPermissionOnline?.apply { setClickedCallback { onPermissionOnlineClick() }; setVisible(false); setIsChrome(true) }
        btnPermissionMap?.apply { setClickedCallback { onPermissionMapClick() }; setVisible(false); setIsChrome(true) }
        btnPermissionEditMine?.apply { setClickedCallback { onPermissionEditMineClick() }; setVisible(false); setIsChrome(true) }
        iconPermissionEditTheirs?.setVisible(false)
        speakingIndicator = getChild("speaking_indicator")
        speakingIndicator?.setChannelState(LLOutputMonitorCtrl.UNDEFINED_CHANNEL)
        infoBtn = getChild<com.firestorm.ui.LLButton>("info_btn").apply {
            setVisible(false); setClickedCallback { onInfoBtnClick() }
        }
        voiceSlider = getChild<com.firestorm.ui.LLUICtrl>("volume_slider").apply {
            setVisible(false); setCommitCallback { data -> onVolumeChange(data) }
        }
        profileBtn = getChild<com.firestorm.ui.LLButton>("profile_btn").apply {
            setVisible(false); setClickedCallback { onProfileBtnClick() }
        }
        if (!staticInitialized) {
            initChildrenWidths(this)
            nameRightPadding = 0 // from default params
            staticInitialized = true
        }
        return true
    }

    fun onVolumeChange(data: LLSD) {
        val volume = data.asReal().toFloat()
        LLVoiceClient.getInstance().setUserVolume(avatarId, volume)
    }

    open fun onVisibilityChange(newVisibility: Boolean) {
        if (newVisibility && speakingIndicator?.getIndicatorToggled() == true) {
            updateChildren()
            speakingIndicator?.setIndicatorToggled(false)
        }
    }

    private fun fetchAvatarName() {
        if (avatarId == NULL_UUID) return
        avatarNameCacheConnection?.close()
        avatarNameCacheConnection = LLAvatarNameCache.get(avatarId) { _, avName ->
            onAvatarNameCache(avName)
        }
    }

    open fun notifyParent(info: LLSD): Int {
        if (info.has("visibility_changed")) {
            updateChildren()
            return 1
        }
        return super.notifyParent(info)
    }

    open fun onMouseEnter(x: Int, y: Int, mask: Int) {
        getChildView("hovered_icon")?.setVisible(true)
        hovered = true
        super.onMouseEnter(x, y, mask)
    }

    open fun onMouseLeave(x: Int, y: Int, mask: Int) {
        getChildView("hovered_icon")?.setVisible(false)
        hovered = false
        super.onMouseLeave(x, y, mask)
    }

    override fun changed(mask: UInt) {
        setOnline(LLAvatarTracker.instance().isBuddyOnline(avatarId))
        val powers = FriendObserver.POWERS.toUInt()
        val perms = FriendObserver.PERMS.toUInt()
        if ((mask and powers != 0u) || (mask and perms != 0u)) {
            showPermissions(showPermissions && gSavedSettings.getBOOL("FriendsListShowPermissions"))
            updateChildren()
        }
    }

    fun setOnline(online: Boolean) {
        if (onlineStatus != OnlineStatus.UNKNOWN && (onlineStatus == OnlineStatus.ONLINE) == online) return
        onlineStatus = if (online) OnlineStatus.ONLINE else OnlineStatus.OFFLINE
        setState(if (online) ItemState.IS_ONLINE else ItemState.IS_OFFLINE)
    }

    fun setAvatarName(name: String) {
        setNameInternal(name, highlightSubstring)
    }

    fun setUseContactSetColors(useColors: Boolean) {
        useContactSetColors = useColors
        setNameInternal(avatarNameBox?.getText() ?: "", highlightSubstring)
    }

    fun setUseContactSetListStyle(useStyle: Boolean) {
        useContactSetListStyle = useStyle
        setNameInternal(avatarNameBox?.getText() ?: "", highlightSubstring)
    }

    fun setAvatarToolTip(tooltip: String) {
        avatarNameBox?.setToolTip(tooltip)
    }

    fun setHighlight(highlight: String) {
        highlightSubstring = highlight
        setNameInternal(avatarNameBox?.getText() ?: "", highlightSubstring)
    }

    fun setState(itemStyle: ItemState) {
        avatarNameStyle = when (itemStyle) {
            ItemState.IS_DEFAULT -> defaultStyle()
            ItemState.IS_VOICE_INVITED -> voiceCallInvitedStyle()
            ItemState.IS_VOICE_JOINED -> voiceCallJoinedStyle()
            ItemState.IS_VOICE_LEFT -> voiceCallLeftStyle()
            ItemState.IS_ONLINE -> onlineStyle()
            ItemState.IS_OFFLINE -> offlineStyle()
            ItemState.IS_GROUPMOD -> groupModeratorStyle()
        }
        setNameInternal(avatarNameBox?.getText() ?: "", highlightSubstring)
        avatarIcon?.setColor(itemIconColorMap[itemStyle] ?: LLColor4.white)
    }

    fun setAvatarId(id: UUID, sessionId: UUID, ignoreStatusChanges: Boolean = false, isResident: Boolean = true) {
        if (avatarId != NULL_UUID) {
            LLAvatarTracker.instance().removeParticularFriendObserver(avatarId, this)
            LLAvatarTracker.instance().removeFriendPermissionObserver(avatarId, this)
        }
        avatarId = id
        speakingIndicator?.setSpeakerId(id, sessionId)
        updateVoiceLevelSlider()
        if (!ignoreStatusChanges && avatarId != NULL_UUID) {
            LLAvatarTracker.instance().addParticularFriendObserver(avatarId, this)
        }
        if (avatarId != NULL_UUID) {
            LLAvatarTracker.instance().addFriendPermissionObserver(avatarId, this)
        }
        if (isResident) {
            avatarIcon?.setValue(LLSD.fromUUID(id))
            fetchAvatarName()
        }
        showPermissions(showPermissions && gSavedSettings.getBOOL("FriendsListShowPermissions"))
        updateChildren()
    }

    private fun onUserVoiceLevelChange(id: UUID) {
        if (id == avatarId) updateVoiceLevelSlider()
    }

    private fun updateVoiceLevelSlider() {
        val slider = voiceSlider ?: return
        if (!slider.getVisible()) return
        if (!LLVoiceClient.getInstance().getVoiceEnabled(avatarId)) return
        val isMuted = AvatarActions.isVoiceMuted(avatarId)
        val volume = if (isMuted) 0f else LLVoiceClient.getInstance().getUserVolume(avatarId)
        slider.setValue(LLSD.fromReal(volume.toDouble()))
    }

    fun showLastInteractionTime(show: Boolean) {
        lastInteractionTimeBox?.setVisible(show)
        updateChildren()
    }

    fun setLastInteractionTime(secsSince: UInt) {
        lastInteractionTimeBox?.setValue(formatSeconds(secsSince))
    }

    fun setShowInfoBtn(show: Boolean) {
        showInfoBtn = show
        val canShow = !rlvCheckShowNames || RlvActions.canShowName(RlvActions.SNC_DEFAULT, avatarId)
        infoBtn?.setVisible(showInfoBtn && canShow)
    }

    fun setShowVoiceVolume(show: Boolean) {
        showVoiceVolume = show
        val canShow = !rlvCheckShowNames || RlvActions.canShowName(RlvActions.SNC_DEFAULT, avatarId)
        voiceSlider?.setVisible(showVoiceVolume && canShow)
        if (show) updateVoiceLevelSlider()
    }

    fun setRlvCheckShowNames(rlvCheckShowNames: Boolean) {
        this.rlvCheckShowNames = rlvCheckShowNames
        updateRlvRestrictions()
    }

    fun updateRlvRestrictions() {
        setShowVoiceVolume(showVoiceVolume)
        setShowInfoBtn(showInfoBtn)
    }

    fun setShowProfileBtn(show: Boolean) {
        showProfileBtn = show
    }

    fun showSpeakingIndicator(visible: Boolean) {
        if (speakingIndicator != null && !visible) {
            speakingIndicator?.setIsActiveChannel(visible)
            speakingIndicator?.setShowParticipantsSpeaking(visible)
        }
    }

    fun setAvatarIconVisible(visible: Boolean) {
        if (avatarIcon?.getVisible() == visible) return
        avatarIcon?.setVisible(visible)
        updateChildren()
    }

    fun showDisplayName(show: Boolean, updateName: Boolean = true) {
        showDisplayName = show
        if (updateName) updateAvatarName()
    }

    fun showUsername(show: Boolean, updateName: Boolean = true) {
        showUsername = show
        if (updateName) updateAvatarName()
    }

    fun onInfoBtnClick() {
        TODO("GPU: show inspect_avatar floater for $avatarId")
    }

    fun onProfileBtnClick() {
        AvatarActions.showProfile(avatarId)
    }

    open fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean {
        val info = infoBtn
        if (info != null && info.getVisible() && info.getEnabled() && info.getRect().pointInRect(x, y)) {
            onInfoBtnClick()
            return true
        }
        val profile = profileBtn
        if (profile != null && profile.getVisible() && profile.getEnabled() && profile.getRect().pointInRect(x, y)) {
            onProfileBtnClick()
            return true
        }
        return super.handleDoubleClick(x, y, mask)
    }

    open fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: Int, cargoData: Any?,
        accept: IntArray, tooltipMsg: StringBuilder
    ): Boolean {
        notifyParent(LLSD.mapOf("select" to LLSD.fromUUID(avatarId)))
        return TODO("GPU: handleGiveDragAndDrop")
    }

    open fun setValue(value: LLSD) {
        if (!value.isMap()) return
        if (!value.has("selected")) return
        getChildView("selected_icon")?.setVisible(value["selected"].asBoolean())
    }

    fun getAvatarId(): UUID = avatarId
    fun getAvatarName(): String = avatarNameBox?.getValue() ?: ""
    fun getAvatarToolTip(): String = avatarNameBox?.getToolTip() ?: ""
    fun getShowingBothNames(): Boolean = showDisplayName && showUsername

    fun updateAvatarName() {
        fetchAvatarName()
    }

    fun setShowCompleteName(show: Boolean, force: Boolean = false) {
        showCompleteName = show
        forceCompleteName = force
    }

    fun setShowPermissions(show: Boolean) {
        showPermissions = show
        showPermissions(show)
    }

    private fun setNameInternal(name: String, highlight: String) {
        // Contact-set list style uses bold/online font if available
        val style = buildNameStyle()
        textboxSetHighlightedVal(avatarNameBox, style, name, highlight)
    }

    private fun buildNameStyle(): Any? {
        var style = avatarNameStyle
        if (useContactSetListStyle) {
            if (onlineStatus == OnlineStatus.ONLINE) {
                style = mergeWithGroupModStyle(style)
            }
            style = mergeWithOnlineColor(style)
        }
        if (useContactSetColors) {
            val color = LGGContactSets.getInstance()
                .getFriendColorIfShouldShow(avatarId, ContactSetType.FRIENDS)
            if (color != null) style = applyColor(style, color)
        }
        return style
    }

    private fun onAvatarNameCache(avName: LLAvatarName) {
        avatarNameCacheConnection = null
        setAvatarName(AvatarList.getNameForDisplay(
            avatarId, avName, showDisplayName, showUsername, forceCompleteName, rlvCheckShowNames
        ))
        val canShowName = !rlvCheckShowNames || RlvActions.canShowName(RlvActions.SNC_DEFAULT, avatarId)
        val anonymName = avName.getAnonymName()
        setAvatarToolTip(if (canShowName) avName.getUserName() else anonymName)
        avatarIcon?.setDrawTooltip(canShowName)
        userName = avName.getUserName()
        notifyParent(LLSD.mapOf("sort" to LLSD.emptyMap()))
        updateChildren()
    }

    private fun formatSeconds(secs: UInt): String {
        val min = 60u; val hour = min * 60u; val day = hour * 24u
        val week = day * 7u; val month = day * 30u; val year = day * 365u
        val (fmt, count) = when {
            secs >= year  -> "FormatYears"   to secs / year
            secs >= month -> "FormatMonths"  to secs / month
            secs >= week  -> "FormatWeeks"   to secs / week
            secs >= day   -> "FormatDays"    to secs / day
            secs >= hour  -> "FormatHours"   to secs / hour
            secs >= min   -> "FormatMinutes" to secs / min
            else          -> "FormatSeconds" to secs
        }
        return getString(fmt, mapOf("[COUNT]" to count.toString()))
    }

    private fun showPermissions(visible: Boolean): Boolean {
        val relation = LLAvatarTracker.instance().getBuddyInfo(getAvatarId())
        if (relation != null && visible) {
            fun colorFor(granted: Boolean) =
                if (granted) LLUIColorTable.instance().getColor("White")
                else LLUIColorTable.instance().getColor("White_10")

            btnPermissionOnline?.setColor(colorFor(relation.isRightGrantedTo(LLRelationship.GRANT_ONLINE_STATUS)))
            btnPermissionMap?.setColor(colorFor(relation.isRightGrantedTo(LLRelationship.GRANT_MAP_LOCATION)))
            btnPermissionEditMine?.setColor(colorFor(relation.isRightGrantedTo(LLRelationship.GRANT_MODIFY_OBJECTS)))
            iconPermissionEditTheirs?.setColor(colorFor(relation.isRightGrantedFrom(LLRelationship.GRANT_MODIFY_OBJECTS)))
            btnPermissionOnline?.setVisible(true)
            btnPermissionMap?.setVisible(true)
            btnPermissionEditMine?.setVisible(true)
            iconPermissionEditTheirs?.setVisible(true)
        } else {
            btnPermissionOnline?.setVisible(false)
            btnPermissionMap?.setVisible(false)
            btnPermissionEditMine?.setVisible(false)
            iconPermissionEditTheirs?.setVisible(false)
        }
        updateChildren()
        return relation != null
    }

    fun onPermissionOnlineClick() {
        val relation = LLAvatarTracker.instance().getBuddyInfo(getAvatarId()) ?: return
        val cur = relation.getRightsGrantedTo()
        val newRights: Int
        if (!relation.isRightGrantedTo(LLRelationship.GRANT_ONLINE_STATUS)) {
            newRights = LLRelationship.GRANT_ONLINE_STATUS or
                (cur and LLRelationship.GRANT_MAP_LOCATION) or
                (cur and LLRelationship.GRANT_MODIFY_OBJECTS)
            btnPermissionOnline?.setColor(LLUIColorTable.instance().getColor("White"))
        } else {
            newRights = (cur and LLRelationship.GRANT_MAP_LOCATION) or
                (cur and LLRelationship.GRANT_MODIFY_OBJECTS)
            btnPermissionOnline?.setColor(LLUIColorTable.instance().getColor("White_10"))
        }
        LLAvatarPropertiesProcessor.getInstance().sendFriendRights(getAvatarId(), newRights)
        btnPermissionOnline?.setFocus(false)
    }

    fun onPermissionMapClick() {
        val relation = LLAvatarTracker.instance().getBuddyInfo(getAvatarId()) ?: return
        val cur = relation.getRightsGrantedTo()
        val newRights: Int
        if (!relation.isRightGrantedTo(LLRelationship.GRANT_MAP_LOCATION)) {
            newRights = LLRelationship.GRANT_MAP_LOCATION or
                (cur and LLRelationship.GRANT_ONLINE_STATUS) or
                (cur and LLRelationship.GRANT_MODIFY_OBJECTS)
            btnPermissionMap?.setColor(LLUIColorTable.instance().getColor("White"))
        } else {
            newRights = (cur and LLRelationship.GRANT_ONLINE_STATUS) or
                (cur and LLRelationship.GRANT_MODIFY_OBJECTS)
            btnPermissionMap?.setColor(LLUIColorTable.instance().getColor("White_10"))
        }
        LLAvatarPropertiesProcessor.getInstance().sendFriendRights(getAvatarId(), newRights)
        btnPermissionMap?.setFocus(false)
    }

    fun onPermissionEditMineClick() {
        val relation = LLAvatarTracker.instance().getBuddyInfo(getAvatarId()) ?: return
        val cur = relation.getRightsGrantedTo()
        if (!relation.isRightGrantedTo(LLRelationship.GRANT_MODIFY_OBJECTS)) {
            val newRights = LLRelationship.GRANT_MODIFY_OBJECTS or
                (cur and LLRelationship.GRANT_MAP_LOCATION) or
                (cur and LLRelationship.GRANT_ONLINE_STATUS)
            confirmModifyRights(true, newRights)
        } else {
            val newRights = (cur and LLRelationship.GRANT_MAP_LOCATION) or
                (cur and LLRelationship.GRANT_ONLINE_STATUS)
            btnPermissionEditMine?.setColor(LLUIColorTable.instance().getColor("White_10"))
            LLAvatarPropertiesProcessor.getInstance().sendFriendRights(getAvatarId(), newRights)
        }
        btnPermissionEditMine?.setFocus(false)
    }

    fun confirmModifyRights(grant: Boolean, rights: Int) {
        val args = LLSD.mapOf("NAME" to LLSD.fromString(
            com.firestorm.ui.LLSLURL("agent", getAvatarId(), "completename").getSLURLString()
        ))
        val notification = if (grant) "GrantModifyRights" else "RevokeModifyRights"
        com.firestorm.ui.LLNotificationsUtil.add(notification, args) { notif, response ->
            rightsConfirmationCallback(notif, response, rights)
        }
    }

    private fun rightsConfirmationCallback(notification: LLSD, response: LLSD, rights: Int) {
        val option = com.firestorm.ui.LLNotificationsUtil.getSelectedOption(notification, response)
        if (option == 0) {
            LLAvatarPropertiesProcessor.getInstance().sendFriendRights(getAvatarId(), rights)
            btnPermissionEditMine?.setColor(LLUIColorTable.instance().getColor("White"))
        }
    }

    private fun updateChildren() {
        TODO("GPU: reflow child widget positions based on childrenWidths[]")
    }

    fun dispose() {
        if (avatarId != NULL_UUID) {
            LLAvatarTracker.instance().removeParticularFriendObserver(avatarId, this)
            LLAvatarTracker.instance().removeFriendPermissionObserver(avatarId, this)
        }
        avatarNameCacheConnection?.close()
        avatarNameCacheConnection = null
        voiceLevelChangeCallbackConnection?.close()
        voiceLevelChangeCallbackConnection = null
    }

    // Stubs for style helpers – full impl depends on UI framework bindings
    private fun defaultStyle(): Any? = null
    private fun voiceCallInvitedStyle(): Any? = null
    private fun voiceCallJoinedStyle(): Any? = null
    private fun voiceCallLeftStyle(): Any? = null
    private fun onlineStyle(): Any? = null
    private fun offlineStyle(): Any? = null
    private fun groupModeratorStyle(): Any? = null
    private fun mergeWithGroupModStyle(base: Any?): Any? = base
    private fun mergeWithOnlineColor(base: Any?): Any? = base
    private fun applyColor(base: Any?, color: LLColor4): Any? = base
    private fun textboxSetHighlightedVal(box: LLTextBox?, style: Any?, text: String, highlight: String) {
        TODO("GPU: render highlighted text into $box")
    }
}
