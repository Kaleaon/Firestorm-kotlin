package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import java.util.UUID

data class AvatarName(
    var displayName: String = "",
    var legacyFirstName: String = "",
    var legacyLastName: String = "",
    var slId: String = "",
    var isDisplayNameDefault: Boolean = true,
    var expires: Double = 0.0
) {
    fun getUserName(): String = slId.ifEmpty { "$legacyFirstName $legacyLastName".trim() }
    fun getAccountName(): String = "$legacyFirstName $legacyLastName".trim()
    fun getDisplayName(): String = displayName.ifEmpty { getAccountName() }
    fun isValidName(maxUnrefreshed: Double): Boolean = expires > maxUnrefreshed
    fun setExpires(deltaSeconds: Double) {
        expires = System.currentTimeMillis() / 1000.0 + deltaSeconds
    }
}

object AvatarNameCache {

    private const val TEMP_CACHE_ENTRY_LIFETIME = 60.0
    private const val MAX_UNREFRESHED_TIME = 20.0 * 60.0
    private const val SECS_BETWEEN_REQUESTS = 0.1f
    private const val PENDING_TIMEOUT_SECS = 5.0 * 60.0
    private const val DEFAULT_EXPIRES_SECS = 60.0 * 60.0

    private var nameLookupUrl: String = ""
    private var usePeopleAPI: Boolean = true
    private var rlvForceDisplayNames: Boolean = false
    private var running: Boolean = false
    private var lastExpireCheck: Double = 0.0
    private var lastRequestTime: Double = 0.0

    private val cache: MutableMap<LLUUID, AvatarName> = mutableMapOf()
    private val askQueue: MutableSet<LLUUID> = mutableSetOf()
    private val pendingQueue: MutableMap<LLUUID, Double> = mutableMapOf()
    private val signalMap: MutableMap<LLUUID, MutableList<(LLUUID, AvatarName) -> Unit>> = mutableMapOf()

    private val useDisplayNamesCallbacks: MutableList<() -> Unit> = mutableListOf()
    var accountNameChangedCallback: ((LLUUID, AvatarName) -> Unit)? = null
    var customNameCheckCallback: ((LLUUID, dnRemovedOut: BooleanArray, customNameOut: Array<String>) -> Boolean)? = null

    fun setNameLookupURL(url: String) { nameLookupUrl = url }
    fun hasNameLookupURL(): Boolean = nameLookupUrl.isNotEmpty()
    fun setUsePeopleAPI(use: Boolean) { usePeopleAPI = use }
    fun usePeopleAPI(): Boolean = hasNameLookupURL() && usePeopleAPI

    fun getForceDisplayNames(): Boolean = rlvForceDisplayNames
    fun setForceDisplayNames(force: Boolean) { rlvForceDisplayNames = force }

    fun addUseDisplayNamesCallback(cb: () -> Unit) { useDisplayNamesCallbacks.add(cb) }

    fun setUseDisplayNames(use: Boolean) {
        val effective = use || rlvForceDisplayNames
        useDisplayNamesCallbacks.forEach { it() }
    }

    fun setUseUsernames(use: Boolean) {
        useDisplayNamesCallbacks.forEach { it() }
    }

    fun insert(agentId: LLUUID, avName: AvatarName) {
        cache[agentId] = avName
    }

    fun erase(agentId: LLUUID) {
        cache.remove(agentId)
    }

    fun fetch(agentId: LLUUID) {
        askQueue.add(agentId)
    }

    fun clearCache() {
        cache.clear()
    }

    fun get(agentId: LLUUID, avName: AvatarName): Boolean = getName(agentId, avName)

    fun getName(agentId: LLUUID, avName: AvatarName): Boolean {
        if (agentId == LLUUID.NULL) return false
        if (running) {
            val cached = cache[agentId]
            if (cached != null) {
                applyCustomName(agentId, cached, avName)
                val now = System.currentTimeMillis() / 1000.0
                if (cached.expires < now && !isRequestPending(agentId)) {
                    askQueue.add(agentId)
                }
                return true
            }
        }
        if (!isRequestPending(agentId)) {
            askQueue.add(agentId)
        }
        return false
    }

    fun get(agentId: LLUUID, slot: (LLUUID, AvatarName) -> Unit): Any? {
        return getNameCallback(agentId, slot)
    }

    fun getNameCallback(agentId: LLUUID, slot: (LLUUID, AvatarName) -> Unit): Any? {
        if (running) {
            val cached = cache[agentId]
            if (cached != null) {
                val out = AvatarName()
                applyCustomName(agentId, cached, out)
                val now = System.currentTimeMillis() / 1000.0
                if (out.expires > now) {
                    slot(agentId, out)
                    return null
                }
            }
        }
        if (!isRequestPending(agentId)) askQueue.add(agentId)
        signalMap.getOrPut(agentId) { mutableListOf() }.add(slot)
        return null
    }

    fun findIdByName(name: String): LLUUID {
        return cache.entries.firstOrNull { it.value.getUserName() == name }?.key ?: LLUUID.NULL
    }

    fun handleAgentError(agentId: LLUUID) {
        if (agentId == LLUUID.NULL) {
            pendingQueue.remove(agentId)
            return
        }
        val existing = cache[agentId]
        if (existing != null) {
            pendingQueue.remove(agentId)
            existing.setExpires(TEMP_CACHE_ENTRY_LIFETIME)
        } else {
            System.err.println("AvatarNameCache: handleAgentError not yet implemented")
        }
    }

    fun processName(agentId: LLUUID, avName: AvatarName) {
        if (agentId == LLUUID.NULL) return
        val existing = cache[agentId]
        val updatedAccount = existing?.getAccountName() != avName.getAccountName()
        cache[agentId] = avName
        pendingQueue.remove(agentId)
        if (updatedAccount) {
            accountNameChangedCallback?.invoke(agentId, avName)
        }
        val signals = signalMap.remove(agentId)
        signals?.forEach { it(agentId, avName) }
    }

    fun nameExpirationFromHeaders(headers: Map<String, String>): Double {
        val cacheControl = headers["cache-control"] ?: headers["Cache-Control"]
        if (cacheControl != null) {
            val maxAge = maxAgeFromCacheControl(cacheControl)
            if (maxAge != null && maxAge > 0) {
                return System.currentTimeMillis() / 1000.0 + maxAge
            }
        }
        return System.currentTimeMillis() / 1000.0 + DEFAULT_EXPIRES_SECS
    }

    fun idle() {
        running = true
        val now = System.currentTimeMillis() / 1000.0
        if (now - lastRequestTime < SECS_BETWEEN_REQUESTS) return
        if (askQueue.isNotEmpty()) {
            if (usePeopleAPI()) {
                requestNamesViaCapability()
            } else {
                requestNamesViaLegacy()
            }
        }
        if (askQueue.isEmpty()) lastRequestTime = now
        eraseUnrefreshed()
    }

    private fun isRequestPending(agentId: LLUUID): Boolean {
        val sentAt = pendingQueue[agentId] ?: return false
        val expireTime = System.currentTimeMillis() / 1000.0 - PENDING_TIMEOUT_SECS
        return sentAt > expireTime
    }

    private fun eraseUnrefreshed() {
        val now = System.currentTimeMillis() / 1000.0
        val maxUnrefreshed = now - MAX_UNREFRESHED_TIME
        if (lastExpireCheck != 0.0 && lastExpireCheck >= maxUnrefreshed) return
        lastExpireCheck = now
        val iter = cache.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            if (entry.value.expires < maxUnrefreshed) iter.remove()
        }
    }

    private fun applyCustomName(agentId: LLUUID, src: AvatarName, dst: AvatarName) {
        val dnRemoved = BooleanArray(1)
        val customName = Array(1) { "" }
        if (customNameCheckCallback?.invoke(agentId, dnRemoved, customName) == true) {
            dst.apply {
                displayName = if (dnRemoved[0]) src.getAccountName() else customName[0]
                legacyFirstName = src.legacyFirstName
                legacyLastName = src.legacyLastName
                slId = src.slId
                isDisplayNameDefault = dnRemoved[0]
                expires = src.expires
            }
        } else {
            dst.apply {
                displayName = src.displayName
                legacyFirstName = src.legacyFirstName
                legacyLastName = src.legacyLastName
                slId = src.slId
                isDisplayNameDefault = src.isDisplayNameDefault
                expires = src.expires
            }
        }
    }

    private fun requestNamesViaCapability() {
        System.err.println("AvatarNameCache: requestNamesViaCapability not yet implemented")
    }

    private fun requestNamesViaLegacy() {
        System.err.println("AvatarNameCache: requestNamesViaLegacy not yet implemented")
    }
}

fun maxAgeFromCacheControl(cacheControl: String): Int? {
    val parts = cacheControl.split(",")
    for (part in parts) {
        val trimmed = part.trim()
        if (trimmed.startsWith("max-age=", ignoreCase = true)) {
            return trimmed.substringAfter("=").trim().toIntOrNull()
        }
    }
    return null
}
