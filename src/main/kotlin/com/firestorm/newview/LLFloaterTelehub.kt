package com.firestorm.newview

import java.util.UUID

const val MAX_SPAWNPOINTS_PER_TELEHUB: Int = 16

class LLFloaterTelehub(key: LLSD) : LLFloater(key) {

    private var telehubObjectID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private var telehubObjectName: String = ""
    private var telehubPos: LLVector3 = LLVector3()
    private var telehubRot: LLQuaternion = LLQuaternion()
    private var numSpawn: Int = 0
    private val spawnPointPos: Array<LLVector3> = Array(MAX_SPAWNPOINTS_PER_TELEHUB) { LLVector3() }
    private var objectSelection: LLSafeHandle<LLObjectSelection>? = null

    override fun postBuild(): Boolean {
        gMessageSystem.setHandlerFunc("TelehubInfo", ::processTelehubInfo)

        getChild<LLUICtrl>("connect_btn").setCommitCallback { onClickConnect() }
        getChild<LLUICtrl>("disconnect_btn").setCommitCallback { onClickDisconnect() }
        getChild<LLUICtrl>("add_spawn_point_btn").setCommitCallback { onClickAddSpawnPoint() }
        getChild<LLUICtrl>("remove_spawn_point_btn").setCommitCallback { onClickRemoveSpawnPoint() }

        val list = getChild<LLScrollListCtrl>("spawn_points_list")
        // arrow keys must remain available for walking while floater is open
        list?.setAllowKeyboardMovement(false)

        return true
    }

    override fun onOpen(key: LLSD) {
        LLToolMgr.instance.setCurrentToolset(gBasicToolset)
        LLToolMgr.instance.currentToolset.selectTool(LLToolCompTranslate.instance)

        sendTelehubInfoRequest()

        objectSelection = LLSelectMgr.instance.getEditSelection()
    }

    override fun draw() {
        if (!isMinimized()) {
            refresh()
        }
        super.draw()
    }

    override fun refresh() {
        val obj = objectSelection?.getFirstRootObject(childrenOk = true)

        val haveSelection = obj != null
        val allVolume = LLSelectMgr.instance.selectionAllPCode(LL_PCODE_VOLUME)
        getChildView("connect_btn").setEnabled(haveSelection && allVolume)

        val haveTelehub = telehubObjectID != NULL_UUID
        getChildView("disconnect_btn").setEnabled(haveTelehub)

        val spaceAvail = numSpawn < MAX_SPAWNPOINTS_PER_TELEHUB
        getChildView("add_spawn_point_btn").setEnabled(haveSelection && allVolume && spaceAvail)

        val list = getChild<LLScrollListCtrl>("spawn_points_list")
        if (list != null) {
            val enableRemove = list.getFirstSelected() != null
            getChildView("remove_spawn_point_btn").setEnabled(enableRemove)
        }
    }

    fun sendTelehubInfoRequest() {
        LLSelectMgr.instance.sendGodlikeRequest("telehub", "info ui")
    }

    fun onClickConnect() {
        LLSelectMgr.instance.sendGodlikeRequest("telehub", "connect")
    }

    fun onClickDisconnect() {
        LLSelectMgr.instance.sendGodlikeRequest("telehub", "delete")
    }

    fun onClickAddSpawnPoint() {
        LLSelectMgr.instance.sendGodlikeRequest("telehub", "spawnpoint add")
        LLSelectMgr.instance.deselectAll()
    }

    fun onClickRemoveSpawnPoint() {
        val list = getChild<LLScrollListCtrl>("spawn_points_list") ?: return

        val spawnIndex = list.getFirstSelectedIndex()
        if (spawnIndex < 0) return

        val msg = gMessageSystem

        // Server will reject unless caller is god or estate owner
        if (gAgent.isGodlike()) {
            msg.newMessage("GodlikeMessage")
        } else {
            msg.newMessage("EstateOwnerMessage")
        }
        msg.nextBlock("AgentData")
        msg.addUUID("AgentID", gAgent.id)
        msg.addUUID("SessionID", gAgent.sessionID)
        msg.addUUID("TransactionID", NULL_UUID)
        msg.nextBlock("MethodData")
        msg.addString("Method", "telehub")
        msg.addUUID("Invoice", NULL_UUID)

        msg.nextBlock("ParamList")
        msg.addString("Parameter", "spawnpoint remove")

        msg.nextBlock("ParamList")
        msg.addString("Parameter", spawnIndex.toString())

        gAgent.sendReliableMessage()
    }

    fun unpackTelehubInfo(msg: LLMessageSystem) {
        telehubObjectID = msg.getUUID("TelehubBlock", "ObjectID")
        telehubObjectName = msg.getString("TelehubBlock", "ObjectName")
        telehubPos = msg.getVector3("TelehubBlock", "TelehubPos")
        telehubRot = msg.getQuat("TelehubBlock", "TelehubRot")

        numSpawn = msg.getNumberOfBlocks("SpawnPointBlock")
        for (i in 0 until numSpawn) {
            spawnPointPos[i] = msg.getVector3("SpawnPointBlock", "SpawnPointPos", i)
        }

        if (telehubObjectID == NULL_UUID) {
            getChildView("status_text_connected").setVisible(false)
            getChildView("status_text_not_connected").setVisible(true)
            getChildView("help_text_connected").setVisible(false)
            getChildView("help_text_not_connected").setVisible(true)
        } else {
            getChild<LLUICtrl>("status_text_connected").setTextArg("[OBJECT]", telehubObjectName)
            getChildView("status_text_connected").setVisible(true)
            getChildView("status_text_not_connected").setVisible(false)
            getChildView("help_text_connected").setVisible(true)
            getChildView("help_text_not_connected").setVisible(false)
        }

        val list = getChild<LLScrollListCtrl>("spawn_points_list")
        if (list != null) {
            list.deleteAllItems()
            for (i in 0 until numSpawn) {
                val pos = "%.1f, %.1f, %.1f".format(
                    spawnPointPos[i].x,
                    spawnPointPos[i].y,
                    spawnPointPos[i].z
                )
                list.addSimpleElement(pos)
            }
            list.selectNthItem(numSpawn - 1)
        }
    }

    companion object {
        fun renderBeacons(): Boolean {
            val floater = LLFloaterReg.findTypedInstance<LLFloaterTelehub>("telehubs")
            return floater != null && floater.telehubObjectID != NULL_UUID
        }

        fun addBeacons() {
            val floater = LLFloaterReg.findTypedInstance<LLFloaterTelehub>("telehubs") ?: return

            var hubPosRegion = floater.telehubPos
            var hubRot = floater.telehubRot

            val obj = gObjectList.findObject(floater.telehubObjectID)
            if (obj != null) {
                hubPosRegion = obj.getPositionRegion()
                hubRot = obj.getRotationRegion()
            }

            gObjectList.addDebugBeacon(hubPosRegion, "", LLColor4.yellow, LLColor4.white, 4)

            val list = floater.getChild<LLScrollListCtrl>("spawn_points_list")
            if (list != null) {
                val spawnIndex = list.getFirstSelectedIndex()
                if (spawnIndex >= 0) {
                    val spawnPos = hubPosRegion + (floater.spawnPointPos[spawnIndex] * hubRot)
                    gObjectList.addDebugBeacon(spawnPos, "", LLColor4.orange, LLColor4.white, 4)
                }
            }
        }

        fun processTelehubInfo(msg: LLMessageSystem, @Suppress("UNUSED_PARAMETER") unused: Any?) {
            LLFloaterReg.findTypedInstance<LLFloaterTelehub>("telehubs")?.unpackTelehubInfo(msg)
        }
    }
}
