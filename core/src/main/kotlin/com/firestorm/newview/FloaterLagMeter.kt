package com.firestorm.newview

import kotlin.math.abs

class FloaterLagMeter(key: Any?) : Floater(key) {

    companion object {
        private const val LAG_CRITICAL_IMAGE = "LagStatus_Critical"
        private const val LAG_WARNING_IMAGE  = "LagStatus_Warning"
        private const val LAG_GOOD_IMAGE     = "LagStatus_Good"
    }

    private var shrunk: Boolean = false
    private var maxWidth: Int = 0
    private var minWidth: Int = 0

    private var clientFrameTimeCriticalMs: Float = 0f
    private var clientFrameTimeWarningMs:  Float = 0f
    private var clientButton: ButtonStub?  = null
    private var clientText:   TextBoxStub? = null
    private var clientCause:  TextBoxStub? = null

    private var networkPacketLossCriticalPct: Float = 0f
    private var networkPacketLossWarningPct:  Float = 0f
    private var networkPingCriticalMs:        Float = 0f
    private var networkPingWarningMs:         Float = 0f
    private var networkButton: ButtonStub?   = null
    private var networkText:   TextBoxStub?  = null
    private var networkCause:  TextBoxStub?  = null

    private var serverFrameTimeCriticalMs:   Float = 0f
    private var serverFrameTimeWarningMs:    Float = 0f
    private var serverSingleProcessMaxTimeMs: Float = 0f
    private var serverButton: ButtonStub?   = null
    private var serverText:   TextBoxStub?  = null
    private var serverCause:  TextBoxStub?  = null

    private val stringArgs: MutableMap<String, String> = mutableMapOf()

    override fun postBuild(): Boolean {
        setIsChrome(true)

        if (isShrunk()) onClickShrink()

        clientButton  = getChild("client_lagmeter")
        clientText    = getChild("client_text")
        clientCause   = getChild("client_lag_cause")

        networkButton = getChild("network_lagmeter")
        networkText   = getChild("network_text")
        networkCause  = getChild("network_lag_cause")

        serverButton  = getChild("server_lagmeter")
        serverText    = getChild("server_text")
        serverCause   = getChild("server_lag_cause")

        clientFrameTimeCriticalMs = secondsFromFps(getString("client_frame_rate_critical_fps"))
        clientFrameTimeWarningMs  = secondsFromFps(getString("client_frame_rate_warning_fps"))

        networkPacketLossCriticalPct = getString("network_packet_loss_critical_pct").toFloatOrNull() ?: 0f
        networkPacketLossWarningPct  = getString("network_packet_loss_warning_pct").toFloatOrNull() ?: 0f

        networkPingCriticalMs = getString("network_ping_critical_ms").toFloatOrNull() ?: 0f
        networkPingWarningMs  = getString("network_ping_warning_ms").toFloatOrNull() ?: 0f

        serverFrameTimeCriticalMs    = secondsFromFps(getString("server_frame_rate_critical_fps")) * 1000f
        serverFrameTimeWarningMs     = secondsFromFps(getString("server_frame_rate_warning_fps")) * 1000f
        serverSingleProcessMaxTimeMs = (getString("server_single_process_max_time_ms").toFloatOrNull() ?: 0f)

        maxWidth = getString("max_width_px").toIntOrNull() ?: 0
        minWidth = getString("min_width_px").toIntOrNull() ?: 0

        stringArgs["[CLIENT_FRAME_RATE_CRITICAL]"] = getString("client_frame_rate_critical_fps")
        stringArgs["[CLIENT_FRAME_RATE_WARNING]"]  = getString("client_frame_rate_warning_fps")
        stringArgs["[NETWORK_PACKET_LOSS_CRITICAL]"] = getString("network_packet_loss_critical_pct")
        stringArgs["[NETWORK_PACKET_LOSS_WARNING]"]  = getString("network_packet_loss_warning_pct")
        stringArgs["[NETWORK_PING_CRITICAL]"] = getString("network_ping_critical_ms")
        stringArgs["[NETWORK_PING_WARNING]"]  = getString("network_ping_warning_ms")
        stringArgs["[SERVER_FRAME_RATE_CRITICAL]"] = getString("server_frame_rate_critical_fps")
        stringArgs["[SERVER_FRAME_RATE_WARNING]"]  = getString("server_frame_rate_warning_fps")

        updateControls(isShrunk())
        return true
    }

    fun onDestroy() {
        if (isShrunk()) onClickShrink()
    }

    override fun draw() {
        determineClient()
        determineNetwork()
        determineServer()
        super.draw()
    }

    private fun determineClient() {
        val clientFrameTimeMs: Float = ViewerStats.getMeanFrameTimeMs()
        var findCause = false

        if (!appHasFocus()) {
            clientButton?.setImage(LAG_GOOD_IMAGE)
            clientText?.text = getString("client_frame_time_window_bg_msg")
            clientCause?.text = ""
        } else if (clientFrameTimeMs >= clientFrameTimeCriticalMs) {
            clientButton?.setImage(LAG_CRITICAL_IMAGE)
            clientText?.text = getString("client_frame_time_critical_msg")
            findCause = true
        } else if (clientFrameTimeMs >= clientFrameTimeWarningMs) {
            clientButton?.setImage(LAG_WARNING_IMAGE)
            clientText?.text = getString("client_frame_time_warning_msg")
            findCause = true
        } else {
            clientButton?.setImage(LAG_GOOD_IMAGE)
            clientText?.text = getString("client_frame_time_normal_msg")
            clientCause?.text = ""
        }

        if (findCause) {
            clientCause?.text = when {
                SavedSettings.getFloat("RenderFarClip") > 128f ->
                    getString("client_draw_distance_cause_msg")
                ViewerStats.getPendingTextureRequests() > 2 ->
                    getString("client_texture_loading_cause_msg")
                else ->
                    getString("client_complex_objects_cause_msg")
            }
        }
    }

    private fun determineNetwork() {
        val packetLossPct:    Float = ViewerStats.getMeanPacketLossPct()
        val pingMs:           Float = ViewerStats.getMeanSimPingMs()
        val clientFrameTimeMs: Float = ViewerStats.getMeanFrameTimeMs()

        var findCauseLoss = false
        var findCausePing = false

        when {
            packetLossPct >= networkPacketLossCriticalPct -> {
                networkButton?.setImage(LAG_CRITICAL_IMAGE)
                networkText?.text = getString("network_packet_loss_critical_msg")
                findCauseLoss = true
            }
            pingMs >= networkPingCriticalMs -> {
                networkButton?.setImage(LAG_CRITICAL_IMAGE)
                if (clientFrameTimeMs < networkPingCriticalMs) {
                    networkText?.text = getString("network_ping_critical_msg")
                    findCausePing = true
                }
            }
            packetLossPct >= networkPacketLossWarningPct -> {
                networkButton?.setImage(LAG_WARNING_IMAGE)
                networkText?.text = getString("network_packet_loss_warning_msg")
                findCauseLoss = true
            }
            pingMs >= networkPingWarningMs -> {
                networkButton?.setImage(LAG_WARNING_IMAGE)
                if (clientFrameTimeMs < networkPingWarningMs) {
                    networkText?.text = getString("network_ping_warning_msg")
                    findCausePing = true
                }
            }
            else -> {
                networkButton?.setImage(LAG_GOOD_IMAGE)
                networkText?.text = getString("network_performance_normal_msg")
            }
        }

        networkCause?.text = when {
            findCauseLoss -> getString("network_packet_loss_cause_msg")
            findCausePing -> getString("network_ping_cause_msg")
            else          -> ""
        }
    }

    private fun determineServer() {
        serverFrameTimeWarningMs  = (1f / SimFeatureHandler.simulatorFpsWarn()) * 1000f
        serverFrameTimeCriticalMs = (1f / SimFeatureHandler.simulatorFpsCrit()) * 1000f

        val simFrameTimeMs: Float = ViewerStats.getLastSimFrameTimeMs()
        var findCause = false

        when {
            simFrameTimeMs >= serverFrameTimeCriticalMs -> {
                serverButton?.setImage(LAG_CRITICAL_IMAGE)
                serverText?.text = getString("server_frame_time_critical_msg")
                findCause = true
            }
            simFrameTimeMs >= serverFrameTimeWarningMs -> {
                serverButton?.setImage(LAG_WARNING_IMAGE)
                serverText?.text = getString("server_frame_time_warning_msg")
                findCause = true
            }
            else -> {
                serverButton?.setImage(LAG_GOOD_IMAGE)
                serverText?.text = getString("server_frame_time_normal_msg")
                serverCause?.text = ""
            }
        }

        if (findCause) {
            serverCause?.text = when {
                ViewerStats.getLastSimPhysicsTimeMs() > serverSingleProcessMaxTimeMs ->
                    getString("server_physics_cause_msg")
                ViewerStats.getLastSimScriptsTimeMs() > serverSingleProcessMaxTimeMs ->
                    getString("server_scripts_cause_msg")
                ViewerStats.getLastSimNetTimeMs() > serverSingleProcessMaxTimeMs ->
                    getString("server_net_cause_msg")
                ViewerStats.getLastSimAgentsTimeMs() > serverSingleProcessMaxTimeMs ->
                    getString("server_agent_cause_msg")
                ViewerStats.getLastSimImagesTimeMs() > serverSingleProcessMaxTimeMs ->
                    getString("server_images_cause_msg")
                else ->
                    getString("server_generic_cause_msg")
            }
        }
    }

    private fun updateControls(shrink: Boolean) {
        val button: ButtonStub? = getChild("minimize")
        val deltaWidth = maxWidth - minWidth

        if (!shrink) {
            setTitle(getString("max_title_msg"))
            translateRect(-deltaWidth, 0)
            reshape(maxWidth)

            setChildValue("client",  getString("client_text_msg") + ":")
            setChildValue("network", getString("network_text_msg") + ":")
            setChildValue("server",  getString("server_text_msg") + ":")
            button?.label = getString("smaller_label")
        } else {
            setTitle(getString("min_title_msg"))
            translateRect(deltaWidth, 0)
            reshape(minWidth)

            setChildValue("client",  getString("client_text_msg"))
            setChildValue("network", getString("network_text_msg"))
            setChildValue("server",  getString("server_text_msg"))
            button?.label = getString("bigger_label")
        }
        button?.setFocus(false)
    }

    private fun isShrunk(): Boolean = SavedSettings.getBool("LagMeterShrunk")

    private fun onClickShrink() {
        val wasShrunk = isShrunk()
        updateControls(!wasShrunk)
        SavedSettings.setBool("LagMeterShrunk", !wasShrunk)
    }

    private fun secondsFromFps(fpsString: String): Float {
        val fps = fpsString.toFloatOrNull() ?: return 0f
        return if (fps != 0f) (1f / fps) * 1000f else 0f
    }

    private fun getString(key: String): String = ""

    private fun setIsChrome(chrome: Boolean) {}
    private fun setTitle(title: String) {}
    private fun translateRect(dx: Int, dy: Int) {}
    private fun reshape(width: Int) {}
    private fun setChildValue(name: String, value: String) {}
    private fun appHasFocus(): Boolean = false

    @Suppress("UNCHECKED_CAST")
    private fun <T> getChild(name: String): T? = null
}

class ButtonStub(var label: String = "") {
    fun setImage(imageName: String) {}
    fun setFocus(focused: Boolean) {}
}

class TextBoxStub(var text: String = "")

object ViewerStats {
    fun getMeanFrameTimeMs(): Float = 0f
    fun getMeanPacketLossPct(): Float = 0f
    fun getMeanSimPingMs(): Float = 0f
    fun getLastSimFrameTimeMs(): Float = 0f
    fun getLastSimPhysicsTimeMs(): Float = 0f
    fun getLastSimScriptsTimeMs(): Float = 0f
    fun getLastSimNetTimeMs(): Float = 0f
    fun getLastSimAgentsTimeMs(): Float = 0f
    fun getLastSimImagesTimeMs(): Float = 0f
    fun getPendingTextureRequests(): Int = 0
}

object SimFeatureHandler {
    fun simulatorFpsWarn(): Float = 0f
    fun simulatorFpsCrit(): Float = 0f
}

object AudioEngine {
    var instance: AudioEngineInstance? = null
}

class AudioEngineInstance {
    fun triggerSound(sound: SoundData) {
        System.err.println("AudioEngineInstance: triggerSound not yet implemented")
    }
}

class SoundData
