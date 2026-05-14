package com.firestorm.newview

import java.util.UUID

// ---------------------------------------------------------------------------
// UI field / button name constants
// ---------------------------------------------------------------------------

private const val TF_NAME        = "experience_title"
private const val TF_DESC        = "experience_description"
private const val TF_SLURL       = "LocationTextText"
private const val TF_MRKT        = "marketplace"
private const val TF_MATURITY    = "ContentRatingText"
private const val TF_OWNER       = "OwnerText"
private const val TF_GROUP       = "GroupText"
private const val TF_GRID_WIDE   = "grid_wide"
private const val TF_PRIVILEGED  = "privileged"
private const val EDIT           = "edit_"

private const val IMG_LOGO       = "logo"

private const val PNL_TOP        = "top panel"
private const val PNL_IMAGE      = "image_panel"
private const val PNL_DESC       = "description panel"
private const val PNL_LOC        = "location panel"
private const val PNL_MRKT       = "marketplace panel"
private const val PNL_GROUP      = "group_panel"
private const val PNL_PERMS      = "perm panel"

private const val BTN_ALLOW          = "allow_btn"
private const val BTN_BLOCK          = "block_btn"
private const val BTN_CANCEL         = "cancel_btn"
private const val BTN_CLEAR_LOCATION = "clear_btn"
private const val BTN_EDIT           = "edit_btn"
private const val BTN_ENABLE         = "enable_btn"
private const val BTN_FORGET         = "forget_btn"
private const val BTN_PRIVATE        = "private_btn"
private const val BTN_REPORT         = "report_btn"
private const val BTN_SAVE           = "save_btn"
private const val BTN_SET_GROUP      = "Group_btn"
private const val BTN_SET_LOCATION   = "location_btn"

// ---------------------------------------------------------------------------
// Minimal stubs for referenced subsystems
// ---------------------------------------------------------------------------

private object LLExperienceCache {
    const val NAME          = "name"
    const val DESCRIPTION   = "description"
    const val SLURL         = "slurl"
    const val METADATA      = "metadata"
    const val MATURITY      = "maturity"
    const val AGENT_ID      = "agent_id"
    const val GROUP_ID      = "group_id"
    const val EXPERIENCE_ID = "experience_id"
    const val PROPERTIES    = "properties"
    const val MISSING       = "missing"

    const val PROPERTY_DISABLED: Int = 0x01
    const val PROPERTY_PRIVATE:  Int = 0x02
    const val PROPERTY_GRID:     Int = 0x04
    const val PROPERTY_PRIVILEGED: Int = 0x08

    fun get(id: UUID, cb: (Map<String, Any?>) -> Unit) {
        System.err.println("LLExperienceCache: get not yet implemented")
    }
    fun fetch(id: UUID, force: Boolean) {
        System.err.println("LLExperienceCache: fetch not yet implemented")
    }
    fun getExperienceAdmin(id: UUID, cb: (Map<String, Any?>) -> Unit) {
        System.err.println("LLExperienceCache: getExperienceAdmin not yet implemented")
    }
    fun getExperiencePermission(id: UUID, cb: (Map<String, Any?>) -> Unit) {
        System.err.println("LLExperienceCache: getExperiencePermission not yet implemented")
    }
    fun setExperiencePermission(id: UUID, perm: String, cb: (Map<String, Any?>) -> Unit) {
        System.err.println("LLExperienceCache: setExperiencePermission not yet implemented")
    }
    fun forgetExperiencePermission(id: UUID, cb: (Map<String, Any?>) -> Unit) {
        System.err.println("LLExperienceCache: forgetExperiencePermission not yet implemented")
    }
    fun updateExperience(pkg: Map<String, Any?>, cb: (Map<String, Any?>) -> Unit) {
        System.err.println("LLExperienceCache: updateExperience not yet implemented")
    }
    fun insert(experience: Map<String, Any?>) {
        System.err.println("LLExperienceCache: insert not yet implemented")
    }
}

private object EventPumps {
    fun obtain(name: String): EventPump = EventPump(name)
}

private class EventPump(val name: String) {
    fun listen(listener: String, cb: (Map<String, Any?>) -> Boolean) {
        System.err.println("EventPump: listen not yet implemented")
    }
    fun stopListening(listener: String) {
        System.err.println("EventPump: stopListening not yet implemented")
    }
    fun post(data: Map<String, Any?>) {
        System.err.println("EventPump: post not yet implemented")
    }
}

private object LLAgent {
    fun getRegion(): LLRegionStub? = null
    fun getID(): UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    fun getPositionGlobal(): Any = Any()
}

private class LLRegionStub {
    fun getCapability(name: String): String = ""
    fun getName(): String = ""
    fun getOriginGlobal(): Any = Any()
}

private object LLTrans {
    fun getString(key: String): String = ""
}

private object LLNotificationsUtil {
    fun add(name: String, subs: Map<String, Any?> = emptyMap(), payload: Map<String, Any?> = emptyMap(),
            cb: ((Map<String, Any?>, Map<String, Any?>) -> Boolean)? = null) {
        System.err.println("LLNotificationsUtil: add not yet implemented")
    }
    fun getSelectedOption(notification: Map<String, Any?>, response: Map<String, Any?>): Int = 0
}

private object LLFloaterReg {
    fun showInstance(name: String, key: Any, focus: Boolean = false) {
        System.err.println("LLFloaterReg: showInstance not yet implemented")
    }
    fun showTypedInstance(name: String, key: Any): Any? = null
}

// ---------------------------------------------------------------------------
// LLFloaterExperienceProfile
// ---------------------------------------------------------------------------

class LLFloaterExperienceProfile(data: Map<String, Any?>) {

    enum class PostSaveAction { NOTHING, CLOSE, VIEW }

    var mExperienceId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private var mExperienceDetails: MutableMap<String, Any?> = mutableMapOf()
    private var mPackage: MutableMap<String, Any?> = mutableMapOf()
    var mLocationSLURL: String = ""
    private var mSaveCompleteAction: PostSaveAction = PostSaveAction.NOTHING
    var mDirty: Boolean = false
    var mForceClose: Boolean = false
    private var mPostEdit: Boolean = false

    init {
        if (data.containsKey("experience_id")) {
            val rawId = data["experience_id"]
            mExperienceId = when (rawId) {
                is UUID   -> rawId
                is String -> UUID.fromString(rawId)
                else      -> UUID.fromString("00000000-0000-0000-0000-000000000000")
            }
            mPostEdit = data["edit_experience"] as? Boolean ?: false
        } else {
            val rawId = data[""] ?: data.values.firstOrNull()
            mExperienceId = when (rawId) {
                is UUID   -> rawId
                is String -> UUID.fromString(rawId)
                else      -> UUID.fromString("00000000-0000-0000-0000-000000000000")
            }
            mPostEdit = false
        }
    }

    // Child widget helpers — resolved by the UI framework
    private fun <T> getChild(name: String): T = error("getChild not yet implemented")
    private fun <T> findChild(name: String): T? = null
    private fun childSetAction(name: String, cb: () -> Unit) {}
    private fun childSetCommitCallback(name: String, cb: () -> Unit) {}
    private fun closeFloater() {}

    private fun getButtonVisible(name: String): Boolean = false
    private fun setButtonEnabled(name: String, enabled: Boolean) {}
    private fun setButtonVisible(name: String, visible: Boolean) {}
    private fun setButtonEnabled(name: String, enabled: Boolean, @Suppress("UNUSED_PARAMETER") dummy: Unit = Unit) {}
    private fun setPanelVisible(name: String, visible: Boolean) {}
    private fun setTextValue(name: String, text: String) {}
    private fun getTextValue(name: String): String = ""
    private fun getTextEditorValue(name: String): String = ""
    private fun getCheckValue(name: String): Boolean = false
    private fun setCheckValue(name: String, value: Boolean) {}
    private fun getComboSelectedIndex(name: String): Int = 0
    private fun setComboSelectedIndex(name: String, index: Int) {}
    private fun getTextureAssetId(name: String): UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private fun setTextureAssetId(name: String, id: UUID) {}
    private fun getButtonSaveEnabled(): Boolean = false
    private fun setButtonSaveEnabled(enabled: Boolean) {}
    private fun getString(key: String): String = ""
    private fun selectTab(name: String) {}
    private fun abortQuit() {}

    fun matchesKey(key: Map<String, Any?>): Boolean {
        return when {
            key.containsKey("experience_id") -> {
                val v = key["experience_id"]
                when (v) {
                    is UUID   -> mExperienceId == v
                    is String -> mExperienceId.toString() == v
                    else      -> false
                }
            }
            else -> mExperienceId.toString() == "00000000-0000-0000-0000-000000000000"
        }
    }

    fun getExperienceId(): UUID = mExperienceId

    fun postBuild(): Boolean {
        if (mExperienceId.toString() != "00000000-0000-0000-0000-000000000000") {
            LLExperienceCache.fetch(mExperienceId, true)
            LLExperienceCache.get(mExperienceId) { exp ->
                experienceCallback(this, exp)
            }

            val region = LLAgent.getRegion()
            if (region != null) {
                LLExperienceCache.getExperienceAdmin(mExperienceId) { result ->
                    experienceIsAdmin(this, result)
                }
            }
        }

        childSetAction(BTN_EDIT)           { onClickEdit() }
        childSetAction(BTN_ALLOW)          { onClickPermission("Allow") }
        childSetAction(BTN_FORGET)         { onClickForget() }
        childSetAction(BTN_BLOCK)          { onClickPermission("Block") }
        childSetAction(BTN_CANCEL)         { onClickCancel() }
        childSetAction(BTN_SAVE)           { onClickSave() }
        childSetAction(BTN_SET_LOCATION)   { onClickLocation() }
        childSetAction(BTN_CLEAR_LOCATION) { onClickClear() }
        childSetAction(BTN_SET_GROUP)      { onPickGroup() }
        childSetAction(BTN_REPORT)         { onReportExperience() }

        System.err.println("LLFloaterExperienceProfile: postBuild keystroke/commit callback wiring not yet implemented")

        EventPumps.obtain("experience_permission").listen("$mExperienceId-profile") { perm ->
            experiencePermission(this, perm)
        }

        if (mPostEdit && mExperienceId.toString() != "00000000-0000-0000-0000-000000000000") {
            mPostEdit = false
            changeToEdit()
        }

        return true
    }

    fun setPreferences(content: Map<String, Any?>) {
        val properties = (mExperienceDetails[LLExperienceCache.PROPERTIES] as? Int) ?: 0
        if (properties and LLExperienceCache.PROPERTY_PRIVILEGED != 0) return

        val experiences = content["experiences"] as? List<*> ?: emptyList<Any>()
        val blocked     = content["blocked"]     as? List<*> ?: emptyList<Any>()

        for (id in experiences) {
            if (idMatches(id)) { experienceAllowed(); return }
        }
        for (id in blocked) {
            if (idMatches(id)) { experienceBlocked(); return }
        }
        experienceForgotten()
    }

    fun refreshExperience(experience: Map<String, Any?>) {
        mExperienceDetails = experience.toMutableMap()
        mPackage = experience.toMutableMap()

        setPanelVisible(PNL_IMAGE,  false)
        setPanelVisible(PNL_DESC,   false)
        setPanelVisible(PNL_LOC,    false)
        setPanelVisible(PNL_MRKT,   false)
        setPanelVisible(PNL_TOP,    false)

        val expId = experience[LLExperienceCache.EXPERIENCE_ID]
        setTextValue(TF_NAME, "secondlife://app/experience/$expId/profile")

        setTextValue(EDIT + TF_NAME, experience[LLExperienceCache.NAME]?.toString() ?: "")

        val desc = experience[LLExperienceCache.DESCRIPTION]?.toString() ?: ""
        System.err.println("LLFloaterExperienceProfile: refreshExperience LLExpandableTextBox setText not yet implemented")
        setPanelVisible(PNL_DESC, desc.isNotEmpty())
        setTextValue(EDIT + TF_DESC, desc)

        val rawSlurl = experience[LLExperienceCache.SLURL]?.toString() ?: ""
        val hasSlurl = rawSlurl.isNotEmpty()
        setPanelVisible(PNL_LOC, hasSlurl)
        mLocationSLURL = rawSlurl
        setTextValue(TF_SLURL, mLocationSLURL)
        setTextValue(EDIT + TF_SLURL, if (hasSlurl) mLocationSLURL else getString("empty_slurl"))

        val maturity = (experience[LLExperienceCache.MATURITY] as? Number)?.toInt() ?: 0
        setMaturityString(maturity.toByte())

        val agentId = experience[LLExperienceCache.AGENT_ID]?.let { toUUID(it) }
            ?: UUID.fromString("00000000-0000-0000-0000-000000000000")
        setTextValue(TF_OWNER, "secondlife://app/agent/$agentId/inspect")

        val groupId = experience[LLExperienceCache.GROUP_ID]?.let { toUUID(it) }
            ?: UUID.fromString("00000000-0000-0000-0000-000000000000")
        setTextValue(TF_GROUP, "secondlife://app/group/$groupId/inspect")
        setPanelVisible(PNL_GROUP, groupId.toString() != "00000000-0000-0000-0000-000000000000")
        setEditGroup(groupId)

        val isOwner = agentId == LLAgent.getID()
        System.err.println("LLFloaterExperienceProfile: refreshExperience BTN_SET_GROUP setEnabled not yet implemented")

        val properties = (mExperienceDetails[LLExperienceCache.PROPERTIES] as? Int) ?: 0
        setCheckValue(EDIT + BTN_ENABLE, (properties and LLExperienceCache.PROPERTY_DISABLED) == 0)
        setCheckValue(EDIT + BTN_PRIVATE, (properties and LLExperienceCache.PROPERTY_PRIVATE) != 0)

        setPanelVisible(PNL_TOP, true)
        val gridWideText = if (properties and LLExperienceCache.PROPERTY_GRID != 0)
            LLTrans.getString("Grid-Scope") else LLTrans.getString("Land-Scope")
        setTextValue(TF_GRID_WIDE, gridWideText)

        if (getButtonVisible(BTN_EDIT)) setPanelVisible(PNL_TOP, true)

        if (properties and LLExperienceCache.PROPERTY_PRIVILEGED != 0) {
            System.err.println("LLFloaterExperienceProfile: refreshExperience TF_PRIVILEGED show not yet implemented")
        } else {
            val region = LLAgent.getRegion()
            if (region != null) {
                LLExperienceCache.getExperiencePermission(mExperienceId) { result ->
                    experiencePermissionResults(mExperienceId, result)
                }
            }
        }

        val metadata = experience[LLExperienceCache.METADATA]?.toString() ?: ""
        if (metadata.isEmpty()) {
            mDirty = false; mForceClose = false
            setButtonSaveEnabled(mDirty)
            return
        }

        System.err.println("LLFloaterExperienceProfile: refreshExperience LLSD XML metadata parse not yet implemented")
        mDirty = false; mForceClose = false
        setButtonSaveEnabled(mDirty)
    }

    fun onSaveComplete(content: Map<String, Any?>) {
        val id = getExperienceId()

        val removed = content["removed"] as? Map<*, *>
        removed?.forEach { (field, data) ->
            val fieldStr = field.toString()
            if (fieldStr == LLExperienceCache.EXPERIENCE_ID) return@forEach
            val dataMap = data as? Map<*, *> ?: return@forEach
            val errorTag = "${dataMap["error_tag"]}ExperienceProfileMessage"
            System.err.println("LLFloaterExperienceProfile: onSaveComplete LLNotificationsUtil add not yet implemented")
        }

        val experienceKeys = content["experience_keys"] as? List<*>
        if (experienceKeys.isNullOrEmpty()) return

        val first = experienceKeys.first() as? Map<*, *> ?: return
        @Suppress("UNCHECKED_CAST")
        val firstMap = first as Map<String, Any?>
        val savedId = firstMap[LLExperienceCache.EXPERIENCE_ID]?.let { toUUID(it) }
        if (savedId != id) return

        refreshExperience(firstMap)
        LLExperienceCache.insert(firstMap)
        LLExperienceCache.fetch(id, true)

        when (mSaveCompleteAction) {
            PostSaveAction.VIEW  -> selectTab("panel_experience_info")
            PostSaveAction.CLOSE -> closeFloater()
            PostSaveAction.NOTHING -> {}
        }
    }

    open fun canClose(): Boolean {
        if (mForceClose || !mDirty) return true
        LLNotificationsUtil.add("SaveChanges", emptyMap(), emptyMap()) { notif, resp ->
            handleSaveChangesDialog(notif, resp, PostSaveAction.CLOSE)
        }
        return false
    }

    open fun onClose(appQuitting: Boolean) {
        EventPumps.obtain("experience_permission").stopListening("$mExperienceId-profile")
    }

    protected fun onClickEdit()   { changeToEdit() }
    protected fun onClickCancel() { changeToView() }
    protected fun onClickSave()   { doSave(PostSaveAction.NOTHING) }

    protected fun onClickPermission(perm: String) {
        val region = LLAgent.getRegion() ?: return
        LLExperienceCache.setExperiencePermission(mExperienceId, perm) { result ->
            experiencePermissionResults(mExperienceId, result)
        }
    }

    protected fun onClickForget() {
        val region = LLAgent.getRegion() ?: return
        LLExperienceCache.forgetExperiencePermission(mExperienceId) { result ->
            experiencePermissionResults(mExperienceId, result)
        }
    }

    protected fun onClickLocation() {
        val region = LLAgent.getRegion() ?: return
        // Builds a SLURL anchored to the region origin so it works in VarRegions (FIRE-30768)
        mLocationSLURL = run {
            System.err.println("LLFloaterExperienceProfile: onClickLocation SLURL build not yet implemented")
            ""
        }
        setTextValue(EDIT + TF_SLURL, mLocationSLURL)
        onFieldChanged()
    }

    protected fun onClickClear() {
        mLocationSLURL = ""
        setTextValue(EDIT + TF_SLURL, getString("empty_slurl"))
        onFieldChanged()
    }

    protected fun onPickGroup() {
        System.err.println("LLFloaterExperienceProfile: onPickGroup not yet implemented")
    }

    protected fun onFieldChanged() {
        updatePackage()
        if (!getButtonVisible(BTN_EDIT)) return

        val st = mExperienceDetails.entries.toList()
        val dt = mPackage.entries.toList()
        mDirty = st.size != dt.size ||
            st.zip(dt).any { (s, d) -> s.key != d.key || s.value?.toString() != d.value?.toString() }

        setButtonSaveEnabled(mDirty)
    }

    protected fun onReportExperience() {
        System.err.println("LLFloaterExperienceProfile: onReportExperience not yet implemented")
    }

    protected fun setEditGroup(groupId: UUID) {
        setTextValue(EDIT + TF_GROUP, "secondlife://app/group/$groupId/inspect")
        mPackage[LLExperienceCache.GROUP_ID] = groupId.toString()
        onFieldChanged()
    }

    protected fun changeToView() {
        if (mForceClose || !mDirty) {
            refreshExperience(mExperienceDetails)
            selectTab("panel_experience_info")
        } else {
            LLNotificationsUtil.add("SaveChanges", emptyMap(), emptyMap()) { notif, resp ->
                handleSaveChangesDialog(notif, resp, PostSaveAction.VIEW)
            }
        }
    }

    protected fun changeToEdit() {
        selectTab("edit_panel_experience_info")
    }

    protected fun experienceForgotten() {
        System.err.println("LLFloaterExperienceProfile: experienceForgotten not yet implemented")
    }

    protected fun experienceBlocked() {
        System.err.println("LLFloaterExperienceProfile: experienceBlocked not yet implemented")
    }

    protected fun experienceAllowed() {
        System.err.println("LLFloaterExperienceProfile: experienceAllowed not yet implemented")
    }

    protected fun handleSaveChangesDialog(
        notification: Map<String, Any?>,
        response: Map<String, Any?>,
        action: PostSaveAction
    ): Boolean {
        return when (LLNotificationsUtil.getSelectedOption(notification, response)) {
            0 -> { doSave(action); false }
            1 -> {
                if (action != PostSaveAction.NOTHING) {
                    mForceClose = true
                    if (action == PostSaveAction.CLOSE) closeFloater() else changeToView()
                }
                false
            }
            else -> { abortQuit(); false }
        }
    }

    protected fun doSave(successAction: PostSaveAction) {
        mSaveCompleteAction = successAction
        val region = LLAgent.getRegion() ?: return
        LLExperienceCache.updateExperience(mPackage) { result ->
            experienceUpdateResult(this, result)
        }
    }

    protected fun updatePackage() {
        mPackage[LLExperienceCache.NAME]        = getTextValue(EDIT + TF_NAME)
        mPackage[LLExperienceCache.DESCRIPTION] = getTextEditorValue(EDIT + TF_DESC)
        mPackage[LLExperienceCache.SLURL]       = mLocationSLURL.ifEmpty { null }
        mPackage[LLExperienceCache.MATURITY]    = getComboSelectedIndex(EDIT + TF_MATURITY)

        System.err.println("LLFloaterExperienceProfile: updatePackage LLSD XML metadata serialise not yet implemented")

        val properties = (mPackage[LLExperienceCache.PROPERTIES] as? Int) ?: 0
        var props = properties
        props = if (getCheckValue(EDIT + BTN_ENABLE))
            props and LLExperienceCache.PROPERTY_DISABLED.inv()
        else
            props or LLExperienceCache.PROPERTY_DISABLED

        props = if (getCheckValue(EDIT + BTN_PRIVATE))
            props or LLExperienceCache.PROPERTY_PRIVATE
        else
            props and LLExperienceCache.PROPERTY_PRIVATE.inv()

        mPackage[LLExperienceCache.PROPERTIES] = props
    }

    protected fun updatePermission(permission: Map<String, Any?>) {
        if (permission.containsKey("experience")) {
            val permExpId = permission["experience"]?.let { toUUID(it) }
            if (permExpId != mExperienceId) return

            when (permission[mExperienceId.toString()]
                    ?.let { (it as? Map<*, *>)?.get("permission")?.toString() }) {
                "Allow"  -> experienceAllowed()
                "Block"  -> experienceBlocked()
                "Forget" -> experienceForgotten()
            }
        } else {
            setPreferences(permission)
        }
    }

    private fun setMaturityString(maturity: Byte) {
        System.err.println("LLFloaterExperienceProfile: setMaturityString not yet implemented")
    }

    private fun idMatches(id: Any?): Boolean =
        id?.let { toUUID(it) } == mExperienceId

    private fun toUUID(v: Any): UUID = when (v) {
        is UUID   -> v
        is String -> UUID.fromString(v)
        else      -> UUID.fromString(v.toString())
    }

    // Static-style callbacks (no instance handle needed: Kotlin closures capture the instance directly)
    private fun experienceCallback(profile: LLFloaterExperienceProfile, experience: Map<String, Any?>) {
        profile.refreshExperience(experience)
    }

    private fun experiencePermission(profile: LLFloaterExperienceProfile, permission: Map<String, Any?>): Boolean {
        profile.updatePermission(permission)
        return false
    }

    private companion object {
        fun hasPermission(content: Map<String, Any?>, name: String, test: UUID): Boolean {
            val list = content[name] as? List<*> ?: return false
            return list.any { entry ->
                when (entry) {
                    is UUID   -> entry == test
                    is String -> entry == test.toString()
                    else      -> false
                }
            }
        }

        fun experiencePermissionResults(experienceId: UUID, result: Map<String, Any?>) {
            val permission = when {
                hasPermission(result, "experiences", experienceId) -> "Allow"
                hasPermission(result, "blocked",     experienceId) -> "Block"
                else                                                -> "Forget"
            }
            val experience = mapOf("permission" to permission)
            val message    = mapOf<String, Any?>(
                "experience"            to experienceId.toString(),
                experienceId.toString() to experience
            )
            EventPumps.obtain("experience_permission").post(message)
        }

        fun experienceIsAdmin(profile: LLFloaterExperienceProfile, result: Map<String, Any?>) {
            val region = LLAgent.getRegion() ?: return
            val url = region.getCapability("UpdateExperience")
            if (url.isNotEmpty() && result["status"] as? Boolean == true) {
                System.err.println("LLFloaterExperienceProfile: experienceIsAdmin show PNL_TOP and BTN_EDIT not yet implemented")
            }
        }

        fun experienceUpdateResult(profile: LLFloaterExperienceProfile, result: Map<String, Any?>) {
            profile.onSaveComplete(result)
        }
    }
}
