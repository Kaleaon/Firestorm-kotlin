package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Rect
import com.firestorm.llui.Panel

// Corresponds to: llstatusbar.h / llstatusbar.cpp
// Bottom status bar of the Firestorm viewer. Displays balance, health,
// FPS, bandwidth, clock, parcel info, and various icon controls.

// ── Supporting types ──────────────────────────────────────────────────────────

/** Mirrors C++ LLRegionDetails — per-region/parcel metadata cached by the status bar. */
data class RegionDetails(
    var regionName: String   = "Unknown",
    var parcelName: String   = "Unknown",
    var accessString: String = "Unknown",
    var x: Int               = 0,
    var y: Int               = 0,
    var z: Int               = 0,
    var area: Int            = 0,
    var forSale: Boolean     = false,
    var owner: String        = "Unknown",
    var traffic: Float       = 0f,
    var balance: Int         = 0,
    var time: String         = "",
    var ping: UInt           = 0u,
)

/**
 * Parcel permission icons shown in the right section of the status bar.
 * Mirrors C++ LLStatusBar::EParcelIcon (order also defines reverse display order).
 */
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

// ── StatusBar ─────────────────────────────────────────────────────────────────

/**
 * Bottom status bar of the viewer.
 *
 * Mirrors C++ [LLStatusBar] (extends LLPanel → Panel here).
 * Shows balance, health, FPS, bandwidth, location, clock, and parcel icons.
 *
 * Complex GL rendering stubs are marked as plain comments.
 */
class StatusBar(rect: Rect = Rect()) : Panel("status_bar", rect) {

    // ── Core display state ────────────────────────────────────────────────────

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
    private var squareMetersCredit: Int    = 0
    private var squareMetersCommitted: Int = 0

    private var audioStreamEnabled: Boolean = false
    private var balanceVisible: Boolean     = true
    private var obscureBalance: Boolean     = false
    private var showParcelIcons: Boolean    = true
    private var rebakeStuck: Boolean        = false

    /** Per-region detail cache, publicly writable (mirrors C++ public field). */
    val regionDetails: RegionDetails = RegionDetails()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Called after UI is built from XML (mirrors C++ postBuild). */
    override fun postBuild(): Boolean {
        initParcelIcons()
        return true
    }

    /** Called once login is complete so parcel / location icons are enabled. */
    fun handleLoginComplete() {
        updateParcelInfoText()
        updateParcelIcons()
    }

    // ── Manipulators ──────────────────────────────────────────────────────────

    fun setBalance(balance: Int) {
        this.balance = balance
        // update mBoxBalance text label (GL stub)
    }

    fun debitBalance(debit: Int)   { setBalance(balance - debit) }
    fun creditBalance(credit: Int) { setBalance(balance + credit) }

    fun setHealth(percent: Int) {
        health = percent.coerceIn(0, 100)
        updateHealth()
    }

    fun setFPS(fps: Float) {
        this.fps = fps
        // update mFPSText label (GL stub)
    }

    fun setLandCredit(credit: Int) {
        squareMetersCredit = credit
    }

    fun setLandCommitted(committed: Int) {
        squareMetersCommitted = committed
    }

    fun setBalanceVisible(visible: Boolean) {
        balanceVisible = visible
        // show/hide mBalancePanel (GL stub)
    }

    fun setVisibleForMouselook(visible: Boolean) {
        // hide/show mouselook-sensitive controls (GL stub)
    }

    fun toggleMedia(enable: Boolean) {
        // toggle media playback button state (GL stub)
    }

    fun toggleStream(enable: Boolean) {
        audioStreamEnabled = enable
        // toggle stream button state (GL stub)
    }

    fun updateCurrencySymbols() {
        // refresh BUY L$ button label with current currency symbol (GL stub)
    }

    fun onTimeFormatChanged(format: String) {
        // update clock display format (GL stub)
    }

    /** Main per-frame / per-tick refresh — updates clock, FPS, net stats. */
    fun refresh() {
        updateParcelInfoText()
        updateParcelIcons()
        updateHealth()
        updateClockDisplay()
        // update mSGBandwidth / mSGPacketLoss graphs (GL stub)
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    fun getBalance(): Int = balance
    fun getHealth(): Int  = health

    fun isUserTiered(): Boolean = squareMetersCredit > 0
    fun getSquareMetersCredit(): Int    = squareMetersCredit
    fun getSquareMetersCommitted(): Int = squareMetersCommitted
    fun getSquareMetersLeft(): Int      = squareMetersCredit - squareMetersCommitted

    fun getAudioStreamEnabled(): Boolean = audioStreamEnabled

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun initParcelIcons() {
        // bind mParcelIcon[] controls from XML (GL stub)
    }

    private fun updateHealth() {
        // update mDamageText visibility and label (GL stub)
    }

    private fun updateParcelInfoText() {
        val showCoords = true  // driven by NavBarShowCoordinates setting
        buildLocationString(showCoords)
        // push locationText into mParcelInfoText control (GL stub)
    }

    /** Updates visibility of parcel restriction icons based on parcel flags. */
    fun updateParcelIcons() {
        // iterate ParcelIcon entries, show/hide mParcelIcon[] controls (GL stub)
    }

    private fun buildLocationString(showCoords: Boolean) {
        val coords = if (showCoords) " (${regionDetails.x}, ${regionDetails.y}, ${regionDetails.z})" else ""
        locationText = "${regionDetails.regionName}${coords} / ${regionDetails.parcelName}"
    }

    private fun updateClockDisplay() {
        // format current UTC/local time and push to mTextTime (GL stub)
    }

    private fun onClickBuyCurrency() {
        // open LLFloaterBuyCurrency (GL stub)
    }

    private fun onClickShop() {
        // open marketplace floater (GL stub)
    }

    private fun onInfoButtonClicked() {
        // open parcel info floater (GL stub)
    }

    private fun onAgentParcelChange() {
        updateParcelInfoText()
        updateParcelIcons()
    }

    fun setRebakeStuck(stuck: Boolean) {
        rebakeStuck = stuck
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    override fun draw() {
        // render status bar panel — super.draw() then overlay stat graphs (GL stub)
    }

    // ── Singleton / global accessor ───────────────────────────────────────────

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
            // send money balance request to server (NET stub)
        }
    }
}

/** Mirrors C++ can_afford_transaction(). Returns true if the agent can pay [cost]. */
fun canAffordTransaction(cost: Int): Boolean =
    (StatusBar.getInstance()?.getBalance() ?: 0) >= cost
