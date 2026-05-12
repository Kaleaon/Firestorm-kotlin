package com.firestorm.newview

import java.util.UUID

object LLInspectRemoteObjectUtil {
    fun registerFloater() {
        TODO("APR: use JVM equivalent of LLFloaterReg::add(\"inspect_remote_object\", \"inspect_remote_object.xml\", builder)")
    }
}

class LLInspectRemoteObject(key: Any?) : LLInspect(key) {

    private var mObjectID: UUID = UUID(0L, 0L)
    private var mOwnerID: UUID = UUID(0L, 0L)
    private var mSLurl: String = ""
    private var mName: String = ""
    private var mRlvHideNames: Boolean = false
    private var mGroupOwned: Boolean = false

    fun postBuild(): Boolean {
        bindButtonCallback("map_btn")   { onClickMap() }
        bindButtonCallback("block_btn") { onClickBlock() }
        bindButtonCallback("close_btn") { onClickClose() }
        return true
    }

    override fun onOpen(data: Any?) {
        super.onOpen(data)
        mObjectID   = extractUuid(data, "object_id")
        mName       = extractString(data, "name")
        mOwnerID    = extractUuid(data, "owner_id")
        mGroupOwned = extractBoolean(data, "group_owned")
        mSLurl      = extractString(data, "slurl")

        if (hasField(data, "rlv_shownames")) {
            mRlvHideNames = extractBoolean(data, "rlv_shownames")
        }

        update()
        repositionInspector(data)
    }

    private fun onClickMap() {
        val url = "secondlife://$mSLurl"
        TODO("APR: use JVM equivalent of LLUrlAction::showLocationOnMap(url=$url)")
        closeFloater(false)
    }

    private fun onClickBlock() {
        TODO("APR: use JVM equivalent of LLMuteList::add(LLMute(mObjectID, mName, OBJECT)) and LLPanelBlockedList::showPanelAndSelect(mObjectID)")
        closeFloater(false)
    }

    private fun onClickClose() {
        closeFloater(false)
    }

    private fun update() {
        setChildValue("object_name", "<nolink>$mName</nolink>")

        val owner: String = when {
            mOwnerID != UUID(0L, 0L) -> {
                if (mGroupOwned) {
                    buildSLURL("group", mOwnerID, "about")
                } else {
                    val action = if (!mRlvHideNames) "about" else "rlvanonym"
                    buildSLURL("agent", mOwnerID, action)
                }
            }
            else -> TODO("APR: use JVM equivalent of LLTrans::getString(\"Unknown\")")
        }
        setChildValue("object_owner", owner)

        val url = if (mSLurl.isNotEmpty()) "secondlife:///app/teleport/$mSLurl" else ""
        setChildValue("object_slurl", url)

        setChildEnabled("map_btn", mSLurl.isNotEmpty())

        val canBlock = mObjectID != UUID(0L, 0L) && !isMuted(mObjectID)
        setChildEnabled("block_btn", canBlock)

        if (isRlvHiddenRegion(mSLurl)) {
            setChildValue("object_slurl", mSLurl)
            setChildEnabled("map_btn", false)
        }
    }

    private fun buildSLURL(scheme: String, id: UUID, action: String): String {
        TODO("APR: use JVM equivalent of LLSLURL(scheme, id, action).getSLURLString()")
    }

    private fun isMuted(id: UUID): Boolean {
        TODO("APR: use JVM equivalent of LLMuteList::getInstance()->isMuted(id)")
    }

    private fun isRlvHiddenRegion(slurl: String): Boolean {
        TODO("APR: use JVM equivalent of rlv_handler_t::isEnabled() && RlvStrings::getString(RlvStringKeys::Hidden::Region) == slurl")
    }

    private fun bindButtonCallback(childName: String, callback: () -> Unit) {
        TODO("APR: use JVM equivalent of getChild<LLUICtrl>(childName)->setCommitCallback(callback)")
    }

    private fun setChildValue(childName: String, value: Any) {
        TODO("APR: use JVM equivalent of getChild<LLUICtrl>(childName)->setValue(value)")
    }

    private fun setChildEnabled(childName: String, enabled: Boolean) {
        TODO("APR: use JVM equivalent of getChild<LLUICtrl>(childName)->setEnabled(enabled)")
    }

    private fun extractUuid(data: Any?, key: String): UUID {
        TODO("APR: use JVM equivalent of data[key].asUUID()")
    }

    private fun extractString(data: Any?, key: String): String {
        TODO("APR: use JVM equivalent of data[key].asString()")
    }

    private fun extractBoolean(data: Any?, key: String): Boolean {
        TODO("APR: use JVM equivalent of data[key].asBoolean()")
    }

    private fun hasField(data: Any?, key: String): Boolean {
        TODO("APR: use JVM equivalent of LLSD::has(key)")
    }
}
