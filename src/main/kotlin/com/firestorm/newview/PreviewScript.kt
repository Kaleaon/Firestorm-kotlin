package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llui.LLSD

const val HELLO_LSL: String = """default
{
   state_entry()
   {
       llSay(0, "Hello, Avatar!");
   }

   touch_start(integer total_number)
   {
       llSay(0, "Touched.");
   }
}
"""

const val HELP_LSL_PORTAL_TOPIC: String = "LSL_Portal"
const val DEFAULT_SCRIPT_NAME: String = "New Script"
const val DEFAULT_SCRIPT_DESC: String = "(No Description)"

class ScriptMovedObserver(private val preview: PreviewLSL) {

    init {
        TODO("APR: use JVM equivalent - register as inventory observer via gInventory.addObserver()")
    }

    fun destroy() {
        TODO("APR: use JVM equivalent - gInventory.removeObserver(this)")
    }

    fun changed(mask: UInt) {
        TODO("APR: use JVM equivalent - check changed IDs against preview.getScriptID(); call preview.setDirty() on STRUCTURE flag")
    }
}

class CallbackTimer(private val period: Float, private val callback: () -> Boolean) {

    fun tick(): Boolean = callback()

    companion object {
        fun setup(period: Float, callback: () -> Boolean): CallbackTimer {
            TODO("APR: use JVM equivalent - schedule repeating timer with given period in seconds")
        }
    }
}
