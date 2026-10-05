package com.firestorm.newview

// This value includes the everyone group.
const val MAX_ROLES: Int = 10

enum class RoleMemberChangeType {
    ADD,
    REMOVE,
    NONE
}

enum class RoleChangeType {
    UPDATE_NONE,
    UPDATE_DATA,
    UPDATE_POWERS,
    UPDATE_ALL,
    CREATE,
    DELETE
}

// Powers
// KNOWN HOLES: bits 0x1L shl 52 and above are available for new single-bit powers.
// Removed: 0x1L shl 41 (GP_ACCOUNTING_VIEW), 0x1L shl 46 (GP_PROPOSAL_VIEW)

const val GP_NO_POWERS: ULong = 0x0UL
const val GP_ALL_POWERS: ULong = 0xFFFFffffFFFFffffUL

// Membership
const val GP_MEMBER_INVITE: ULong             = 0x1UL shl 1
const val GP_MEMBER_EJECT: ULong              = 0x1UL shl 2
const val GP_MEMBER_OPTIONS: ULong            = 0x1UL shl 3
const val GP_MEMBER_VISIBLE_IN_DIR: ULong     = 0x1UL shl 47

// Roles
const val GP_ROLE_CREATE: ULong               = 0x1UL shl 4
const val GP_ROLE_DELETE: ULong               = 0x1UL shl 5
const val GP_ROLE_PROPERTIES: ULong           = 0x1UL shl 6
const val GP_ROLE_ASSIGN_MEMBER_LIMITED: ULong = 0x1UL shl 7
const val GP_ROLE_ASSIGN_MEMBER: ULong        = 0x1UL shl 8
const val GP_ROLE_REMOVE_MEMBER: ULong        = 0x1UL shl 9
const val GP_ROLE_CHANGE_ACTIONS: ULong       = 0x1UL shl 10

// Group Identity
const val GP_GROUP_CHANGE_IDENTITY: ULong     = 0x1UL shl 11

// Parcel Management
const val GP_LAND_DEED: ULong                 = 0x1UL shl 12
const val GP_LAND_RELEASE: ULong              = 0x1UL shl 13
const val GP_LAND_SET_SALE_INFO: ULong        = 0x1UL shl 14
const val GP_LAND_DIVIDE_JOIN: ULong          = 0x1UL shl 15

// Parcel Identity
const val GP_LAND_FIND_PLACES: ULong          = 0x1UL shl 17
const val GP_LAND_CHANGE_IDENTITY: ULong      = 0x1UL shl 18
const val GP_LAND_SET_LANDING_POINT: ULong    = 0x1UL shl 19

// Parcel Settings
const val GP_LAND_CHANGE_MEDIA: ULong         = 0x1UL shl 20
const val GP_LAND_EDIT: ULong                 = 0x1UL shl 21
const val GP_LAND_OPTIONS: ULong              = 0x1UL shl 22

// Parcel Powers
const val GP_LAND_ALLOW_EDIT_LAND: ULong      = 0x1UL shl 23
const val GP_LAND_ALLOW_FLY: ULong            = 0x1UL shl 24
const val GP_LAND_ALLOW_CREATE: ULong         = 0x1UL shl 25
const val GP_LAND_ALLOW_LANDMARK: ULong       = 0x1UL shl 26
const val GP_LAND_ALLOW_SET_HOME: ULong       = 0x1UL shl 28
const val GP_LAND_ALLOW_HOLD_EVENT: ULong     = 0x1UL shl 41
const val GP_LAND_ALLOW_ENVIRONMENT: ULong    = 0x1UL shl 46

// Parcel Access
const val GP_LAND_MANAGE_ALLOWED: ULong       = 0x1UL shl 29
const val GP_LAND_MANAGE_BANNED: ULong        = 0x1UL shl 30
const val GP_LAND_MANAGE_PASSES: ULong        = 0x1UL shl 31
const val GP_LAND_ADMIN: ULong                = 0x1UL shl 32

// Parcel Content
const val GP_LAND_RETURN_GROUP_SET: ULong     = 0x1UL shl 33
const val GP_LAND_RETURN_NON_GROUP: ULong     = 0x1UL shl 34
const val GP_LAND_RETURN_GROUP_OWNED: ULong   = 0x1UL shl 48

// Composite return power across all ownership categories
val GP_LAND_RETURN: ULong = GP_LAND_RETURN_GROUP_OWNED or
        GP_LAND_RETURN_GROUP_SET or
        GP_LAND_RETURN_NON_GROUP

const val GP_LAND_GARDENING: ULong            = 0x1UL shl 35

// Object Management
const val GP_OBJECT_DEED: ULong               = 0x1UL shl 36
const val GP_OBJECT_MANIPULATE: ULong         = 0x1UL shl 38
const val GP_OBJECT_SET_SALE: ULong           = 0x1UL shl 39

// Accounting
const val GP_ACCOUNTING_ACCOUNTABLE: ULong    = 0x1UL shl 40

// Notices
const val GP_NOTICES_SEND: ULong              = 0x1UL shl 42
const val GP_NOTICES_RECEIVE: ULong           = 0x1UL shl 43

// Proposals (deprecated as part of vote removal DEV-24856)
const val GP_PROPOSAL_START: ULong            = 0x1UL shl 44
const val GP_PROPOSAL_VOTE: ULong             = 0x1UL shl 45

// Group chat moderation
const val GP_SESSION_JOIN: ULong              = 0x1UL shl 16
const val GP_SESSION_VOICE: ULong             = 0x1UL shl 27
const val GP_SESSION_MODERATOR: ULong         = 0x1UL shl 37

// Experiences
const val GP_EXPERIENCE_ADMIN: ULong          = 0x1UL shl 49
const val GP_EXPERIENCE_CREATOR: ULong        = 0x1UL shl 50

// Group Banning
const val GP_GROUP_BAN_ACCESS: ULong          = 0x1UL shl 51

val GP_DEFAULT_MEMBER: ULong = GP_ACCOUNTING_ACCOUNTABLE or
        GP_LAND_ALLOW_SET_HOME or
        GP_NOTICES_RECEIVE or
        GP_SESSION_JOIN or
        GP_SESSION_VOICE

val GP_DEFAULT_OFFICER: ULong = GP_DEFAULT_MEMBER or
        GP_GROUP_CHANGE_IDENTITY or
        GP_LAND_ADMIN or
        GP_LAND_ALLOW_EDIT_LAND or
        GP_LAND_ALLOW_FLY or
        GP_LAND_ALLOW_CREATE or
        GP_LAND_ALLOW_ENVIRONMENT or
        GP_LAND_ALLOW_LANDMARK or
        GP_LAND_CHANGE_IDENTITY or
        GP_LAND_CHANGE_MEDIA or
        GP_LAND_DEED or
        GP_LAND_DIVIDE_JOIN or
        GP_LAND_EDIT or
        GP_LAND_FIND_PLACES or
        GP_LAND_GARDENING or
        GP_LAND_MANAGE_ALLOWED or
        GP_LAND_MANAGE_BANNED or
        GP_LAND_MANAGE_PASSES or
        GP_LAND_OPTIONS or
        GP_LAND_RELEASE or
        GP_LAND_RETURN_GROUP_OWNED or
        GP_LAND_RETURN_GROUP_SET or
        GP_LAND_RETURN_NON_GROUP or
        GP_LAND_SET_LANDING_POINT or
        GP_LAND_SET_SALE_INFO or
        GP_MEMBER_EJECT or
        GP_MEMBER_INVITE or
        GP_MEMBER_OPTIONS or
        GP_MEMBER_VISIBLE_IN_DIR or
        GP_NOTICES_SEND or
        GP_OBJECT_DEED or
        GP_OBJECT_MANIPULATE or
        GP_OBJECT_SET_SALE or
        GP_ROLE_ASSIGN_MEMBER_LIMITED or
        GP_ROLE_PROPERTIES or
        GP_SESSION_MODERATOR
