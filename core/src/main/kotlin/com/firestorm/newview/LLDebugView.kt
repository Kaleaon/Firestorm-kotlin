package com.firestorm.newview

class LLDebugView(params: Params) : LLView(params) {

    class Params : LLView.Params() {
        init {
            mouseOpaque = false
        }
    }

    var mFastTimerView: LLFastTimerView? = null
    var mDebugConsolep: LLConsole? = null
    var mFloaterSnapRegion: LLView? = null

    fun init() {
        val rect = getLocalRect()

        val cp = LLConsole.Params()
        cp.name = "debug console"
        cp.maxLines = 20
        cp.rect = LLRect(10, rect.height - 100, (rect.width * 3) / 4, 100)
        cp.font = LLFontGL.getFontMonospace()
        cp.followsFlags = FOLLOWS_BOTTOM or FOLLOWS_LEFT
        cp.visible = false
        mDebugConsolep = LLUICtrlFactory.create(cp)
        addChild(mDebugConsolep!!)

        val timerRect = LLRect(
            25,
            rect.height - 50,
            (gViewerWindow.windowRectScaled.width * 0.75f).toInt(),
            (gViewerWindow.windowRectScaled.height * 0.75f).toInt()
        )

        mFastTimerView = LLFloaterReg.getInstance("block_timers") as? LLFastTimerView

        gSceneView = LLSceneView(timerRect)
        gSceneView!!.setFollowsTop()
        gSceneView!!.setFollowsLeft()
        gSceneView!!.setVisible(false)
        addChild(gSceneView!!)
        gSceneView!!.setRect(rect)

        gSceneMonitorView = LLSceneMonitorView(timerRect)
        gSceneMonitorView!!.setFollowsTop()
        gSceneMonitorView!!.setFollowsLeft()
        gSceneMonitorView!!.setVisible(false)
        addChild(gSceneMonitorView!!)
        gSceneMonitorView!!.setRect(rect)

        val tvp = LLTextureView.Params()
        tvp.name = "gTextureView"
        tvp.rect = LLRect(150, rect.height - 60, 965, 100)
        tvp.followsFlags = FOLLOWS_TOP or FOLLOWS_LEFT
        tvp.visible = false
        gTextureView = LLUICtrlFactory.create(tvp)
        addChild(gTextureView!!)
    }

    override fun draw() {
        if (mFloaterSnapRegion == null) {
            mFloaterSnapRegion = gViewerWindow.floaterSnapRegion
        }

        val debugRect = LLRect()
        mFloaterSnapRegion!!.localRectToOtherView(mFloaterSnapRegion!!.getLocalRect(), debugRect, getParent())

        setShape(debugRect)
        super.draw()
    }

    override fun destroy() {
        gDebugView = null
        gTextureView = null
        gSceneView = null
        gSceneMonitorView = null
    }
}

var gDebugView: LLDebugView? = null
