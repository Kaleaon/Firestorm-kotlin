package com.firestorm.newview

import kotlin.math.abs

private const val TRACKER_FILE = "tracked_regions.json"
private const val REGION_UPDATE_TIMER = 60.0

// Stubs for SL-specific types not available on JVM
abstract class LLFloater(val key: Any)
abstract class LLEventTimer(val intervalSeconds: Float) {
    protected var timerRunning = false
    fun start() { timerRunning = true }
    fun stop() { timerRunning = false }
    fun getStarted() = timerRunning
    abstract fun tick(): Boolean
}

// LLSD is the Linden Scripting Data format; modelled as a flexible map/list/scalar container
typealias LLSD = MutableMap<String, Any?>

fun llsdMapOf(vararg pairs: Pair<String, Any?>): LLSD = mutableMapOf(*pairs)

// Stub UI widget types
open class LLButton {
    fun setEnabled(enabled: Boolean): Unit { System.err.println("LLButton: setEnabled not yet implemented") }
    fun setClickedCallback(cb: () -> Unit): Unit { System.err.println("LLButton: setClickedCallback not yet implemented") }
}

open class LLScrollListCtrl {
    fun setCommitOnSelectionChange(v: Boolean): Unit { System.err.println("LLScrollListCtrl: setCommitOnSelectionChange not yet implemented") }
    fun setCommitCallback(cb: () -> Unit): Unit { System.err.println("LLScrollListCtrl: setCommitCallback not yet implemented") }
    fun setDoubleClickCallback(cb: () -> Unit): Unit { System.err.println("LLScrollListCtrl: setDoubleClickCallback not yet implemented") }
    fun getNumSelected(): Int { System.err.println("LLScrollListCtrl: getNumSelected not yet implemented"); return 0 }
    fun getSelectedValue(): String { System.err.println("LLScrollListCtrl: getSelectedValue not yet implemented"); return "" }
    fun getScrollPos(): Int { System.err.println("LLScrollListCtrl: getScrollPos not yet implemented"); return 0 }
    fun deleteAllItems(): Unit { System.err.println("LLScrollListCtrl: deleteAllItems not yet implemented") }
    fun deleteSelectedItems(): Unit { System.err.println("LLScrollListCtrl: deleteSelectedItems not yet implemented") }
    fun getAllSelected(): List<LLScrollListItem> { System.err.println("LLScrollListCtrl: getAllSelected not yet implemented"); return emptyList() }
    fun addRow(row: ScrollListRowParams): Unit { System.err.println("LLScrollListCtrl: addRow not yet implemented") }
    fun selectByValue(value: String): Unit { System.err.println("LLScrollListCtrl: selectByValue not yet implemented") }
    fun setScrollPos(pos: Int): Unit { System.err.println("LLScrollListCtrl: setScrollPos not yet implemented") }
    fun getFirstSelected(): LLScrollListItem? { System.err.println("LLScrollListCtrl: getFirstSelected not yet implemented"); return null }
}

class LLScrollListItem {
    fun getValue(): String { System.err.println("LLScrollListItem: getValue not yet implemented"); return "" }
}

data class ScrollListRowParams(
    val value: String,
    val columns: MutableList<ScrollListCellParams> = mutableListOf()
)

data class ScrollListCellParams(
    val column: String,
    val type: String,
    val value: Any? = null,
    val color: Any? = null,
    val fontHalign: Any? = null
)

// Stub for world-state queries
object WorldMapStub {
    fun simInfoFromName(name: String): SimInfo? { System.err.println("WorldMapStub: simInfoFromName not yet implemented"); return null }
}

class SimInfo {
    fun getAccessIcon(): String = ""
    fun updateAgentCount(elapsedSeconds: Double) {
        System.err.println("SimInfo: updateAgentCount not yet implemented")
    }
    fun getAgentCount(): Int = 0
    fun isDown(): Boolean = false
}

object WorldMapMessageStub {
    fun sendNamedRegionRequest(name: String) {
        System.err.println("WorldMapMessageStub: sendNamedRegionRequest not yet implemented")
    }
}

object AgentStub {
    fun getRegion(): RegionStub? = null
    fun getPositionGlobal(): Any = Unit
}

class RegionStub {
    fun getName(): String = ""
}

object FloaterRegStub {
    fun showInstance(name: String, center: String) {
        System.err.println("FloaterRegStub: showInstance not yet implemented")
    }
    fun findWorldMap(): WorldMapFloaterStub? = null
}

class WorldMapFloaterStub {
    fun trackURL(region: String, x: Int, y: Int, z: Int) {
        System.err.println("WorldMapFloaterStub: trackURL not yet implemented")
    }
}

object DirUtilStub {
    fun getExpandedFilename(pathType: String, filename: String): String = ""
}

object LLTimer {
    fun getElapsedSeconds(): Double = System.currentTimeMillis() / 1000.0
}

object NotificationsUtil {
    fun getSelectedOption(notification: LLSD, response: LLSD): Int = 0
}

/**
 * Floater that tracks a user-curated list of regions, polling sim info and agent counts
 * on a 5-second tick timer, and persisting the list as JSON between sessions.
 *
 * C++ heritage: ALFloaterRegionTracker : LLFloater, LLEventTimer
 * friend LLFloaterReg controls construction — mirrored here by making the primary
 * constructor internal and exposing a factory on the companion object.
 */
class ALFloaterRegionTracker internal constructor(key: LLSD) :
    LLFloater(key), LLEventTimer(5f) {

    private val regionMap: LLSD = mutableMapOf()
    private val refreshRegionListBtn = LLButton()
    private val removeRegionBtn = LLButton()
    private val openMapBtn = LLButton()
    private val regionScrollList = LLScrollListCtrl()
    private var lastRegionUpdate: Double = 0.0

    init {
        loadFromJSON()
    }

    fun postBuild(): Boolean {
        refreshRegionListBtn.setClickedCallback { refresh() }
        removeRegionBtn.setClickedCallback { removeRegions() }
        openMapBtn.setClickedCallback { openMap() }

        regionScrollList.setCommitOnSelectionChange(true)
        regionScrollList.setCommitCallback { updateHeader() }
        regionScrollList.setDoubleClickCallback { openMap() }

        updateHeader()
        return true
    }

    fun onOpen(key: LLSD) {
        requestRegionData()
        start()
        refresh()
    }

    fun onClose(appQuitting: Boolean) {
        stop()
    }

    override fun tick(): Boolean {
        refresh()
        return false
    }

    private fun updateHeader() {
        val numSelected = regionScrollList.getNumSelected()
        refreshRegionListBtn.setEnabled(regionMap.isNotEmpty())
        removeRegionBtn.setEnabled(numSelected > 0)
        openMapBtn.setEnabled(numSelected <= 1)
    }

    fun refresh() {
        if (regionMap.isEmpty()) {
            updateHeader()
            return
        }

        val savedSelectedValue = regionScrollList.getSelectedValue()
        val savedScrollPos = regionScrollList.getScrollPos()
        regionScrollList.deleteAllItems()

        val curRegionName: String = AgentStub.getRegion()?.getName() ?: ""

        val timeNow = LLTimer.getElapsedSeconds()
        val requestRegionUpdate = (timeNow - lastRegionUpdate > REGION_UPDATE_TIMER)
        if (requestRegionUpdate) {
            lastRegionUpdate = timeNow
        }

        for ((simName, data) in regionMap) {
            @Suppress("UNCHECKED_CAST")
            val dataMap = data as? MutableMap<String, Any?> ?: continue

            val labelCell = ScrollListCellParams(column = "region_label", type = "text",
                value = dataMap["label"]?.toString() ?: "")
            var maturityCell = ScrollListCellParams(column = "region_maturity_icon", type = "icon",
                fontHalign = "hcenter")
            var regionCell = ScrollListCellParams(column = "region_name", type = "text", value = simName)
            var countCell = ScrollListCellParams(column = "region_agent_count", type = "text", value = "...")

            val info = WorldMapStub.simInfoFromName(simName)
            val row: ScrollListRowParams
            if (info != null) {
                val accessIcon = info.getAccessIcon()
                info.updateAgentCount(LLTimer.getElapsedSeconds())
                val agentCount = info.getAgentCount()
                if (info.isDown()) {
                    maturityCell = maturityCell.copy(value = accessIcon, color = "red")
                    regionCell = regionCell.copy(color = "red")
                    countCell = countCell.copy(color = "red", value = 0)
                } else {
                    maturityCell = maturityCell.copy(value = accessIcon)
                    countCell = countCell.copy(
                        value = if (simName == curRegionName) agentCount + 1 else agentCount
                    )
                }
                if (requestRegionUpdate) {
                    WorldMapMessageStub.sendNamedRegionRequest(simName)
                }
                row = ScrollListRowParams(value = simName,
                    columns = mutableListOf(labelCell, maturityCell, regionCell, countCell))
            } else {
                val grey = "grey"
                row = ScrollListRowParams(value = simName, columns = mutableListOf(
                    labelCell.copy(color = grey),
                    maturityCell.copy(color = grey),
                    regionCell.copy(color = grey),
                    countCell.copy(color = grey)
                ))
                WorldMapMessageStub.sendNamedRegionRequest(simName)
                if (!timerRunning) start()
            }
            regionScrollList.addRow(row)
        }

        if (savedSelectedValue.isNotEmpty()) {
            regionScrollList.selectByValue(savedSelectedValue)
        }
        regionScrollList.setScrollPos(savedScrollPos)
    }

    private fun requestRegionData() {
        if (regionMap.isEmpty()) return
        for ((name, _) in regionMap) {
            val info = WorldMapStub.simInfoFromName(name)
            if (info != null) {
                info.updateAgentCount(LLTimer.getElapsedSeconds())
            } else {
                WorldMapMessageStub.sendNamedRegionRequest(name)
            }
        }
        start()
    }

    private fun removeRegions() {
        for (item in regionScrollList.getAllSelected()) {
            regionMap.remove(item.getValue())
        }
        regionScrollList.deleteSelectedItems()
        saveToJSON()
        updateHeader()
    }

    private fun saveToJSON(): Boolean {
        val filename = DirUtilStub.getExpandedFilename("LL_PATH_PER_SL_ACCOUNT", TRACKER_FILE)
        return try {
            val json = buildString {
                append("{\n")
                regionMap.entries.forEachIndexed { idx, (k, v) ->
                    append("  \"$k\": $v${if (idx < regionMap.size - 1) "," else ""}\n")
                }
                append("}")
            }
            java.io.File(filename).writeText(json)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun loadFromJSON(): Boolean {
        val filename = DirUtilStub.getExpandedFilename("LL_PATH_PER_SL_ACCOUNT", TRACKER_FILE)
        return try {
            val file = java.io.File(filename)
            if (!file.exists()) return false
            // Full LLSD/JSON deserialisation requires a proper parser; stub here.
            System.err.println("ALFloaterRegionTracker: loadFromJSON not yet implemented")
            false
        } catch (_: Exception) {
            false
        }
    }

    fun getRegionLabelIfExists(name: String): String {
        @Suppress("UNCHECKED_CAST")
        val entry = regionMap[name] as? MutableMap<String, Any?> ?: return ""
        return entry["label"]?.toString() ?: ""
    }

    fun onRegionAddedCallback(notification: LLSD, response: LLSD) {
        val option = NotificationsUtil.getSelectedOption(notification, response)
        if (option != 0) return

        @Suppress("UNCHECKED_CAST")
        val payload = notification["payload"] as? MutableMap<String, Any?> ?: return
        val name = payload["name"]?.toString() ?: return
        var label = response["label"]?.toString()?.trim() ?: return
        if (name.isEmpty() || label.isEmpty()) return

        if (regionMap.containsKey(name)) {
            @Suppress("UNCHECKED_CAST")
            val existing = regionMap[name] as? MutableMap<String, Any?>
            existing?.set("label", label)
        } else {
            regionMap[name] = mutableMapOf("label" to label)
        }
        saveToJSON()
        refresh()
    }

    private fun openMap() {
        if (regionScrollList.getNumSelected() == 0) {
            FloaterRegStub.showInstance("world_map", "center")
        } else {
            val region = regionScrollList.getFirstSelected()?.getValue() ?: return
            val worldMap = FloaterRegStub.findWorldMap()
            if (region.isNotEmpty() && worldMap != null) {
                worldMap.trackURL(region, 128, 128, 0)
                FloaterRegStub.showInstance("world_map", "center")
            }
        }
    }

    companion object {
        fun create(key: LLSD): ALFloaterRegionTracker = ALFloaterRegionTracker(key)
    }
}
