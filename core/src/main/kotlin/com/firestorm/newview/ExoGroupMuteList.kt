package com.firestorm.newview

import java.util.UUID

object exoGroupMuteList {

    private val muted: MutableSet<UUID> = mutableSetOf()
    private val deferredGroupChatSessionIds: MutableSet<UUID> = mutableSetOf()

    fun isMuted(group: UUID): Boolean {
        return false
    }

    fun isLoaded(): Boolean {
        return false
    }

    fun add(group: UUID) {
        System.err.println("exoGroupMuteList: add not yet implemented")
    }

    fun remove(group: UUID) {
        System.err.println("exoGroupMuteList: remove not yet implemented")
    }

    fun loadMuteList(): Boolean {
        return false
    }

    fun addDeferredGroupChat(group: UUID) {
        deferredGroupChatSessionIds.add(group)
    }

    fun restoreDeferredGroupChat(group: UUID): Boolean {
        if (!isLoaded()) return false

        if (!deferredGroupChatSessionIds.remove(group)) return false

        return false
    }

    private fun saveMuteList(): Boolean {
        return false
    }

    private fun getFilePath(): String {
        return ""
    }

    private fun mutelistString(group: UUID): String = "Group:${group}"
}
