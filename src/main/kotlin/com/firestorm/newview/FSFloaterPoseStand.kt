package com.firestorm.newview

class FSFloaterPoseStand(key: Map<String, Any?>) {

    private var comboPose: Any? = null
    private var poseStandLock: Boolean = false
    private var aoPaused: Boolean = false

    fun postBuild(): Boolean {
        System.err.println("FSFloaterPoseStand: use JVM equivalent: find child combo box 'pose_combo' and assign to comboPose not yet implemented")
        // Wire combo commit callback to onCommitCombo.
        loadPoses()
        return true
    }

    fun onOpen(key: Map<String, Any?>) {
        System.err.println("FSFloaterPoseStand: use JVM equivalent: check isAgentAvatarValid(); return if not not yet implemented")
        System.err.println("FSFloaterPoseStand: use JVM equivalent: check UseAO saved-per-account setting; if true, set to false and set aoPaused = true not yet implemented")
        System.err.println("FSFloaterPoseStand: use JVM equivalent: check FSPoseStandLock setting, avatar not sitting, and no RLV sit restriction; if all true call setLock(true) not yet implemented")
        System.err.println("FSFloaterPoseStand: use JVM equivalent: gAgent.stopCurrentAnimations(true) not yet implemented")
        System.err.println("FSFloaterPoseStand: use JVM equivalent: gAgent.setCustomAnim(true) not yet implemented")
        System.err.println("FSFloaterPoseStand: use JVM equivalent: release keyboard and mouse focus not yet implemented")
        System.err.println("FSFloaterPoseStand: use JVM equivalent: read FSPoseStandLastSelectedPose setting; if non-empty select that value in comboPose not yet implemented")
        onCommitCombo()
    }

    fun onClose(appQuitting: Boolean) {
        System.err.println("FSFloaterPoseStand: use JVM equivalent: check isAgentAvatarValid(); return if not not yet implemented")
        if (poseStandLock) {
            System.err.println("FSFloaterPoseStand: use JVM equivalent: check gAgentAvatarp.isSitting(); if true, setLock(false) and gAgent.standUp() not yet implemented")
        }
        System.err.println("FSFloaterPoseStand: use JVM equivalent: gAgent.setCustomAnim(false) not yet implemented")
        System.err.println("FSFloaterPoseStand: use JVM equivalent: FSPose.getInstance().stopPose() not yet implemented")
        System.err.println("FSFloaterPoseStand: use JVM equivalent: gAgent.stopCurrentAnimations(true) not yet implemented")
        if (aoPaused) {
            System.err.println("FSFloaterPoseStand: use JVM equivalent: if UseAO setting is false, set it to true and set aoPaused = false not yet implemented")
        }
    }

    fun setLock(enabled: Boolean) {
        if (enabled) {
            System.err.println("FSFloaterPoseStand: use JVM equivalent: gAgent.sitDown() not yet implemented")
        } else {
            System.err.println("FSFloaterPoseStand: use JVM equivalent: gAgent.standUp() not yet implemented")
        }
        poseStandLock = enabled
    }

    fun onCommitCombo() {
        System.err.println("FSFloaterPoseStand: use JVM equivalent: read selected value from comboPose not yet implemented")
        System.err.println("FSFloaterPoseStand: use JVM equivalent: save selected pose to FSPoseStandLastSelectedPose setting not yet implemented")
        System.err.println("FSFloaterPoseStand: use JVM equivalent: FSPose.getInstance().setPose(selectedPose) not yet implemented")
    }

    private fun loadPoses() {
        System.err.println("FSFloaterPoseStand: use JVM equivalent: locate posestand.xml in app settings, parse LLSD, populate comboPose with animation UUID entries, then sortByName() not yet implemented")
    }
}
