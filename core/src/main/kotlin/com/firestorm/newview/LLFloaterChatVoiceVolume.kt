package com.firestorm.newview

class LLFloaterChatVoiceVolume(key: LLSD) : LLInspect(key), LLTransientFloater {

    override fun onOpen(key: LLSD) {
        super.onOpen(key)
        repositionInspector(key)
    }

    override fun getGroup(): LLTransientFloaterMgr.ETransientGroup {
        return LLTransientFloaterMgr.ETransientGroup.GLOBAL
    }

    override fun onDestroy() {
        LLTransientFloaterMgr.getInstance().removeControlView(this)
        super.onDestroy()
    }
}
