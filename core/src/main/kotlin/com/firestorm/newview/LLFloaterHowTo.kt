package com.firestorm.newview

private const val STACK_WIDTH = 300
private const val STACK_HEIGHT = 505

class LLFloaterHowTo(key: Params) : LLFloaterWebContent(key) {

    private var mShowPageTitle: Boolean = false

    override fun postBuild(): Boolean {
        super.postBuild()
        mShowPageTitle = false
        return true
    }

    override fun onOpen(key: Any) {
        val p = LLFloaterWebContent.Params(key)
        if (!p.urlIsProvided() || p.url.getValue().isEmpty()) {
            val url = gSavedSettings.getString("GuidebookURL")
            p.url = LLWeb.expandURLSubstitutions(url, emptyMap())
        }
        p.showChrome = false

        super.onOpen(p)

        if (p.preferredMediaSize().isEmpty()) {
            val stack = getChild<LLLayoutStack>("stack1")
            val stackRect = stack.getRect()
            stack.reshape(STACK_WIDTH, STACK_HEIGHT)
            stack.setOrigin(stackRect.mLeft, stackRect.mTop - STACK_HEIGHT)
            stack.updateLayout()
        }
    }

    override fun handleKeyHere(key: Int, mask: Int): Boolean {
        if (key == KEY_F1) {
            closeFloater()
            return true
        }
        return false
    }

    override fun matchesKey(key: Any): Boolean = true

    companion object {
        fun getInstance(): LLFloaterHowTo? = LLFloaterReg.getTypedInstance("guidebook")
    }
}
