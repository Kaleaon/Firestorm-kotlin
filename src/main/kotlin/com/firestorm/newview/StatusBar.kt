package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llui.LLPanel
import com.firestorm.llui.LLRect

// Corresponds to: llstatusbar.h / llstatusbar.cpp
// Bottom status bar of the Firestorm viewer. Displays balance, health,
// FPS, bandwidth, clock, parcel info, and various icon controls.

/** Mirrors C++ LLRegionDetails — per-region/parcel metadata cached by the status bar. */
data class RegionDetails(
    var regionName: String  = "Unknown",
    var parcelName: String  = "Unknown",
    var accessString: String = "Unknown",
    var x: Int              = 0,
    var y: Int              = 0,
    var z: Int              = 0,
    var area: Int           = 0,
    var forSale: Boolean    = false,
    var owner: String       = "Unknown",
    var traffic: Float      = 0f,
    var balance: Int        = 0,
    var time: String        = "",
    var ping: UInt          = 0u,
)

/** Parcel permission icons shown in the right section of the status bar. */
enum class ParcelIcon {
    VOICE,
    FLY,
    PUSH,
    BUILD,
    SCRIPTS,
    SEE_AVATARS,
    PATHFINDING_DIRTY,
    PATHFINDING_DISABLED,
    DAMAGE,
}

/**
 * Bottom status bar of the viewer.
 *
 * Mirrors C++ [LLStatusBar] (extends LLPanel). Shows balance, health,
 * FPS, bandwidth, location, clock, and parcel restriction icons.
 *
 * Complex GL rendering stubs are marked TODO("GL: ...").
 */
class StatusBar(rect: LLRect) : LLPanel(rect) {

    // ── Core display state ──────────────────────────────────────────────────

    var balance: Int = 0
        private set

    var health: Int = 100
        private set

    /** Frames per second (updated on a timer, not every frame). */
    var fps: Float = 0f
        private set

    /** Current bandwidth in kbits/s. */
    var kbits: Float = 0f
        private set

    /** Current displayed location string (region + parcel + optional coords). */
    var locationText: String = ""
        private set

    // Land credit / committed tracking (used to derive "land left")
    private var squareMetersCredit: Int     = 0
    private var squareMetersCommitted: Int  = 0

    private var audioStreamEnabled: Boolean = false
    private var balanceVisible: Boolean     = true
    private var obscureBalance: Boolean     = false
    private var showParcelIcons: Boolean    = true
    private var rebakeStuck: Boolean        = false

    /** Per-region detail cache, publicly writable (mirrors C++ public field). */
    val regionDetails: RegionDetails = RegionDetails()

    // ── Lifecycle ───────────────────────────────────────────────────────────

    /** Called after UI is built from XML (mirrors C++ postBuild). */
    fun postBuild(): Boolean {
        initParcelIcons()
        return true
    }

    /** Called once login is complete so parcel / location icons are enabled. */
    fun handleLoginComplete() {
        updateParcelInfoText()
        updateParcelIcons()
    }

    // ── Manipulators ────────────────────────────────────────────────────────

    fun setBalance(balance: Int) {
        this.balance = balance
        // TODO("GL: update mBoxBalance text label")
    }

    fun debitBalance(debit: Int)   { setBalance(balance - debit) }
    fun creditBalance(credit: Int) { setBalance(balance + credit) }

    fun setHealth(percent: Int) {
        health = percent.coerceIn(0, 100)
        updateHealth()
    }

    fun setFPS(fps: Float) {
        this.fps = fps
        // TODO("GL: update mFPSText label")
    }

    fun setLandCredit(credit: Int) {
        squareMetersCredit = credit
    }

    fun setLandCommitted(committed: Int) {
        squareMetersCommitted = committed
    }

    fun setBalanceVisible(visible: Boolean) {
        balanceVisible = visible
        // TODO("GL: show/hide mBalancePanel")
    }

    fun setVisibleForMouselook(visible: Boolean) {
        // TODO("GL: hide/show mouselook-sensitive controls")
    }

    fun toggleMedia(enable: Boolean) {
        // TODO("GL: toggle media playback button state")
    }

    fun toggleStream(enable: Boolean) {
        audioStreamEnabled = enable
        // TODO("GL: toggle stream button state")
    }

    fun updateCurrencySymbols() {
        // TODO("GL: refresh BUY L$ button label with current currency symbol")
    }

    fun onTimeFormatChanged(format: String) {
        // TODO("GL: update clock display format")
    }

    /** Main per-frame / per-tick refresh — updates clock, FPS, net stats. */
    fun refresh() {
        updateParcelInfoText()
        updateParcelIcons()
        updateHealth()
        updateClockDisplay()
        // TODO("GL: update mSGBandwidth / mSGPacketLoss graphs")
    }

    // ── Accessors ───────────────────────────────────────────────────────────

    fun getBalance(): Int = balance
    fun getHealth(): Int  = health

    fun isUserTiered(): Boolean = squareMetersCredit > 0
    fun getSquareMetersCredit(): Int    = squareMetersCredit
    fun getSquareMetersCommitted(): Int = squareMetersCommitted
    fun getSquareMetersLeft(): Int      = squareMetersCredit - squareMetersCommitted

    fun getAudioStreamEnabled(): Boolean = audioStreamEnabled

    // ── Internal helpers ────────────────────────────────────────────────────

    private fun initParcelIcons() {
        // TODO("GL: bind mParcelIcon[] controls from XML")
    }

    private fun updateHealth() {
        // TODO("GL: update mDamageText visibility and label")
    }

    private fun updateParcelInfoText() {
        val showCoords = true  // driven by NavBarShowCoordinates setting
        buildLocationString(showCoords)
        // TODO("GL: push locationText into mParcelInfoText control")
    }

    /** Updates visibility of parcel restriction icons based on parcel flags. */
    fun updateParcelIcons() {
        // TODO("GL: iterate ParcelIcon entries, show/hide mParcelIcon[] controls")
    }

    private fun buildLocationString(showCoords: Boolean) {
        val coords = if (showCoords) " (${regionDetails.x}, ${regionDetails.y}, ${regionDetails.z})" else ""
        locationText = "${regionDetails.regionName}${coords} / ${regionDetails.parcelName}"
    }

    private fun updateClockDisplay() {
        // TODO("GL: format current UTC/local time using mClockFormat and push to mTextTime")
    }

    private fun onClickBuyCurrency() {
        // TODO("GL: open LLFloaterBuyCurrency")
    }

    private fun onClickShop() {
        // TODO("GL: open marketplace floater")
    }

    private fun onInfoButtonClicked() {
        // TODO("GL: open parcel info floater")
    }

    private fun onAgentParcelChange() {
        update()
    }

    private fun update() {
        updateParcelInfoText()
        updateParcelIcons()
    }

    fun setRebakeStuck(stuck: Boolean) {
        rebakeStuck = stuck
    }

    // ── Draw ────────────────────────────────────────────────────────────────

    override fun draw() {
        TODO("GL: render status bar panel — super.draw() then overlay stat graphs")
    }

    // ── Singleton / global accessor ─────────────────────────────────────────

    companion object {
        private var instance: StatusBar? = null

        /** Global accessor matching C++ [gStatusBar] pattern. */
        fun getInstance(): StatusBar? = instance

        @JvmStatic
        fun setInstance(bar: StatusBar?) {
            instance = bar
        }

        /** Mirrors C++ LLStatusBar::sendMoneyBalanceRequest(). */
        fun sendMoneyBalanceRequest() {
            // TODO("NET: send money balance request to server")
        }
    }
}

/** Mirrors C++ can_afford_transaction(). Returns true if the agent can pay [cost]. */
fun canAffordTransaction(cost: Int): Boolean {
    return (StatusBar.getInstance()?.getBalance() ?: 0) >= cost
}
