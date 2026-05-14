package com.firestorm.newview

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

private const val ADD_LIMIT: UInt = 50u
private const val COLLAPSED_BY_USER = "collapsed_by_user"
private const val SECONDS_IN_DAY = 24 * 60 * 60L

data class TeleportHistoryItem(
    val title: String,
    val date: Instant,
    val globalPos: Triple<Double, Double, Double>
)

class TeleportHistoryFlatItem(
    var index: Int,
    private val menu: Any?,
    var regionName: String,
    var date: Instant,
    var localPos: Triple<Float, Float, Float>,
    var highlight: String
) {
    fun setIndex(i: Int) { index = i }
    fun setRegionName(name: String) { regionName = name }
    fun setDate(d: Instant) { date = d }
    fun setLocalPos(pos: Triple<Float, Float, Float>) { localPos = pos }
    fun setHighlightedText(text: String) { highlight = text }

    fun getTimestamp(use24h: Boolean, timezone: String): String {
        val zoneId = when (timezone) {
            "local" -> ZoneId.systemDefault()
            "utc" -> ZoneId.of("UTC")
            else -> ZoneId.of("America/Los_Angeles")
        }
        val zdt = ZonedDateTime.ofInstant(date, zoneId)
        return if (use24h) {
            "%02d:%02d".format(zdt.hour, zdt.minute)
        } else {
            val ampm = if (zdt.hour < 12) "AM" else "PM"
            val hour12 = when (val h = zdt.hour % 12) { 0 -> 12; else -> h }
            "%d:%02d %s".format(hour12, zdt.minute, ampm)
        }
    }

    fun updateTitle() {
        System.err.println("GPU: render regionName with highlight text in title TextBox and localPos in position TextBox")
    }

    fun updateTimestamp() {
        System.err.println("GPU: render getTimestamp result in mTimeTextBox with highlight")
    }

    fun postBuild(): Boolean {
        updateTitle()
        updateTimestamp()
        return true
    }

    fun onMouseEnter(x: Int, y: Int) {
        System.err.println("GPU: show hovered_icon and profile button")
    }

    fun onMouseLeave(x: Int, y: Int) {
        System.err.println("GPU: hide hovered_icon and profile button")
    }

    fun handleRightMouseDown(x: Int, y: Int): Boolean {
        showMenu(x, y)
        return true
    }

    private fun showMenu(x: Int, y: Int) {
        System.err.println("GPU: display context menu at ($x, $y) anchored to this item")
    }

    companion object {
        fun showPlaceInfoPanel(index: Int) {
            System.err.println("APR: use JVM equivalent - show FSFloaterPlaceDetails for teleport_history item at $index")
        }
    }
}

object TeleportHistoryFlatItemStorage {
    private val items: MutableList<TeleportHistoryFlatItem> = mutableListOf()

    fun getFlatItemForPersistentItem(
        menu: Any?,
        persistentItem: TeleportHistoryItem,
        curItemIndex: Int,
        highlight: String
    ): TeleportHistoryFlatItem {
        val regionWidth = 256.0
        val localPos = Triple(
            (persistentItem.globalPos.first % regionWidth).toFloat(),
            (persistentItem.globalPos.second % regionWidth).toFloat(),
            persistentItem.globalPos.third.toFloat()
        )

        val existing = items.getOrNull(curItemIndex)
        if (existing != null) {
            existing.setIndex(curItemIndex)
            existing.setRegionName(persistentItem.title)
            existing.setDate(persistentItem.date)
            existing.setLocalPos(localPos)
            existing.setHighlightedText(highlight)
            existing.updateTitle()
            existing.updateTimestamp()
            return existing
        }

        val newItem = TeleportHistoryFlatItem(
            index = curItemIndex,
            menu = menu,
            regionName = persistentItem.title,
            date = persistentItem.date,
            localPos = localPos,
            highlight = highlight
        )
        items.add(newItem)
        return newItem
    }

    fun removeItem(item: TeleportHistoryFlatItem) {
        items.remove(item)
    }

    fun purge() {
        items.clear()
    }
}

class TeleportHistoryPanel {

    private var dirty: Boolean = true
    private var currentItem: Int = 0
    private var lastSelectedItemIndex: Int = -1
    private var isStandAlone: Boolean = false
    var filterString: String = ""

    private var teleportHistory: List<TeleportHistoryItem> = emptyList()
    private val itemContainers: MutableList<AccordionTab> = mutableListOf()
    private var lastSelectedFlatList: FlatListView? = null

    private var teleportHistoryChangedListeners: MutableList<(Int) -> Unit> = mutableListOf()

    private var gearItemMenu: Any? = null
    private var sortingMenu: Any? = null
    private var accordionTabMenu: Any? = null

    fun postBuild(): Boolean {
        gearItemMenu = null.also { System.err.println("GPU: create menu_teleport_history_item.xml menu") }
        sortingMenu = null.also { System.err.println("GPU: create menu_teleport_history_gear.xml menu") }

        teleportHistory = TeleportHistoryStorage.getItems()
        teleportHistoryChangedListeners.add { removedIndex -> onTeleportHistoryChange(removedIndex) }
        TeleportHistoryStorage.addHistoryChangedCallback { removedIndex -> onTeleportHistoryChange(removedIndex) }

        System.err.println("GPU: find history_accordion, iterate accordion tabs, set up right-click, expand callbacks, add to itemContainers")
        System.err.println("GPU: open first 2 tabs by default")
        return true
    }

    fun draw() {
        if (dirty) refresh()
        System.err.println("GPU: call super draw")
    }

    fun onSearchEdit(string: String) {
        if (isStandAlone) filterString = string
        else System.err.println("APR: use JVM equivalent - set sFilterSubString = $string")
        showTeleportHistory()
    }

    fun isSingleItemSelected(): Boolean =
        lastSelectedFlatList?.getSelectedItem() != null

    fun onShowOnMap() {
        val item = lastSelectedFlatList?.getSelectedItem() as? TeleportHistoryFlatItem ?: return
        val pos = teleportHistory[item.index].globalPos
        if (pos.first != 0.0 || pos.second != 0.0 || pos.third != 0.0) {
            System.err.println("GPU: track location on world map and show map floater")
        }
    }

    fun onShowProfile() {
        val item = lastSelectedFlatList?.getSelectedItem() as? TeleportHistoryFlatItem ?: return
        TeleportHistoryFlatItem.showPlaceInfoPanel(item.index)
    }

    fun onTeleport() {
        val item = lastSelectedFlatList?.getSelectedItem() as? TeleportHistoryFlatItem ?: return
        confirmTeleport(item.index)
    }

    fun onRemoveSelected() {
        System.err.println("APR: use JVM equivalent - show ConfirmClearTeleportHistory dialog, call onClearTeleportHistoryDialog on response")
    }

    fun updateVerbs() {
        System.err.println("GPU: enable/disable teleport/map/profile buttons based on selection and RLVa restrictions")
    }

    fun getSelectionMenu(): Any? = gearItemMenu
    fun getSortingMenu(): Any? = sortingMenu
    fun getCreateMenu(): Any? = null

    fun setIsStandAlone(standalone: Boolean) { isStandAlone = standalone }

    fun handleDragAndDropToTrash(drop: Boolean, cargoType: Any?, cargoData: Any?, accept: Any?): Boolean = false

    private fun getNextTab(itemDate: Instant, tabIdx: IntArray, tabDate: LongArray) {
        val timezoneSetting = "".also { System.err.println("APR: use JVM equivalent - read FSTPHistoryTZ setting") }
        val tabsCount = itemContainers.size

        val nowEpoch = Instant.now().epochSecond
        val adjustedNow = when (timezoneSetting) {
            "local" -> nowEpoch - getLocalTimeOffset()
            "utc" -> nowEpoch
            else -> nowEpoch - getPacificTimeOffset()
        }

        val adjustedZdt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(adjustedNow), ZoneId.of("UTC"))
        var cutoff = ZonedDateTime.of(
            adjustedZdt.year, adjustedZdt.monthValue, adjustedZdt.dayOfMonth,
            0, 0, 0, 0, ZoneId.of("UTC")
        ).toEpochSecond() + SECONDS_IN_DAY

        val cutoffUtc = when (timezoneSetting) {
            "local" -> cutoff + getLocalTimeOffset()
            "utc" -> cutoff
            else -> cutoff + getPacificTimeOffset()
        }

        tabIdx[0] = -1
        var boundary = cutoffUtc

        while (tabIdx[0] < tabsCount - 1 && itemDate.epochSecond < boundary) {
            tabIdx[0]++
            boundary = when {
                tabIdx[0] <= tabsCount - 4 -> boundary - SECONDS_IN_DAY
                tabIdx[0] == tabsCount - 3 -> {
                    val now = ZonedDateTime.now(ZoneId.of("UTC"))
                    var m = now.monthValue - 1
                    var y = now.year
                    if (m == 0) { m = 12; y-- }
                    ZonedDateTime.of(y, m, now.dayOfMonth, 0, 0, 0, 0, ZoneId.of("UTC")).toEpochSecond()
                }
                tabIdx[0] == tabsCount - 2 -> {
                    val now = ZonedDateTime.now(ZoneId.of("UTC"))
                    var m = now.monthValue - 6
                    var y = now.year
                    if (m <= 0) { m += 12; y-- }
                    ZonedDateTime.of(y, m, now.dayOfMonth, 0, 0, 0, 0, ZoneId.of("UTC")).toEpochSecond()
                }
                else -> 0L
            }
        }
        tabDate[0] = boundary
    }

    private fun refresh() {
        val items = teleportHistory
        var tabBoundary = Instant.now().epochSecond
        var currFlatView: FlatListView? = null
        val filter = if (isStandAlone) filterString else "".also { System.err.println("APR: use JVM equivalent - sFilterSubString") }
        val filterUpper = filter.uppercase()

        var addedItems = 0u

        while (currentItem >= 0) {
            if (filterUpper.isNotEmpty()) {
                val title = items[currentItem].title.uppercase()
                if (!title.contains(filterUpper)) {
                    currentItem--
                    continue
                }
            }

            val itemEpoch = items[currentItem].date.epochSecond
            if (itemEpoch < tabBoundary) {
                val tabIdxArr = intArrayOf(0)
                val tabDateArr = longArrayOf(0L)
                getNextTab(items[currentItem].date, tabIdxArr, tabDateArr)
                tabBoundary = tabDateArr[0]
                val tabIdx = itemContainers.size - 1 - tabIdxArr[0]
                if (tabIdx >= 0) {
                    val tab = itemContainers[tabIdx]
                    tab.setVisible(true)
                    if (filterUpper.isNotEmpty()) {
                        tab.setDisplayChildren(true)
                    } else {
                        tab.setDisplayChildren(!isAccordionCollapsedByUser(tab))
                    }
                    currFlatView = getFlatListViewFromTab(tab)
                }
            }

            val flatItem = TeleportHistoryFlatItemStorage.getFlatItemForPersistentItem(
                gearItemMenu, items[currentItem], currentItem, filterUpper
            )
            currFlatView?.addItem(flatItem)
            if (lastSelectedItemIndex == currentItem) currFlatView?.selectItem(flatItem)

            currentItem--
            if (++addedItems >= ADD_LIMIT) break
        }

        System.err.println("GPU: notify all flat list views to rearrange; set accordion filter; call accordion.arrange()")
        updateVerbs()
        if (currentItem < 0) dirty = false
    }

    private fun onTeleportHistoryChange(removedIndex: Int) {
        lastSelectedItemIndex = -1
        if (removedIndex == -1) showTeleportHistory()
        else {
            replaceItem(removedIndex)
            updateVerbs()
        }
    }

    private fun replaceItem(removedIndex: Int) {
        val fv = if (itemContainers.isNotEmpty())
            getFlatListViewFromTab(itemContainers.last())
        else null

        if (fv == null || fv.size() == 0) {
            showTeleportHistory()
            return
        }

        val historyItems = teleportHistory
        val newest = TeleportHistoryFlatItemStorage.getFlatItemForPersistentItem(
            gearItemMenu,
            historyItems.last(),
            historyItems.size,
            if (isStandAlone) filterString else "".also { System.err.println("APR: use JVM equivalent - sFilterSubString") }
        )
        fv.addItemAtTop(newest)

        for (tabIdx in itemContainers.indices.reversed()) {
            val tab = itemContainers[tabIdx]
            if (!tab.isVisible()) continue
            val flatView = getFlatListViewFromTab(tab) ?: run {
                showTeleportHistory()
                return
            }
            val panelItems = flatView.getItems()
            for (panelItem in panelItems) {
                val fi = panelItem as? TeleportHistoryFlatItem ?: continue
                if (fi.index == removedIndex) {
                    TeleportHistoryFlatItemStorage.removeItem(fi)
                    flatView.removeItem(fi)
                    if (flatView.size() == 0) tab.setVisible(false)
                    System.err.println("GPU: accordion.arrange()")
                    return
                }
                fi.setIndex(fi.index - 1)
            }
        }
    }

    private fun showTeleportHistory() {
        dirty = true
        teleportHistory = TeleportHistoryStorage.getItems()
        currentItem = teleportHistory.size - 1
        for (tab in itemContainers.reversed()) {
            tab.setVisible(false)
            getFlatListViewFromTab(tab)?.detachItems()
        }
    }

    private fun handleItemSelect(selected: FlatListView) {
        lastSelectedFlatList = selected
        val item = selected.getSelectedItem() as? TeleportHistoryFlatItem
        if (item != null) lastSelectedItemIndex = item.index

        for (tab in itemContainers) {
            if (!tab.isVisible()) continue
            val flv = getFlatListViewFromTab(tab) ?: continue
            if (flv != selected) flv.resetSelection()
        }
        updateVerbs()
    }

    private fun onReturnKeyPressed() { onTeleport() }
    private fun onDoubleClickItem() { onTeleport() }

    private fun onAccordionTabRightClick(view: AccordionTab, x: Int, y: Int) {
        System.err.println("GPU: show context menu for accordion tab at ($x, $y) with open/close actions")
    }

    private fun onAccordionTabOpen(tab: AccordionTab) {
        tab.setDisplayChildren(true)
        System.err.println("GPU: accordion.arrange()")
    }

    private fun onAccordionTabClose(tab: AccordionTab) {
        tab.setDisplayChildren(false)
        System.err.println("GPU: accordion.arrange()")
    }

    private fun onTimeZoneChecked(userdata: String) {
        System.err.println("APR: use JVM equivalent - save FSTPHistoryTZ setting = $userdata")
        onTeleportHistoryChange(-1)
    }

    private fun isTimeZoneChecked(userdata: String): Boolean {
        System.err.println("APR: use JVM equivalent - read FSTPHistoryTZ setting and compare to $userdata")
        return false
    }

    private fun onClearTeleportHistory() {
        System.err.println("APR: use JVM equivalent - purge LLTeleportHistory and LLTeleportHistoryStorage, save")
    }

    private fun onClearTeleportHistoryDialog(option: Int): Boolean {
        if (option == 0) onClearTeleportHistory()
        return false
    }

    private fun onGearMenuAction(command: String) {
        when (command) {
            "expand_all" -> {
                for (tab in itemContainers) tab.setDisplayChildren(true)
                System.err.println("GPU: accordion.arrange()")
            }
            "collapse_all" -> {
                for (tab in itemContainers) tab.setDisplayChildren(false)
                System.err.println("GPU: accordion.arrange()")
                lastSelectedFlatList?.resetSelection()
            }
        }

        val item = lastSelectedFlatList?.getSelectedItem() as? TeleportHistoryFlatItem
        val index = item?.index ?: -1

        when (command) {
            "teleport" -> confirmTeleport(index)
            "view" -> TeleportHistoryFlatItem.showPlaceInfoPanel(index)
            "show_on_map" -> System.err.println("APR: use JVM equivalent - TeleportHistoryStorage.showItemOnMap($index)")
            "copy_slurl" -> System.err.println("APR: use JVM equivalent - get SLURL for global pos at $index, copy to clipboard")
            "remove" -> {
                System.err.println("APR: use JVM equivalent - TeleportHistoryStorage.removeItem($index) and save")
                showTeleportHistory()
            }
            "clear_history" -> onRemoveSelected()
        }
    }

    private fun isActionEnabled(command: String): Boolean {
        return when (command) {
            "collapse_all" -> itemContainers.any { it.isVisible() && it.isExpanded() }
            "expand_all" -> itemContainers.any { it.isVisible() && !it.isExpanded() }
            "clear_history" -> teleportHistory.isNotEmpty()
            "teleport" -> {
                val item = lastSelectedFlatList?.getSelectedItem() as? TeleportHistoryFlatItem
                item != null && false.also { System.err.println("APR: use JVM equivalent - RlvActions.canTeleportToLocation()") }
            }
            "show_on_map" -> {
                val item = lastSelectedFlatList?.getSelectedItem() as? TeleportHistoryFlatItem
                item != null && false.also { System.err.println("APR: use JVM equivalent - !gRlvHandler.hasBehaviour(SHOWWORLDMAP)") }
            }
            "view", "copy_slurl", "remove" ->
                lastSelectedFlatList?.getSelectedItem() as? TeleportHistoryFlatItem != null
            else -> false
        }
    }

    private fun setAccordionCollapsedByUser(tab: AccordionTab, collapsed: Boolean) {
        tab.setUserData(COLLAPSED_BY_USER, collapsed)
    }

    private fun isAccordionCollapsedByUser(tab: AccordionTab): Boolean =
        tab.getUserData(COLLAPSED_BY_USER) as? Boolean ?: false

    private fun onAccordionExpand(ctrl: AccordionTab, expanded: Boolean) {
        setAccordionCollapsedByUser(ctrl, !expanded)
        if (!expanded) lastSelectedFlatList?.resetSelection()
    }

    private fun getFlatListViewFromTab(tab: AccordionTab): FlatListView? =
        tab.getFirstChildOfType<FlatListView>()

    private fun getLocalTimeOffset(): Long {
        System.err.println("APR: use JVM equivalent - java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis())/1000L")
        return 0L
    }

    private fun getPacificTimeOffset(): Long {
        System.err.println("APR: use JVM equivalent - TimeZone.getTimeZone('America/Los_Angeles').getOffset(System.currentTimeMillis())/1000L")
        return 0L
    }

    private fun gotSLURLCallback(slurl: String) {
        if (slurl.isEmpty()) {
            System.err.println("APR: use JVM equivalent - show LandmarkLocationUnknown notification")
            return
        }
        System.err.println("APR: use JVM equivalent - copy $slurl to system clipboard and show CopySLURL notification")
    }

    companion object {
        fun confirmTeleport(histIdx: Int) {
            val entry = TeleportHistoryStorage.getItems().getOrNull(histIdx)?.title ?: return
            System.err.println("APR: use JVM equivalent - show TeleportToHistoryEntry dialog with entry='$entry', call onTeleportConfirmation on response")
        }

        fun onTeleportConfirmation(option: Int, histIdx: Int): Boolean {
            if (option == 0) TeleportHistoryStorage.goToItem(histIdx)
            return false
        }
    }
}

interface AccordionTab {
    fun setVisible(visible: Boolean)
    fun isVisible(): Boolean
    fun setDisplayChildren(expanded: Boolean)
    fun isExpanded(): Boolean
    fun setUserData(key: String, value: Any)
    fun getUserData(key: String): Any?
    fun <T> getFirstChildOfType(): T?
}

interface FlatListView {
    fun addItem(item: TeleportHistoryFlatItem)
    fun addItemAtTop(item: TeleportHistoryFlatItem)
    fun removeItem(item: TeleportHistoryFlatItem)
    fun getSelectedItem(): Any?
    fun selectItem(item: TeleportHistoryFlatItem)
    fun resetSelection(keepSelection: Boolean = false)
    fun size(): Int
    fun getItems(): List<Any>
    fun detachItems()
}

object TeleportHistoryStorage {
    private val items: MutableList<TeleportHistoryItem> = mutableListOf()
    private val listeners: MutableList<(Int) -> Unit> = mutableListOf()

    fun getItems(): List<TeleportHistoryItem> = items
    fun addHistoryChangedCallback(cb: (Int) -> Unit) { listeners.add(cb) }
    fun removeItem(index: Int) { if (index in items.indices) items.removeAt(index) }
    fun goToItem(index: Int) { System.err.println("APR: use JVM equivalent - teleport to items[index].globalPos") }
    fun showItemOnMap(index: Int) { System.err.println("GPU: show items[index].globalPos on world map") }
    fun purgeItems() { items.clear() }
    fun save() { System.err.println("APR: use JVM equivalent - persist teleport history to disk") }
}
