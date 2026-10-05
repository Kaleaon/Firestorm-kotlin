package com.firestorm.llui

typealias WidgetCreatorFunc = (node: XmlNode, parent: View?, outputNode: XmlNode?) -> View?

object UICtrlFactory {

    private val fileNameStack: MutableList<String> = mutableListOf()
    private val widgetRegistry: MutableMap<String, WidgetCreatorFunc> = mutableMapOf()
    private val widgetNameRegistry: MutableMap<String, String> = mutableMapOf()
    private val paramDefaults: MutableMap<String, Any> = mutableMapOf()

    private var dummyPanel: Panel? = null

    fun getCurFileName(): String = if (fileNameStack.isEmpty()) "" else fileNameStack.last()

    fun pushFileName(name: String) {
        fileNameStack.add(resolveSkinnedFileBaseLang(name))
    }

    fun popFileName() {
        if (fileNameStack.isNotEmpty()) fileNameStack.removeAt(fileNameStack.lastIndex)
    }

    fun saveToXML(view: View, filename: String): Int = 0

    fun createFromXML(
        node: XmlNode,
        parent: View?,
        filename: String,
        registry: Map<String, WidgetCreatorFunc>,
        outputNode: XmlNode?
    ): View? {
        val ctrlType = node.name.lowercase()
        val creator = registry[ctrlType] ?: return null

        val effectiveParent = parent ?: run {
            if (dummyPanel == null) dummyPanel = Panel()
            dummyPanel!!
        }

        return creator(node, effectiveParent, outputNode)
    }

    fun createChildren(
        viewp: View,
        node: XmlNode?,
        registry: Map<String, WidgetCreatorFunc>,
        outputNode: XmlNode? = null
    ) {
        node ?: return
        for (childNode in node.children) {
            val outputChild = outputNode?.createChild()
            val created = createFromXML(childNode, viewp, "", registry, outputChild)
            if (created == null) {
                val childName = childNode.name
                if (widgetRegistry.containsKey(childName)) {
                    System.err.println("$childName is not a valid child of ${node.name}")
                } else {
                    System.err.println("Could not create widget named ${childNode.name}")
                }
            }
        }
    }

    fun getLayeredXMLNode(filename: String, constraint: SkinConstraint = SkinConstraint.CURRENT_SKIN): XmlNode? {
        return null
    }

    inline fun <reified T : View> createFromFile(
        filename: String,
        parent: View?,
        registry: Map<String, WidgetCreatorFunc>
    ): T? {
        pushFileName(filename)
        return try {
            val root = getLayeredXMLNode(filename) ?: run {
                System.err.println("Couldn't parse XUI from: $filename")
                return null
            }
            val view = createFromXML(root, parent, filename, registry, null)
            when (val w = view as? T) {
                null -> {
                    System.err.println("Widget in $filename was not expected type ${T::class.simpleName}")
                    null
                }
                else -> w
            }
        } finally {
            popFileName()
        }
    }

    fun registerWidget(widgetType: String, paramBlockType: String, tag: String) {
        val existing = widgetNameRegistry[paramBlockType]
        if (existing != null) {
            if (existing != tag) {
                error("Duplicate entry for Params type $paramBlockType: existing='$existing' vs new='$tag'")
            }
            return
        }
        widgetNameRegistry[paramBlockType] = tag
    }

    fun loadWidgetTemplate(widgetTag: String): XmlNode? {
        val filename = "widgets/$widgetTag.xml"
        return null
    }

    fun setCtrlParent(view: View, parent: View, tabGroup: Int) {
        val effectiveGroup = if (tabGroup == Int.MAX_VALUE) parent.getLastTabGroup() else tabGroup
        parent.addChild(view, effectiveGroup)
    }

    private fun resolveSkinnedFileBaseLang(name: String): String {
        return ""
    }

    enum class SkinConstraint { CURRENT_SKIN, ALL_SKINS }
}

// --- Locate/padding widget (internal to factory) ---
class UICtrlLocate(params: UICtrl.Params = UICtrl.Params()) : UICtrl(params) {
    init { params.tabStop = false }
    override fun draw() {}
}
