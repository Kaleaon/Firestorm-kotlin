package com.firestorm.newview

import com.firestorm.floater.Floater
import com.firestorm.objects.BBox
import com.firestorm.objects.SelectionCost
import com.firestorm.objects.SelectedObjectFunctor
import com.firestorm.objects.ViewerObject
import com.firestorm.parcel.Parcel
import com.firestorm.ui.LLSD
import com.firestorm.ui.TextBox

private val LOD_STRINGS = arrayOf("lowest_lod", "low_lod", "medium_lod", "high_lod")

class CrossParcelFunctor : SelectedObjectFunctor() {
    private var boundingBox = BBox()

    override fun apply(obj: ViewerObject): Boolean {
        val halfScale = obj.scale * 0.5f
        boundingBox.addBBoxAgent(
            BBox(obj.positionRegion, obj.rotationRegion, -halfScale, halfScale).axisAligned
        )

        for (child in obj.children) {
            val childHalf = child.scale * 0.5f
            boundingBox.addBBoxAgent(
                BBox(child.positionRegion, child.rotationRegion, -childHalf, childHalf).axisAligned
            )
        }

        val region = obj.region ?: return false
        return region.objectsCrossParcel(listOf(boundingBox))
    }
}

class FloaterObjectWeights(key: LLSD) : Floater(key), AccountingCostObserver {

    private var selectedObjects: TextBox? = null
    private var selectedPrims: TextBox? = null

    private var selectedDownloadWeight: TextBox? = null
    private var selectedPhysicsWeight: TextBox? = null
    private var selectedServerWeight: TextBox? = null
    private var selectedDisplayWeight: TextBox? = null

    private var selectedOnLand: TextBox? = null
    private var rezzedOnLand: TextBox? = null
    private var remainingCapacity: TextBox? = null
    private var totalCapacity: TextBox? = null

    private var lodLevel: TextBox? = null
    private var trianglesShown: TextBox? = null
    private var pixelArea: TextBox? = null

    override fun postBuild(): Boolean {
        selectedObjects       = getChild("objects")
        selectedPrims         = getChild("prims")
        selectedDownloadWeight = getChild("download")
        selectedPhysicsWeight = getChild("physics")
        selectedServerWeight  = getChild("server")
        selectedDisplayWeight = getChild("display")
        selectedOnLand        = getChild("selected")
        rezzedOnLand          = getChild("rezzed_on_land")
        remainingCapacity     = getChild("remaining_capacity")
        totalCapacity         = getChild("total_capacity")
        lodLevel              = getChild("lod_level")
        trianglesShown        = getChild("triangles_shown")
        pixelArea             = getChild("pixel_area")
        return true
    }

    override fun onOpen(key: LLSD) {
        refresh()
        updateLandImpacts(ViewerParcelMgr.instance.floatingParcelSelection.parcel)
    }

    override fun onWeightsUpdate(selectionCost: SelectionCost) {
        selectedDownloadWeight?.setText("%.1f".format(selectionCost.networkCost))
        selectedPhysicsWeight?.setText("%.1f".format(selectionCost.physicsCost))
        selectedServerWeight?.setText("%.1f".format(selectionCost.simulationCost))

        val renderCost = SelectMgr.instance.selection.selectedObjectRenderCost
        selectedDisplayWeight?.setText("%d".format(renderCost))

        toggleWeightsLoadingIndicators(false)
    }

    override fun setErrorStatus(status: Int, reason: String) {
        val text = getString("nothing_selected")
        selectedDownloadWeight?.setText(text)
        selectedPhysicsWeight?.setText(text)
        selectedServerWeight?.setText(text)
        selectedDisplayWeight?.setText(text)
        toggleWeightsLoadingIndicators(false)
    }

    override fun draw() {
        val selection = SelectMgr.instance.selection
        if (selection.isEmpty) {
            val text = getString("nothing_selected")
            lodLevel?.setText(text)
            trianglesShown?.setText(text)
            pixelArea?.setText(text)
            toggleRenderLoadingIndicators(false)
        } else {
            var objectLod = -1
            var multipleLods = false
            var totalTris = 0
            var pixelAreaAcc = 0f

            for (node in selection.validRootIterator()) {
                val obj = node.`object`
                val lod = obj.lod
                when {
                    objectLod < 0  -> objectLod = lod
                    objectLod != lod -> multipleLods = true
                }
                if (obj.isRootEdit) {
                    totalTris += obj.recursiveGetTriangleCount()
                    pixelAreaAcc += obj.pixelArea
                }
            }

            when {
                multipleLods -> {
                    lodLevel?.setText(getString("multiple_lods"))
                    toggleRenderLoadingIndicators(false)
                }
                objectLod < 0 -> toggleRenderLoadingIndicators(true)
                else -> {
                    lodLevel?.setText(getString(LOD_STRINGS[objectLod]))
                    toggleRenderLoadingIndicators(false)
                }
            }
            trianglesShown?.setText("%d".format(totalTris))
            pixelArea?.setText("%d".format(pixelAreaAcc.toLong()))
        }
        super.draw()
    }

    fun updateLandImpacts(parcel: Parcel?) {
        if (parcel == null || SelectMgr.instance.selection.isEmpty) {
            updateIfNothingSelected()
            return
        }

        var totalCap = parcel.simWideMaxPrimCapacity
        val region = ViewerParcelMgr.instance.selectionRegion
        if (region != null) {
            totalCap = minOf(totalCap, region.maxTasks.toInt())
        }
        val rezzed = parcel.simWidePrimCount
        rezzedOnLand?.setText("%d".format(rezzed))
        remainingCapacity?.setText("%d".format(totalCap - rezzed))
        totalCapacity?.setText("%d".format(totalCap))
        toggleLandImpactsLoadingIndicators(false)
    }

    override fun refresh() {
        val selMgr = SelectMgr.instance
        if (selMgr.selection.isEmpty) {
            updateIfNothingSelected()
            return
        }

        val primCount = selMgr.selection.objectCount
        val linkCount = selMgr.selection.rootObjectCount
        val primEquiv = selMgr.selection.selectedLinksetCost

        selectedObjects?.setText("%d".format(linkCount))
        selectedPrims?.setText("%d".format(primCount))
        selectedOnLand?.setText("%d".format(primEquiv.toInt()))

        val func = CrossParcelFunctor()
        if (selMgr.selection.applyToRootObjects(func, true)) {
            val text = getString("nothing_selected")
            rezzedOnLand?.setText(text)
            remainingCapacity?.setText(text)
            totalCapacity?.setText(text)
            toggleLandImpactsLoadingIndicators(false)
        }

        val region = Agent.region
        if (region != null && region.capabilitiesReceived) {
            for (node in selMgr.selection.validRootIterator()) {
                AccountingCostMgr.instance.addObject(node.`object`.id)
            }
            val url = region.getCapability("ResourceCostSelected")
            if (url.isNotEmpty()) {
                generateTransactionId()
                AccountingCostMgr.instance.fetchCosts(FetchType.Roots, url, observerHandle)
                toggleWeightsLoadingIndicators(true)
            }
        }
    }

    override fun generateTransactionId() {
        transactionId.generate()
    }

    private fun toggleWeightsLoadingIndicators(visible: Boolean) {
        childSetVisible("download_loading_indicator", visible)
        childSetVisible("physics_loading_indicator", visible)
        childSetVisible("server_loading_indicator", visible)
        childSetVisible("display_loading_indicator", visible)
        selectedDownloadWeight?.setVisible(!visible)
        selectedPhysicsWeight?.setVisible(!visible)
        selectedServerWeight?.setVisible(!visible)
        selectedDisplayWeight?.setVisible(!visible)
    }

    private fun toggleLandImpactsLoadingIndicators(visible: Boolean) {
        childSetVisible("selected_loading_indicator", visible)
        childSetVisible("rezzed_on_land_loading_indicator", visible)
        childSetVisible("remaining_capacity_loading_indicator", visible)
        childSetVisible("total_capacity_loading_indicator", visible)
        selectedOnLand?.setVisible(!visible)
        rezzedOnLand?.setVisible(!visible)
        remainingCapacity?.setVisible(!visible)
        totalCapacity?.setVisible(!visible)
    }

    private fun toggleRenderLoadingIndicators(visible: Boolean) {
        childSetVisible("lod_level_loading_indicator", visible)
        childSetVisible("triangles_shown_loading_indicator", visible)
        childSetVisible("pixel_area_loading_indicator", visible)
        lodLevel?.setVisible(!visible)
        trianglesShown?.setVisible(!visible)
        pixelArea?.setVisible(!visible)
    }

    private fun updateIfNothingSelected() {
        val text = getString("nothing_selected")
        selectedObjects?.setText(text)
        selectedPrims?.setText(text)
        selectedDownloadWeight?.setText(text)
        selectedPhysicsWeight?.setText(text)
        selectedServerWeight?.setText(text)
        selectedDisplayWeight?.setText(text)
        selectedOnLand?.setText(text)
        rezzedOnLand?.setText(text)
        remainingCapacity?.setText(text)
        totalCapacity?.setText(text)
        lodLevel?.setText(text)
        trianglesShown?.setText(text)
        pixelArea?.setText(text)
        toggleWeightsLoadingIndicators(false)
        toggleLandImpactsLoadingIndicators(false)
        toggleRenderLoadingIndicators(false)
    }
}
