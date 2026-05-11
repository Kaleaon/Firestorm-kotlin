package com.firestorm.newview

private const val MAX_HISTORY_ENTRIES = 10
private const val SCROLL_STEP_DELAY = 0.25f
private const val SCROLL_END_DELAY = 4.0f

object FSStreamTitleManager {

    private val updateListeners: MutableList<(String) -> Unit> = mutableListOf()
    private val historyUpdateListeners: MutableList<(List<String>) -> Unit> = mutableListOf()

    private var metadataUpdateSlot: ((Any) -> Unit)? = null

    var currentStreamTitle: String = ""
        private set
    val streamTitleHistory: MutableList<String> = mutableListOf()

    fun init() {
        TODO("APR: use JVM equivalent: check gAudiop and getStreamingAudioImpl(); if present, connect metadataUpdateSlot to setMetadataUpdateCallback and call processMetadataUpdate with current metadata")
    }

    fun destroy() {
        metadataUpdateSlot = null
    }

    fun setUpdateCallback(cb: (String) -> Unit): () -> Unit {
        updateListeners.add(cb)
        return { updateListeners.remove(cb) }
    }

    fun setHistoryUpdateCallback(cb: (List<String>) -> Unit): () -> Unit {
        historyUpdateListeners.add(cb)
        return { historyUpdateListeners.remove(cb) }
    }

    fun processMetadataUpdate(metadata: Map<String, Any?>) {
        var chat = ""
        val artist = metadata["ARTIST"] as? String
        val title = metadata["TITLE"] as? String

        if (!artist.isNullOrEmpty()) chat = artist
        if (!title.isNullOrEmpty()) {
            if (chat.isNotEmpty()) chat += " - "
            chat += title
        }

        if (chat != currentStreamTitle) {
            currentStreamTitle = chat

            if (currentStreamTitle.isNotEmpty() &&
                (streamTitleHistory.isEmpty() || streamTitleHistory.last() != currentStreamTitle)
            ) {
                streamTitleHistory.add(currentStreamTitle)
                if (streamTitleHistory.size > MAX_HISTORY_ENTRIES) {
                    streamTitleHistory.removeAt(0)
                }
                historyUpdateListeners.forEach { it(streamTitleHistory.toList()) }
            }

            updateListeners.forEach { it(currentStreamTitle) }
        }
    }
}

class FSFloaterStreamTitleHistory(key: Map<String, Any?>) {

    private var historyCtrl: Any? = null
    private var updateConnection: (() -> Unit)? = null
    private var owner: Any? = null
    private var contextConeOpacity: Float = 0f

    fun destroy() {
        updateConnection?.invoke()
        updateConnection = null
    }

    fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent: find child FSScrollListCtrl 'history' and assign to historyCtrl")
        updateConnection = FSStreamTitleManager.setHistoryUpdateCallback { history -> updateHistory(history) }
        updateHistory(FSStreamTitleManager.streamTitleHistory.toList())
        return true
    }

    fun draw() {
        TODO("APR: use JVM equivalent: call LLFloater.draw()")
        TODO("GPU: drawConeToOwner(contextConeOpacity, maxOpacity from PickerContextOpacity setting, owner)")
    }

    fun setOwnerOrigin(owner: Any) {
        this.owner = owner
    }

    private fun updateHistory(history: List<String>) {
        TODO("APR: use JVM equivalent: clear historyCtrl rows")
        for (entry in history) {
            TODO("APR: use JVM equivalent: add element to historyCtrl at top with 'title' column value = entry")
        }
    }
}

class FSFloaterStreamTitle(key: Map<String, Any?>) {

    private var historyBtn: Any? = null
    private var titletext: Any? = null

    private var historyFloaterHandle: Any? = null
    private var updateConnection: (() -> Unit)? = null

    private var currentTitle: String = ""
    private var currentDrawnTitle: String = ""
    private var resetTitle: Boolean = false

    private var tickPeriod: Float = SCROLL_STEP_DELAY
    private var timerRunning: Boolean = false

    fun destroy() {
        updateConnection?.invoke()
        updateConnection = null
    }

    fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent: find child LLTextBox 'streamtitle' and assign to titletext")
        TODO("APR: use JVM equivalent: find child LLButton 'btn_history' and assign to historyBtn")

        updateConnection = FSStreamTitleManager.setUpdateCallback { streamtitle -> updateStreamTitle(streamtitle) }
        updateStreamTitle(FSStreamTitleManager.currentStreamTitle)

        TODO("APR: use JVM equivalent: wire historyBtn commit callback to toggleHistory()")
        TODO("APR: use JVM equivalent: wire historyBtn isToggled callback to check if fs_streamtitlehistory floater instance is visible")
        TODO("APR: use JVM equivalent: register visibility-changed callback to closeHistory()")

        return true
    }

    fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        TODO("APR: use JVM equivalent: call LLFloater.reshape(width, height, calledFromParent)")
        checkTitleWidth()
    }

    fun tick(): Boolean {
        if (resetTitle) {
            tickPeriod = SCROLL_END_DELAY
            currentDrawnTitle = currentTitle
            resetTitle = false
        } else {
            tickPeriod = SCROLL_STEP_DELAY
            if (currentDrawnTitle.isNotEmpty()) {
                currentDrawnTitle = currentDrawnTitle.drop(1)
            }
        }

        TODO("APR: use JVM equivalent: set titletext text to currentDrawnTitle")
        TODO("APR: use JVM equivalent: measure text width of currentDrawnTitle via titletext font")
        TODO("APR: use JVM equivalent: if textboxWidth > textWidth, set tickPeriod = SCROLL_END_DELAY, reset timer, set resetTitle = true")

        return false
    }

    private fun updateStreamTitle(streamtitle: String) {
        val display = if (streamtitle.isEmpty()) {
            TODO("APR: use JVM equivalent: return getString(\"NoStream\")") as String
        } else {
            streamtitle
        }

        TODO("APR: use JVM equivalent: set titletext text to display")
        currentTitle = display
        currentDrawnTitle = currentTitle
        TODO("APR: use JVM equivalent: set tooltip on titletext to currentTitle")
        checkTitleWidth()
    }

    private fun toggleHistory() {
        TODO("APR: use JVM equivalent: find root floater via gFloaterView.getParentFloater(this)")
        val historyFloater = TODO("APR: use JVM equivalent: LLFloaterReg.findTypedInstance<FSFloaterStreamTitleHistory>(\"fs_streamtitlehistory\")")
        if (historyFloater == null) {
            TODO("APR: use JVM equivalent: show fs_streamtitlehistory floater, add as dependent of root floater, set owner origin, store handle in historyFloaterHandle")
        } else {
            closeHistory()
        }
    }

    private fun closeHistory() {
        TODO("APR: use JVM equivalent: if historyFloaterHandle is non-null, call closeFloater() on the referenced history floater")
    }

    private fun checkTitleWidth() {
        TODO("APR: use JVM equivalent: measure text width of currentTitle via titletext font and compare to titletext rect width")
        TODO("APR: use JVM equivalent: if text wider than textbox, clear resetTitle flag and start/reset the tick timer; otherwise stop timer and restore full title text")
    }
}
