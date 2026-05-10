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
        TODO("APR: use JVM equivalent for experience cache async get")
    }
    fun fetch(id: UUID, force: Boolean) {
        TODO("APR: use JVM equivalent for experience cache fetch")
    }
    fun getExperienceAdmin(id: UUID, cb: (Map<String, Any?>) -> Unit) {
        TODO("APR: use JVM equivalent for experience admin query")
    }
    fun getExperiencePermission(id: UUID, cb: (Map<String, Any?>) -> Unit) {
        TODO("APR: use JVM equivalent for experience permission query")
    }
    fun setExperiencePermission(id: UUID, perm: String, cb: (Map<String, Any?>) -> Unit) {
        TODO("APR: use JVM equivalent for setting experience permission")
    }
    fun forgetExperiencePermission(id: UUID, cb: (Map<String, Any?>) -> Unit) {
        TODO("APR: use JVM equivalent for forgetting experience permission")
    }
    fun updateExperience(pkg: Map<String, Any?>, cb: (Map<String, Any?>) -> Unit) {
        TODO("APR: use JVM equivalent for updating experience via cap")
    }
    fun insert(experience: Map<String, Any?>) {
        TODO("APR: use JVM equivalent for inserting into experience cache")
    }
}

private object EventPumps {
    fun obtain(name: String): EventPump = EventPump(name)
}

private class EventPump(val name: String) {
    fun listen(listener: String, cb: (Map<String, Any?>) -> Boolean) {
        TODO("APR: use JVM equivalent for event pump listener registration")
    }
    fun stopListening(listener: String) {
        TODO("APR: use JVM equivalent for removing event pump listener")
    }
    fun post(data: Map<String, Any?>) {
        TODO("APR: use JVM equivalent for event pump post")
    }
}

private object LLAgent {
    fun getRegion(): LLRegionStub? = TODO("APR: use JVM equivalent for agent region")
    fun getID(): UUID = TODO("APR: use JVM equivalent for agent UUID")
    fun getPositionGlobal(): Any = TODO("APR: use JVM equivalent for agent global position")
}

private class LLRegionStub {
    fun getCapability(name: String): String = TODO("APR: use JVM equivalent for region capability URL")
    fun getName(): String = TODO("APR: use JVM equivalent for region name")
    fun getOriginGlobal(): Any = TODO("APR: use JVM equivalent for region origin")
}

private object LLTrans {
    fun getString(key: String): String = TODO("APR: use JVM equivalent for localised string lookup")
}

private object LLNotificationsUtil {
    fun add(name: String, subs: Map<String, Any?> = emptyMap(), payload: Map<String, Any?> = emptyMap(),
            cb: ((Map<String, Any?>, Map<String, Any?>) -> Boolean)? = null) {
        TODO("APR: use JVM equivalent for notification")
    }
    fun getSelectedOption(notification: Map<String, Any?>, response: Map<String, Any?>): Int =
        TODO("APR: use JVM equivalent for notification option index")
}

private object LLFloaterReg {
    fun showInstance(name: String, key: Any, focus: Boolean = false) {
        TODO("GPU: show floater instance $name")
    }
    fun showTypedInstance(name: String, key: Any): Any? =
        TODO("GPU: show typed floater instance $name")
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
    private fun <T> getChild(name: String): T = TODO("GPU: getChild<$name>")
    private fun <T> findChild(name: String): T? = TODO("GPU: findChild<$name>")
    private fun childSetAction(name: String, cb: () -> Unit) = TODO("GPU: childSetAction($name)")
    private fun childSetCommitCallback(name: String, cb: () -> Unit) = TODO("GPU: childSetCommitCallback($name)")
    private fun closeFloater() = TODO("GPU: closeFloater()")

    private fun getButtonVisible(name: String): Boolean = TODO("GPU: getChild<LLButton>($name).getVisible()")
    private fun setButtonEnabled(name: String, enabled: Boolean) = TODO("GPU: getChild<LLButton>($name).setEnabled($enabled)")
    private fun setButtonVisible(name: String, visible: Boolean) = TODO("GPU: getChild<LLButton>($name).setVisible($visible)")
    private fun setButtonEnabled(name: String, enabled: Boolean, @Suppress("UNUSED_PARAMETER") dummy: Unit = Unit) {}
    private fun setPanelVisible(name: String, visible: Boolean) = TODO("GPU: getChild<LLLayoutPanel>($name).setVisible($visible)")
    private fun setTextValue(name: String, text: String) = TODO("GPU: getChild<LLTextBox>($name).setText($text)")
    private fun getTextValue(name: String): String = TODO("GPU: getChild<LLLineEditor>($name).getText()")
    private fun getTextEditorValue(name: String): String = TODO("GPU: getChild<LLTextEditor>($name).getText()")
    private fun getCheckValue(name: String): Boolean = TODO("GPU: getChild<LLCheckBoxCtrl>($name).get()")
    private fun setCheckValue(name: String, value: Boolean) = TODO("GPU: getChild<LLCheckBoxCtrl>($name).set($value)")
    private fun getComboSelectedIndex(name: String): Int = TODO("GPU: getChild<LLComboBox>($name).getSelectedValue().asInteger()")
    private fun setComboSelectedIndex(name: String, index: Int) = TODO("GPU: getChild<LLComboBox>($name).setCurrentByIndex($index)")
    private fun getTextureAssetId(name: String): UUID = TODO("GPU: getChild<LLTextureCtrl>($name).getImageAssetID()")
    private fun setTextureAssetId(name: String, id: UUID) = TODO("GPU: getChild<LLTextureCtrl>($name).setImageAssetID($id)")
    private fun getButtonSaveEnabled(): Boolean = TODO("GPU: getChild<LLButton>(BTN_SAVE).getEnabled()")
    private fun setButtonSaveEnabled(enabled: Boolean) = TODO("GPU: getChild<LLButton>(BTN_SAVE).setEnabled($enabled)")
    private fun getString(key: String): String = TODO("GPU: getString($key)")
    private fun selectTab(name: String) = TODO("GPU: getChild<LLTabContainer>(\"tab_container\").selectTabByName($name)")
    private fun abortQuit() = TODO("APR: use JVM equivalent for LLAppViewer::instance()->abortQuit()")

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

        TODO("GPU: wire keystroke/commit callbacks on text fields: EDIT+TF_DESC, EDIT+TF_MATURITY, EDIT+TF_MRKT, EDIT+TF_NAME, EDIT+BTN_ENABLE, EDIT+BTN_PRIVATE, EDIT+IMG_LOGO")

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
        TODO("GPU: set LLExpandableTextBox(TF_DESC).setText($desc)")
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
        TODO("GPU: getChild<LLButton>(BTN_SET_GROUP).setEnabled($isOwner)")

        val properties = (mExperienceDetails[LLExperienceCache.PROPERTIES] as? Int) ?: 0
        setCheckValue(EDIT + BTN_ENABLE, (properties and LLExperienceCache.PROPERTY_DISABLED) == 0)
        setCheckValue(EDIT + BTN_PRIVATE, (properties and LLExperienceCache.PROPERTY_PRIVATE) != 0)

        setPanelVisible(PNL_TOP, true)
        val gridWideText = if (properties and LLExperienceCache.PROPERTY_GRID != 0)
            LLTrans.getString("Grid-Scope") else LLTrans.getString("Land-Scope")
        setTextValue(TF_GRID_WIDE, gridWideText)

        if (getButtonVisible(BTN_EDIT)) setPanelVisible(PNL_TOP, true)

        if (properties and LLExperienceCache.PROPERTY_PRIVILEGED != 0) {
            TODO("GPU: show TF_PRIVILEGED text box")
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

        TODO("APR: use JVM equivalent for LLSD XML parse of metadata; extract TF_MRKT and IMG_LOGO")
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
            TODO("APR: use JVM equivalent for LLNotificationsUtil::add with error_tag or GenericAlert")
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
        mLocationSLURL = TODO("APR: use JVM equivalent for LLSLURL(region.name, region.originGlobal, agent.positionGlobal).getSLURLString()")
        setTextValue(EDIT + TF_SLURL, mLocationSLURL)
        onFieldChanged()
    }

    protected fun onClickClear() {
        mLocationSLURL = ""
        setTextValue(EDIT + TF_SLURL, getString("empty_slurl"))
        onFieldChanged()
    }

    protected fun onPickGroup() {
        TODO("GPU: show group picker floater, register setEditGroup callback")
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
        TODO("GPU: LLFloaterReporter.showFromExperience(mExperienceId)")
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
        TODO("GPU: allow_btn enable=true; forget_btn enable=false; block_btn enable=true")
    }

    protected fun experienceBlocked() {
        TODO("GPU: allow_btn enable=true; forget_btn enable=true; block_btn enable=false")
    }

    protected fun experienceAllowed() {
        TODO("GPU: allow_btn enable=false; forget_btn enable=true; block_btn enable=true")
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

        TODO("APR: use JVM equivalent for LLSD XML formatter to serialise metadata (TF_MRKT + IMG_LOGO) into mPackage[METADATA]")

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
        TODO("GPU: set maturity icon + text on TF_MATURITY and EDIT+TF_MATURITY combo")
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
                TODO("GPU: show PNL_TOP and BTN_EDIT on profile floater")
            }
        }

        fun experienceUpdateResult(profile: LLFloaterExperienceProfile, result: Map<String, Any?>) {
            profile.onSaveComplete(result)
        }
    }
}
