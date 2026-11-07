package com.firestorm.newview

class LLFloaterPathfindingLinksets private constructor(seed: LLSD) : LLFloaterPathfindingObjects(seed) {

    companion object {
        private const val XUI_LINKSET_USE_NONE = 0
        private const val XUI_LINKSET_USE_WALKABLE = 1
        private const val XUI_LINKSET_USE_STATIC_OBSTACLE = 2
        private const val XUI_LINKSET_USE_DYNAMIC_OBSTACLE = 3
        private const val XUI_LINKSET_USE_MATERIAL_VOLUME = 4
        private const val XUI_LINKSET_USE_EXCLUSION_VOLUME = 5
        private const val XUI_LINKSET_USE_DYNAMIC_PHANTOM = 6

        fun openLinksetsWithSelectedObjects() {
            val linksetsFloater = LLFloaterReg.getTypedInstance<LLFloaterPathfindingLinksets>("pathfinding_linksets")
            linksetsFloater.clearFilters()
            linksetsFloater.showFloaterWithSelectionObjects()
        }
    }

    private var filterByName: LLSearchEditor? = null
    private var filterByDescription: LLSearchEditor? = null
    private var filterByLinksetUse: LLComboBox? = null
    private var editLinksetUse: LLComboBox? = null
    private var editLinksetUseUnset: LLScrollListItem? = null
    private var editLinksetUseWalkable: LLScrollListItem? = null
    private var editLinksetUseStaticObstacle: LLScrollListItem? = null
    private var editLinksetUseDynamicObstacle: LLScrollListItem? = null
    private var editLinksetUseMaterialVolume: LLScrollListItem? = null
    private var editLinksetUseExclusionVolume: LLScrollListItem? = null
    private var editLinksetUseDynamicPhantom: LLScrollListItem? = null
    private var labelWalkabilityCoefficients: LLTextBase? = null
    private var labelEditA: LLTextBase? = null
    private var labelSuggestedUseA: LLTextBase? = null
    private var editA: LLLineEditor? = null
    private var labelEditB: LLTextBase? = null
    private var labelSuggestedUseB: LLTextBase? = null
    private var editB: LLLineEditor? = null
    private var labelEditC: LLTextBase? = null
    private var labelSuggestedUseC: LLTextBase? = null
    private var editC: LLLineEditor? = null
    private var labelEditD: LLTextBase? = null
    private var labelSuggestedUseD: LLTextBase? = null
    private var editD: LLLineEditor? = null
    private var applyEditsButton: LLButton? = null

    private var beaconColor: LLColor4 = LLColor4()

    private var previousValueA: LLSD = LLSD(LLPathfindingLinkset.MAX_WALKABILITY_VALUE)
    private var previousValueB: LLSD = LLSD(LLPathfindingLinkset.MAX_WALKABILITY_VALUE)
    private var previousValueC: LLSD = LLSD(LLPathfindingLinkset.MAX_WALKABILITY_VALUE)
    private var previousValueD: LLSD = LLSD(LLPathfindingLinkset.MAX_WALKABILITY_VALUE)

    override fun postBuild(): Boolean {
        beaconColor = LLUIColorTable.getInstance()!!.getColor("PathfindingLinksetBeaconColor")

        filterByName = getChild("filter_by_name")!!
        filterByName!!.setCommitCallback { onApplyAllFilters() }
        filterByName!!.setCommitOnFocusLost(true)

        filterByDescription = getChild("filter_by_description")!!
        filterByDescription!!.setCommitCallback { onApplyAllFilters() }
        filterByDescription!!.setCommitOnFocusLost(true)

        filterByLinksetUse = findChild("filter_by_linkset_use")!!
        filterByLinksetUse!!.setCommitCallback { onApplyAllFilters() }

        childSetAction("apply_filters") { onApplyAllFilters() }
        childSetAction("clear_filters") { onClearFiltersClicked() }

        editLinksetUse = findChild("edit_linkset_use")!!
        editLinksetUse!!.clearRows()

        editLinksetUseUnset = editLinksetUse!!.addElement(
            buildLinksetUseScrollListData(getString("linkset_choose_use"), XUI_LINKSET_USE_NONE))
        editLinksetUseWalkable = editLinksetUse!!.addElement(
            buildLinksetUseScrollListData(getLinksetUseString(LLPathfindingLinkset.ELinksetUse.WALKABLE), XUI_LINKSET_USE_WALKABLE))
        editLinksetUseStaticObstacle = editLinksetUse!!.addElement(
            buildLinksetUseScrollListData(getLinksetUseString(LLPathfindingLinkset.ELinksetUse.STATIC_OBSTACLE), XUI_LINKSET_USE_STATIC_OBSTACLE))
        editLinksetUseDynamicObstacle = editLinksetUse!!.addElement(
            buildLinksetUseScrollListData(getLinksetUseString(LLPathfindingLinkset.ELinksetUse.DYNAMIC_OBSTACLE), XUI_LINKSET_USE_DYNAMIC_OBSTACLE))
        editLinksetUseMaterialVolume = editLinksetUse!!.addElement(
            buildLinksetUseScrollListData(getLinksetUseString(LLPathfindingLinkset.ELinksetUse.MATERIAL_VOLUME), XUI_LINKSET_USE_MATERIAL_VOLUME))
        editLinksetUseExclusionVolume = editLinksetUse!!.addElement(
            buildLinksetUseScrollListData(getLinksetUseString(LLPathfindingLinkset.ELinksetUse.EXCLUSION_VOLUME), XUI_LINKSET_USE_EXCLUSION_VOLUME))
        editLinksetUseDynamicPhantom = editLinksetUse!!.addElement(
            buildLinksetUseScrollListData(getLinksetUseString(LLPathfindingLinkset.ELinksetUse.DYNAMIC_PHANTOM), XUI_LINKSET_USE_DYNAMIC_PHANTOM))

        editLinksetUse!!.selectFirstItem()

        labelWalkabilityCoefficients = findChild("walkability_coefficients_label")!!
        labelEditA = findChild("edit_a_label")!!
        labelSuggestedUseA = findChild("suggested_use_a_label")!!
        editA = findChild("edit_a_value")!!
        editA!!.setPrevalidate(LLTextValidate::validateNonNegativeS32)
        editA!!.setCommitCallback { ctrl, _ -> onWalkabilityCoefficientEntered(ctrl, previousValueA) }

        labelEditB = findChild("edit_b_label")!!
        labelSuggestedUseB = findChild("suggested_use_b_label")!!
        editB = findChild("edit_b_value")!!
        editB!!.setPrevalidate(LLTextValidate::validateNonNegativeS32)
        editB!!.setCommitCallback { ctrl, _ -> onWalkabilityCoefficientEntered(ctrl, previousValueB) }

        labelEditC = findChild("edit_c_label")!!
        labelSuggestedUseC = findChild("suggested_use_c_label")!!
        editC = findChild("edit_c_value")!!
        editC!!.setPrevalidate(LLTextValidate::validateNonNegativeS32)
        editC!!.setCommitCallback { ctrl, _ -> onWalkabilityCoefficientEntered(ctrl, previousValueC) }

        labelEditD = findChild("edit_d_label")!!
        labelSuggestedUseD = findChild("suggested_use_d_label")!!
        editD = findChild("edit_d_value")!!
        editD!!.setPrevalidate(LLTextValidate::validateNonNegativeS32)
        editD!!.setCommitCallback { ctrl, _ -> onWalkabilityCoefficientEntered(ctrl, previousValueD) }

        applyEditsButton = findChild("apply_edit_values")!!
        applyEditsButton!!.setCommitCallback { onApplyChangesClicked() }

        return super.postBuild()
    }

    override fun requestGetObjects() {
        LLPathfindingManager.getInstance().requestGetLinksets(getNewRequestId()) { id, status, list ->
            handleNewObjectList(id, status, list)
        }
    }

    override fun buildObjectsScrollList(objectListPtr: LLPathfindingObjectListPtr) {
        val nameFilter = filterByName!!.getText()
        val descriptionFilter = filterByDescription!!.getText()
        val linksetUseFilter = getFilterLinksetUse()
        val isFilteringName = nameFilter.isNotEmpty()
        val isFilteringDescription = descriptionFilter.isNotEmpty()
        val isFilteringLinksetUse = linksetUseFilter != LLPathfindingLinkset.ELinksetUse.UNKNOWN

        val avatarPosition = gAgent.getPositionAgent()

        val nameFilterUpper = nameFilter.uppercase()
        val descFilterUpper = descriptionFilter.uppercase()

        for ((_, objectPtr) in objectListPtr) {
            val linkset = objectPtr as? LLPathfindingLinkset ?: continue

            if (isFilteringName || isFilteringDescription || isFilteringLinksetUse) {
                val linksetName = if (linkset.isTerrain()) getString("linkset_terrain_name") else linkset.getName()
                val linksetDescription = linkset.getDescription()

                val nameMatch = !isFilteringName || linksetName.uppercase().contains(nameFilterUpper)
                val descMatch = !isFilteringDescription || linksetDescription.uppercase().contains(descFilterUpper)
                val useMatch = !isFilteringLinksetUse || linkset.getLinksetUse() == linksetUseFilter

                if (nameMatch && descMatch && useMatch) {
                    addObjectToScrollList(objectPtr, buildLinksetScrollListItemData(linkset, avatarPosition))
                }
            } else {
                addObjectToScrollList(objectPtr, buildLinksetScrollListItemData(linkset, avatarPosition))
            }
        }
    }

    override fun updateControlsOnScrollListChange() {
        super.updateControlsOnScrollListChange()
        updateEditFieldValues()
        updateStateOnEditFields()
        updateStateOnEditLinksetUse()
    }

    override fun getNameColumnIndex(): Int = 0

    override fun getOwnerNameColumnIndex(): Int = 2

    override fun getOwnerName(obj: LLPathfindingObject): String {
        return when {
            !obj.hasOwner() -> getString("linkset_owner_unknown")
            !obj.hasOwnerName() -> getString("linkset_owner_loading")
            obj.isGroupOwned() -> obj.getOwnerName() + " " + getString("linkset_owner_group")
            else -> obj.getOwnerName()
        }
    }

    override fun getBeaconColor(): LLColor4 = beaconColor

    override fun getEmptyObjectList(): LLPathfindingObjectListPtr = LLPathfindingLinksetList()

    private fun requestSetLinksets(
        linksetList: LLPathfindingObjectListPtr,
        linksetUse: LLPathfindingLinkset.ELinksetUse,
        a: Int, b: Int, c: Int, d: Int
    ) {
        LLPathfindingManager.getInstance().requestSetLinksets(
            getNewRequestId(), linksetList, linksetUse, a, b, c, d
        ) { id, status, list -> handleUpdateObjectList(id, status, list) }
    }

    private fun onApplyAllFilters() {
        rebuildObjectsScrollList()
    }

    private fun onClearFiltersClicked() {
        clearFilters()
        rebuildObjectsScrollList()
    }

    private fun onWalkabilityCoefficientEntered(ctrl: LLUICtrl, previousValue: LLSD) {
        val lineEditor = ctrl as LLLineEditor
        val valueString = lineEditor.getText()

        val value: LLSD
        val doResetValue: Boolean

        if (valueString.isEmpty()) {
            value = previousValue
            doResetValue = true
        } else {
            val intValue = valueString.toIntOrNull()
            if (intValue != null) {
                doResetValue = intValue < LLPathfindingLinkset.MIN_WALKABILITY_VALUE ||
                        intValue > LLPathfindingLinkset.MAX_WALKABILITY_VALUE
                value = LLSD(intValue.coerceIn(
                    LLPathfindingLinkset.MIN_WALKABILITY_VALUE,
                    LLPathfindingLinkset.MAX_WALKABILITY_VALUE))
            } else {
                value = LLSD(LLPathfindingLinkset.MAX_WALKABILITY_VALUE)
                doResetValue = true
            }
        }

        if (doResetValue) {
            lineEditor.setValue(value)
        }

        when (lineEditor) {
            editA -> previousValueA = value
            editB -> previousValueB = value
            editC -> previousValueC = value
            editD -> previousValueD = value
        }
    }

    private fun onApplyChangesClicked() {
        applyEdit()
    }

    private fun clearFilters() {
        filterByName!!.clear()
        filterByDescription!!.clear()
        setFilterLinksetUse(LLPathfindingLinkset.ELinksetUse.UNKNOWN)
    }

    private fun updateEditFieldValues() {
        val numSelected = getNumSelectedObjects()
        if (numSelected <= 0) {
            editLinksetUse!!.selectFirstItem()
            editA!!.clear()
            editB!!.clear()
            editC!!.clear()
            editD!!.clear()
        } else {
            val firstSelected = getFirstSelectedObject()
            val linkset = firstSelected as? LLPathfindingLinkset ?: return
            setEditLinksetUse(linkset.getLinksetUse())
            previousValueA = LLSD(linkset.getWalkabilityCoefficientA())
            previousValueB = LLSD(linkset.getWalkabilityCoefficientB())
            previousValueC = LLSD(linkset.getWalkabilityCoefficientC())
            previousValueD = LLSD(linkset.getWalkabilityCoefficientD())
            editA!!.setValue(previousValueA)
            editB!!.setValue(previousValueB)
            editC!!.setValue(previousValueC)
            editD!!.setValue(previousValueD)
        }
    }

    private fun buildLinksetScrollListItemData(linkset: LLPathfindingLinkset, avatarPosition: LLVector3): LLSD {
        val columns = LLSD.emptyArray()

        if (linkset.isTerrain()) {
            columns[0] = LLSD().also { it["column"] = "name"; it["value"] = getString("linkset_terrain_name") }
            columns[1] = LLSD().also { it["column"] = "description"; it["value"] = getString("linkset_terrain_description") }
            columns[2] = LLSD().also { it["column"] = "owner"; it["value"] = getString("linkset_terrain_owner") }
            columns[3] = LLSD().also { it["column"] = "scripted"; it["value"] = getString("linkset_terrain_scripted") }
            columns[4] = LLSD().also { it["column"] = "land_impact"; it["value"] = getString("linkset_terrain_land_impact") }
            columns[5] = LLSD().also { it["column"] = "dist_from_you"; it["value"] = getString("linkset_terrain_dist_from_you") }
        } else {
            columns[0] = LLSD().also { it["column"] = "name"; it["value"] = linkset.getName() }
            columns[1] = LLSD().also { it["column"] = "description"; it["value"] = linkset.getDescription() }
            columns[2] = LLSD().also { it["column"] = "owner"; it["value"] = getOwnerName(linkset) }
            val scriptedValue = when {
                !linkset.hasIsScripted() -> getString("linkset_is_unknown_scripted")
                linkset.isScripted() -> getString("linkset_is_scripted")
                else -> getString("linkset_is_not_scripted")
            }
            columns[3] = LLSD().also { it["column"] = "scripted"; it["value"] = scriptedValue }
            columns[4] = LLSD().also { it["column"] = "land_impact"; it["value"] = linkset.getLandImpact().toString() }
            columns[5] = LLSD().also { it["column"] = "dist_from_you"; it["value"] = "%.0f m".format(distVec(avatarPosition, linkset.getLocation())) }
        }

        var linksetUseStr = getLinksetUseString(linkset.getLinksetUse())
        linksetUseStr += when {
            linkset.isTerrain() -> " " + getString("linkset_is_terrain")
            !linkset.isModifiable() && linkset.canBeVolume() -> " " + getString("linkset_is_restricted_state")
            linkset.isModifiable() && !linkset.canBeVolume() -> " " + getString("linkset_is_non_volume_state")
            !linkset.isModifiable() && !linkset.canBeVolume() -> " " + getString("linkset_is_restricted_non_volume_state")
            else -> ""
        }
        columns[6] = LLSD().also { it["column"] = "linkset_use"; it["value"] = linksetUseStr }

        columns[7] = LLSD().also { it["column"] = "a_percent"; it["value"] = "%3d".format(linkset.getWalkabilityCoefficientA()) }
        columns[8] = LLSD().also { it["column"] = "b_percent"; it["value"] = "%3d".format(linkset.getWalkabilityCoefficientB()) }
        columns[9] = LLSD().also { it["column"] = "c_percent"; it["value"] = "%3d".format(linkset.getWalkabilityCoefficientC()) }
        columns[10] = LLSD().also { it["column"] = "d_percent"; it["value"] = "%3d".format(linkset.getWalkabilityCoefficientD()) }

        return columns
    }

    private fun buildLinksetUseScrollListData(label: String, value: Int): LLSD {
        val columns = LLSD()
        columns[0] = LLSD().also { it["column"] = "name"; it["value"] = label; it["font"] = "SANSSERIF" }
        val element = LLSD()
        element["value"] = value
        element["column"] = columns
        return element
    }

    private fun isShowUnmodifiablePhantomWarning(linksetUse: LLPathfindingLinkset.ELinksetUse): Boolean {
        if (linksetUse == LLPathfindingLinkset.ELinksetUse.UNKNOWN) return false
        val selectedObjects = getSelectedObjects()
        if (selectedObjects.isEmpty()) return false
        val linksetList = selectedObjects as? LLPathfindingLinksetList ?: return false
        return linksetList.isShowUnmodifiablePhantomWarning(linksetUse)
    }

    private fun isShowPhantomToggleWarning(linksetUse: LLPathfindingLinkset.ELinksetUse): Boolean {
        if (linksetUse == LLPathfindingLinkset.ELinksetUse.UNKNOWN) return false
        val selectedObjects = getSelectedObjects()
        if (selectedObjects.isEmpty()) return false
        val linksetList = selectedObjects as? LLPathfindingLinksetList ?: return false
        return linksetList.isShowPhantomToggleWarning(linksetUse)
    }

    private fun isShowCannotBeVolumeWarning(linksetUse: LLPathfindingLinkset.ELinksetUse): Boolean {
        if (linksetUse == LLPathfindingLinkset.ELinksetUse.UNKNOWN) return false
        val selectedObjects = getSelectedObjects()
        if (selectedObjects.isEmpty()) return false
        val linksetList = selectedObjects as? LLPathfindingLinksetList ?: return false
        return linksetList.isShowCannotBeVolumeWarning(linksetUse)
    }

    private fun updateStateOnEditFields() {
        val numSelected = getNumSelectedObjects()
        val isEditEnabled = numSelected > 0

        editLinksetUse!!.setEnabled(isEditEnabled)
        labelWalkabilityCoefficients!!.setEnabled(isEditEnabled)
        labelEditA!!.setEnabled(isEditEnabled)
        labelEditB!!.setEnabled(isEditEnabled)
        labelEditC!!.setEnabled(isEditEnabled)
        labelEditD!!.setEnabled(isEditEnabled)
        labelSuggestedUseA!!.setEnabled(isEditEnabled)
        labelSuggestedUseB!!.setEnabled(isEditEnabled)
        labelSuggestedUseC!!.setEnabled(isEditEnabled)
        labelSuggestedUseD!!.setEnabled(isEditEnabled)
        editA!!.setEnabled(isEditEnabled)
        editB!!.setEnabled(isEditEnabled)
        editC!!.setEnabled(isEditEnabled)
        editD!!.setEnabled(isEditEnabled)
        applyEditsButton!!.setEnabled(isEditEnabled && getMessagingState() == EMessagingState.COMPLETE)
    }

    private fun updateStateOnEditLinksetUse() {
        var useWalkable = false
        var useStaticObstacle = false
        var useDynamicObstacle = false
        var useMaterialVolume = false
        var useExclusionVolume = false
        var useDynamicPhantom = false

        val selectedObjects = getSelectedObjects()
        if (!selectedObjects.isEmpty()) {
            val linksetList = selectedObjects as? LLPathfindingLinksetList
            linksetList?.determinePossibleStates(
                useWalkable, useStaticObstacle, useDynamicObstacle,
                useMaterialVolume, useExclusionVolume, useDynamicPhantom
            )
        }

        editLinksetUseWalkable?.setEnabled(useWalkable)
        editLinksetUseStaticObstacle?.setEnabled(useStaticObstacle)
        editLinksetUseDynamicObstacle?.setEnabled(useDynamicObstacle)
        editLinksetUseMaterialVolume?.setEnabled(useMaterialVolume)
        editLinksetUseExclusionVolume?.setEnabled(useExclusionVolume)
        editLinksetUseDynamicPhantom?.setEnabled(useDynamicPhantom)
    }

    private fun applyEdit() {
        val linksetUse = getEditLinksetUse()
        val showPhantomToggle = isShowPhantomToggleWarning(linksetUse)
        val showUnmodifiable = isShowUnmodifiablePhantomWarning(linksetUse)
        val showCannotBeVolume = isShowCannotBeVolumeWarning(linksetUse)

        if (showPhantomToggle || showUnmodifiable || showCannotBeVolume) {
            val restrictedUse = LLPathfindingLinkset.getLinksetUseWithToggledPhantom(linksetUse)
            val substitutions = LLSD()
            substitutions["REQUESTED_TYPE"] = getLinksetUseString(linksetUse)
            substitutions["RESTRICTED_TYPE"] = getLinksetUseString(restrictedUse)

            var notificationName = "PathfindingLinksets"
            if (showPhantomToggle) notificationName += "_WarnOnPhantom"
            if (showUnmodifiable) notificationName += "_MismatchOnRestricted"
            if (showCannotBeVolume) notificationName += "_MismatchOnVolume"

            LLNotificationsUtil.add(notificationName, substitutions, LLSD()) { notification, response ->
                handleApplyEdit(notification, response)
            }
        } else {
            doApplyEdit()
        }
    }

    private fun handleApplyEdit(notification: LLSD, response: LLSD) {
        if (LLNotificationsUtil.getSelectedOption(notification, response) == 0) {
            doApplyEdit()
        }
    }

    private fun doApplyEdit() {
        val selectedObjects = getSelectedObjects()
        if (selectedObjects.isEmpty()) return

        val linksetUse = getEditLinksetUse()
        val aValue = editA!!.getText().toIntOrNull() ?: 0
        val bValue = editB!!.getText().toIntOrNull() ?: 0
        val cValue = editC!!.getText().toIntOrNull() ?: 0
        val dValue = editD!!.getText().toIntOrNull() ?: 0

        requestSetLinksets(selectedObjects, linksetUse, aValue, bValue, cValue, dValue)
    }

    private fun getLinksetUseString(linksetUse: LLPathfindingLinkset.ELinksetUse): String {
        return when (linksetUse) {
            LLPathfindingLinkset.ELinksetUse.WALKABLE -> getString("linkset_use_walkable")
            LLPathfindingLinkset.ELinksetUse.STATIC_OBSTACLE -> getString("linkset_use_static_obstacle")
            LLPathfindingLinkset.ELinksetUse.DYNAMIC_OBSTACLE -> getString("linkset_use_dynamic_obstacle")
            LLPathfindingLinkset.ELinksetUse.MATERIAL_VOLUME -> getString("linkset_use_material_volume")
            LLPathfindingLinkset.ELinksetUse.EXCLUSION_VOLUME -> getString("linkset_use_exclusion_volume")
            LLPathfindingLinkset.ELinksetUse.DYNAMIC_PHANTOM -> getString("linkset_use_dynamic_phantom")
            LLPathfindingLinkset.ELinksetUse.UNKNOWN -> getString("linkset_use_dynamic_obstacle")
        }
    }

    private fun getFilterLinksetUse(): LLPathfindingLinkset.ELinksetUse =
        convertToLinksetUse(filterByLinksetUse!!.getValue())

    private fun setFilterLinksetUse(linksetUse: LLPathfindingLinkset.ELinksetUse) {
        filterByLinksetUse!!.setValue(convertToXuiValue(linksetUse))
    }

    private fun getEditLinksetUse(): LLPathfindingLinkset.ELinksetUse =
        convertToLinksetUse(editLinksetUse!!.getValue())

    private fun setEditLinksetUse(linksetUse: LLPathfindingLinkset.ELinksetUse) {
        editLinksetUse!!.setValue(convertToXuiValue(linksetUse))
    }

    private fun convertToLinksetUse(xuiValue: LLSD): LLPathfindingLinkset.ELinksetUse {
        return when (xuiValue.asInteger()) {
            XUI_LINKSET_USE_WALKABLE -> LLPathfindingLinkset.ELinksetUse.WALKABLE
            XUI_LINKSET_USE_STATIC_OBSTACLE -> LLPathfindingLinkset.ELinksetUse.STATIC_OBSTACLE
            XUI_LINKSET_USE_DYNAMIC_OBSTACLE -> LLPathfindingLinkset.ELinksetUse.DYNAMIC_OBSTACLE
            XUI_LINKSET_USE_MATERIAL_VOLUME -> LLPathfindingLinkset.ELinksetUse.MATERIAL_VOLUME
            XUI_LINKSET_USE_EXCLUSION_VOLUME -> LLPathfindingLinkset.ELinksetUse.EXCLUSION_VOLUME
            XUI_LINKSET_USE_DYNAMIC_PHANTOM -> LLPathfindingLinkset.ELinksetUse.DYNAMIC_PHANTOM
            else -> LLPathfindingLinkset.ELinksetUse.UNKNOWN
        }
    }

    private fun convertToXuiValue(linksetUse: LLPathfindingLinkset.ELinksetUse): LLSD {
        return LLSD(when (linksetUse) {
            LLPathfindingLinkset.ELinksetUse.WALKABLE -> XUI_LINKSET_USE_WALKABLE
            LLPathfindingLinkset.ELinksetUse.STATIC_OBSTACLE -> XUI_LINKSET_USE_STATIC_OBSTACLE
            LLPathfindingLinkset.ELinksetUse.DYNAMIC_OBSTACLE -> XUI_LINKSET_USE_DYNAMIC_OBSTACLE
            LLPathfindingLinkset.ELinksetUse.MATERIAL_VOLUME -> XUI_LINKSET_USE_MATERIAL_VOLUME
            LLPathfindingLinkset.ELinksetUse.EXCLUSION_VOLUME -> XUI_LINKSET_USE_EXCLUSION_VOLUME
            LLPathfindingLinkset.ELinksetUse.DYNAMIC_PHANTOM -> XUI_LINKSET_USE_DYNAMIC_PHANTOM
            LLPathfindingLinkset.ELinksetUse.UNKNOWN -> XUI_LINKSET_USE_NONE
        })
    }
}
