package com.firestorm.llui

import com.firestorm.llmath.Color4

class UIColor {
    private var colorValue: Color4
    private var reference: String? = null

    constructor(color: Color4) {
        this.colorValue = color
    }

    constructor(name: String) {
        this.reference = name
        this.colorValue = Color4.TRANSPARENT
    }

    constructor(other: UIColor) {
        this.colorValue = other.colorValue
        this.reference = other.reference
    }

    fun set(color: Color4) {
        reference = null
        colorValue = color
    }

    fun set(other: UIColor) {
        reference = other.reference
        colorValue = other.colorValue
    }

    fun get(): Color4 = reference?.let { UIColorTable.get(it) } ?: colorValue

    fun isReference(): Boolean = reference != null

    operator fun invoke(): Color4 = get()
}

