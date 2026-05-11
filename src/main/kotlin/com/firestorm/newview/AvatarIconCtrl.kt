package com.firestorm.newview

import com.firestorm.ui.AvatarName
import com.firestorm.ui.AvatarNameCache
import com.firestorm.ui.AvatarPropertiesObserver
import com.firestorm.ui.AvatarPropertiesProcessor
import com.firestorm.ui.EAvatarProcessorType
import com.firestorm.ui.GridManager
import com.firestorm.ui.IconCtrl
import com.firestorm.ui.LLSD
import java.util.UUID

data class AvatarIconIDCacheItem(val iconId: UUID, val cachedTimeMs: Long) {
    fun expired(): Boolean {
        val thresholdMs = (24.0 + 1.0) * 60.0 * 60.0 * 1000.0
        return (System.currentTimeMillis() - cachedTimeMs) > thresholdMs
    }
}

object AvatarIconIDCache {
    private val cache: MutableMap<UUID, AvatarIconIDCacheItem> = mutableMapOf()
    private val filename: String = run {
        val gridId = GridManager.getInstance().getGridId()
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .lowercase()
        "avatar_icons_cache.$gridId.txt"
    }

    fun load() {
        TODO("APR: use JVM equivalent for reading $filename from cache dir")
    }

    fun save() {
        TODO("APR: use JVM equivalent for writing $filename to cache dir")
    }

    fun get(avatarId: UUID): UUID? {
        val item = cache[avatarId] ?: return null
        if (item.expired()) return null
        return item.iconId
    }

    fun add(avatarId: UUID, iconId: UUID) {
        cache[avatarId] = AvatarIconIDCacheItem(iconId, System.currentTimeMillis())
    }

    fun remove(avatarId: UUID) {
        cache.remove(avatarId)
    }
}

enum class SymbolPos {
    BOTTOM_LEFT, BOTTOM_RIGHT, TOP_LEFT, TOP_RIGHT
}

open class AvatarIconCtrl(
    avatarId: UUID? = null,
    drawTooltip: Boolean = true,
    defaultIconName: String = "",
    val symbolHpad: Int = 0,
    val symbolVpad: Int = 0,
    val symbolSize: Int = 1,
    val symbolPos: SymbolPos = SymbolPos.BOTTOM_RIGHT,
    minWidth: Int = 32,
    minHeight: Int = 32,
) : IconCtrl(minWidth = minWidth, minHeight = minHeight), AvatarPropertiesObserver {

    companion object {
        val NULL_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    }

    protected var avatarId: UUID = NULL_ID
        private set
    protected var fullName: String = ""
        private set
    var drawTooltip: Boolean = drawTooltip
        private set
    protected val defaultIconName: String = defaultIconName

    private var avatarNameCacheConnection: AutoCloseable? = null

    init {
        if (avatarId != null) {
            setValue(LLSD.fromUUID(avatarId))
        } else {
            super.setValue(LLSD.fromString(defaultIconName))
        }
    }

    fun setDrawTooltip(value: Boolean) {
        drawTooltip = value
    }

    fun getAvatarId(): UUID = avatarId
    fun getFullName(): String = fullName

    open fun setValue(value: LLSD) {
        if (value.isUUID()) {
            val app = AvatarPropertiesProcessor.getInstance()
            val newId = value.asUUID()
            if (avatarId != NULL_ID) {
                app.removeObserver(avatarId, this)
            }
            if (avatarId != newId) {
                avatarId = newId
                if (!updateFromCache()) {
                    super.setValue(LLSD.fromString(defaultIconName))
                    app.addObserver(avatarId, this)
                    app.sendAvatarLegacyPropertiesRequest(avatarId)
                } else if (agentId() == avatarId) {
                    app.addObserver(avatarId, this)
                }
            }
        } else {
            super.setValue(value)
        }
        fetchAvatarName()
    }

    private fun fetchAvatarName() {
        if (avatarId == NULL_ID) return
        avatarNameCacheConnection?.close()
        avatarNameCacheConnection = AvatarNameCache.get(avatarId) { id, avName ->
            onAvatarNameCache(id, avName)
        }
    }

    protected fun updateFromCache(): Boolean {
        val iconId = AvatarIconIDCache.get(avatarId) ?: return false
        if (iconId == NULL_ID) {
            super.setValue(LLSD.fromString(defaultIconName))
            return false
        }
        super.setValue(LLSD.fromUUID(iconId))
        return true
    }

    override fun processProperties(data: Any?, type: EAvatarProcessorType) {
        when (type) {
            EAvatarProcessorType.APT_PROPERTIES_LEGACY -> {
                val avatarData = data as? AvatarLegacyData ?: return
                if (avatarData.avatarId != avatarId) return
                AvatarIconIDCache.add(avatarId, avatarData.imageId)
                updateFromCache()
            }
            EAvatarProcessorType.APT_PROPERTIES -> {
                val avatarData = data as? AvatarData ?: return
                if (avatarData.avatarId != avatarId) return
                AvatarIconIDCache.add(avatarId, avatarData.imageId)
                updateFromCache()
            }
            else -> Unit
        }
    }

    private fun onAvatarNameCache(agentId: UUID, avName: AvatarName) {
        avatarNameCacheConnection = null
        if (agentId != avatarId) return
        fullName = avName.getUserName()
        if (drawTooltip) setToolTip(fullName) else setToolTip("")
    }

    open fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        if (!drawTooltip) return false
        return super.handleToolTip(x, y, mask)
    }

    fun dispose() {
        if (avatarId != NULL_ID) {
            AvatarPropertiesProcessor.getInstance().removeObserver(avatarId, this)
        }
        avatarNameCacheConnection?.close()
        avatarNameCacheConnection = null
    }

    private fun agentId(): UUID = TODO("APR: return gAgent.getID()")
}
