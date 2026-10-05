package com.firestorm.newview

val DEFAULT_OBJECT_SCALE: Vector3 = Vector3(0.5f, 0.5f, 0.5f)

open class ToolPlacer : Tool("Create") {

    companion object {
        var objectType: PCode = PCode.CUBE

        fun setObjectType(type: PCode) { objectType = type }
        fun getObjectType(): PCode = objectType

        private fun <P : PlantSpecies> getSelectedPlant(list: Map<UInt, P>, type: String, max: Int): UInt {
            if (type.isNotEmpty() && list.isNotEmpty()) {
                val lastSelected = SavedSettings.getString("LastSelected$type")
                if (lastSelected.isNotEmpty()) {
                    for (i in list.indices) {
                        if (list[i.toUInt()]?.name == lastSelected) return i.toUInt()
                    }
                }
            }
            return (Math.random() * max).toUInt()
        }
    }

    open override fun placeObject(x: Int, y: Int, mask: Int): Boolean {
        if (RlvHandler.isEnabled() &&
            (RlvHandler.instance.hasBehaviour(RlvBehaviour.REZ) ||
             RlvHandler.instance.hasBehaviour(RlvBehaviour.INTERACT))
        ) {
            return true
        }

        val added = if (SavedSettings.getBool("CreateToolCopySelection")) {
            addDuplicate(x, y)
        } else {
            addObject(objectType, x, y, usePhysics = false)
        }

        if (added && !SavedSettings.getBool("CreateToolKeepSelected")) {
            ToolMgr.instance.getCurrentToolset().selectTool(ToolCompTranslate.instance)
        }

        return added
    }

    open override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        ViewerWindow.instance.setCursor(UiCursor.TOOLCREATE)
        return true
    }

    open override fun handleSelect() {
        FloaterTools.instance?.setStatusText("place")
    }

    open override fun handleDeselect() {}

    private fun raycastForNewObjPos(
        x: Int, y: Int,
        hitObjOut: Array<ViewerObject?>,
        hitFaceOut: IntArray,
        bHitLandOut: BooleanArray,
        rayStartRegionOut: Array<Vector3>,
        rayEndRegionOut: Array<Vector3>,
        regionOut: Array<ViewerRegion?>
    ): Boolean {
        val limitSelectDistance = SavedSettings.getBool("LimitSelectDistance")
        val maxDistFromCamera = SavedSettings.getFloat("MaxSelectDistance")

        val pick = ViewerWindow.instance.pickImmediate(x, y, false, false)

        if (pick.pickType == PickInfo.PickType.FLORA) {
            hitObjOut[0] = null
            hitFaceOut[0] = -1
        } else {
            hitObjOut[0] = pick.getObject()
            hitFaceOut[0] = pick.objectFace
        }

        bHitLandOut[0] = hitObjOut[0] == null && !pick.posGlobal.isExactlyZero()
        val landPosGlobal = pick.posGlobal

        val surfacePosGlobal: Vector3d
        val bypassSimRaycast: Boolean

        when {
            bHitLandOut[0] -> {
                surfacePosGlobal = landPosGlobal
                bypassSimRaycast = true
            }
            hitObjOut[0] != null -> {
                surfacePosGlobal = hitObjOut[0]!!.getPositionGlobal()
                bypassSimRaycast = false
            }
            else -> return false
        }

        val rayStartGlobal = AgentCamera.instance.getCameraPositionGlobal()
        val distToSurfaceSq = (surfacePosGlobal - Agent.instance.getPositionGlobal()).magSq().toFloat()

        if (limitSelectDistance && distToSurfaceSq > maxDistFromCamera * maxDistFromCamera) {
            return false
        }

        if (RlvHandler.instance.hasBehaviour(RlvBehaviour.FARTOUCH)) {
            val fartouchDist = RlvCachedBehaviourModifier.getFartouchDist()
            if (Agent.instance.getPositionGlobal().distanceSq(pick.posGlobal) > fartouchDist * fartouchDist) {
                return false
            }
        }

        val region = World.instance.getRegionFromPosGlobal(surfacePosGlobal) ?: return false

        val mouseDir = Vector3d(ViewerWindow.instance.mouseDirectionGlobal(x, y))

        regionOut[0] = region
        var rayStartRegion = region.getPosRegionFromGlobal(rayStartGlobal)
        val nearClip = ViewerCamera.instance.getNear() + 0.01f
        rayStartRegion += ViewerCamera.instance.getAtAxis() * nearClip
        rayStartRegionOut[0] = rayStartRegion

        if (bypassSimRaycast) {
            rayEndRegionOut[0] = region.getPosRegionFromGlobal(surfacePosGlobal)
        } else {
            val maxRaycastDist = if (limitSelectDistance) maxDistFromCamera else 129.0f
            val rayEndGlobal = rayStartGlobal + mouseDir * maxRaycastDist.toDouble()
            rayEndRegionOut[0] = region.getPosRegionFromGlobal(rayEndGlobal)
        }

        return true
    }

    private fun addObject(pcode: PCode, x: Int, y: Int, usePhysics: Boolean): Boolean {
        val hitObj = arrayOfNulls<ViewerObject>(1)
        val hitFace = intArrayOf(-1)
        val bHitLand = booleanArrayOf(false)
        val rayStartRegion = arrayOf(Vector3.ZERO)
        val rayEndRegion = arrayOf(Vector3.ZERO)
        val region = arrayOfNulls<ViewerRegion>(1)

        if (!raycastForNewObjPos(x, y, hitObj, hitFace, bHitLand, rayStartRegion, rayEndRegion, region)) {
            return false
        }

        if (hitObj[0] != null && (hitObj[0]!!.isAvatar() || hitObj[0]!!.isAttachment())) {
            return false
        }

        val regionp = region[0] ?: return false

        val scale = Vector3(
            SavedSettings.getFloat("FSBuildPrefs_Xsize"),
            SavedSettings.getFloat("FSBuildPrefs_Ysize"),
            SavedSettings.getFloat("FSBuildPrefs_Zsize")
        )

        val defaultMaterial = SavedSettings.getString("FSBuildPrefs_Material")
        val material: UByte = when (defaultMaterial) {
            "Wood"    -> MaterialCode.WOOD
            "Stone"   -> MaterialCode.STONE
            "Metal"   -> MaterialCode.METAL
            "Glass"   -> MaterialCode.GLASS
            "Flesh"   -> MaterialCode.FLESH
            "Rubber"  -> MaterialCode.RUBBER
            "Plastic" -> MaterialCode.PLASTIC
            else      -> MaterialCode.WOOD
        }

        var rotation = Quaternion.IDENTITY
        var state: UByte = 0u
        var createSelected = false
        val volumeParams = VolumeParams()

        when (pcode) {
            PCode.LEGACY_GRASS -> {
                scale.set(10f + randomFloat(20f), 10f + randomFloat(20f), 1f + randomFloat(2f))
                state = getSelectedPlant(VOGrass.speciesTable, "Grass", VOGrass.maxGrassSpecies).toUByte()
            }
            PCode.LEGACY_TREE,
            PCode.TREE_NEW -> {
                state = getSelectedPlant(VOTree.speciesTable, "Tree", VOTree.maxTreeSpecies).toUByte()
            }
            PCode.SPHERE -> {
                rotation = Quaternion.fromAxisAngle(Vector3.Y_AXIS, 90f * DEG_TO_RAD)
                volumeParams.setType(ProfileCode.CIRCLE_HALF, PathCode.CIRCLE)
                volumeParams.setBeginAndEndS(0f, 1f); volumeParams.setBeginAndEndT(0f, 1f)
                volumeParams.setRatio(1f, 1f); volumeParams.setShear(0f, 0f)
                createSelected = true
            }
            PCode.TORUS -> {
                rotation = Quaternion.fromAxisAngle(Vector3.Y_AXIS, 90f * DEG_TO_RAD)
                volumeParams.setType(ProfileCode.CIRCLE, PathCode.CIRCLE)
                volumeParams.setBeginAndEndS(0f, 1f); volumeParams.setBeginAndEndT(0f, 1f)
                volumeParams.setRatio(1f, 0.25f); volumeParams.setShear(0f, 0f)
                createSelected = true
            }
            PCode.SQUARE_TORUS -> {
                rotation = Quaternion.fromAxisAngle(Vector3.Y_AXIS, 90f * DEG_TO_RAD)
                volumeParams.setType(ProfileCode.SQUARE, PathCode.CIRCLE)
                volumeParams.setBeginAndEndS(0f, 1f); volumeParams.setBeginAndEndT(0f, 1f)
                volumeParams.setRatio(1f, 0.25f); volumeParams.setShear(0f, 0f)
                createSelected = true
            }
            PCode.TRIANGLE_TORUS -> {
                rotation = Quaternion.fromAxisAngle(Vector3.Y_AXIS, 90f * DEG_TO_RAD)
                volumeParams.setType(ProfileCode.EQUALTRI, PathCode.CIRCLE)
                volumeParams.setBeginAndEndS(0f, 1f); volumeParams.setBeginAndEndT(0f, 1f)
                volumeParams.setRatio(1f, 0.25f); volumeParams.setShear(0f, 0f)
                createSelected = true
            }
            PCode.SPHERE_HEMI -> {
                volumeParams.setType(ProfileCode.CIRCLE_HALF, PathCode.CIRCLE)
                volumeParams.setBeginAndEndT(0f, 0.5f)
                volumeParams.setRatio(1f, 1f); volumeParams.setShear(0f, 0f)
                createSelected = true
            }
            PCode.CUBE -> {
                volumeParams.setType(ProfileCode.SQUARE, PathCode.LINE)
                volumeParams.setBeginAndEndS(0f, 1f); volumeParams.setBeginAndEndT(0f, 1f)
                volumeParams.setRatio(1f, 1f); volumeParams.setShear(0f, 0f)
                createSelected = true
            }
            PCode.PRISM -> {
                volumeParams.setType(ProfileCode.SQUARE, PathCode.LINE)
                volumeParams.setBeginAndEndS(0f, 1f); volumeParams.setBeginAndEndT(0f, 1f)
                volumeParams.setRatio(0f, 1f); volumeParams.setShear(-0.5f, 0f)
                createSelected = true
            }
            PCode.PYRAMID -> {
                volumeParams.setType(ProfileCode.SQUARE, PathCode.LINE)
                volumeParams.setBeginAndEndS(0f, 1f); volumeParams.setBeginAndEndT(0f, 1f)
                volumeParams.setRatio(0f, 0f); volumeParams.setShear(0f, 0f)
                createSelected = true
            }
            PCode.TETRAHEDRON -> {
                volumeParams.setType(ProfileCode.EQUALTRI, PathCode.LINE)
                volumeParams.setBeginAndEndS(0f, 1f); volumeParams.setBeginAndEndT(0f, 1f)
                volumeParams.setRatio(0f, 0f); volumeParams.setShear(0f, 0f)
                createSelected = true
            }
            PCode.CYLINDER -> {
                volumeParams.setType(ProfileCode.CIRCLE, PathCode.LINE)
                volumeParams.setBeginAndEndS(0f, 1f); volumeParams.setBeginAndEndT(0f, 1f)
                volumeParams.setRatio(1f, 1f); volumeParams.setShear(0f, 0f)
                createSelected = true
            }
            PCode.CYLINDER_HEMI -> {
                volumeParams.setType(ProfileCode.CIRCLE, PathCode.LINE)
                volumeParams.setBeginAndEndS(0.25f, 0.75f); volumeParams.setBeginAndEndT(0f, 1f)
                volumeParams.setRatio(1f, 1f); volumeParams.setShear(0f, 0f)
                createSelected = true
            }
            PCode.CONE -> {
                volumeParams.setType(ProfileCode.CIRCLE, PathCode.LINE)
                volumeParams.setBeginAndEndS(0f, 1f); volumeParams.setBeginAndEndT(0f, 1f)
                volumeParams.setRatio(0f, 0f); volumeParams.setShear(0f, 0f)
                createSelected = true
            }
            PCode.CONE_HEMI -> {
                volumeParams.setType(ProfileCode.CIRCLE, PathCode.LINE)
                volumeParams.setBeginAndEndS(0.25f, 0.75f); volumeParams.setBeginAndEndT(0f, 1f)
                volumeParams.setRatio(0f, 0f); volumeParams.setShear(0f, 0f)
                createSelected = true
            }
            else -> createSelected = false
        }

        if (SavedSettings.getBool("PlayModeUISndObjectCreate")) {
            AudioEngine.instance.triggerSound(
                SavedSettings.getString("UISndObjectCreate"),
                Agent.instance.getId(),
                1.0f,
                AudioEngine.AudioType.UI
            )
        }

        UIUsage.instance.logCommand("Build.ObjectAdd")

        var flags: UInt = 0u
        if (usePhysics) flags = flags or FLAGS_USE_PHYSICS
        if (createSelected && !RlvHandler.instance.hasBehaviour(RlvBehaviour.EDIT)) {
            flags = flags or FLAGS_CREATE_SELECTED
        }

        val rayTargetId = hitObj[0]?.getId()

        System.err.println("ToolPlacer: sendObjectAdd not yet implemented")

        if (createSelected) {
            FSCommon.objectAddMsg++
            SelectMgr.instance.deselectAll()
            ViewerWindow.instance.getWindow().incBusyCount()
        }

        val effect = HUDManager.instance.createViewerEffect(HUDObject.Type.BEAM) as HUDEffectSpiral
        effect.setSourceObject(AgentAvatarSelf.instance)
        effect.setPositionGlobal(regionp.getPosGlobalFromRegion(rayEndRegion[0]))
        effect.setDuration(HUD_DUR_SHORT)
        effect.setColor(Agent.instance.getEffectColor())

        StatViewer.add(StatViewer.OBJECT_CREATE, 1)

        return true
    }

    private fun addDuplicate(x: Int, y: Int): Boolean {
        val hitObj = arrayOfNulls<ViewerObject>(1)
        val hitFace = intArrayOf(-1)
        val bHitLand = booleanArrayOf(false)
        val rayStartRegion = arrayOf(Vector3.ZERO)
        val rayEndRegion = arrayOf(Vector3.ZERO)
        val region = arrayOfNulls<ViewerRegion>(1)

        if (!raycastForNewObjPos(x, y, hitObj, hitFace, bHitLand, rayStartRegion, rayEndRegion, region)) {
            makeUiSound("UISndInvalidOp")
            return false
        }

        if (hitObj[0] != null && (hitObj[0]!!.isAvatar() || hitObj[0]!!.isAttachment())) {
            makeUiSound("UISndInvalidOp")
            return false
        }

        val rayTargetId = hitObj[0]?.getId()

        SelectMgr.instance.selectDuplicateOnRay(
            rayStartRegion[0],
            rayEndRegion[0],
            bHitLand[0],
            false,
            rayTargetId,
            SavedSettings.getBool("CreateToolCopyCenters"),
            SavedSettings.getBool("CreateToolCopyRotates"),
            false
        )

        return true
    }
}
