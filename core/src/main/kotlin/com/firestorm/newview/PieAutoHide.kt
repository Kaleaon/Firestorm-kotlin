package com.firestorm.newview

open class PieAutoHide(
    private val autohide: Boolean,
    private val startAutohide: Boolean
) {
    fun getStartAutohide(): Boolean = startAutohide

    // Returns true for both chain-starters and interior chain members
    fun getAutohide(): Boolean = startAutohide || autohide
}
