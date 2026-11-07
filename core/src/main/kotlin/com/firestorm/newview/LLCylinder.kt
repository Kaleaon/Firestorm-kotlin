package com.firestorm.newview

import kotlin.math.cos
import kotlin.math.sin

class LLCone {

    fun render(sides: Int = 12) {
        // no-op
        // The base fan iterates i in 0 until sides:
        //   a = (i.toFloat() / sides) * PI * 2f
        //   x = cos(a) * 0.5f
        //   y = sin(a) * 0.5f
        //   vertex(x, y, -0.5f)
        // then closes with vertex(cos(0)*0.5, sin(0)*0.5, -0.5)
    }
}

val gCone = LLCone()
