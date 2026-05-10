package com.firestorm.newview

import java.util.UUID

object exoGroupMuteList {

    private val muted: MutableSet<UUID> = mutableSetOf()
    private val deferredGroupChatSessionIds: MutableSet<UUID> = mutableSetOf()

    fun isMuted(group: UUID): Boolean {
        TODO("APR: if running on OpenSim check muted set directly; on SL delegate to LLMuteList.isMuted(null, mutelistString(group))")
    }

    fun isLoaded(): Boolean {
        TODO("APR: on OpenSim return true; on SL return LLMuteList.isLoaded()")
    }

    fun add(group: UUID) {
        TODO("APR: end ongoing IM session for group; on OpenSim insert into muted and saveMuteList(); on SL call LLMuteList.add(BY_NAME mute)")
    }

    fun remove(group: UUID) {
        TODO("APR: on OpenSim erase from muted and saveMuteList(); on SL call LLMuteList.remove(BY_NAME mute)")
    }

    fun loadMuteList(): Boolean {
        TODO("APR: read muted_groups.xml from per-account directory into muted set")
    }

    fun addDeferredGroupChat(group: UUID) {
        deferredGroupChatSessionIds.add(group)
    }

    fun restoreDeferredGroupChat(group: UUID): Boolean {
        if (!isLoaded()) return false

        if (!deferredGroupChatSessionIds.remove(group)) return false

        TODO("APR: look up groupData for group; if not muted open IM session via IMMgr and play UISndNewIncomingGroupIMSession if not DND; if muted clear pending invitation and send leave session")
    }

    private fun saveMuteList(): Boolean {
        TODO("APR: serialize muted set to muted_groups.xml in per-account directory using XML")
    }

    private fun getFilePath(): String {
        TODO("APR: return gDirUtilp->getExpandedFilename(LL_PATH_PER_SL_ACCOUNT, \"muted_groups.xml\") equivalent")
    }

    private fun mutelistString(group: UUID): String = "Group:${group}"
}
