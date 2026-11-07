package com.firestorm.newview

import com.firestorm.ui.LLFloater
import com.firestorm.ui.LLLineEditor
import com.firestorm.ui.LLTextBox
import com.firestorm.llsd.LLSD

class LLFloaterSLappTest(key: LLSD) : LLFloater("floater_test_slapp") {

    override fun postBuild(): Boolean {
        getChild<LLLineEditor>("remove_folder_id").setKeystrokeCallback { editor, _ ->
            val slapp: String = getString("remove_folder_slapp")
            getChild<LLTextBox>("remove_folder_txt").setValue(slapp + editor.getValue().asString())
        }
        return true
    }
}
