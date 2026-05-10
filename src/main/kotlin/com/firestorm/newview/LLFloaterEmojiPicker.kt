package com.firestorm.newview

import java.io.File

// ---------------------------------------------------------------------------
// Domain types
// ---------------------------------------------------------------------------

data class LLEmojiDescriptor(
    val character: Int,
    val shortCodes: List<String>
)

data class LLEmojiSearchResult(
    val character: Int,
    val string: String,
    val begin: Int,
    val end: Int
)

// ---------------------------------------------------------------------------
// Module-level state (replaces C++ anonymous-namespace statics)
// ---------------------------------------------------------------------------

private val recentlyUsed: ArrayDeque<Int> = ArrayDeque()
private val frequentlyUsed: ArrayDeque<Pair<Int, UInt>> = ArrayDeque()
private var stateFileName: String = ""

private const val ALL_EMOJIS_GROUP_INDEX = -2
private const val ALL_EMOJIS_IMAGE_INDEX = 0x1F50D
private const val USED_EMOJIS_GROUP_INDEX = -1
private const val USED_EMOJIS_IMAGE_INDEX = 0x23F2
private const val EMPTY_LIST_IMAGE_INDEX = 0x1F6D1
private const val FREQUENTLY_USED_CATEGORY = "frequently used"

private const val KEY_RECENTLY_USED = "RecentlyUsed"
private const val KEY_FREQUENTLY_USED = "FrequentlyUsed"
private const val MAX_STATE_ENTRIES = 20

// ---------------------------------------------------------------------------
// Inner grid widget types (stubs for GPU-rendered panels)
// ---------------------------------------------------------------------------

private class LLEmojiGridRow {
    val icons: MutableList<LLEmojiGridIcon> = mutableListOf()
    fun addIcon(icon: LLEmojiGridIcon) { icons.add(icon) }
}

private class LLEmojiGridIcon(val data: LLEmojiSearchResult) {
    var backgroundVisible: Boolean = false
    fun setMouseEnterCallback(cb: (LLEmojiGridIcon) -> Unit) {}
    fun setMouseLeaveCallback(cb: (LLEmojiGridIcon) -> Unit) {}
    fun setMouseDownCallback(cb: (LLEmojiGridIcon) -> Unit) {}
    fun setMouseUpCallback(cb: (LLEmojiGridIcon) -> Unit) {}
    fun setBackgroundColor(color: Any) {}
    fun setBackgroundOpaque(v: Int) {}
    fun draw() { TODO("GPU: render emoji icon") }
}

private class LLEmojiPreviewPanel {
    var visible: Boolean = false
    private var currentEmoji: Int = 0
    private var title: String = ""
    private var matchBegin: Int = 0
    private var matchEnd: Int = 0

    fun setIcon(icon: LLEmojiGridIcon?) {
        if (icon != null) {
            setData(icon.data.character, icon.data.string, icon.data.begin, icon.data.end)
        } else {
            setData(0, "", 0, 0)
        }
    }

    fun setData(emoji: Int, t: String, begin: Int, end: Int) {
        currentEmoji = emoji; title = t; matchBegin = begin; matchEnd = end
    }

    fun draw() { TODO("GPU: render emoji preview panel with icon + highlighted name") }
}

// ---------------------------------------------------------------------------
// Dictionary / helper singletons (stubs)
// ---------------------------------------------------------------------------

private object LLEmojiDictionary {
    data class EmojiGroup(val character: Int, val categories: List<String>)

    fun getGroups(): List<EmojiGroup> = TODO("APR: use JVM equivalent for emoji group list")
    fun getEmoji2Descr(): Map<Int, LLEmojiDescriptor> = TODO("APR: use JVM equivalent")
    fun getCategory2Descrs(): Map<String, List<LLEmojiDescriptor>> = TODO("APR: use JVM equivalent")

    fun searchInShortCode(shortcode: String, pattern: String): Triple<Boolean, Int, Int> =
        TODO("APR: use JVM equivalent for emoji short-code search")
}

private object LLEmojiHelper {
    fun setIsHideDisabled(disabled: Boolean) {}
    fun hideHelper(arg: Any?, force: Boolean) {}
}

// ---------------------------------------------------------------------------
// LLFloaterEmojiPicker
// ---------------------------------------------------------------------------

class LLFloaterEmojiPicker(key: Any) {

    // Signals for live-updating recently-used consumers
    private val recentEmojisUpdatedListeners: MutableList<(List<Int>) -> Unit> = mutableListOf()

    // UI child references (resolved via postBuild)
    private var groups: Any? = null          // LLPanel
    private var badge: Any? = null           // LLPanel
    private var emojiScroll: Any? = null     // LLScrollContainer
    private var emojiGrid: MutableList<Any> = mutableListOf()
    private var dummy: Any? = null           // LLTextBox
    private var preview: LLEmojiPreviewPanel? = null

    // Filter / selection state
    private val filteredEmojiGroups: MutableList<Int> = mutableListOf()
    private val filteredEmojis: MutableList<Map<String, List<LLEmojiSearchResult>>> = mutableListOf()
    private val groupButtons: MutableList<Any> = mutableListOf()

    private var hint: String = ""
    private var filterPattern: String = ""
    private var selectedGroupIndex: UInt = 0u
    private var recentMaxIcons: Int = 0
    private var focusedIconRow: Int = 0
    private var focusedIconCol: Int = 0
    private var focusedIcon: LLEmojiGridIcon? = null
    private var hoveredIcon: LLEmojiGridIcon? = null
    private var recentReturnPressedMs: ULong = 0u

    // Grid rows built during fillEmojis
    private val gridRows: MutableList<LLEmojiGridRow> = mutableListOf()

    companion object {
        private val recentEmojisUpdatedCallbacks: MutableList<(List<Int>) -> Unit> = mutableListOf()

        fun setRecentEmojisUpdatedCallback(cb: (List<Int>) -> Unit) {
            recentEmojisUpdatedCallbacks.add(cb)
        }

        fun getRecentlyUsed(): List<Int> {
            loadState()
            return recentlyUsed.toList()
        }

        fun onEmojiUsed(emoji: Int) {
            recentlyUsed.remove(emoji)
            recentlyUsed.addFirst(emoji)

            // Increment count in frequentlyUsed; bubble up while count is higher than predecessor
            val idx = frequentlyUsed.indexOfFirst { it.first == emoji }
            if (idx >= 0) {
                val newCount = frequentlyUsed[idx].second + 1u
                frequentlyUsed[idx] = emoji to newCount
                var i = idx
                while (i > 0 && frequentlyUsed[i - 1].second <= frequentlyUsed[i].second) {
                    val tmp = frequentlyUsed[i - 1]
                    frequentlyUsed[i - 1] = frequentlyUsed[i]
                    frequentlyUsed[i] = tmp
                    i--
                }
            } else {
                // Insert before any entries that already have count == 1
                val insertAt = frequentlyUsed.indexOfFirst { it.second <= 1u }.let {
                    if (it == -1) frequentlyUsed.size else it
                }
                frequentlyUsed.add(insertAt, emoji to 1u)
            }

            recentEmojisUpdatedCallbacks.forEach { it(recentlyUsed.toList()) }
        }

        fun loadState() {
            if (stateFileName.isNotEmpty()) return

            stateFileName = TODO("APR: use JVM equivalent for per-account emoji_floater_state.xml path")

            val file = File(stateFileName)
            if (!file.exists()) return

            TODO("APR: use JVM equivalent for LLSD XML deserialisation of emoji state file")
        }

        fun saveState() {
            if (stateFileName.isEmpty()) return

            TODO("APR: use JVM equivalent for LLSD XML serialisation of emoji state to $stateFileName")
        }
    }

    open fun postBuild(): Boolean {
        preview = LLEmojiPreviewPanel().also { it.visible = false }
        return true
    }

    open fun dirtyRect() {
        if (preview == null) return

        TODO("GPU: recompute preview rect and resize emoji grid when outer scroll width changes")
    }

    open fun goneFromFront() {
        hideFloater()
    }

    fun hideFloater() {
        LLEmojiHelper.hideHelper(null, true)
    }

    open fun onOpen(key: Map<String, Any?>) {
        hint = key["hint"] as? String ?: ""
        LLEmojiHelper.setIsHideDisabled(hint.isEmpty())
        filterPattern = hint
        initialize()
        TODO("GPU: gFloaterView->adjustToFitScreen")
    }

    open fun onClose(appQuitting: Boolean) {
        if (!appQuitting) {
            LLEmojiHelper.hideHelper(null, true)
        }
    }

    open fun handleKey(key: Int, mask: Int, calledFromParent: Boolean): Boolean {
        val MASK_NONE = 0
        val MASK_ALT  = 0x01
        val KEY_UP    = 0x82; val KEY_DOWN   = 0x83
        val KEY_LEFT  = 0x84; val KEY_RIGHT  = 0x85
        val KEY_ESCAPE = 0x01; val KEY_RETURN = '\r'.code
        val KEY_BACKSPACE = '\b'.code

        if (mask == MASK_NONE) {
            when (key) {
                KEY_UP    -> { moveFocusedIconUp();   return true }
                KEY_DOWN  -> { moveFocusedIconDown(); return true }
                KEY_LEFT  -> { moveFocusedIconPrev(); return true }
                KEY_RIGHT -> { moveFocusedIconNext(); return true }
                KEY_ESCAPE -> { hideFloater();        return true }
            }
        }

        if (mask == MASK_ALT) {
            when (key) {
                KEY_LEFT -> {
                    val sz = groupButtons.size.toUInt()
                    if (sz > 0u) selectEmojiGroup((selectedGroupIndex + filteredEmojis.size.toUInt()) % sz)
                    return true
                }
                KEY_RIGHT -> {
                    val sz = groupButtons.size.toUInt()
                    if (sz > 0u) selectEmojiGroup((selectedGroupIndex + 1u) % sz)
                    return true
                }
            }
        }

        if (key == KEY_RETURN) {
            val time = System.currentTimeMillis().toULong()
            // Shift+Return fires twice for an unknown reason; guard with 100 ms debounce
            if (focusedIcon != null && (time - recentReturnPressedMs > 100u)) {
                onEmojiMouseDown(focusedIcon!!)
                onEmojiMouseUp(focusedIcon!!)
            }
            recentReturnPressedMs = time
            return true
        }

        if (hint.isEmpty()) {
            if (key in 0x20 until 0x80) {
                if (gridRows.isNotEmpty()) {
                    if (filterPattern.isEmpty()) filterPattern = ":"
                    filterPattern += key.toChar()
                    initialize()
                }
                return true
            } else if (key == KEY_BACKSPACE) {
                if (filterPattern.isNotEmpty()) {
                    filterPattern = filterPattern.dropLast(1)
                    if (filterPattern == ":") filterPattern = ""
                    initialize()
                }
                return true
            }
        }

        return false
    }

    // -----------------------------------------------------------------------
    // Private implementation
    // -----------------------------------------------------------------------

    private fun initialize() {
        val groupIndex = if (selectedGroupIndex != 0u && selectedGroupIndex <= filteredEmojiGroups.size.toUInt()) {
            filteredEmojiGroups[(selectedGroupIndex - 1u).toInt()]
        } else {
            ALL_EMOJIS_GROUP_INDEX
        }

        fillGroups()

        if (filteredEmojis.isEmpty()) {
            if (hint.isNotEmpty()) {
                hideFloater()
                return
            }

            focusedIconRow = -1; focusedIconCol = -1
            focusedIcon = null; hoveredIcon = null
            gridRows.clear()

            if (filterPattern.isEmpty()) {
                showPreview(false)
            } else {
                val (_, begin, end) = LLEmojiDictionary.searchInShortCode(filterPattern, filterPattern)
                preview?.setData(EMPTY_LIST_IMAGE_INDEX, "No emoji for filter: $filterPattern", begin, end)
                showPreview(true)
            }
            return
        }

        selectedGroupIndex = if (groupIndex == ALL_EMOJIS_GROUP_INDEX) {
            0u
        } else {
            val pos = filteredEmojiGroups.indexOf(groupIndex)
            ((if (pos >= 0) pos + 1 else 0) % (1 + filteredEmojiGroups.size)).toUInt()
        }

        TODO("GPU: set toggle/font-color state on groupButtons[selectedGroupIndex]")
        fillEmojis()
    }

    private fun fillGroups() {
        groupButtons.clear()
        filteredEmojiGroups.clear()
        filteredEmojis.clear()

        // "All categories" button
        createGroupButton("all_categories", ALL_EMOJIS_IMAGE_INDEX)

        // "Frequently used" group
        if (frequentlyUsed.isNotEmpty()) {
            val cats = mutableMapOf<String, List<LLEmojiSearchResult>>()
            fillCategoryFrequentlyUsed(cats)
            if (cats.isNotEmpty()) {
                filteredEmojiGroups.add(USED_EMOJIS_GROUP_INDEX)
                filteredEmojis.add(cats)
                createGroupButton("used_categories", USED_EMOJIS_IMAGE_INDEX)
            }
        }

        val groups = LLEmojiDictionary.getGroups()
        for ((i, group) in groups.withIndex()) {
            val cats = mutableMapOf<String, List<LLEmojiSearchResult>>()
            fillGroupEmojis(cats, i)
            if (cats.isNotEmpty()) {
                filteredEmojiGroups.add(i)
                filteredEmojis.add(cats)
                createGroupButton("group_$i", group.character)
            }
        }

        resizeGroupButtons()
    }

    private fun fillCategoryFrequentlyUsed(cats: MutableMap<String, List<LLEmojiSearchResult>>) {
        if (frequentlyUsed.isEmpty()) return
        val emojis = mutableListOf<LLEmojiSearchResult>()

        if (filterPattern.isNotEmpty()) {
            val emoji2descr = LLEmojiDictionary.getEmoji2Descr()
            for ((emojiChar, _) in frequentlyUsed) {
                val descr = emoji2descr[emojiChar] ?: continue
                for (shortcode in descr.shortCodes) {
                    val (found, begin, end) = LLEmojiDictionary.searchInShortCode(shortcode, filterPattern)
                    if (found) emojis.add(LLEmojiSearchResult(emojiChar, shortcode, begin, end))
                }
            }
            if (emojis.isEmpty()) return
        }

        cats[FREQUENTLY_USED_CATEGORY] = emojis
    }

    private fun fillGroupEmojis(cats: MutableMap<String, List<LLEmojiSearchResult>>, index: Int) {
        val groups = LLEmojiDictionary.getGroups()
        val category2Descrs = LLEmojiDictionary.getCategory2Descrs()

        for (category in groups[index].categories) {
            val descrs = category2Descrs[category] ?: continue
            val emojis = mutableListOf<LLEmojiSearchResult>()

            if (filterPattern.isNotEmpty()) {
                for (descr in descrs) {
                    for (shortcode in descr.shortCodes) {
                        val (found, begin, end) = LLEmojiDictionary.searchInShortCode(shortcode, filterPattern)
                        if (found) emojis.add(LLEmojiSearchResult(descr.character, shortcode, begin, end))
                    }
                }
                if (emojis.isEmpty()) continue
            }

            cats[category] = emojis
        }
    }

    private fun createGroupButton(name: String, emoji: Int) {
        TODO("GPU: create LLButton labelled with emoji glyph, register click/hover callbacks")
    }

    private fun resizeGroupButtons() {
        TODO("GPU: distribute group buttons evenly across mGroups panel width; reposition badge")
    }

    private fun selectEmojiGroup(index: UInt) {
        if (index == selectedGroupIndex || index >= groupButtons.size.toUInt()) return
        TODO("GPU: toggle old/new group button font-color and badge rect, then fillEmojis()")
        selectedGroupIndex = index
        fillEmojis()
    }

    private fun fillEmojis(fromResize: Boolean = false) {
        TODO("GPU: compute max_icons from scroll container width, build LLEmojiGridRow / LLEmojiGridIcon panels")
    }

    private fun fillEmojisCategory(
        emojis: List<LLEmojiSearchResult>,
        category: String,
        maxIcons: Int,
        row: Array<LLEmojiGridRow?>
    ) {
        val title = when {
            category == FREQUENTLY_USED_CATEGORY -> "Frequently Used"
            category.first().isUpperCase() -> category
            else -> category.replaceFirstChar { it.uppercase() }
        }
        TODO("GPU: add LLEmojiGridDivider titled '$title'; iterate emojis via createEmojiIcon")
    }

    private fun createEmojiIcon(
        emoji: LLEmojiSearchResult,
        category: String,
        maxIcons: Int,
        iconIndex: IntArray,
        row: Array<LLEmojiGridRow?>
    ) {
        if (iconIndex[0] % maxIcons == 0) {
            row[0] = LLEmojiGridRow().also { gridRows.add(it) }
        }
        val icon = LLEmojiGridIcon(emoji)
        icon.setMouseEnterCallback { onEmojiMouseEnter(it) }
        icon.setMouseLeaveCallback { onEmojiMouseLeave(it) }
        icon.setMouseDownCallback  { onEmojiMouseDown(it) }
        icon.setMouseUpCallback    { onEmojiMouseUp(it) }
        row[0]?.addIcon(icon)
        iconIndex[0]++
    }

    private fun showPreview(show: Boolean) {
        TODO("GPU: toggle mDummy/mPreview visibility")
    }

    private fun onGroupButtonClick(index: Int) {
        selectEmojiGroup(index.toUInt())
    }

    private fun onGroupButtonMouseEnter(index: Int) {
        TODO("GPU: set button font color active on hover enter")
    }

    private fun onGroupButtonMouseLeave(index: Int) {
        TODO("GPU: restore button font color on hover leave unless it is the selected group")
    }

    private fun onEmojiMouseEnter(icon: LLEmojiGridIcon) {
        if (focusedIcon != null && focusedIcon != icon && focusedIcon!!.backgroundVisible) {
            unselectGridIcon(focusedIcon!!)
        }
        if (hoveredIcon != null && hoveredIcon != icon) {
            unselectGridIcon(hoveredIcon!!)
        }
        selectGridIcon(icon)
        hoveredIcon = icon
    }

    private fun onEmojiMouseLeave(icon: LLEmojiGridIcon) {
        if (icon == hoveredIcon) {
            if (icon != focusedIcon) unselectGridIcon(icon)
            hoveredIcon = null
        }
        if (hoveredIcon == null && focusedIcon != null && !focusedIcon!!.backgroundVisible) {
            selectGridIcon(focusedIcon!!)
        }
    }

    private fun onEmojiMouseDown(icon: LLEmojiGridIcon) {
        TODO("GPU: play UISndClick if sound flags permit")
    }

    private fun onEmojiMouseUp(icon: LLEmojiGridIcon) {
        TODO("GPU: play UISndClickRelease if sound flags permit; commit emoji char and conditionally hide floater")
    }

    private fun selectFocusedIcon() {
        if (focusedIcon != null && focusedIcon != hoveredIcon) {
            unselectGridIcon(focusedIcon!!)
        }
        val row = gridRows.getOrNull(focusedIconRow) ?: return
        focusedIcon = row.icons.getOrNull(focusedIconCol)
        if (focusedIcon != null && hoveredIcon == null) {
            selectGridIcon(focusedIcon!!)
        }
    }

    private fun moveFocusedIconUp(): Boolean {
        for (i in focusedIconRow - 1 downTo 0) {
            val row = gridRows[i]
            if (row.icons.size > focusedIconCol) {
                TODO("GPU: scroll emojiScroll to show row bounding rect")
                focusedIconRow = i
                selectFocusedIcon()
                return true
            }
        }
        return false
    }

    private fun moveFocusedIconDown(): Boolean {
        for (i in focusedIconRow + 1 until gridRows.size) {
            val row = gridRows[i]
            if (row.icons.size > focusedIconCol) {
                TODO("GPU: scroll emojiScroll to show row bounding rect")
                focusedIconRow = i
                selectFocusedIcon()
                return true
            }
        }
        return false
    }

    private fun moveFocusedIconPrev(): Boolean {
        if (hoveredIcon != null) return false
        if (focusedIconCol > 0) {
            focusedIconCol--
            selectFocusedIcon()
            return true
        }
        for (i in focusedIconRow - 1 downTo 0) {
            val row = gridRows[i]
            if (row.icons.isNotEmpty()) {
                TODO("GPU: scroll emojiScroll to show row bounding rect")
                focusedIconCol = row.icons.size - 1
                focusedIconRow = i
                selectFocusedIcon()
                return true
            }
        }
        return false
    }

    private fun moveFocusedIconNext(): Boolean {
        if (hoveredIcon != null) return false
        val row = gridRows.getOrNull(focusedIconRow)
        val colCount = row?.icons?.size ?: 0
        if (focusedIconCol < colCount - 1) {
            focusedIconCol++
            selectFocusedIcon()
            return true
        }
        for (i in focusedIconRow + 1 until gridRows.size) {
            val nextRow = gridRows[i]
            if (nextRow.icons.isNotEmpty()) {
                TODO("GPU: scroll emojiScroll to show row bounding rect")
                focusedIconCol = 0
                focusedIconRow = i
                selectFocusedIcon()
                return true
            }
        }
        return false
    }

    private fun selectGridIcon(icon: LLEmojiGridIcon) {
        icon.backgroundVisible = true
        preview?.setIcon(icon)
    }

    private fun unselectGridIcon(icon: LLEmojiGridIcon) {
        icon.backgroundVisible = false
        preview?.setIcon(null)
    }
}
