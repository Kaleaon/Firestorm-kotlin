package com.firestorm.newview

object ToolFace : Tool("Texture") {

    var textureGrabbed: Boolean = false
    var textureObject: ViewerObject? = null
    var faceGrabbed: Int = 0

    var grabX: Int = 0
    var grabY: Int = 0

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        ViewerWindow.instance.pickAsync(x, y, mask, ::pickCallback)
        grabX = x
        grabY = y
        return true
    }

    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        textureObject?.sendTEUpdate()
        ViewerWindow.instance.showCursor()
        textureGrabbed = false
        return true
    }

    override fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean {
        return if (!SelectMgr.instance.getSelection().isEmpty()) {
            FloaterReg.showInstance("build", "Texture")
            true
        } else {
            false
        }
    }

    override fun handleSelect() {
        SelectMgr.instance.setTEMode(true)
    }

    override fun handleDeselect() {
        SelectMgr.instance.setTEMode(false)
        stopGrabbing()
    }

    override fun render() {
        if (!textureGrabbed || textureObject == null || textureObject?.permModify() != true) {
            return
        }

        val dx = ViewerWindow.instance.getCurrentMouseX() - grabX
        val dy = grabY - ViewerWindow.instance.getCurrentMouseY()

        if (dx == 0 && dy == 0) {
            return
        }

        ViewerWindow.instance.getWindow()
            .setCursorPosition(grabX, ViewerWindow.instance.getWindowHeightRaw() - grabY - 1)

        val face = textureObject!!.getTE(faceGrabbed) ?: return
        var u = face.getOffsetU()
        var v = face.getOffsetV()
        var scaleU = face.getScaleU()
        var scaleV = face.getScaleV()

        val currentMask = Keyboard.instance.currentMask(false)

        if (Keyboard.instance.getKeyDown(Key.CAPSLOCK)) {
            var scale = 10.0f
            if (currentMask and MASK_CONTROL != 0) scale = 100.0f
            else if (currentMask and MASK_SHIFT != 0) scale = 1000.0f

            scaleU -= dx.toFloat() / scale
            scaleV += dy.toFloat() / scale

            if (scaleU < 0.0f) scaleU = 0.0f
            if (scaleV < 0.0f) scaleV = 0.0f

            textureObject!!.setTEScale(faceGrabbed, scaleU, scaleV)
        } else {
            var scale = 100.0f
            if (currentMask and MASK_CONTROL != 0) scale = 1000.0f
            else if (currentMask and MASK_SHIFT != 0) scale = 10000.0f

            u -= (dx.toFloat() / scale) * scaleU
            v += (dy.toFloat() / scale) * scaleV

            if (u > 1.0f) u -= 1.0f else if (u < -1.0f) u += 1.0f
            if (v > 1.0f) v -= 1.0f else if (v < -1.0f) v += 1.0f

            textureObject!!.setTEOffset(faceGrabbed, u, v)
        }

        val toolsFloater = FloaterReg.findInstance("build") as? FloaterTools
        toolsFloater?.refreshPanelFace()
    }

    fun stopGrabbing() {
        textureObject?.sendTEUpdate()
        ViewerWindow.instance.showCursor()
        textureGrabbed = false
        textureObject = null
        faceGrabbed = 0
    }

    fun pickCallback(pickInfo: PickInfo) {
        val hitObj = pickInfo.getObject() ?: run {
            if (pickInfo.keyMask != MASK_SHIFT) {
                SelectMgr.instance.deselectAll()
            }
            return
        }

        if (hitObj.isAvatar()) return

        if (RlvActions.isRlvEnabled() &&
            (!RlvActions.canEdit(hitObj) || !RlvActions.canInteract(hitObj, pickInfo.objectOffset))
        ) {
            return
        }

        val hitFace = pickInfo.objectFace

        if (pickInfo.keyMask and MASK_SHIFT != 0) {
            if (!hitObj.isSelected()) {
                SelectMgr.instance.selectObjectOnly(hitObj, hitFace)
            } else if (!SelectMgr.instance.getSelection().contains(hitObj, hitFace)) {
                SelectMgr.instance.addAsIndividual(hitObj, hitFace)
            } else {
                SelectMgr.instance.remove(hitObj, hitFace)
            }
        } else {
            SelectMgr.instance.deselectAll()
            SelectMgr.instance.selectObjectOnly(hitObj, hitFace)
        }

        if (SavedSettings.getBool("FSExperimentalDragTexture")) {
            if (textureObject === hitObj && faceGrabbed == pickInfo.objectFace) {
                textureGrabbed = true
                ViewerWindow.instance.hideCursor()
            } else {
                textureGrabbed = false
                ViewerWindow.instance.showCursor()
            }
        }

        textureObject = hitObj
        faceGrabbed = pickInfo.objectFace
    }
}
