package com.firestorm.newview

// Handles secondlife:///app/floater/* SLURLs, e.g. secondlife:///app/floater/self/close
class LLFloaterHandler : LLCommandHandler("floater", UNTRUSTED_BLOCK) {

    override fun handle(params: LLSDArray, queryMap: LLSDMap, grid: String, web: LLMediaCtrl?): Boolean {
        if (params.size() < 1) return false

        var floater: LLFloater? = null

        if (params[0].asString() == "destinations") {
            LLFloaterReg.toggleInstanceOrBringToFront("destinations")
            return true
        }

        if (params[0].asString() == "self") {
            if (web != null) {
                floater = getParentFloater(web)
            }
        }

        if (params[1].asString() == "close") {
            if (floater != null) {
                floater.closeFloater()
                return true
            }
        }

        return false
    }

    private fun getParentFloater(view: LLView): LLFloater? {
        var parent = view.getParent()
        while (parent != null) {
            val floater = parent as? LLFloater
            if (floater != null) return floater
            parent = parent.getParent()
        }
        return null
    }
}

val gFloaterHandler = LLFloaterHandler()
