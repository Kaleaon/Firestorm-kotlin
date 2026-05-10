package com.firestorm.newview

import com.firestorm.ui.Floater
import com.firestorm.ui.ScrollListCtrl
import com.firestorm.viewer.Agent
import com.firestorm.viewer.FloaterReg
import com.firestorm.viewer.FloaterWorldMap
import com.firestorm.viewer.GroupActions
import com.firestorm.viewer.MessageSystem
import com.firestorm.viewer.ProductInfoRequestManager
import com.firestorm.viewer.StatusBar
import java.util.UUID

class FloaterLandHoldings(key: LLSD) : Floater(key) {

    private var actualArea: Int = 0
    private var billableArea: Int = 0
    private var firstPacketReceived: Boolean = false
    private var sortColumn: String = ""
    private var sortAscending: Boolean = true

    override fun postBuild(): Boolean {
        childSetAction("Teleport")    { onClickTeleport(this) }
        childSetAction("Show on Map") { onClickMap(this) }

        val grantList = getChild<ScrollListCtrl>("grant list")
        grantList.sortByColumnIndex(0, ascending = true)
        grantList.setDoubleClickCallback { onGrantList(this) }

        Agent.groups.forEach { group ->
            val areaStr = getString("area_string").replace("[AREA]", "${group.contribution}")
            grantList.addElement(
                LLSD.map(
                    "id" to group.id,
                    "columns" to LLSD.array(
                        LLSD.map("column" to "group", "value" to group.name, "font" to "SANSSERIF"),
                        LLSD.map("column" to "area",  "value" to areaStr,   "font" to "SANSSERIF")
                    )
                )
            )
        }

        center()
        return true
    }

    override fun onOpen(key: LLSD) {
        val list = getChild<ScrollListCtrl>("parcel list")
        list.clearRows()
        TODO("APR: use JVM equivalent — send places query for agent-owned parcels (DFQ_AGENT_OWNED)")
    }

    override fun draw() {
        refresh()
        super.draw()
    }

    fun refresh() {
        val list = childGetSelectionInterface("parcel list")
        val enableBtns = list != null && list.getFirstSelectedIndex() > -1
        getChildView("Teleport").setEnabled(enableBtns)
        getChildView("Show on Map").setEnabled(enableBtns)
        refreshAggregates()
    }

    fun buttonCore(which: Int) {
        val list = getChild<ScrollListCtrl>("parcel list")
        val index = list.firstSelectedIndex
        if (index < 0) return

        val location = list.getSelectedItemLabel(list.numColumns - 1)
        val parts = location.trim().split(" ")
        val globalX = parts.getOrNull(0)?.toFloatOrNull() ?: 0f
        val globalY = parts.getOrNull(1)?.toFloatOrNull() ?: 0f
        val globalZ = Agent.positionGlobal.z

        val posGlobal = Vector3d(globalX.toDouble(), globalY.toDouble(), globalZ)
        val worldMap = FloaterWorldMap.getInstance()

        when (which) {
            0 -> {
                Agent.teleportViaLocation(posGlobal)
                worldMap?.trackLocation(posGlobal)
            }
            1 -> {
                worldMap?.trackLocation(posGlobal)
                FloaterReg.showInstance("world_map", "center")
            }
        }
    }

    private fun refreshAggregates() {
        val allowedArea   = StatusBar.squareMetersCredit
        val currentArea   = StatusBar.squareMetersCommitted
        val availableArea = StatusBar.squareMetersLeft
        getChild<UICtrl>("allowed_text").setTextArg("[AREA]", "$allowedArea")
        getChild<UICtrl>("current_text").setTextArg("[AREA]", "$currentArea")
        getChild<UICtrl>("available_text").setTextArg("[AREA]", "$availableArea")
    }

    companion object {
        private const val LINDEN_HOMES_SKU = "131"

        var hasLindenHome: Boolean = false
            private set

        fun processPlacesReply(msg: MessageSystem) {
            val self = FloaterReg.findTypedInstance<FloaterLandHoldings>("land_holdings")
            val count = msg.getNumberOfBlocks("QueryData")
            hasLindenHome = false

            if (self == null) {
                for (i in 0 until count) {
                    if (msg.getSize("QueryData", i, "ProductSKU") > 0) {
                        val sku = msg.getString("QueryData", "ProductSKU", i)
                        if (sku == LINDEN_HOMES_SKU) { hasLindenHome = true; return }
                    }
                }
                return
            }

            val list = self.childGetListInterface("parcel list") ?: return

            if (!self.firstPacketReceived) {
                self.firstPacketReceived = true
                list.operateOnAll(SelectionOp.OP_DELETE)
            }

            for (i in 0 until count) {
                val ownerId      = msg.getUUID("QueryData", "OwnerID", i)
                val name         = msg.getString("QueryData", "Name", i)
                val actualArea   = msg.getInt("QueryData", "ActualArea", i)
                val billable     = msg.getInt("QueryData", "BillableArea", i)
                val globalX      = msg.getFloat("QueryData", "GlobalX", i)
                val globalY      = msg.getFloat("QueryData", "GlobalY", i)
                val simName      = msg.getString("QueryData", "SimName", i)

                val landSku: String
                val landType: String
                if (msg.getSize("QueryData", i, "ProductSKU") > 0) {
                    landSku = msg.getString("QueryData", "ProductSKU", i)
                    landType = ProductInfoRequestManager.instance.getDescriptionForSku(landSku)
                    if (landSku == LINDEN_HOMES_SKU) hasLindenHome = true
                } else {
                    landSku = ""
                    landType = Trans.getString("land_type_unknown")
                }

                if (ownerId == NULL_UUID) continue

                self.actualArea   += actualArea
                self.billableArea += billable

                val regionX = Math.round(globalX) % REGION_WIDTH_UNITS
                val regionY = Math.round(globalY) % REGION_WIDTH_UNITS

                val location = "$simName ($regionX, $regionY)"
                val area = if (billable == actualArea) "$billable" else "$billable / $actualArea"
                val hidden = "$globalX $globalY"

                val element = LLSD.map(
                    "columns" to LLSD.array(
                        LLSD.map("column" to "name",     "value" to name,     "font" to "SANSSERIF"),
                        LLSD.map("column" to "location", "value" to location, "font" to "SANSSERIF"),
                        LLSD.map("column" to "area",     "value" to area,     "font" to "SANSSERIF"),
                        LLSD.map("column" to "type",     "value" to landType, "font" to "SANSSERIF"),
                        LLSD.map("column" to "hidden",   "value" to hidden)
                    )
                )
                list.addElement(element)
            }

            self.refreshAggregates()
        }

        private fun onClickTeleport(self: FloaterLandHoldings) {
            self.buttonCore(0)
            self.closeFloater()
        }

        private fun onClickMap(self: FloaterLandHoldings) {
            self.buttonCore(1)
        }

        private fun onGrantList(self: FloaterLandHoldings) {
            val list = self.childGetSelectionInterface("grant list") ?: return
            val groupId = list.currentId
            if (groupId != NULL_UUID) GroupActions.show(groupId)
        }

        private val NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
        private const val REGION_WIDTH_UNITS = 256
    }
}
