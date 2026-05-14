package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

typealias ExperienceGetFn = (Map<String, Any?>) -> Unit
typealias CapabilityQuery = (String) -> String

object ExperienceCache {

    const val NAME        = "name"
    const val EXPERIENCE_ID = "public_id"
    const val AGENT_ID    = "agent_id"
    const val GROUP_ID    = "group_id"
    const val PROPERTIES  = "properties"
    const val EXPIRES     = "expiration"
    const val DESCRIPTION = "description"
    const val QUOTA       = "quota"
    const val MATURITY    = "maturity"
    const val METADATA    = "extended_metadata"
    const val SLURL       = "slurl"
    const val MISSING     = "DoesNotExist"

    const val PRIVATE_KEY = "private_id"

    const val PROPERTY_INVALID   = 1 shl 0
    const val PROPERTY_PRIVILEGED = 1 shl 3
    const val PROPERTY_GRID      = 1 shl 4
    const val PROPERTY_PRIVATE   = 1 shl 5
    const val PROPERTY_DISABLED  = 1 shl 6
    const val PROPERTY_SUSPENDED = 1 shl 7

    const val DEFAULT_EXPIRATION = 600.0
    const val DEFAULT_QUOTA      = 128
    const val SEARCH_PAGE_SIZE   = 30

    @Volatile var shutdown = false

    var currentGridId: String = ""
    var isInOpenSim: Boolean = false

    private val cache: MutableMap<LLUUID, MutableMap<String, Any?>> = mutableMapOf()
    private val signalMap: MutableMap<LLUUID, MutableList<ExperienceGetFn>> = mutableMapOf()
    private val requestQueue: MutableSet<LLUUID> = mutableSetOf()
    private val pendingQueue: MutableMap<LLUUID, Double> = mutableMapOf()

    private val privateToPublicKeyMap: MutableMap<LLUUID, LLUUID> = mutableMapOf()

    var capability: CapabilityQuery? = null
    var cacheFileName: String = ""

    private const val PENDING_TIMEOUT_SECS = 5.0 * 60.0
    private const val SECS_BETWEEN_REQUESTS = 0.5
    private const val ERASE_EXPIRED_TIMEOUT = 60.0

    fun setCapabilityQuery(queryfn: CapabilityQuery) {
        capability = queryfn
    }

    fun setCurrentGrid(gridId: String, inOpenSim: Boolean) {
        currentGridId = gridId
        isInOpenSim = inOpenSim
    }

    fun cleanup() {
        shutdown = true
        exportFile()
    }

    fun erase(key: LLUUID) {
        cache.remove(key)
    }

    fun fetch(key: LLUUID, refresh: Boolean = false): Boolean {
        if (!key.isNull() && !isRequestPending(key) && (refresh || !cache.containsKey(key))) {
            requestQueue.add(key)
            return true
        }
        return false
    }

    fun insert(experienceData: Map<String, Any?>) {
        val id = experienceData[EXPERIENCE_ID] as? LLUUID ?: return
        processExperience(id, experienceData)
    }

    fun get(key: LLUUID): Map<String, Any?> {
        if (key.isNull()) return emptyMap()
        val cached = cache[key]
        if (cached != null) return cached
        fetch(key)
        return emptyMap()
    }

    fun get(key: LLUUID, slot: ExperienceGetFn) {
        if (key.isNull()) return
        val cached = cache[key]
        if (cached != null) {
            slot(cached)
            return
        }
        fetch(key)
        signalMap.getOrPut(key) { mutableListOf() }.add(slot)
    }

    fun isRequestPending(publicKey: LLUUID): Boolean {
        val sentAt = pendingQueue[publicKey] ?: return false
        val expireTime = nowSeconds() - PENDING_TIMEOUT_SECS
        return sentAt > expireTime
    }

    fun fetchAssociatedExperience(objectId: LLUUID, itemId: LLUUID, fn: ExperienceGetFn) {
        fetchAssociatedExperience(objectId, itemId, "", fn)
    }

    fun fetchAssociatedExperience(objectId: LLUUID, itemId: LLUUID, url: String, fn: ExperienceGetFn) {
        val cap = capability ?: run { return }
        val resolvedUrl = url.ifEmpty { cap("GetMetadata") }
        if (resolvedUrl.isEmpty()) return
        System.err.println("ExperienceCache: fetchAssociatedExperience not yet implemented")
    }

    fun findExperienceByName(text: String, page: Int, fn: ExperienceGetFn) {
        val cap = capability ?: return
        val url = cap("FindExperienceByName")
        if (url.isEmpty()) return
        System.err.println("ExperienceCache: findExperienceByName not yet implemented")
    }

    fun getGroupExperiences(groupId: LLUUID, fn: ExperienceGetFn) {
        val cap = capability ?: return
        val url = cap("GroupExperiences")
        if (url.isEmpty()) return
        System.err.println("ExperienceCache: getGroupExperiences not yet implemented")
    }

    fun getRegionExperiences(regioncaps: CapabilityQuery, fn: ExperienceGetFn) {
        val url = regioncaps("RegionExperiences")
        if (url.isEmpty()) return
        System.err.println("ExperienceCache: getRegionExperiences not yet implemented")
    }

    fun setRegionExperiences(regioncaps: CapabilityQuery, experiences: Map<String, Any?>, fn: ExperienceGetFn) {
        val url = regioncaps("RegionExperiences")
        if (url.isEmpty()) return
        System.err.println("ExperienceCache: setRegionExperiences not yet implemented")
    }

    fun getExperiencePermission(experienceId: LLUUID, fn: ExperienceGetFn) {
        val cap = capability ?: return
        val url = cap("ExperiencePreferences") + "?" + experienceId.toString()
        System.err.println("ExperienceCache: getExperiencePermission not yet implemented")
    }

    fun setExperiencePermission(experienceId: LLUUID, permission: String, fn: ExperienceGetFn) {
        val cap = capability ?: return
        val url = cap("ExperiencePreferences")
        if (url.isEmpty()) return
        System.err.println("ExperienceCache: setExperiencePermission not yet implemented")
    }

    fun forgetExperiencePermission(experienceId: LLUUID, fn: ExperienceGetFn) {
        val cap = capability ?: return
        val url = cap("ExperiencePreferences") + "?" + experienceId.toString()
        System.err.println("ExperienceCache: forgetExperiencePermission not yet implemented")
    }

    fun getExperienceAdmin(experienceId: LLUUID, fn: ExperienceGetFn) {
        val cap = capability ?: return
        val url = cap("IsExperienceAdmin")
        if (url.isEmpty()) return
        System.err.println("ExperienceCache: getExperienceAdmin not yet implemented")
    }

    fun updateExperience(updateData: MutableMap<String, Any?>, fn: ExperienceGetFn) {
        val cap = capability ?: return
        val url = cap("UpdateExperience")
        if (url.isEmpty()) return
        updateData.remove(QUOTA)
        updateData.remove(EXPIRES)
        updateData.remove(AGENT_ID)
        System.err.println("ExperienceCache: updateExperience not yet implemented")
    }

    private fun processExperience(publicKey: LLUUID, experience: Map<String, Any?>) {
        val row = experience.toMutableMap()
        val expiresRaw = row[EXPIRES]
        if (expiresRaw != null) {
            val expiresDouble = (expiresRaw as? Double) ?: (expiresRaw as? Number)?.toDouble() ?: 0.0
            row[EXPIRES] = expiresDouble + nowSeconds()
        }
        cache[publicKey] = row
        val expId = row[EXPERIENCE_ID]
        if (expId is LLUUID) pendingQueue.remove(expId)
        val signals = signalMap.remove(publicKey)
        signals?.forEach { it(row) }
    }

    private fun requestExperiences() {
        val cap = capability ?: return
        val urlBase = cap("GetExperienceInfo").let {
            if (it.isEmpty()) return
            if (it.endsWith("/")) "${it}id/" else "$it/id/"
        }
        val now = nowSeconds()
        val pageSize = 3000 / 36
        val sb = StringBuilder("$urlBase?page_size=$pageSize")
        val batch = mutableSetOf<LLUUID>()

        val iter = requestQueue.iterator()
        while (iter.hasNext() && !shutdown) {
            val key = iter.next()
            iter.remove()
            batch.add(key)
            sb.append("&$EXPERIENCE_ID=$key")
            pendingQueue[key] = now
            if (requestQueue.isEmpty() || sb.length > 3000) {
                val url = sb.toString()
                val batchSnapshot = batch.toSet()
                System.err.println("ExperienceCache: requestExperiences not yet implemented")
                sb.clear()
                sb.append("$urlBase?page_size=$pageSize")
                batch.clear()
            }
        }
    }

    private fun eraseExpired() {
        val now = nowSeconds()
        val iter = cache.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val exp = entry.value
            val expires = (exp[EXPIRES] as? Double) ?: continue
            if (expires < now) {
                val id = exp[EXPERIENCE_ID] as? LLUUID
                val privateKey = exp[PRIVATE_KEY] as? LLUUID
                if (id != null && (privateKey != null || !exp.containsKey(MISSING))) {
                    fetch(id, true)
                } else {
                    iter.remove()
                }
            }
        }
    }

    private fun getExperienceId(privateKey: LLUUID, nullIfNotFound: Boolean = false): LLUUID {
        if (privateKey.isNull()) return LLUUID.NULL
        val mapped = privateToPublicKeyMap[privateKey]
        if (mapped == null) {
            return if (nullIfNotFound) LLUUID.NULL else privateKey
        }
        return mapped
    }

    private fun nowSeconds(): Double = System.currentTimeMillis() / 1000.0

    fun importFile(content: String) {
        System.err.println("ExperienceCache: importFile not yet implemented")
    }

    fun exportFile() {
        System.err.println("ExperienceCache: exportFile not yet implemented")
    }

    fun getErrorRetryDeltaTime(status: Int, headers: Map<String, String>): Double {
        val retryAfter = headers["retry-after"]
        if (retryAfter != null) {
            val delta = retryAfter.trim().toIntOrNull() ?: 0
            if (delta > 0) return delta.toDouble()
        }
        val cacheControl = headers["cache-control"]
        if (cacheControl != null) {
            val maxAge = maxAgeFromCacheControl(cacheControl)
            if (maxAge != null) return maxAge.toDouble()
        }
        return when (status) {
            503 -> 600.0
            499 -> 10.0
            else -> DEFAULT_EXPIRATION
        }
    }
}
