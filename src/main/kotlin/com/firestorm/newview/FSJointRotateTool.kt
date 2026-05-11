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
        TODO("APR: use JVM equivalent: apply joint rotation via FSPoserAnimator APIs")
    }

    private fun findSelectedManipulator(x: Int, y: Int): Boolean = false

    private fun computeManipulatorSize() {
        manipulatorSize = 5.0f
    }

    private fun renderManipulators() {
        TODO("GPU: render joint rotation manipulator handles using OpenGL")
    }

    private fun getAgentAvatar(): Any? = TODO("APR: use JVM equivalent for gAgentAvatarp")
}
