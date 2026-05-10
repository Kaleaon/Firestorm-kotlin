package com.firestorm.newview

import com.firestorm.llxml.ControlGroup

object PreferencesMgr {

    val global: ControlGroup = ControlGroup("Global")
    val savedSettings: ControlGroup = ControlGroup("Saved")
    val perAccount: ControlGroup = ControlGroup("PerAccount")
    val warnings: ControlGroup = ControlGroup("Warnings")
    val crashes: ControlGroup = ControlGroup("Crashes")

    fun loadSettings(path: String) {}

    fun saveSettings(path: String) {}

    fun getBool(name: String): Boolean = global.getBool(name) ?: false

    fun getS32(name: String): Int = global.getS32(name) ?: 0

    fun getF32(name: String): Float = global.getF32(name) ?: 0f

    fun getString(name: String): String = global.getString(name) ?: ""

    fun setBool(name: String, v: Boolean) {
        global.setBool(name, v)
    }

    fun setS32(name: String, v: Int) {
        global.setS32(name, v)
    }

    fun setF32(name: String, v: Float) {
        global.setF32(name, v)
    }

    fun setString(name: String, v: String) {
        global.setString(name, v)
    }
}
