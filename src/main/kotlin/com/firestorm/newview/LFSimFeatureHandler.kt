package com.firestorm.newview

import java.util.UUID

enum class ExportSupport {
    EXPORT_UNDEFINED,
    EXPORT_ALLOWED,
    EXPORT_DENIED
}

class SignaledType<T>(initial: T) {

    private var value: T = initial
    private val listeners: MutableList<(T) -> Unit> = mutableListOf()

    fun connect(slot: (T) -> Unit): () -> Unit {
        listeners.add(slot)
        return { listeners.remove(slot) }
    }

    fun get(): T = value

    fun set(newValue: T) {
        if (newValue != value) {
            value = newValue
            listeners.forEach { it(value) }
        }
    }

    operator fun invoke(): T = value
}

object LFSimFeatureHandler {

    private val supportsExport = SignaledType(false)
    private var mapServerURL: String = ""
    private var gridStatusURL: String = ""
    private var gridStatusRSS: String = ""
    private var hyperGridPrefix: String = ""
    private val searchURL = SignaledType("")
    private val sayRange = SignaledType(20u)
    private val shoutRange = SignaledType(100u)
    private val whisperRange = SignaledType(10u)
    private val avatarPickerURL = SignaledType("")
    private val destinationGuideURL = SignaledType("")
    private var simulatorFPS: Float = 45f
    private var simulatorFPSFactor: Float = 1f
    private var simulatorFPSWarn: Float = 30f
    private var simulatorFPSCrit: Float = 20f
    private var hasAvatarPicker: Boolean = false
    private var hasDestinationGuide: Boolean = false
    private var helperUriOverride: String = ""
    private var currencySymbolOverride: String = ""

    init {
        System.err.println("LFSimFeatureHandler: init not yet implemented")
    }

    fun exportPolicy(): ExportSupport {
        return ExportSupport.EXPORT_UNDEFINED
    }

    fun handleRegionChange() {
        System.err.println("LFSimFeatureHandler: handleRegionChange not yet implemented")
    }

    fun onSimulatorFeaturesReceived(regionId: UUID) {
        System.err.println("LFSimFeatureHandler: onSimulatorFeaturesReceived not yet implemented")
    }

    fun setSupportedFeatures() {
        System.err.println("LFSimFeatureHandler: setSupportedFeatures not yet implemented")
    }

    fun updateCurrencySymbols() {
        System.err.println("LFSimFeatureHandler: updateCurrencySymbols not yet implemented")
    }

    fun simSupportsExport(): Boolean = supportsExport.get()
    fun mapServerURL(): String = mapServerURL
    fun gridStatusURL(): String = gridStatusURL
    fun gridStatusRSS(): String = gridStatusRSS
    fun hyperGridURL(): String = hyperGridPrefix
    fun searchURL(): String = searchURL.get()
    fun sayRange(): UInt = sayRange.get()
    fun shoutRange(): UInt = shoutRange.get()
    fun whisperRange(): UInt = whisperRange.get()
    fun avatarPickerURL(): String = avatarPickerURL.get()
    fun destinationGuideURL(): String = destinationGuideURL.get()
    fun simulatorFPS(): Float = simulatorFPS
    fun simulatorFPSFactor(): Float = simulatorFPSFactor
    fun simulatorFPSWarn(): Float = simulatorFPSWarn
    fun simulatorFPSCrit(): Float = simulatorFPSCrit
    fun hasAvatarPicker(): Boolean = hasAvatarPicker
    fun hasDestinationGuide(): Boolean = hasDestinationGuide
    fun helperUriOverride(): String = helperUriOverride
    fun currencySymbolOverride(): String = currencySymbolOverride

    fun setSupportsExportCallback(slot: (Boolean) -> Unit): () -> Unit =
        supportsExport.connect(slot)

    fun setSearchURLCallback(slot: (String) -> Unit): () -> Unit =
        searchURL.connect(slot)

    fun setSayRangeCallback(slot: (UInt) -> Unit): () -> Unit =
        sayRange.connect(slot)

    fun setShoutRangeCallback(slot: (UInt) -> Unit): () -> Unit =
        shoutRange.connect(slot)

    fun setWhisperRangeCallback(slot: (UInt) -> Unit): () -> Unit =
        whisperRange.connect(slot)

    fun setAvatarPickerCallback(slot: (String) -> Unit): () -> Unit =
        avatarPickerURL.connect(slot)

    fun setDestinationGuideCallback(slot: (String) -> Unit): () -> Unit =
        destinationGuideURL.connect(slot)
}
