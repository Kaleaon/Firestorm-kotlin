package com.firestorm.newview

import java.util.UUID

class FSFloaterPartialInventory(key: LLSD) : LLFloater(key) {

    private val rootFolderId: UUID = key["start_folder_id"].asUUID()
    private var inventoryList: LLInventoryPanel? = null
    private var filterEdit: LLFilterEditor? = null

    override fun postBuild(): Boolean {
        val params = LLInventoryPanel.Params()
        params.startFolderId(rootFolderId)
        params.follows(FOLLOWS_ALL)
        params.layout("topleft")
        params.name("inv_panel")
        inventoryList = LLUICtrlFactory.create<LLInventoryPanel>(params)

        val wrapperPanel = getChild<LLPanel>("pnl_inv_wrap")
        wrapperPanel.addChild(inventoryList!!)
        inventoryList!!.reshape(wrapperPanel.getRect().width, wrapperPanel.getRect().height)
        inventoryList!!.setOrigin(0, 0)

        filterEdit = getChild<LLFilterEditor>("flt_search")
        filterEdit!!.setCommitCallback { _, param ->
            inventoryList!!.setFilterSubString(param.asString())
        }

        return true
    }

    override fun onOpen(key: LLSD) {
        val args = mutableMapOf("FOLDERNAME" to key["start_folder_name"].asString())
        setTitle(getString("title", args))
    }

    fun getInventoryPanel(): LLInventoryPanel? = inventoryList
}
