package com.firestorm.newview

import java.util.UUID

private const val LAG_CRITICAL_IMAGE_NAME = "LagStatus_Critical"
private const val LAG_WARNING_IMAGE_NAME  = "LagStatus_Warning"
private const val LAG_GOOD_IMAGE_NAME     = "LagStatus_Good"

class LLFloaterLagMeter(key: LLSD) : LLFloater(key) {

    private var mShrunk: Boolean = false
    private var mMaxWidth: Int = 0
    private var mMinWidth: Int = 0

    private var mClientFrameTimeCritical: Float = 0f
    private var mClientFrameTimeWarning: Float = 0f
    private var mClientButton: LLButton? = null
    private var mClientText: LLTextBox? = null
    private var mClientCause: LLTextBox? = null

    private var mNetworkPacketLossCritical: Float = 0f
    private var mNetworkPacketLossWarning: Float = 0f
    private var mNetworkPingCritical: Float = 0f
    private var mNetworkPingWarning: Float = 0f
    private var mNetworkButton: LLButton? = null
    private var mNetworkText: LLTextBox? = null
    private var mNetworkCause: LLTextBox? = null

    private var mServerFrameTimeCritical: Float = 0f
    private var mServerFrameTimeWarning: Float = 0f
    private var mServerSingleProcessMaxTime: Float = 0f
    private var mServerButton: LLButton? = null
    private var mServerText: LLTextBox? = null
    private var mServerCause: LLTextBox? = null

    private val mStringArgs: MutableMap<String, String> = mutableMapOf()

    init {
        mCommitCallbackRegistrar.add("LagMeter.ClickShrink") { onClickShrink() }
    }

    override fun postBuild(): Boolean {
        setIsChrome(true)

        if (isShrunk()) {
            onClickShrink()
        }

        mClientButton = getChild<LLButton>("client_lagmeter")
        mClientText   = getChild<LLTextBox>("client_text")
        mClientCause  = getChild<LLTextBox>("client_lag_cause")

        mNetworkButton = getChild<LLButton>("network_lagmeter")
        mNetworkText   = getChild<LLTextBox>("network_text")
        mNetworkCause  = getChild<LLTextBox>("network_lag_cause")

        mServerButton = getChild<LLButton>("server_lagmeter")
        mServerText   = getChild<LLTextBox>("server_text")
        mServerCause  = getChild<LLTextBox>("server_lag_cause")

        var configString = getString("client_frame_rate_critical_fps", mStringArgs)
        mClientFrameTimeCritical = 1.0f / configString.toFloat()
        configString = getString("client_frame_rate_warning_fps", mStringArgs)
        mClientFrameTimeWarning = 1.0f / configString.toFloat()

        configString = getString("network_packet_loss_critical_pct", mStringArgs)
        mNetworkPacketLossCritical = configString.toFloat()
        configString = getString("network_packet_loss_warning_pct", mStringArgs)
        mNetworkPacketLossWarning = configString.toFloat()

        configString = getString("network_ping_critical_ms", mStringArgs)
        mNetworkPingCritical = configString.toFloat()
        configString = getString("network_ping_warning_ms", mStringArgs)
        mNetworkPingWarning = configString.toFloat()

        configString = getString("server_frame_rate_critical_fps", mStringArgs)
        mServerFrameTimeCritical = 1.0f / configString.toFloat()
        configString = getString("server_frame_rate_warning_fps", mStringArgs)
        mServerFrameTimeWarning = 1.0f / configString.toFloat()
        configString = getString("server_single_process_max_time_ms", mStringArgs)
        mServerSingleProcessMaxTime = configString.toFloat()

        configString = getString("max_width_px", mStringArgs)
        mMaxWidth = configString.toInt()
        configString = getString("min_width_px", mStringArgs)
        mMinWidth = configString.toInt()

        mStringArgs["[CLIENT_FRAME_RATE_CRITICAL]"] = getString("client_frame_rate_critical_fps")
        mStringArgs["[CLIENT_FRAME_RATE_WARNING]"]  = getString("client_frame_rate_warning_fps")
        mStringArgs["[NETWORK_PACKET_LOSS_CRITICAL]"] = getString("network_packet_loss_critical_pct")
        mStringArgs["[NETWORK_PACKET_LOSS_WARNING]"]  = getString("network_packet_loss_warning_pct")
        mStringArgs["[NETWORK_PING_CRITICAL]"] = getString("network_ping_critical_ms")
        mStringArgs["[NETWORK_PING_WARNING]"]  = getString("network_ping_warning_ms")
        mStringArgs["[SERVER_FRAME_RATE_CRITICAL]"] = getString("server_frame_rate_critical_fps")
        mStringArgs["[SERVER_FRAME_RATE_WARNING]"]  = getString("server_frame_rate_warning_fps")

        updateControls(isShrunk())
        return true
    }

    override fun onDestroy() {
        if (isShrunk()) {
            onClickShrink()
        }
    }

    override fun draw() {
        determineClient()
        determineNetwork()
        determineServer()
        super.draw()
    }

    private fun determineClient() {
        val clientFrameTime: Float = TODO("GPU: LLTrace::get_frame_recording().getPeriodMean(FRAME_STACKTIME) in ms")
        var findCause = false

        if (!appHasFocus()) {
            mClientButton?.setImageUnselected(getUIImage(LAG_GOOD_IMAGE_NAME))
            mClientButton?.setImagePressed(getUIImage(LAG_GOOD_IMAGE_NAME))
            mClientText?.setText(getString("client_frame_time_window_bg_msg", mStringArgs))
            mClientCause?.setText("")
        } else if (clientFrameTime >= mClientFrameTimeCritical) {
            mClientButton?.setImageUnselected(getUIImage(LAG_CRITICAL_IMAGE_NAME))
            mClientButton?.setImagePressed(getUIImage(LAG_CRITICAL_IMAGE_NAME))
            mClientText?.setText(getString("client_frame_time_critical_msg", mStringArgs))
            findCause = true
        } else if (clientFrameTime >= mClientFrameTimeWarning) {
            mClientButton?.setImageUnselected(getUIImage(LAG_WARNING_IMAGE_NAME))
            mClientButton?.setImagePressed(getUIImage(LAG_WARNING_IMAGE_NAME))
            mClientText?.setText(getString("client_frame_time_warning_msg", mStringArgs))
            findCause = true
        } else {
            mClientButton?.setImageUnselected(getUIImage(LAG_GOOD_IMAGE_NAME))
            mClientButton?.setImagePressed(getUIImage(LAG_GOOD_IMAGE_NAME))
            mClientText?.setText(getString("client_frame_time_normal_msg", mStringArgs))
            mClientCause?.setText("")
        }

        if (findCause) {
            val renderFarClip: Float = TODO("APR: use JVM equivalent for gSavedSettings.getF32(\"RenderFarClip\")")
            val textureFetchRequests: Int = TODO("APR: use JVM equivalent for LLAppViewer::instance()->getTextureFetch()->getNumRequests()")
            when {
                renderFarClip > 128f ->
                    mClientCause?.setText(getString("client_draw_distance_cause_msg", mStringArgs))
                textureFetchRequests > 2 ->
                    mClientCause?.setText(getString("client_texture_loading_cause_msg", mStringArgs))
                else ->
                    mClientCause?.setText(getString("client_complex_objects_cause_msg", mStringArgs))
            }
        }
    }

    private fun determineNetwork() {
        val packetLoss: Float = TODO("GPU: frame_recording.getPeriodMean(PACKETS_LOST_PERCENT)")
        val pingTime: Float   = TODO("GPU: frame_recording.getPeriodMean(SIM_PING) in ms")
        val clientFrameTime: Float = TODO("GPU: frame_recording.getPeriodMean(FRAME_STACKTIME) in ms")
        var findCauseLoss = false
        var findCausePing = false

        when {
            packetLoss >= mNetworkPacketLossCritical -> {
                mNetworkButton?.setImageUnselected(getUIImage(LAG_CRITICAL_IMAGE_NAME))
                mNetworkButton?.setImagePressed(getUIImage(LAG_CRITICAL_IMAGE_NAME))
                mNetworkText?.setText(getString("network_packet_loss_critical_msg", mStringArgs))
                findCauseLoss = true
            }
            pingTime >= mNetworkPingCritical -> {
                mNetworkButton?.setImageUnselected(getUIImage(LAG_CRITICAL_IMAGE_NAME))
                mNetworkButton?.setImagePressed(getUIImage(LAG_CRITICAL_IMAGE_NAME))
                if (clientFrameTime < mNetworkPingCritical) {
                    mNetworkText?.setText(getString("network_ping_critical_msg", mStringArgs))
                    findCausePing = true
                }
            }
            packetLoss >= mNetworkPacketLossWarning -> {
                mNetworkButton?.setImageUnselected(getUIImage(LAG_WARNING_IMAGE_NAME))
                mNetworkButton?.setImagePressed(getUIImage(LAG_WARNING_IMAGE_NAME))
                mNetworkText?.setText(getString("network_packet_loss_warning_msg", mStringArgs))
                findCauseLoss = true
            }
            pingTime >= mNetworkPingWarning -> {
                mNetworkButton?.setImageUnselected(getUIImage(LAG_WARNING_IMAGE_NAME))
                mNetworkButton?.setImagePressed(getUIImage(LAG_WARNING_IMAGE_NAME))
                if (clientFrameTime < mNetworkPingWarning) {
                    mNetworkText?.setText(getString("network_ping_warning_msg", mStringArgs))
                    findCausePing = true
                }
            }
            else -> {
                mNetworkButton?.setImageUnselected(getUIImage(LAG_GOOD_IMAGE_NAME))
                mNetworkButton?.setImagePressed(getUIImage(LAG_GOOD_IMAGE_NAME))
                mNetworkText?.setText(getString("network_performance_normal_msg", mStringArgs))
            }
        }

        when {
            findCauseLoss -> mNetworkCause?.setText(getString("network_packet_loss_cause_msg", mStringArgs))
            findCausePing -> mNetworkCause?.setText(getString("network_ping_cause_msg", mStringArgs))
            else          -> mNetworkCause?.setText("")
        }
    }

    private fun determineServer() {
        // Adjust thresholds dynamically for OpenSim regions that report different FPS caps.
        mServerFrameTimeWarning   = 1.0f / LFSimFeatureHandler.instance().simulatorFPSWarn()
        mServerFrameTimeCritical  = 1.0f / LFSimFeatureHandler.instance().simulatorFPSCrit()

        val simFrameTime: Float = TODO("GPU: get_frame_recording().getLastRecording().getLastValue(SIM_FRAME_TIME) in ms")
        var findCause = false

        when {
            simFrameTime >= mServerFrameTimeCritical -> {
                mServerButton?.setImageUnselected(getUIImage(LAG_CRITICAL_IMAGE_NAME))
                mServerButton?.setImagePressed(getUIImage(LAG_CRITICAL_IMAGE_NAME))
                mServerText?.setText(getString("server_frame_time_critical_msg", mStringArgs))
                findCause = true
            }
            simFrameTime >= mServerFrameTimeWarning -> {
                mServerButton?.setImageUnselected(getUIImage(LAG_WARNING_IMAGE_NAME))
                mServerButton?.setImagePressed(getUIImage(LAG_WARNING_IMAGE_NAME))
                mServerText?.setText(getString("server_frame_time_warning_msg", mStringArgs))
                findCause = true
            }
            else -> {
                mServerButton?.setImageUnselected(getUIImage(LAG_GOOD_IMAGE_NAME))
                mServerButton?.setImagePressed(getUIImage(LAG_GOOD_IMAGE_NAME))
                mServerText?.setText(getString("server_frame_time_normal_msg", mStringArgs))
                mServerCause?.setText("")
            }
        }

        if (findCause) {
            val physicsTime: Float   = TODO("GPU: lastRecording.getLastValue(SIM_PHYSICS_TIME) in ms")
            val scriptsTime: Float   = TODO("GPU: lastRecording.getLastValue(SIM_SCRIPTS_TIME) in ms")
            val netTime: Float       = TODO("GPU: lastRecording.getLastValue(SIM_NET_TIME) in ms")
            val agentsTime: Float    = TODO("GPU: lastRecording.getLastValue(SIM_AGENTS_TIME) in ms")
            val imagesTime: Float    = TODO("GPU: lastRecording.getLastValue(SIM_IMAGES_TIME) in ms")

            when {
                physicsTime > mServerSingleProcessMaxTime ->
                    mServerCause?.setText(getString("server_physics_cause_msg", mStringArgs))
                scriptsTime > mServerSingleProcessMaxTime ->
                    mServerCause?.setText(getString("server_scripts_cause_msg", mStringArgs))
                netTime > mServerSingleProcessMaxTime ->
                    mServerCause?.setText(getString("server_net_cause_msg", mStringArgs))
                agentsTime > mServerSingleProcessMaxTime ->
                    mServerCause?.setText(getString("server_agent_cause_msg", mStringArgs))
                imagesTime > mServerSingleProcessMaxTime ->
                    mServerCause?.setText(getString("server_images_cause_msg", mStringArgs))
                else ->
                    mServerCause?.setText(getString("server_generic_cause_msg", mStringArgs))
            }
        }
    }

    private fun updateControls(shrink: Boolean) {
        val button = getChild<LLButton>("minimize")
        val deltaWidth = mMaxWidth - mMinWidth
        val r = getRect()

        if (!shrink) {
            setTitle(getString("max_title_msg", mStringArgs))
            r.translate(-deltaWidth, 0)
            setRect(r)
            reshape(mMaxWidth, getRect().height)
            getChild<LLUICtrl>("client").setValue(getString("client_text_msg", mStringArgs) + ":")
            getChild<LLUICtrl>("network").setValue(getString("network_text_msg", mStringArgs) + ":")
            getChild<LLUICtrl>("server").setValue(getString("server_text_msg", mStringArgs) + ":")
            button?.setLabel(getString("smaller_label", mStringArgs))
        } else {
            setTitle(getString("min_title_msg", mStringArgs))
            r.translate(deltaWidth, 0)
            setRect(r)
            reshape(mMinWidth, getRect().height)
            getChild<LLUICtrl>("client").setValue(getString("client_text_msg", mStringArgs))
            getChild<LLUICtrl>("network").setValue(getString("network_text_msg", mStringArgs))
            getChild<LLUICtrl>("server").setValue(getString("server_text_msg", mStringArgs))
            button?.setLabel(getString("bigger_label", mStringArgs))
        }
        button?.setFocus(false)
    }

    private fun isShrunk(): Boolean {
        return gSavedSettings.getBool("LagMeterShrunk")
    }

    private fun onClickShrink() {
        val shrunk = isShrunk()
        updateControls(!shrunk)
        gSavedSettings.setBool("LagMeterShrunk", !shrunk)
    }
}
