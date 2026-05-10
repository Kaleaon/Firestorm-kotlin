package com.firestorm.newview

import kotlin.math.cos
import kotlin.math.sin

class LLCone {

    fun render(sides: Int = 12) {
        TODO("GPU: gGL.begin(TRIANGLE_FAN); emit cone apex at (0,0,0) then ${sides} base vertices at radius 0.5 on z=-0.5 plane, close with first base vertex; repeat for cap fan from (0,0,0.5)")
        // The base fan iterates i in 0 until sides:
        //   a = (i.toFloat() / sides) * PI * 2f
        //   x = cos(a) * 0.5f
        //   y = sin(a) * 0.5f
        //   vertex(x, y, -0.5f)
        // then closes with vertex(cos(0)*0.5, sin(0)*0.5, -0.5)
    }
}

val gCone = LLCone()
