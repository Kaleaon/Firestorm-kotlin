package com.firestorm.newview

import kotlin.math.PI

object AgentConstants {

    // region geometry
    const val REGION_WIDTH_METERS: Float = 256f
    const val REGION_WIDTH_UNITS: Int = 256
    const val REGION_HEIGHT_METERS: Float = 4096f

    // default agent body dimensions
    const val DEFAULT_AGENT_DEPTH: Float = 0.45f
    const val DEFAULT_AGENT_WIDTH: Float = 0.60f
    const val DEFAULT_AGENT_HEIGHT: Float = 1.9f

    // default water height
    const val DEFAULT_WATER_HEIGHT: Float = 20.0f

    // sim access / maturity ratings
    const val SIM_ACCESS_MIN: UByte = 0u
    const val SIM_ACCESS_PG: UByte = 13u
    const val SIM_ACCESS_MATURE: UByte = 21u
    const val SIM_ACCESS_ADULT: UByte = 42u
    const val SIM_ACCESS_DOWN: UByte = 254u
    const val SIM_ACCESS_MAX: UByte = SIM_ACCESS_ADULT

    // god levels
    const val GOD_MAINTENANCE: UByte = 250u
    const val GOD_FULL: UByte = 200u
    const val GOD_LIAISON: UByte = 150u
    const val GOD_CUSTOMER_SERVICE: UByte = 100u
    const val GOD_LIKE: UByte = 1u
    const val GOD_NOT: UByte = 0u

    // attachment constants
    const val ATTACHMENT_ADD: UByte = 0x80u
    const val MAX_ATTACHMENT_DIST: Float = 3.5f

    // camera field of view (radians)
    val DEFAULT_FIELD_OF_VIEW: Float = (60f * PI / 180f).toFloat()
    val MIN_FIELD_OF_VIEW: Float = (5f * PI / 180f).toFloat()
    val MAX_FIELD_OF_VIEW: Float = (175f * PI / 180f).toFloat()

    // control bit indices
    const val CONTROL_AT_POS_INDEX: UInt = 0u
    const val CONTROL_AT_NEG_INDEX: UInt = 1u
    const val CONTROL_LEFT_POS_INDEX: UInt = 2u
    const val CONTROL_LEFT_NEG_INDEX: UInt = 3u
    const val CONTROL_UP_POS_INDEX: UInt = 4u
    const val CONTROL_UP_NEG_INDEX: UInt = 5u
    const val CONTROL_PITCH_POS_INDEX: UInt = 6u
    const val CONTROL_PITCH_NEG_INDEX: UInt = 7u
    const val CONTROL_YAW_POS_INDEX: UInt = 8u
    const val CONTROL_YAW_NEG_INDEX: UInt = 9u
    const val CONTROL_FAST_AT_INDEX: UInt = 10u
    const val CONTROL_FAST_LEFT_INDEX: UInt = 11u
    const val CONTROL_FAST_UP_INDEX: UInt = 12u
    const val CONTROL_FLY_INDEX: UInt = 13u
    const val CONTROL_STOP_INDEX: UInt = 14u
    const val CONTROL_FINISH_ANIM_INDEX: UInt = 15u
    const val CONTROL_STAND_UP_INDEX: UInt = 16u
    const val CONTROL_SIT_ON_GROUND_INDEX: UInt = 17u
    const val CONTROL_MOUSELOOK_INDEX: UInt = 18u
    const val CONTROL_NUDGE_AT_POS_INDEX: UInt = 19u
    const val CONTROL_NUDGE_AT_NEG_INDEX: UInt = 20u
    const val CONTROL_NUDGE_LEFT_POS_INDEX: UInt = 21u
    const val CONTROL_NUDGE_LEFT_NEG_INDEX: UInt = 22u
    const val CONTROL_NUDGE_UP_POS_INDEX: UInt = 23u
    const val CONTROL_NUDGE_UP_NEG_INDEX: UInt = 24u
    const val CONTROL_TURN_LEFT_INDEX: UInt = 25u
    const val CONTROL_TURN_RIGHT_INDEX: UInt = 26u
    const val CONTROL_AWAY_INDEX: UInt = 27u
    const val CONTROL_LBUTTON_DOWN_INDEX: UInt = 28u
    const val CONTROL_LBUTTON_UP_INDEX: UInt = 29u
    const val CONTROL_ML_LBUTTON_DOWN_INDEX: UInt = 30u
    const val CONTROL_ML_LBUTTON_UP_INDEX: UInt = 31u
    const val TOTAL_CONTROLS: UInt = 32u

    // agent control flags
    const val AGENT_CONTROL_AT_POS: UInt = 0x00000001u
    const val AGENT_CONTROL_AT_NEG: UInt = 0x00000002u
    const val AGENT_CONTROL_LEFT_POS: UInt = 0x00000004u
    const val AGENT_CONTROL_LEFT_NEG: UInt = 0x00000008u
    const val AGENT_CONTROL_UP_POS: UInt = 0x00000010u
    const val AGENT_CONTROL_UP_NEG: UInt = 0x00000020u
    const val AGENT_CONTROL_PITCH_POS: UInt = 0x00000040u
    const val AGENT_CONTROL_PITCH_NEG: UInt = 0x00000080u
    const val AGENT_CONTROL_YAW_POS: UInt = 0x00000100u
    const val AGENT_CONTROL_YAW_NEG: UInt = 0x00000200u
    const val AGENT_CONTROL_FAST_AT: UInt = 0x00000400u
    const val AGENT_CONTROL_FAST_LEFT: UInt = 0x00000800u
    const val AGENT_CONTROL_FAST_UP: UInt = 0x00001000u
    const val AGENT_CONTROL_FLY: UInt = 0x00002000u
    const val AGENT_CONTROL_STOP: UInt = 0x00004000u
    const val AGENT_CONTROL_FINISH_ANIM: UInt = 0x00008000u
    const val AGENT_CONTROL_STAND_UP: UInt = 0x00010000u
    const val AGENT_CONTROL_SIT_ON_GROUND: UInt = 0x00020000u
    const val AGENT_CONTROL_MOUSELOOK: UInt = 0x00040000u
    const val AGENT_CONTROL_NUDGE_AT_POS: UInt = 0x00080000u
    const val AGENT_CONTROL_NUDGE_AT_NEG: UInt = 0x00100000u
    const val AGENT_CONTROL_NUDGE_LEFT_POS: UInt = 0x00200000u
    const val AGENT_CONTROL_NUDGE_LEFT_NEG: UInt = 0x00400000u
    const val AGENT_CONTROL_NUDGE_UP_POS: UInt = 0x00800000u
    const val AGENT_CONTROL_NUDGE_UP_NEG: UInt = 0x01000000u
    const val AGENT_CONTROL_TURN_LEFT: UInt = 0x02000000u
    const val AGENT_CONTROL_TURN_RIGHT: UInt = 0x04000000u
    const val AGENT_CONTROL_AWAY: UInt = 0x08000000u
    const val AGENT_CONTROL_LBUTTON_DOWN: UInt = 0x10000000u
    const val AGENT_CONTROL_LBUTTON_UP: UInt = 0x20000000u
    const val AGENT_CONTROL_ML_LBUTTON_DOWN: UInt = 0x40000000u
    const val AGENT_CONTROL_ML_LBUTTON_UP: UInt = 0x80000000u

    // attachment id packing
    const val AGENT_ATTACH_OFFSET: UInt = 4u
    const val AGENT_ATTACH_MASK: UInt = 0xfu shl 4

    // click actions
    const val CLICK_ACTION_NONE: UByte = 0u
    const val CLICK_ACTION_TOUCH: UByte = 0u
    const val CLICK_ACTION_SIT: UByte = 1u
    const val CLICK_ACTION_BUY: UByte = 2u
    const val CLICK_ACTION_PAY: UByte = 3u
    const val CLICK_ACTION_OPEN: UByte = 4u
    const val CLICK_ACTION_PLAY: UByte = 5u
    const val CLICK_ACTION_OPEN_MEDIA: UByte = 6u
    const val CLICK_ACTION_ZOOM: UByte = 7u
    const val CLICK_ACTION_DISABLED: UByte = 8u
    const val CLICK_ACTION_IGNORE: UByte = 9u

    // teleport flags
    const val TELEPORT_FLAGS_DEFAULT: UInt = 0u
    const val TELEPORT_FLAGS_SET_HOME_TO_TARGET: UInt = 1u shl 0
    const val TELEPORT_FLAGS_SET_LAST_TO_TARGET: UInt = 1u shl 1
    const val TELEPORT_FLAGS_VIA_LURE: UInt = 1u shl 2
    const val TELEPORT_FLAGS_VIA_LANDMARK: UInt = 1u shl 3
    const val TELEPORT_FLAGS_VIA_LOCATION: UInt = 1u shl 4
    const val TELEPORT_FLAGS_VIA_HOME: UInt = 1u shl 5
    const val TELEPORT_FLAGS_VIA_TELEHUB: UInt = 1u shl 6
    const val TELEPORT_FLAGS_VIA_LOGIN: UInt = 1u shl 7
    const val TELEPORT_FLAGS_VIA_GODLIKE_LURE: UInt = 1u shl 8
    const val TELEPORT_FLAGS_GODLIKE: UInt = 1u shl 9
    const val TELEPORT_FLAGS_911: UInt = 1u shl 10
    const val TELEPORT_FLAGS_DISABLE_CANCEL: UInt = 1u shl 11
    const val TELEPORT_FLAGS_VIA_REGION_ID: UInt = 1u shl 12
    const val TELEPORT_FLAGS_IS_FLYING: UInt = 1u shl 13
    const val TELEPORT_FLAGS_SHOW_RESET_HOME: UInt = 1u shl 14
    const val TELEPORT_FLAGS_FORCE_REDIRECT: UInt = 1u shl 15
    const val TELEPORT_FLAGS_VIA_GLOBAL_COORDS: UInt = 1u shl 16
    const val TELEPORT_FLAGS_WITHIN_REGION: UInt = 1u shl 17

    // region handshake flags
    const val REGION_HANDSHAKE_SUPPORTS_SELF_APPEARANCE: UInt = 1u shl 2

    // object cache limit
    const val MAX_OBJECT_CACHE_ENTRIES: UInt = 50000u

    // chat radius
    const val CHAT_NORMAL_RADIUS: Float = 20f
}
