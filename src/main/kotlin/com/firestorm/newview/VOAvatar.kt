package com.firestorm.newview

import com.firestorm.llcharacter.MotionController
import com.firestorm.llcommon.LLUUID
import com.firestorm.llmessage.AvatarName

open class VOAvatar(id: LLUUID, localId: UInt) : ViewerObject(id, localId, 0u) {

    var avatarName: AvatarName? = null
    open var isSelf: Boolean = false
    var isSitting: Boolean = false
    var isTyping: Boolean = false
    var isAway: Boolean = false
    var isBusy: Boolean = false
    var visualComplexity: Int = 0
    var displayName: String = ""
    var legacyName: String = ""

    var motionController: MotionController = MotionController()

    enum class VisualMuteSettings { DEFAULT, ALWAYS_SHOW, NEVER_SHOW }
    var visualMuteSettings: VisualMuteSettings = VisualMuteSettings.DEFAULT

    override fun isAvatar(): Boolean = true

    fun getFullname(): String = avatarName?.getCompleteName() ?: id.toString()

    fun getAttachments(): List<ViewerObject> = children.filter { it.isAttachment() }

    fun attachObject(obj: ViewerObject, attachPt: Int) {}

    fun detachObject(obj: ViewerObject) {}

    fun startMotion(id: LLUUID, stopPrevious: Boolean = false): Boolean {
        return motionController.startMotion(id, stopPrevious)
    }

    fun stopMotion(id: LLUUID, stopImmediately: Boolean = false): Boolean {
        return motionController.stopMotion(id, stopImmediately)
    }

    open fun updateCharacter(dt: Float) {}
}
