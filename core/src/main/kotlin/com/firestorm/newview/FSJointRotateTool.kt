package com.firestorm.newview

object FSJointRotateTool {

    private var manipulatorSize: Float = 0f
    private var highlightedAxis: Int = 0
    private var highlightedDirection: Float = 0f
    private var force: Boolean = false

    fun handleSelect() {
        println("Rotate tool selected")
    }

    fun handleDeselect() {
        println("Rotate tool deselected")
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (findSelectedManipulator(x, y)) {
            val avatar = getAgentAvatar()
            if (avatar != null) rotateJoint(avatar)
        }
        return true
    }

    fun handleHover(x: Int, y: Int, mask: Int): Boolean = findSelectedManipulator(x, y)

    fun render() {
        computeManipulatorSize()
        renderManipulators()
    }

    private fun rotateJoint(avatar: Any) {
        System.err.println("FSJointRotateTool: rotateJoint not yet implemented")
    }

    private fun findSelectedManipulator(x: Int, y: Int): Boolean = false

    private fun computeManipulatorSize() {
        manipulatorSize = 5.0f
    }

    private fun renderManipulators() {
        // no-op
    }

    private fun getAgentAvatar(): Any? {
        System.err.println("FSJointRotateTool: getAgentAvatar not yet implemented")
        return null
    }
}
