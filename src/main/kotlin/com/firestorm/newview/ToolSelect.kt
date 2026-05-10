package com.firestorm.newview

import java.util.UUID

const val SELECTION_ROTATION_THRESHOLD: Float = 0.1f
const val SELECTION_SITTING_ROTATION_THRESHOLD: Float = 3.2f

open class ToolSelect(composite: ToolComposite?) : Tool("Select", composite) {

    protected var ignoreGroup: Boolean = false
    protected var selectObjectId: UUID = UUID.randomUUID()
    protected var pick: PickInfo = PickInfo()

    open override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val pickRigged = false
        val selectInvisible = SavedSettings.getBool("SelectInvisibleObjects")
        val selectReflectionProbes = SavedSettings.getBool("SelectReflectionProbes")

        pick = ViewerWindow.instance.pickImmediate(x, y, selectInvisible, pickRigged, false, true, selectReflectionProbes)

        super.handleMouseDown(x, y, mask)

        return pick.getObject() != null
    }

    open override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        ignoreGroup = SavedSettings.getBool("EditLinkedParts")
        handleObjectSelection(pick, ignoreGroup, false)
        return super.handleMouseUp(x, y, mask)
    }

    open override fun handleDeselect() {
        if (hasMouseCapture()) {
            setMouseCapture(false)
        }
    }

    open override fun stopEditing() {
        if (hasMouseCapture()) {
            setMouseCapture(false)
        }
    }

    open override fun onMouseCaptureLost() {
        SelectMgr.instance.enableSilhouette(true)
        ignoreGroup = false
    }

    companion object {
        fun handleObjectSelection(
            pick: PickInfo,
            ignoreGroup: Boolean,
            tempSelect: Boolean,
            selectRoot: Boolean = false
        ): ObjectSelectionHandle {
            var obj = pick.getObject()
            if (selectRoot) {
                obj = obj?.getRootEdit()
            }

            if (obj != null && RlvActions.isRlvEnabled()) {
                if (!RlvActions.canEdit(obj)) {
                    return if (!tempSelect) {
                        SelectMgr.instance.getSelection()
                    } else {
                        if (ToolMgr.instance.inBuildMode()) ToolMgr.instance.leaveBuildMode()
                        SelectMgr.instance.getSelection()
                    }
                }

                if (RlvActions.hasBehaviour(RlvBehaviour.FARTOUCH) &&
                    (!obj.isAttachment() || !obj.permYouOwner())
                ) {
                    val fartouchDist = RlvCachedBehaviourModifier.getFartouchDist()
                    val fartouchDistSq = fartouchDist * fartouchDist

                    if (Agent.instance.getPositionAgent().distanceSq(obj.getPositionRegion()) > fartouchDistSq) {
                        if (Agent.instance.getPositionAgent().distanceSq(pick.intersection) > 1.5f * 1.5f) {
                            if (FloaterReg.instanceVisible("build") &&
                                pick.keyMask != MASK_SHIFT && pick.keyMask != MASK_CONTROL
                            ) {
                                SelectMgr.instance.deselectAll()
                            }
                            return SelectMgr.instance.getSelection()
                        } else if (ToolMgr.instance.inBuildMode()) {
                            ToolMgr.instance.leaveBuildMode()
                        }
                    }
                }
            }

            val selectOwned = SavedSettings.getBool("SelectOwnedOnly")
            val selectMovable = SavedSettings.getBool("SelectMovableOnly")
            val selectCopyable = SavedSettings.getBool("FSSelectCopyableOnly")
            val selectLocked = SavedSettings.getBool("FSSelectLockedOnly")

            if (tempSelect || SelectMgr.instance.allowSelectAvatar) {
                SavedSettings.setBool("SelectOwnedOnly", false)
                SavedSettings.setBool("SelectMovableOnly", false)
                SavedSettings.setBool("FSSelectCopyableOnly", false)
                SavedSettings.setBool("FSSelectLockedOnly", false)
                SelectMgr.instance.setForceSelection(true)
            }

            val extendSelect = (pick.keyMask == MASK_SHIFT) || (pick.keyMask == MASK_CONTROL)

            if (obj == null) {
                val lastHitHudIcon = pick.hudIcon
                if (lastHitHudIcon?.getSourceObject() != null) {
                    FloaterScriptDebug.show(lastHitHudIcon.getSourceObject()!!.getId())
                } else if (!extendSelect) {
                    SelectMgr.instance.deselectAll()
                }
            } else {
                var alreadySelected = obj.isSelected()

                if (alreadySelected &&
                    obj.getNumTEs() > 0 &&
                    !SelectMgr.instance.getSelection().contains(obj, SELECT_ALL_TES)
                ) {
                    val tep = obj.getTE(pick.objectFace)
                    if (tep != null && !tep.isSelected() && !ViewerMediaFocus.instance.getFocusedObjectId().isNull()) {
                        ViewerMediaFocus.instance.clearFocus()
                        alreadySelected = false
                    }
                }

                if (extendSelect) {
                    if (alreadySelected) {
                        if (ignoreGroup) {
                            SelectMgr.instance.deselectObjectOnly(obj)
                        } else {
                            SelectMgr.instance.deselectObjectAndFamily(obj, true, true)
                        }
                    } else {
                        if (ignoreGroup) {
                            SelectMgr.instance.selectObjectOnly(obj, SELECT_ALL_TES)
                        } else {
                            SelectMgr.instance.selectObjectAndFamily(obj)
                        }
                    }
                } else {
                    val targetZoom = FloatArray(1)
                    val currentZoom = FloatArray(1)
                    SelectMgr.instance.getAgentHudZoom(targetZoom, currentZoom)

                    if (!alreadySelected || ignoreGroup) {
                        SelectMgr.instance.deselectAll()
                    }

                    if (ignoreGroup) {
                        SelectMgr.instance.selectObjectOnly(obj, SELECT_ALL_TES, pick.gltfNodeIndex, pick.gltfPrimitiveIndex)
                    } else {
                        SelectMgr.instance.selectObjectAndFamily(obj)
                    }

                    SelectMgr.instance.setAgentHudZoom(targetZoom[0], currentZoom[0])
                }

                if (!AgentCamera.instance.getFocusOnAvatar() &&
                    VOAvatar.findAvatarFromAttachment(obj) != AgentAvatarSelf.instance &&
                    obj != AgentAvatarSelf.instance &&
                    SavedSettings.getBool("FSTurnAvatarToSelectedObject")
                ) {
                    var selectionCenter = SelectMgr.instance.getSelectionCenterGlobal()
                    selectionCenter = selectionCenter - Agent.instance.getPositionGlobal()
                    var selectionDir = Vector3(selectionCenter)
                    selectionDir.z = 0f
                    selectionDir = selectionDir.normalized()

                    if (!obj.isAvatar() && Agent.instance.getAtAxis().dot(selectionDir) < 0.6f) {
                        val targetRot = Quaternion.shortestArc(Vector3.X_AXIS, selectionDir)
                        Agent.instance.startAutoPilotGlobal(
                            Agent.instance.getPositionGlobal(),
                            "",
                            targetRot,
                            null,
                            null,
                            MAX_FAR_CLIP,
                            if (AgentAvatarSelf.instance.isSitting()) SELECTION_SITTING_ROTATION_THRESHOLD
                            else SELECTION_ROTATION_THRESHOLD
                        )
                    }
                }

                if (tempSelect && !alreadySelected) {
                    val rootObject = obj.getRootEdit()
                    val selection = SelectMgr.instance.getSelection()
                    selection.findNode(rootObject)?.setTransient(true)

                    for (child in rootObject.getChildren()) {
                        selection.findNode(child)?.setTransient(true)
                    }
                }
            }

            if (tempSelect || SelectMgr.instance.allowSelectAvatar) {
                SavedSettings.setBool("SelectOwnedOnly", selectOwned)
                SavedSettings.setBool("SelectMovableOnly", selectMovable)
                SavedSettings.setBool("FSSelectCopyableOnly", selectCopyable)
                SavedSettings.setBool("FSSelectLockedOnly", selectLocked)
                SelectMgr.instance.setForceSelection(false)
            }

            return SelectMgr.instance.getSelection()
        }
    }
}
