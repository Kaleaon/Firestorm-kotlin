package com.firestorm.newview

import com.firestorm.trans.Trans

object AnimStateLabels {
    fun getStateLabel(animName: String): String = Trans.getString("anim_$animName")
}
