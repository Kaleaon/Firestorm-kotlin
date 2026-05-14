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
        System.err.println("ScriptMovedObserver: init not yet implemented")
    }

    fun destroy() {
        System.err.println("ScriptMovedObserver: destroy not yet implemented")
    }

    fun changed(mask: UInt) {
        System.err.println("ScriptMovedObserver: changed not yet implemented")
    }
}

class CallbackTimer(private val period: Float, private val callback: () -> Boolean) {

    fun tick(): Boolean = callback()

    companion object {
        fun setup(period: Float, callback: () -> Boolean): CallbackTimer {
            System.err.println("CallbackTimer: setup not yet implemented")
            return CallbackTimer(period, callback)
        }
    }
}
