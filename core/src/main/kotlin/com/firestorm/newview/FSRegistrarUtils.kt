package com.firestorm.newview

import java.util.UUID

enum class RegistrarActionType {
    ACT_ADD_FRIEND,
    ACT_REMOVE_FRIEND,
    ACT_SEND_IM,
    ACT_VIEW_TRANSCRIPT,
    ACT_ZOOM_IN,
    ACT_OFFER_TELEPORT,
    ACT_SHOW_PROFILE,
    ACT_TRACK_AVATAR,
    ACT_TELEPORT_TO,
    ACT_REQUEST_TELEPORT,
    CHK_AVATAR_BLOCKED,
    CHK_IS_SELF,
    CHK_IS_NOT_SELF,
    CHK_WAITING_FOR_GROUP_DATA,
    CHK_HAVE_GROUP_DATA,
    CHK_CAN_LEAVE_GROUP,
    CHK_CAN_JOIN_GROUP,
    CHK_GROUP_NOT_ACTIVE,
}

class FSRegistrarUtils {

    private var enableCheckFunction: ((UUID, RegistrarActionType) -> Boolean)? = null

    fun setEnableCheckFunction(func: (UUID, RegistrarActionType) -> Boolean) {
        enableCheckFunction = func
    }

    fun checkIsEnabled(avatarId: UUID, action: RegistrarActionType): Boolean =
        enableCheckFunction?.invoke(avatarId, action) ?: false
}

val gFSRegistrarUtils = FSRegistrarUtils()
