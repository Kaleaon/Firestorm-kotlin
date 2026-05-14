package com.firestorm.newview

import java.util.UUID

object LLInspectRemoteObjectUtil {
    fun registerFloater() {
        System.err.println("LLInspectRemoteObjectUtil: registerFloater not yet implemented")
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
        System.err.println("LLInspectRemoteObject: onClickMap not yet implemented")
        closeFloater(false)
    }

    private fun onClickBlock() {
        System.err.println("LLInspectRemoteObject: onClickBlock not yet implemented")
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
            else -> ""
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
        return ""
    }

    private fun isMuted(id: UUID): Boolean {
        return false
    }

    private fun isRlvHiddenRegion(slurl: String): Boolean {
        return false
    }

    private fun bindButtonCallback(childName: String, callback: () -> Unit) {
        System.err.println("LLInspectRemoteObject: bindButtonCallback not yet implemented")
    }

    private fun setChildValue(childName: String, value: Any) {
        System.err.println("LLInspectRemoteObject: setChildValue not yet implemented")
    }

    private fun setChildEnabled(childName: String, enabled: Boolean) {
        System.err.println("LLInspectRemoteObject: setChildEnabled not yet implemented")
    }

    private fun extractUuid(data: Any?, key: String): UUID {
        return UUID(0L, 0L)
    }

    private fun extractString(data: Any?, key: String): String {
        return ""
    }

    private fun extractBoolean(data: Any?, key: String): Boolean {
        return false
    }

    private fun hasField(data: Any?, key: String): Boolean {
        return false
    }
}
