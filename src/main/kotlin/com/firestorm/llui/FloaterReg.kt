package com.firestorm.llui

object FloaterReg {
    data class BuildData(
        val buildFunc: (Any?) -> Floater,
        val file: String,
        val fileFunc: (() -> String)? = null
    )

    private val nullInstanceList: MutableList<Floater> = mutableListOf()
    private val instanceMap: MutableMap<String, MutableList<Floater>> = mutableMapOf()
    private val buildMap: MutableMap<String, BuildData> = mutableMapOf()
    private val groupMap: MutableMap<String, String> = mutableMapOf()
    private var blockShowFloaters: Boolean = false
    private val alwaysShowableList: MutableSet<String> = mutableSetOf()

    val validateCallbacks: MutableList<(String, Any?) -> Boolean> = mutableListOf()

    fun add(name: String, file: String, buildFunc: (Any?) -> Floater, groupname: String = "") {
        buildMap[name] = BuildData(buildFunc, file)
        val group = if (groupname.isEmpty()) name else groupname
        groupMap[name] = group
        groupMap[group] = group
    }

    fun isRegistered(name: String): Boolean = buildMap.containsKey(name)

    fun getLastFloaterInGroup(name: String): Floater? {
        val groupname = groupMap[name] ?: return null
        if (groupname.isEmpty()) return null
        val list = instanceMap[groupname] ?: return null
        return list.lastOrNull { it.visible && !it.isMinimized }
    }

    fun getAllFloatersInGroup(floater: Floater): List<Floater> {
        for ((_, groupName) in groupMap) {
            if (groupName.isEmpty()) continue
            val instances = instanceMap[groupName] ?: continue
            if (floater in instances) return instances.toList()
        }
        return emptyList()
    }

    fun getLastFloaterCascading(): Floater? {
        var candidateTop = Int.MAX_VALUE
        var candidateFloater: Floater? = null
        for ((_, groupName) in groupMap) {
            val instances = instanceMap[groupName] ?: continue
            for (inst in instances) {
                if (inst.visible && inst.isCascading()) {
                    val top = inst.rectTop()
                    if (top < candidateTop) {
                        candidateTop = top
                        candidateFloater = inst
                    }
                }
            }
        }
        return candidateFloater
    }

    fun findInstance(name: String, key: Any? = null): Floater? {
        val groupname = groupMap[name] ?: return null
        if (groupname.isEmpty()) return null
        val list = instanceMap[groupname] ?: return null
        return list.firstOrNull { it.matchesKey(key) }
    }

    fun getInstance(name: String, key: Any? = null): Floater? {
        var res = findInstance(name, key)
        if (res == null) {
            val buildData = buildMap[name] ?: run {
                println("Floater type: '$name' not registered.")
                return null
            }
            val groupname = groupMap[name]
            if (!groupname.isNullOrEmpty()) {
                val list = instanceMap.getOrPut(groupname) { mutableListOf() }
                val built = buildData.buildFunc(key) ?: run {
                    println("Failed to build floater type: '$name'.")
                    return null
                }
                val built2 = built.also { f ->
                    if (!f.buildFromFile(buildData.file)) {
                        println("Failed to build floater type: '$name'.")
                        return null
                    }
                    if (f.key == null) f.key = key
                    f.instanceName = name
                    val lastFloater = list.lastOrNull()
                    f.applyControlsAndPosition(lastFloater)
                    list.add(f)
                }
                res = built2
                TODO("APR: adjustToFitScreen for newly created floater '$name'")
            }
        }
        if (res == null) println("Floater type: '$name' not registered.")
        return res
    }

    fun removeInstance(name: String, key: Any? = null): Floater? {
        val groupname = groupMap[name] ?: return null
        if (groupname.isEmpty()) return null
        val list = instanceMap[groupname] ?: return null
        val iter = list.iterator()
        while (iter.hasNext()) {
            val inst = iter.next()
            if (inst.matchesKey(key)) {
                iter.remove()
                return inst
            }
        }
        return null
    }

    fun destroyInstance(name: String, key: Any? = null): Boolean {
        val inst = removeInstance(name, key) ?: return false
        inst.destroy()
        return true
    }

    fun getFloaterList(name: String): List<Floater> =
        instanceMap[name] ?: nullInstanceList

    fun canShowInstance(name: String, key: Any? = null): Boolean =
        validateCallbacks.all { it(name, key) }

    fun showInstance(name: String, key: Any? = null, focus: Boolean = false): Floater? {
        val blocked = blockShowFloaters && name !in alwaysShowableList
        if (blocked || !canShowInstance(name, key)) return null
        val instance = getInstance(name, key) ?: return null
        instance.openFloater(key)
        if (focus) instance.setFocus(true)
        return instance
    }

    fun hideInstance(name: String, key: Any? = null): Boolean {
        val instance = findInstance(name, key) ?: return false
        instance.closeHostedFloater()
        return true
    }

    fun toggleInstance(name: String, key: Any? = null): Boolean {
        val instance = findInstance(name, key)
        if (instance != null && instance.isShown()) {
            instance.closeHostedFloater()
            return false
        }
        return showInstance(name, key, true) != null
    }

    fun instanceVisible(name: String, key: Any? = null): Boolean {
        val instance = findInstance(name, key) ?: return false
        return instance.isVisible()
    }

    fun showInitialVisibleInstances() {
        for ((name, _) in buildMap) {
            val controlName = getVisibilityControlName(name)
            if (floaterControlExists(controlName)) {
                val isVis = getFloaterControlBool(controlName)
                if (isVis) {
                    val floater = showInstance(name, null)
                    floater?.updateTransparency()
                }
            }
        }
    }

    fun hideVisibleInstances(exceptions: Set<String> = emptySet()) {
        for ((name, list) in instanceMap) {
            if (name in exceptions) continue
            list.forEach { it.pushVisible(false) }
        }
    }

    fun restoreVisibleInstances() {
        for ((_, list) in instanceMap) {
            list.forEach { it.popVisible() }
        }
    }

    fun getRectControlName(name: String): String = "floater_rect_${getBaseControlName(name)}"

    fun declareRectControl(name: String): String {
        val controlName = getRectControlName(name)
        TODO("APR: declare rect control variable '$controlName' for floater '$name'")
    }

    fun declarePosXControl(name: String): String {
        val controlName = "floater_pos_${getBaseControlName(name)}_x"
        TODO("APR: declare posX control variable '$controlName' for floater '$name'")
    }

    fun declarePosYControl(name: String): String {
        val controlName = "floater_pos_${getBaseControlName(name)}_y"
        TODO("APR: declare posY control variable '$controlName' for floater '$name'")
    }

    fun getVisibilityControlName(name: String): String = "floater_vis_${getBaseControlName(name)}"

    fun declareVisibilityControl(name: String): String {
        val controlName = getVisibilityControlName(name)
        TODO("APR: declare visibility control variable '$controlName' for floater '$name'")
    }

    fun getBaseControlName(name: String): String = name.replace(' ', '_')

    fun declareDockStateControl(name: String): String {
        val controlName = getDockStateControlName(name)
        TODO("APR: declare dock-state control variable '$controlName' for floater '$name'")
    }

    fun getDockStateControlName(name: String): String =
        "floater_dock_${name.replace(' ', '_')}"

    fun registerControlVariables() {
        for ((name, _) in buildMap) {
            if (!floaterControlExists(getRectControlName(name))) {
                declareRectControl(name)
            }
            if (!floaterControlExists(getVisibilityControlName(name))) {
                declareVisibilityControl(name)
            }
        }
        TODO("APR: populate alwaysShowableList from settings config")
    }

    fun toggleInstanceOrBringToFront(sdname: Any, key: Any? = null) {
        val name = sdname.toString()
        val instance = getInstance(name, key) ?: run {
            println("Unable to get instance of floater '$name'")
            return
        }
        val host = instance.getHost()
        if (host != null) {
            if (host.isMinimized || !host.isShown() || (!host.hasFocus() || !host.isFrontmost())) {
                host.isMinimized = false
                instance.openFloater(key)
                instance.setVisibleAndFrontmost(true, key)
            } else if (!instance.visible) {
                instance.openFloater(key)
                instance.setVisibleAndFrontmost(true, key)
                instance.setFocus(true)
            } else {
                instance.closeHostedFloater()
            }
        } else {
            when {
                instance.isMinimized -> {
                    instance.isMinimized = false
                    instance.setVisibleAndFrontmost(true, key)
                }
                !instance.isShown() -> {
                    instance.openFloater(key)
                    instance.setVisibleAndFrontmost(true, key)
                }
                !instance.hasFocus() || !instance.isFrontmost() -> {
                    instance.setVisibleAndFrontmost(true, key)
                }
                else -> instance.closeHostedFloater()
            }
        }
    }

    fun showInstanceOrBringToFront(sdname: Any, key: Any? = null) {
        val name = sdname.toString()
        val instance = getInstance(name, key) ?: run {
            println("Unable to get instance of floater '$name'")
            return
        }
        val host = instance.getHost()
        if (host != null) {
            if (host.isMinimized || !host.isShown() || !host.isFrontmost()) {
                host.isMinimized = false
                instance.openFloater(key)
                instance.setVisibleAndFrontmost(true, key)
            } else if (!instance.visible) {
                instance.openFloater(key)
                instance.setVisibleAndFrontmost(true, key)
                instance.setFocus(true)
            }
        } else {
            when {
                instance.isMinimized -> {
                    instance.isMinimized = false
                    instance.setVisibleAndFrontmost(true, key)
                }
                !instance.isShown() -> {
                    instance.openFloater(key)
                    instance.setVisibleAndFrontmost(true, key)
                }
                !instance.isFrontmost() -> {
                    instance.setVisibleAndFrontmost(true, key)
                }
            }
        }
    }

    fun getVisibleFloaterInstanceCount(): UInt {
        var count = 0u
        for ((_, groupName) in groupMap) {
            val instances = instanceMap[groupName] ?: continue
            for (inst in instances) {
                if (inst.visible && !inst.isMinimized) count++
            }
        }
        return count
    }

    fun blockShowFloaters(value: Boolean) { blockShowFloaters = value }
}

private fun floaterControlExists(name: String): Boolean {
    TODO("APR: check if floater control variable '$name' exists")
}

private fun getFloaterControlBool(name: String): Boolean {
    TODO("APR: get boolean value of floater control variable '$name'")
}

private fun Floater.rectTop(): Int { TODO("APR: get floater rect top") }
private fun Floater.isCascading(): Boolean { TODO("APR: check if floater uses cascading positioning") }
private fun Floater.matchesKey(key: Any?): Boolean = this.key == key
private fun Floater.destroy() { TODO("APR: destroy floater") }
private fun Floater.buildFromFile(file: String): Boolean { TODO("APR: build floater from XUI file '$file'") }
private fun Floater.applyControlsAndPosition(lastFloater: Floater?) { TODO("APR: apply saved controls and position") }
private fun Floater.openFloater(key: Any?) { this.key = key; open_() }
private fun Floater.closeHostedFloater() { close() }
private fun Floater.isShown(): Boolean = visible && !isMinimized
private fun Floater.isVisible(): Boolean = visible
private fun Floater.setFocus(focus: Boolean) { TODO("APR: set keyboard focus on floater") }
private fun Floater.hasFocus(): Boolean { TODO("APR: check if floater has focus") }
private fun Floater.isFrontmost(): Boolean { TODO("APR: check if floater is frontmost") }
private fun Floater.setVisibleAndFrontmost(vis: Boolean, key: Any?) { TODO("APR: set floater visible and bring to front") }
private fun Floater.getHost(): Floater? { TODO("APR: get host multi-floater") }
private fun Floater.pushVisible(vis: Boolean) { TODO("APR: push visibility state") }
private fun Floater.popVisible() { TODO("APR: pop visibility state") }
private fun Floater.updateTransparency() { TODO("APR: update floater transparency") }

var Floater.key: Any?
    get() = TODO("APR: get floater key")
    set(value) { TODO("APR: set floater key to '$value'") }

var Floater.instanceName: String
    get() = name
    set(value) { TODO("APR: set floater instance name to '$value'") }
