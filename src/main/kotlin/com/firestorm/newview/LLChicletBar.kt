package com.firestorm.newview

import java.util.UUID

// LLChicletBar is a singleton panel that owns the chiclet panel and well buttons.
// LLSingleton<T> → Kotlin object; session observer interface is implemented inline.
object LLChicletBar {

    var mChicletPanel: LLChicletPanel? = null
    private var mToolbarStack: Any? = null

    init {
        System.err.println("LLChicletBar: init not yet implemented")
    }

    fun destroy() {
        System.err.println("LLChicletBar: destroy not yet implemented")
    }

    // ---- LLIMSessionObserver ------------------------------------------------

    fun sessionAdded(sessionId: UUID, name: String, otherParticipantId: UUID, hasOfflineMsg: Boolean) {
        val panel = mChicletPanel ?: return
        System.err.println("LLChicletBar: sessionAdded not yet implemented")
    }

    fun sessionActivated(sessionId: UUID, name: String, otherParticipantId: UUID) {}

    fun sessionVoiceOrIMStarted(sessionId: UUID) {}

    fun sessionRemoved(sessionId: UUID) {
        val panel = mChicletPanel ?: return
        System.err.println("LLChicletBar: sessionRemoved not yet implemented")
    }

    fun sessionIDUpdated(oldSessionId: UUID, newSessionId: UUID) {
        val panel = mChicletPanel ?: return
        panel.findChiclet<LLChiclet>(oldSessionId)?.sessionId = newSessionId
    }

    // ---- public API --------------------------------------------------------

    fun getTotalUnreadIMCount(): Int =
        mChicletPanel?.getTotalUnreadIMCount() ?: 0

    fun createIMChiclet(sessionId: UUID): LLIMChiclet? {
        val type = LLIMChiclet.getIMSessionType(sessionId)
        return when (type) {
            LLIMChiclet.EType.TYPE_IM    -> mChicletPanel?.createChiclet<LLIMP2PChiclet>(sessionId)
            LLIMChiclet.EType.TYPE_GROUP -> mChicletPanel?.createChiclet<LLIMGroupChiclet>(sessionId)
            LLIMChiclet.EType.TYPE_AD_HOC -> mChicletPanel?.createChiclet<LLAdHocChiclet>(sessionId)
            else -> null
        }
    }

    fun postBuild(): Boolean {
        return false
    }

    fun getChicletPanel(): LLChicletPanel? = mChicletPanel

    fun reshape(width: Int, height: Int, calledFromParent: Boolean) {
        System.err.println("LLChicletBar: reshape not yet implemented")
    }

    fun showWellButton(wellName: String, visible: Boolean) {
        System.err.println("LLChicletBar: showWellButton not yet implemented")
    }

    fun updateVisibility(data: Any?) {
        System.err.println("LLChicletBar: updateVisibility not yet implemented")
    }

    // ---- private helpers ---------------------------------------------------

    private fun processWidthDecreased(deltaWidth: Int): Int {
        val shrinkHeadroom = getChicletPanelShrinkHeadroom()
        if (shrinkHeadroom > 0) {
            val shrinkBy = minOf(-deltaWidth, shrinkHeadroom)
            System.err.println("LLChicletBar: processWidthDecreased not yet implemented")
            return 0
        }
        return -deltaWidth
    }

    private fun getChicletPanelShrinkHeadroom(): Int {
        val panel = mChicletPanel ?: return 0
        return 0
    }

    private fun fitWithTopInfoBar() {
        // Not used in Firestorm (mini location panel is absent).
    }

    private fun log(panel: Any?, descr: String) {
        System.err.println("LLChicletBar: log not yet implemented")
    }
}
