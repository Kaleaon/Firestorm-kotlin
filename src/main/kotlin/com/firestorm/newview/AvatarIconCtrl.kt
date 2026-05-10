package com.firestorm.newview

import com.firestorm.ui.IconCtrl
import com.firestorm.ui.LLSingleton
import com.firestorm.ui.LLSD
import com.firestorm.ui.LLAvatarName
import com.firestorm.ui.LLAvatarPropertiesObserver
import com.firestorm.ui.EAvatarProcessorType
import com.firestorm.ui.LLAvatarPropertiesProcessor
import com.firestorm.ui.LLAvatarNameCache
import com.firestorm.ui.LLGridManager
import java.io.File
import java.util.UUID

// ---------------------------------------------------------------------------
// AvatarIconIDCache  (C++ LLAvatarIconIDCache singleton)
// ---------------------------------------------------------------------------

data class AvatarIconIDCacheItem(val iconId: UUID, val cachedTime: Long) {
    fun expired(): Boolean {
        val secPerDayPlusHour = (24.0 + 1.0) * 60.0 * 60.0 * 1000L
        return (System.currentTimeMillis() - cachedTime) > secPerDayPlusHour
    }
}

object AvatarIconIDCache {
    private val cache: MutableMap<UUID, AvatarIconIDCacheItem> = mutableMapOf()
    private val filename: String = run {
        val gridIdStr = LLGridManager.getInstance().gridId
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .lowercase()
        "avatar_icons_cache.$gridIdStr.txt"
    }

    fun load() {
        TODO("APR: use JVM equivalent")
    }

    fun save() {
        TODO("APR: use JVM equivalent")
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

// ---------------------------------------------------------------------------
// SymbolPos enum  (C++ LLAvatarIconCtrlEnums::ESymbolPos)
// ---------------------------------------------------------------------------

enum class SymbolPos {
    BOTTOM_LEFT, BOTTOM_RIGHT, TOP_LEFT, TOP_RIGHT
}

// ---------------------------------------------------------------------------
// AvatarIconCtrl  (C++ LLAvatarIconCtrl)
// ---------------------------------------------------------------------------

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
) : IconCtrl(minWidth = minWidth, minHeight = minHeight), LLAvatarPropertiesObserver {

    protected var avatarId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    protected var fullName: String = ""
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
            val app = LLAvatarPropertiesProcessor.getInstance()
            val newId = value.asUUID()
            if (avatarId != UUID.fromString("00000000-0000-0000-0000-000000000000")) {
                app.removeObserver(avatarId, this)
            }
            if (avatarId != newId) {
                avatarId = newId
                if (!updateFromCache()) {
                    super.setValue(LLSD.fromString(defaultIconName))
                    app.addObserver(avatarId, this)
                    app.sendAvatarLegacyPropertiesRequest(avatarId)
                } else if (com.firestorm.newview.agentID == avatarId) {
                    app.addObserver(avatarId, this)
                }
            }
        } else {
            super.setValue(value)
        }
        fetchAvatarName()
    }

    private fun fetchAvatarName() {
        val id = avatarId
        if (id == UUID.fromString("00000000-0000-0000-0000-000000000000")) return
        avatarNameCacheConnection?.close()
        avatarNameCacheConnection = LLAvatarNameCache.get(id) { agentId, avName ->
            onAvatarNameCache(agentId, avName)
        }
    }

    protected fun updateFromCache(): Boolean {
        val iconId = AvatarIconIDCache.get(avatarId) ?: return false
        if (iconId == UUID.fromString("00000000-0000-0000-0000-000000000000")) {
            super.setValue(LLSD.fromString(defaultIconName))
            return false
        }
        super.setValue(LLSD.fromUUID(iconId))
        return true
    }

    override fun processProperties(data: Any?, type: EAvatarProcessorType) {
        when (type) {
            EAvatarProcessorType.APT_PROPERTIES_LEGACY -> {
                val avatarData = data as? LLAvatarLegacyData ?: return
                if (avatarData.avatarId != avatarId) return
                AvatarIconIDCache.add(avatarId, avatarData.imageId)
                updateFromCache()
            }
            EAvatarProcessorType.APT_PROPERTIES -> {
                val avatarData = data as? LLAvatarData ?: return
                if (avatarData.avatarId != avatarId) return
                AvatarIconIDCache.add(avatarId, avatarData.imageId)
                updateFromCache()
            }
            else -> Unit
        }
    }

    private fun onAvatarNameCache(agentId: UUID, avName: LLAvatarName) {
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
        val nullId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        if (avatarId != nullId) {
            LLAvatarPropertiesProcessor.getInstance().removeObserver(avatarId, this)
        }
        avatarNameCacheConnection?.close()
        avatarNameCacheConnection = null
    }
}
