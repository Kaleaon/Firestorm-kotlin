package com.firestorm.newview

class FSFloaterPoseStand(key: Map<String, Any?>) {

    private var comboPose: Any? = null
    private var poseStandLock: Boolean = false
    private var aoPaused: Boolean = false

    fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent: find child combo box 'pose_combo' and assign to comboPose")
        // Wire combo commit callback to onCommitCombo.
        loadPoses()
        return true
    }

    fun onOpen(key: Map<String, Any?>) {
        TODO("APR: use JVM equivalent: check isAgentAvatarValid(); return if not")
        TODO("APR: use JVM equivalent: check UseAO saved-per-account setting; if true, set to false and set aoPaused = true")
        TODO("APR: use JVM equivalent: check FSPoseStandLock setting, avatar not sitting, and no RLV sit restriction; if all true call setLock(true)")
        TODO("APR: use JVM equivalent: gAgent.stopCurrentAnimations(true)")
        TODO("APR: use JVM equivalent: gAgent.setCustomAnim(true)")
        TODO("APR: use JVM equivalent: release keyboard and mouse focus")
        TODO("APR: use JVM equivalent: read FSPoseStandLastSelectedPose setting; if non-empty select that value in comboPose")
        onCommitCombo()
    }

    fun onClose(appQuitting: Boolean) {
        TODO("APR: use JVM equivalent: check isAgentAvatarValid(); return if not")
        if (poseStandLock) {
            TODO("APR: use JVM equivalent: check gAgentAvatarp.isSitting(); if true, setLock(false) and gAgent.standUp()")
        }
        TODO("APR: use JVM equivalent: gAgent.setCustomAnim(false)")
        TODO("APR: use JVM equivalent: FSPose.getInstance().stopPose()")
        TODO("APR: use JVM equivalent: gAgent.stopCurrentAnimations(true)")
        if (aoPaused) {
            TODO("APR: use JVM equivalent: if UseAO setting is false, set it to true and set aoPaused = false")
        }
    }

    fun setLock(enabled: Boolean) {
        if (enabled) {
            TODO("APR: use JVM equivalent: gAgent.sitDown()")
        } else {
            TODO("APR: use JVM equivalent: gAgent.standUp()")
        }
        poseStandLock = enabled
    }

    fun onCommitCombo() {
        TODO("APR: use JVM equivalent: read selected value from comboPose")
        TODO("APR: use JVM equivalent: save selected pose to FSPoseStandLastSelectedPose setting")
        TODO("APR: use JVM equivalent: FSPose.getInstance().setPose(selectedPose)")
    }

    private fun loadPoses() {
        TODO("APR: use JVM equivalent: locate posestand.xml in app settings, parse LLSD, populate comboPose with animation UUID entries, then sortByName()")
    }
}
