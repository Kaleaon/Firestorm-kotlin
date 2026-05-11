package com.firestorm.newview

import java.io.File
import java.util.UUID

enum class VisualMuteSettings(val value: Int) {
    AV_RENDER_NORMALLY(0),
    AV_DO_NOT_RENDER(1),
    AV_ALWAYS_RENDER(2);

    companion object {
        fun fromInt(v: Int): VisualMuteSettings =
            entries.firstOrNull { it.value == v } ?: AV_RENDER_NORMALLY
    }
}

object FSAvatarRenderPersistence {

    private val avatarRenderMap: MutableMap<UUID, VisualMuteSettings> = mutableMapOf()

    val onRenderSettingChanged: MutableList<(UUID, VisualMuteSettings) -> Unit> = mutableListOf()

    fun init() {
        loadAvatarRenderSettings()
    }

    fun getAvatarRenderSettings(avatarId: UUID): VisualMuteSettings =
        avatarRenderMap[avatarId] ?: VisualMuteSettings.AV_RENDER_NORMALLY

    fun setAvatarRenderSettings(avatarId: UUID, renderSettings: VisualMuteSettings) {
        if (renderSettings == VisualMuteSettings.AV_RENDER_NORMALLY) {
            avatarRenderMap.remove(avatarId)
        } else {
            avatarRenderMap[avatarId] = renderSettings
        }
        onRenderSettingChanged.forEach { it(avatarId, renderSettings) }
    }

    fun getAvatarRenderMap(): Map<UUID, VisualMuteSettings> = avatarRenderMap.toMap()

    private fun loadAvatarRenderSettings() {
        val filename = resolvePerAccountFilename("avatar_render_settings.xml")
        val file = File(filename)
        if (!file.exists()) return

        val parsed = parseXmlSettingsFile(file)
        avatarRenderMap.clear()
        parsed.forEach { (key, valueStr) ->
            val id = runCatching { UUID.fromString(key) }.getOrNull() ?: return@forEach
            val setting = valueStr.toIntOrNull()?.let { VisualMuteSettings.fromInt(it) } ?: return@forEach
            avatarRenderMap[id] = setting
        }
    }

    fun saveAvatarRenderSettings() {
        val filename = resolvePerAccountFilename("avatar_render_settings.xml")
        val data = avatarRenderMap.entries.associate { (id, setting) ->
            id.toString() to setting.value.toString()
        }
        writeXmlSettingsFile(File(filename), data)
    }

    private fun resolvePerAccountFilename(name: String): String {
        TODO("APR: use JVM equivalent of gDirUtilp->getExpandedFilename(LL_PATH_PER_SL_ACCOUNT, name)")
    }

    private fun parseXmlSettingsFile(file: File): Map<String, String> {
        TODO("APR: use JVM XML parser (e.g. javax.xml or kotlinx.serialization) to deserialise LLSD map from file")
    }

    private fun writeXmlSettingsFile(file: File, data: Map<String, String>) {
        TODO("APR: serialise data as LLSD-compatible XML and write to file using standard JVM I/O")
    }
}
