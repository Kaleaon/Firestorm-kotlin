package com.firestorm.newview

import com.firestorm.llxml.ControlGroup
import com.firestorm.llxml.PersistMode

var hackGodmode: Boolean = false

var lastRunVersion: String = ""

val gSavedSettings: ControlGroup = ControlGroup("Global")
val gSavedPerAccountSettings: ControlGroup = ControlGroup("PerAccount")
val gCrashSettings: ControlGroup = ControlGroup("CrashSettings")
val gWarningSettings: ControlGroup = ControlGroup("Warnings")

object ViewerControl {

    fun toggleShowNavigationPanel(newValue: Boolean): Boolean {
        System.err.println("ViewerControl: update navigation bar visibility not yet implemented")
        return false
    }

    fun toggleShowFavoritesPanel(newValue: Boolean): Boolean {
        System.err.println("ViewerControl: update favorites panel visibility not yet implemented")
        return false
    }

    fun handleSetShaderChanged(newValue: Boolean): Boolean {
        System.err.println("ViewerControl: rebuild shader program set; invalidate bump map cache; call pipeline refreshCachedSettings not yet implemented")
        return false
    }

    fun applyGlobalOnlineStatusChange(notification: Map<String, Any?>, response: Map<String, Any?>) {
        System.err.println("ViewerControl: propagate online-status grant changes to all buddies via HTTP capability not yet implemented")
    }

    fun setupListeners() {
        gSavedSettings.getControl("RenderAvatarMouselook")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: set VOAvatar.sVisibleInFirstPerson = newVal.asBoolean() not yet implemented")
        }

        gSavedSettings.getControl("RenderFarClip")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: update agentCamera drawDistance and world far clip = newVal.asReal().toFloat() not yet implemented")
        }

        gSavedSettings.getControl("RenderTerrainScale")?.addCommitListener { _, newVal, _ ->
            val scale = newVal.asReal()
            if (scale != 0.0) {
                System.err.println("ViewerControl: DrawPoolTerrain.sDetailScale = (1.0 / scale).toFloat() not yet implemented")
            }
        }

        gSavedSettings.getControl("RenderPBRTerrainScale")?.addCommitListener { _, newVal, _ ->
            val scale = newVal.asReal()
            if (scale != 0.0) {
                System.err.println("ViewerControl: DrawPoolTerrain.sPBRDetailScale = (1.0 / scale).toFloat() not yet implemented")
            }
        }

        gSavedSettings.getControl("DebugAvatarJoints")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: LLJoint.setDebugJointNames(newVal.asString()) not yet implemented")
        }

        gSavedSettings.getControl("DebugQualityPerformance")?.addCommitListener { _, newVal, _ ->
            if (gSavedSettings.getU32("RenderQualityPerformance") != newVal.asInt().toUInt()) {
                System.err.println("ViewerControl: sync RenderQualityPerformance to newVal and notify FloaterPreference.onChangeQuality not yet implemented")
            }
        }

        gSavedSettings.getControl("AvatarHoverOffsetZ")?.addCommitListener { _, _, _ ->
            System.err.println("ViewerControl: gAgentAvatarp.setHoverIfRegionEnabled() not yet implemented")
        }

        gSavedSettings.getControl("RenderTransparentWater")?.addCommitListener { _, _, _ ->
            System.err.println("ViewerControl: pipeline.updateRenderTransparentWater(); releaseGLBuffers(); createGLBuffers(); setShaders(); updateWaterObjects() not yet implemented")
        }

        gSavedSettings.getControl("RenderShadowResolutionScale")?.addCommitListener { _, _, _ ->
            System.err.println("ViewerControl: pipeline.requestResizeShadowTexture() not yet implemented")
        }

        gSavedSettings.getControl("RenderHiDPI")?.addCommitListener { _, _, _ ->
            System.err.println("ViewerControl: pipeline.requestResizeScreenTexture() not yet implemented")
        }

        gSavedSettings.getControl("RenderDeferredSSAO")?.addCommitListener { _, _, _ ->
            System.err.println("ViewerControl: pipeline.releaseGLBuffers(); createGLBuffers() not yet implemented")
        }

        gSavedSettings.getControl("RenderEnableEmissiveBuffer")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: releaseGLBuffers/createGLBuffers + setShaders not yet implemented")
        }

        gSavedSettings.getControl("RenderHDREnabled")?.addCommitListener { _, _, _ ->
            System.err.println("ViewerControl: reflectionMapManager.reset(); heroProbeManager.reset(); releaseGLBuffers; createGLBuffers; setShaders not yet implemented")
        }

        gSavedSettings.getControl("RenderEnableVintageMode")?.addCommitListener { _, newVal, _ ->
            gSavedSettings.setBool("RenderEnableEmissiveBuffer", newVal.asBoolean())
            gSavedSettings.setBool("RenderHDREnabled", newVal.asBoolean())
        }

        gSavedSettings.getControl("RenderAnisotropic")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: LLImageGL.sGlobalUseAnisotropic = newVal.asBoolean(); dirtyTexOptions() not yet implemented")
        }

        gSavedSettings.getControl("RenderVSyncEnable")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: LLPerfStats.tunables.vsyncEnabled = newVal.asBoolean(); window.toggleVSync(); clamp TargetFPS to refresh rate not yet implemented")
        }

        gSavedSettings.getControl("RenderVolumeLODFactor")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: VOVolume.sLODFactor = clamp(newVal.asReal().toFloat(), 0.01f, MAX_LOD_FACTOR); sDistanceFactor = 1 - sLODFactor * 0.1f not yet implemented")
        }

        gSavedSettings.getControl("RenderAvatarLODFactor")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: VOAvatar.sLODFactor = clamp(newVal.asReal().toFloat(), 0f, MAX_AVATAR_LOD_FACTOR) not yet implemented")
        }

        gSavedSettings.getControl("RenderAvatarPhysicsLODFactor")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: VOAvatar.sPhysicsLODFactor = clamp(newVal.asReal().toFloat(), 0f, MAX_AVATAR_LOD_FACTOR) not yet implemented")
        }

        gSavedSettings.getControl("RenderTerrainLODFactor")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: VOSurfacePatch.sLODFactor = newVal.asReal().toFloat() squared not yet implemented")
        }

        gSavedSettings.getControl("RenderTreeLODFactor")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: VOTree.sTreeFactor = newVal.asReal().toFloat() not yet implemented")
        }

        gSavedSettings.getControl("RenderFlexTimeFactor")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: VolumeImplFlexible.sUpdateFactor = newVal.asReal().toFloat() not yet implemented")
        }

        gSavedSettings.getControl("RenderGamma")?.addCommitListener { _, newVal, _ ->
            var gamma = newVal.asReal().toFloat()
            if (gamma == 0f) gamma = 1f
            System.err.println("ViewerControl: viewerWindow.window.setGamma(gamma) not yet implemented")
        }

        gSavedSettings.getControl("RenderFogRatio")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: gSky.setFogRatio(clamp(newVal.asReal().toFloat(), MIN_USER_FOG_RATIO, MAX_USER_FOG_RATIO)) not yet implemented")
        }

        gSavedSettings.getControl("RenderMaxPartCount")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: ViewerPartSim.setMaxPartCount(newVal.asInt()) not yet implemented")
        }

        gSavedSettings.getControl("ChatFontSize")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: gConsole.setFontSize(newVal.asInt()) not yet implemented")
        }

        gSavedSettings.getControl("ChatPersistTime")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: gConsole.setLinePersistTime(newVal.asReal().toFloat()) not yet implemented")
        }

        gSavedSettings.getControl("ConsoleMaxLines")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: gConsole.setMaxLines(newVal.asInt()) not yet implemented")
        }

        gSavedSettings.getControl("AudioLevelMaster")?.addCommitListener { _, _, _ ->
            System.err.println("ViewerControl: audio_update_volume(true) not yet implemented")
        }

        gSavedSettings.getControl("JoystickEnabled")?.addCommitListener { _, _, _ ->
            System.err.println("ViewerControl: ViewerJoystick.getInstance().setCameraNeedsUpdate(true) not yet implemented")
        }

        gSavedSettings.getControl("UseOcclusion")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: Pipeline.sUseOcclusion = if (newVal.asBoolean() && featureAvailable && !wireframe) 2 else 0 not yet implemented")
        }

        gSavedSettings.getControl("RenderUseStreamVBO")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: Pipeline.sForceOldBakedUpload = newVal.asBoolean() not yet implemented")
        }

        gSavedSettings.getControl("WLSkyDetail")?.addCommitListener { _, _, _ ->
            System.err.println("ViewerControl: gSky.mVOWLSkyp.updateGeometry(drawable) not yet implemented")
        }

        gSavedSettings.getControl("OctreeMaxNodeCapacity")?.addCommitListener { _, _, _ ->
            System.err.println("ViewerControl: re-partition octree with new capacity not yet implemented")
        }

        gSavedSettings.getControl("RenderDynamicLOD")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: Pipeline.sDynamicLOD = newVal.asBoolean() not yet implemented")
        }

        gSavedSettings.getControl("RenderReflectionProbeDetail")?.addCommitListener { _, _, _ ->
            System.err.println("ViewerControl: reflectionMapManager.refreshSettings(); reset(); release/create GL buffers; setShaders() not yet implemented")
        }

        gSavedSettings.getControl("RenderReflectionProbeCount")?.addCommitListener { _, _, _ ->
            System.err.println("ViewerControl: reflectionMapManager.refreshSettings() not yet implemented")
        }

        gSavedSettings.getControl("RenderHeroProbeResolution")?.addCommitListener { _, _, _ ->
            System.err.println("ViewerControl: heroProbeManager.reset(); release/create GL buffers not yet implemented")
        }

        gSavedSettings.getControl("RenderDebugPipeline")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: gDebugPipeline = newVal.asBoolean() not yet implemented")
        }

        gSavedSettings.getControl("RenderResolutionDivisor")?.addCommitListener { _, _, _ ->
            System.err.println("ViewerControl: gResizeScreenTexture = true not yet implemented")
        }

        gSavedSettings.getControl("DebugViews")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: LLView.sDebugRects = newVal.asBoolean() not yet implemented")
        }

        gSavedSettings.getControl("LogFile")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: reroute log output to newVal.asString() not yet implemented")
        }

        gSavedSettings.getControl("HideGroupTitle")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: gAgent.setHideGroupTitle(newVal) not yet implemented")
        }

        gSavedSettings.getControl("EffectColor")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: gAgent.setEffectColor(Color4(newVal)) not yet implemented")
        }

        gSavedSettings.getControl("HighResSnapshot")?.addCommitListener { _, newVal, _ ->
            if (newVal.asBoolean()) gSavedSettings.setBool("RenderUIInSnapshot", false)
        }

        gSavedSettings.getControl("EnableVoiceChat")?.addCommitListener { _, _, _ ->
            System.err.println("ViewerControl: VoiceClient.getInstance().updateSettings() not yet implemented")
        }

        gSavedSettings.getControl("VelocityInterpolate")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: send VelocityInterpolateOn/Off UDP message not yet implemented")
        }

        gSavedSettings.getControl("ForceShowGrid")?.addCommitListener { _, _, _ ->
            System.err.println("ViewerControl: FSPanelLogin.updateLocationSelectorsVisibility() not yet implemented")
        }

        gSavedSettings.getControl("LoginLocation")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: LLStartUp.setStartSLURL(LLSLURL(newVal.asString())) not yet implemented")
        }

        gSavedSettings.getControl("SpellCheck")?.addCommitListener { _, _, _ ->
            System.err.println("ViewerControl: reinitialise spell checker with current SpellCheckDictionary setting not yet implemented")
        }

        gSavedSettings.getControl("ShowNavbarNavigationPanel")?.addCommitListener { _, newVal, _ ->
            gSavedSettings.setBool("FSInternalShowNavbarNavigationPanel", newVal.asBoolean())
        }

        gSavedSettings.getControl("ShowNavbarFavoritesPanel")?.addCommitListener { _, newVal, _ ->
            gSavedSettings.setBool("FSInternalShowNavbarFavoritesPanel", newVal.asBoolean())
        }

        gSavedSettings.getControl("NaclAntiSpamGlobalQueue")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: NACLAntiSpamRegistry.instance().setGlobalQueue(newVal.asBoolean()) not yet implemented")
        }

        gSavedSettings.getControl("NaclAntiSpamTime")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: NACLAntiSpamRegistry.instance().setAllQueueTimes(newVal.asInt()) not yet implemented")
        }

        gSavedSettings.getControl("NaclAntiSpamAmount")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: NACLAntiSpamRegistry.instance().setAllQueueAmounts(newVal.asInt()) not yet implemented")
        }

        gSavedPerAccountSettings.getControl("GlobalOnlineStatusToggle")?.addCommitListener { _, newVal, _ ->
            System.err.println("ViewerControl: show ConfirmGlobalOnlineStatusToggle notification not yet implemented")
        }
    }

    fun createGraphicsGroup(group: ControlGroup) {
        group.declareF32("RenderVolumeLODFactor",    1.0f,  "Volume LOD factor")
        group.declareS32("RenderMaxTextureIndex",    16,    "Max texture index for object rendering")
        group.declareBool("RenderVBOEnable",         true,  "Enable vertex buffer objects")
        group.declareS32("RenderGlowResolutionPow",  9,     "Glow buffer resolution (2^n)")
        group.declareF32("RenderGlowStrength",       0.35f, "Glow post-process strength")
        group.declareBool("RenderGlow",              true,  "Enable glow effect")
        group.declareS32("RenderShadowDetail",       2,     "Shadow quality level")
        group.declareBool("RenderDeferredSSAO",      true,  "Enable SSAO in deferred pipeline")
        group.declareBool("RenderDepthOfField",      false, "Enable depth-of-field post-process")
    }
}
