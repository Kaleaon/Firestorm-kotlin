package com.firestorm.llui

import com.firestorm.llmath.Rect

class StatView(
    name: String,
    rect: Rect = Rect(),
    label: String = "",
    showLabel: Boolean = true,
    displayChildrenInitial: Boolean = true,
    private val setting: String = ""
) : ContainerView(name, rect, label, showLabel, displayChildrenInitial) {

    init {
        if (setting.isNotEmpty()) {
            val persisted = loadBoolSetting(setting)
            if (persisted != null) {
                displayChildren = persisted
            }
        }
    }

    fun destroy() {
        if (setting.isNotEmpty()) {
            saveBoolSetting(setting, displayChildren)
        }
    }

    private fun loadBoolSetting(key: String): Boolean? {
        return null
    }

    private fun saveBoolSetting(key: String, value: Boolean) {
        System.err.println("StatView: saveBoolSetting not yet implemented")
    }
}
