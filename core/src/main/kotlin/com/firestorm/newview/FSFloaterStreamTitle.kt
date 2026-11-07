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
        System.err.println("FSStreamTitleManager: init not yet implemented")
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
        System.err.println("FSFloaterStreamTitleHistory: postBuild not yet implemented")
        updateConnection = FSStreamTitleManager.setHistoryUpdateCallback { history -> updateHistory(history) }
        updateHistory(FSStreamTitleManager.streamTitleHistory.toList())
        return true
    }

    fun draw() {
        System.err.println("FSFloaterStreamTitleHistory: draw not yet implemented")
        // no-op
    }

    fun setOwnerOrigin(owner: Any) {
        this.owner = owner
    }

    private fun updateHistory(history: List<String>) {
        System.err.println("FSFloaterStreamTitleHistory: updateHistory clear not yet implemented")
        for (entry in history) {
            System.err.println("FSFloaterStreamTitleHistory: updateHistory add entry not yet implemented")
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
        System.err.println("FSFloaterStreamTitle: postBuild titletext not yet implemented")
        System.err.println("FSFloaterStreamTitle: postBuild historyBtn not yet implemented")

        updateConnection = FSStreamTitleManager.setUpdateCallback { streamtitle -> updateStreamTitle(streamtitle) }
        updateStreamTitle(FSStreamTitleManager.currentStreamTitle)

        System.err.println("FSFloaterStreamTitle: postBuild historyBtn commit callback not yet implemented")
        System.err.println("FSFloaterStreamTitle: postBuild historyBtn isToggled callback not yet implemented")
        System.err.println("FSFloaterStreamTitle: postBuild visibility-changed callback not yet implemented")

        return true
    }

    fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        System.err.println("FSFloaterStreamTitle: reshape not yet implemented")
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

        System.err.println("FSFloaterStreamTitle: tick set titletext not yet implemented")
        System.err.println("FSFloaterStreamTitle: tick measure text width not yet implemented")
        System.err.println("FSFloaterStreamTitle: tick compare widths not yet implemented")

        return false
    }

    private fun updateStreamTitle(streamtitle: String) {
        val display = if (streamtitle.isEmpty()) {
            ""
        } else {
            streamtitle
        }

        System.err.println("FSFloaterStreamTitle: updateStreamTitle set titletext not yet implemented")
        currentTitle = display
        currentDrawnTitle = currentTitle
        System.err.println("FSFloaterStreamTitle: updateStreamTitle set tooltip not yet implemented")
        checkTitleWidth()
    }

    private fun toggleHistory() {
        System.err.println("FSFloaterStreamTitle: toggleHistory find root floater not yet implemented")
        val historyFloater: FSFloaterStreamTitleHistory? = null
        if (historyFloater == null) {
            System.err.println("FSFloaterStreamTitle: toggleHistory show history floater not yet implemented")
        } else {
            closeHistory()
        }
    }

    private fun closeHistory() {
        System.err.println("FSFloaterStreamTitle: closeHistory not yet implemented")
    }

    private fun checkTitleWidth() {
        System.err.println("FSFloaterStreamTitle: checkTitleWidth measure not yet implemented")
        System.err.println("FSFloaterStreamTitle: checkTitleWidth timer not yet implemented")
    }
}
