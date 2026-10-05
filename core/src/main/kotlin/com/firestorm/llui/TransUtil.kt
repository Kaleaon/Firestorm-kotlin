package com.firestorm.llui

object TransUtil {

    fun parseStrings(xmlFilename: String, defaultArgs: Set<String>): Boolean {
        val root = UICtrlFactory.getLayeredXMLNode(xmlFilename, UICtrlFactory.SkinConstraint.ALL_SKINS)
            ?: run {
                val error = "Firestorm couldn't access some of the files it needs and will be closed." +
                    "\n\nPlease reinstall viewer from https://www.firestormviewer.org/download and " +
                    "contact https://www.firestormviewer.org/support if issue persists after reinstall."
                error(error)
            }
        return Trans.parseStrings(root, defaultArgs)
    }

    fun parseLanguageStrings(xmlFilename: String): Boolean {
        val root = UICtrlFactory.getLayeredXMLNode(xmlFilename)
            ?: error("Couldn't load localization table $xmlFilename")
        return Trans.parseLanguageStrings(root)
    }
}
