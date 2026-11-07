package com.firestorm.newview

import com.firestorm.llcommon.LLSD
import com.firestorm.llcommon.LLFloater
import com.firestorm.llcommon.LLFloaterReg
import com.firestorm.llcommon.LLNotificationsUtil
import com.firestorm.llcommon.LLScrollListCtrl
import com.firestorm.llcommon.LLTrans
import com.firestorm.llcommon.LLViewerParcelMedia

class FloaterMediaLists(key: LLSD) : LLFloater(key) {

    private var whitelistSLC: LLScrollListCtrl? = null
    private var blacklistSLC: LLScrollListCtrl? = null

    override fun postBuild(): Boolean {
        whitelistSLC = getChild("whitelist")
        blacklistSLC = getChild("blacklist")

        childSetAction("add_whitelist") { onWhitelistAdd() }
        childSetAction("remove_whitelist") { onWhitelistRemove() }
        childSetAction("add_blacklist") { onBlacklistAdd() }
        childSetAction("remove_blacklist") { onBlacklistRemove() }

        val inst = LLViewerParcelMedia.getInstance()
        for (entry in inst.mediaFilterList) {
            val element = LLSD()
            element["columns"][0]["column"] = "list"
            element["columns"][0]["value"] = entry["domain"].asString()
            when (entry["action"].asString()) {
                "allow" -> whitelistSLC?.addElement(element)
                "deny"  -> blacklistSLC?.addElement(element)
            }
        }

        whitelistSLC?.sortByColumn("list", ascending = true)
        blacklistSLC?.sortByColumn("list", ascending = true)

        return true
    }

    private fun onWhitelistAdd() {
        val args = LLSD()
        val payload = LLSD()
        args["LIST"] = LLTrans.getString("MediaFilterWhitelist")
        payload["whitelist"] = true
        LLNotificationsUtil.add("AddToMediaList", args, payload, ::handleAddDomainCallback)
    }

    private fun onWhitelistRemove() {
        val selected = whitelistSLC?.getFirstSelected() ?: return
        val domain = whitelistSLC?.getSelectedItemLabel() ?: return

        val inst = LLViewerParcelMedia.getInstance()
        val iter = inst.mediaFilterList.iterator()
        while (iter.hasNext()) {
            val item = iter.next()
            if (item["domain"].asString() == domain) {
                iter.remove()
                inst.saveDomainFilterList()
                // Force re-evaluation: the deleted domain may match the currently playing URL.
                inst.mediaLastURL = ""
                inst.audioLastURL = ""
                inst.mediaReFilter = true
                break
            }
        }
        whitelistSLC?.deleteSelectedItems()
    }

    private fun onBlacklistAdd() {
        val args = LLSD()
        val payload = LLSD()
        args["LIST"] = LLTrans.getString("MediaFilterBlacklist")
        payload["whitelist"] = false
        LLNotificationsUtil.add("AddToMediaList", args, payload, ::handleAddDomainCallback)
    }

    private fun onBlacklistRemove() {
        val selected = blacklistSLC?.getFirstSelected() ?: return
        val domain = blacklistSLC?.getSelectedItemLabel() ?: return

        val inst = LLViewerParcelMedia.getInstance()
        val iter = inst.mediaFilterList.iterator()
        while (iter.hasNext()) {
            val item = iter.next()
            if (item["domain"].asString() == domain) {
                iter.remove()
                inst.saveDomainFilterList()
                // Force re-evaluation: the deleted domain may match the currently playing URL.
                inst.mediaLastURL = ""
                inst.audioLastURL = ""
                inst.mediaReFilter = true
                break
            }
        }
        blacklistSLC?.deleteSelectedItems()
    }

    companion object {
        fun handleAddDomainCallback(notification: LLSD, response: LLSD): Boolean {
            val option = LLNotificationsUtil.getSelectedOption(notification, response)
            if (option != 0) return false

            val inst = LLViewerParcelMedia.getInstance()
            val domain = inst.extractDomain(response["url"].asString())
            if (domain.isEmpty()) return false

            val whitelist = notification["payload"]["whitelist"].asBoolean()
            val newMedia = LLSD()
            newMedia["domain"] = domain
            newMedia["action"] = if (whitelist) "allow" else "deny"
            inst.mediaFilterList.add(newMedia)
            inst.saveDomainFilterList()

            val floater = LLFloaterReg.findInstance("media_lists")
            if (floater != null) {
                val listName = if (whitelist) "whitelist" else "blacklist"
                val list = floater.getChild<LLScrollListCtrl>(listName)
                val element = LLSD()
                element["columns"][0]["column"] = "list"
                element["columns"][0]["value"] = domain
                list?.addElement(element)
                list?.sortByColumn("list", ascending = true)
            }

            return false
        }
    }
}
