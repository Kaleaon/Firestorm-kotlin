package com.firestorm.newview

import java.util.UUID

private const val LINDEN_HOMES_SKU = "131"

class LLFloaterLandHoldings(key: LLSD) : LLFloater(key) {

    companion object {
        var sHasLindenHome: Boolean = false

        fun processPlacesReply(msg: LLMessageSystem) {
            val self = LLFloaterReg.findTypedInstance<LLFloaterLandHoldings>("land_holdings")
            val count = msg.getNumberOfBlocks("QueryData")
            sHasLindenHome = false

            if (self == null) {
                for (i in 0 until count) {
                    if (msg.getSizeFast("QueryData", i, "ProductSKU") > 0) {
                        val landSku = msg.getStringFast("QueryData", "ProductSKU", i)
                        if (landSku == LINDEN_HOMES_SKU) {
                            sHasLindenHome = true
                            return
                        }
                    }
                }
                return
            }

            val list = self.childGetListInterface("parcel list") ?: return

            if (!self.mFirstPacketReceived) {
                self.mFirstPacketReceived = true
                list.operateOnAll(LLCtrlSelectionInterface.OP_DELETE)
            }

            for (i in 0 until count) {
                val ownerId    = msg.getUUID("QueryData", "OwnerID", i)
                val name       = msg.getString("QueryData", "Name", i)
                val actualArea = msg.getS32("QueryData", "ActualArea", i)
                val billArea   = msg.getS32("QueryData", "BillableArea", i)
                val globalX    = msg.getF32("QueryData", "GlobalX", i)
                val globalY    = msg.getF32("QueryData", "GlobalY", i)
                val simName    = msg.getString("QueryData", "SimName", i)

                val landSku: String
                val landType: String
                if (msg.getSizeFast("QueryData", i, "ProductSKU") > 0) {
                    landSku  = msg.getStringFast("QueryData", "ProductSKU", i)
                    landType = LLProductInfoRequestManager.instance().getDescriptionForSku(landSku)
                    if (landSku == LINDEN_HOMES_SKU) sHasLindenHome = true
                } else {
                    landSku  = ""
                    landType = LLTrans.getString("land_type_unknown")
                }

                if (ownerId != null) {
                    self.mActualArea   += actualArea
                    self.mBillableArea += billArea

                    val regionX = Math.round(globalX) % REGION_WIDTH_UNITS
                    val regionY = Math.round(globalY) % REGION_WIDTH_UNITS
                    val location = "$simName ($regionX, $regionY)"

                    val area = if (billArea == actualArea) billArea.toString()
                    else "$billArea / $actualArea"

                    val hidden = "$globalX $globalY"

                    val element = buildListElement(name, location, area, landType, hidden)
                    list.addElement(element)
                }
            }

            self.refreshAggregates()
        }

        private fun buildListElement(
            name: String, location: String, area: String, type: String, hidden: String
        ): LLSD {
            return llsd {
                "columns" to listOf(
                    mapOf("column" to "name",     "value" to name,     "font" to "SANSSERIF"),
                    mapOf("column" to "location", "value" to location, "font" to "SANSSERIF"),
                    mapOf("column" to "area",     "value" to area,     "font" to "SANSSERIF"),
                    mapOf("column" to "type",     "value" to type,     "font" to "SANSSERIF"),
                    mapOf("column" to "hidden",   "value" to hidden)
                )
            }
        }
    }

    private var mActualArea: Int = 0
    private var mBillableArea: Int = 0
    private var mFirstPacketReceived: Boolean = false
    private var mSortColumn: String = ""
    private var mSortAscending: Boolean = true

    override fun postBuild(): Boolean {
        childSetAction("Teleport")     { onClickTeleport(this) }
        childSetAction("Show on Map")  { onClickMap(this) }

        val grantList = getChild<LLScrollListCtrl>("grant list")
        grantList?.sortByColumnIndex(0, true)
        grantList?.setDoubleClickCallback { onGrantList(this) }

        for (i in gAgent.mGroups.indices) {
            val group    = gAgent.mGroups[i]
            val areaStr  = getString("area_string").replace("[AREA]", group.mContribution.toString())
            grantList?.addElement(
                mapOf(
                    "id" to group.mID,
                    "columns" to listOf(
                        mapOf("column" to "group", "value" to group.mName, "font" to "SANSSERIF"),
                        mapOf("column" to "area",  "value" to areaStr,    "font" to "SANSSERIF")
                    )
                )
            )
        }

        center()
        return true
    }

    override fun onOpen(key: LLSD) {
        getChild<LLScrollListCtrl>("parcel list")?.clearRows()
        System.err.println("LLFloaterLandHoldings: onOpen send_places_query not yet implemented")
    }

    override fun draw() {
        refresh()
        super.draw()
    }

    fun refresh() {
        val list = childGetSelectionInterface("parcel list")
        val enableBtns = list != null && list.getFirstSelectedIndex() > -1
        getChildView("Teleport")?.setEnabled(enableBtns)
        getChildView("Show on Map")?.setEnabled(enableBtns)
        refreshAggregates()
    }

    fun buttonCore(which: Int) {
        val list = getChild<LLScrollListCtrl>("parcel list") ?: return
        val index = list.getFirstSelectedIndex()
        if (index < 0) return

        val location = list.getSelectedItemLabel(list.getNumColumns() - 1)
        val parts = location.trim().split(" ")
        val globalX = parts.getOrNull(0)?.toFloatOrNull() ?: 0f
        val globalY = parts.getOrNull(1)?.toFloatOrNull() ?: 0f
        val globalZ = gAgent.getPositionGlobal().z

        val posGlobal = LLVector3d(globalX.toDouble(), globalY.toDouble(), globalZ)
        val floaterWorldMap = LLFloaterWorldMap.getInstance()

        when (which) {
            0 -> {
                gAgent.teleportViaLocation(posGlobal)
                floaterWorldMap?.trackLocation(posGlobal)
            }
            1 -> {
                floaterWorldMap?.trackLocation(posGlobal)
                LLFloaterReg.showInstance("world_map", "center")
            }
        }
    }

    private fun refreshAggregates() {
        val allowed   = gStatusBar.getSquareMetersCredit()
        val current   = gStatusBar.getSquareMetersCommitted()
        val available = gStatusBar.getSquareMetersLeft()
        getChild<LLUICtrl>("allowed_text")?.setTextArg("[AREA]", allowed.toString())
        getChild<LLUICtrl>("current_text")?.setTextArg("[AREA]", current.toString())
        getChild<LLUICtrl>("available_text")?.setTextArg("[AREA]", available.toString())
    }
}

private fun onClickTeleport(self: LLFloaterLandHoldings) {
    self.buttonCore(0)
    self.closeFloater()
}

private fun onClickMap(self: LLFloaterLandHoldings) {
    self.buttonCore(1)
}

private fun onGrantList(self: LLFloaterLandHoldings) {
    val list = self.childGetSelectionInterface("grant list") ?: return
    val groupId = list.getCurrentID()
    if (groupId != null) {
        LLGroupActions.show(groupId)
    }
}
