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
        TODO("GPU: update navigation bar visibility")
    }

    fun toggleShowFavoritesPanel(newValue: Boolean): Boolean {
        TODO("GPU: update favorites panel visibility")
    }

    fun handleSetShaderChanged(newValue: Boolean): Boolean {
        TODO("GPU: rebuild shader program set; invalidate bump map cache; call pipeline refreshCachedSettings")
    }

    fun applyGlobalOnlineStatusChange(notification: Map<String, Any?>, response: Map<String, Any?>) {
        TODO("APR: use JVM equivalent — propagate online-status grant changes to all buddies via HTTP capability")
    }

    fun setupListeners() {
        gSavedSettings.getControl("RenderAvatarMouselook")?.addCommitListener { _, newVal, _ ->
            TODO("GPU: set VOAvatar.sVisibleInFirstPerson = newVal.asBoolean()")
        }

        gSavedSettings.getControl("RenderFarClip")?.addCommitListener { _, newVal, _ ->
            TODO("GPU: update agentCamera drawDistance and world far clip = newVal.asReal().toFloat()")
        }

        gSavedSettings.getControl("RenderTerrainScale")?.addCommitListener { _, newVal, _ ->
            val scale = newVal.asReal()
            if (scale != 0.0) {
                TODO("GPU: DrawPoolTerrain.sDetailScale = (1.0 / scale).toFloat()")
            }
        }

        gSavedSettings.getControl("RenderPBRTerrainScale")?.addCommitListener { _, newVal, _ ->
            val scale = newVal.asReal()
            if (scale != 0.0) {
                TODO("GPU: DrawPoolTerrain.sPBRDetailScale = (1.0 / scale).toFloat()")
            }
        }

        gSavedSettings.getControl("DebugAvatarJoints")?.addCommitListener { _, newVal, _ ->
            TODO("GPU: LLJoint.setDebugJointNames(newVal.asString())")
        }

        gSavedSettings.getControl("DebugQualityPerformance")?.addCommitListener { _, newVal, _ ->
            if (gSavedSettings.getU32("RenderQualityPerformance") != newVal.asInt().toUInt()) {
                TODO("GPU: sync RenderQualityPerformance to newVal and notify FloaterPreference.onChangeQuality")
            }
        }

        gSavedSettings.getControl("AvatarHoverOffsetZ")?.addCommitListener { _, _, _ ->
            TODO("GPU: gAgentAvatarp.setHoverIfRegionEnabled()")
        }

        gSavedSettings.getControl("RenderTransparentWater")?.addCommitListener { _, _, _ ->
            TODO("GPU: pipeline.updateRenderTransparentWater(); releaseGLBuffers(); createGLBuffers(); setShaders(); updateWaterObjects()")
        }

        gSavedSettings.getControl("RenderShadowResolutionScale")?.addCommitListener { _, _, _ ->
            TODO("GPU: pipeline.requestResizeShadowTexture()")
        }

        gSavedSettings.getControl("RenderHiDPI")?.addCommitListener { _, _, _ ->
            TODO("GPU: pipeline.requestResizeScreenTexture()")
        }

        gSavedSettings.getControl("RenderDeferredSSAO")?.addCommitListener { _, _, _ ->
            TODO("GPU: pipeline.releaseGLBuffers(); createGLBuffers()")
        }

        gSavedSettings.getControl("RenderEnableEmissiveBuffer")?.addCommitListener { _, newVal, _ ->
            TODO("GPU: releaseGLBuffers/createGLBuffers + setShaders")
        }

        gSavedSettings.getControl("RenderHDREnabled")?.addCommitListener { _, _, _ ->
            TODO("GPU: reflectionMapManager.reset(); heroProbeManager.reset(); releaseGLBuffers; createGLBuffers; setShaders")
        }

        gSavedSettings.getControl("RenderEnableVintageMode")?.addCommitListener { _, newVal, _ ->
            gSavedSettings.setBool("RenderEnableEmissiveBuffer", newVal.asBoolean())
            gSavedSettings.setBool("RenderHDREnabled", newVal.asBoolean())
        }

        gSavedSettings.getControl("RenderAnisotropic")?.addCommitListener { _, newVal, _ ->
            TODO("GPU: LLImageGL.sGlobalUseAnisotropic = newVal.asBoolean(); dirtyTexOptions()")
        }

        gSavedSettings.getControl("RenderVSyncEnable")?.addCommitListener { _, newVal, _ ->
            TODO("GPU: LLPerfStats.tunables.vsyncEnabled = newVal.asBoolean(); window.toggleVSync(); clamp TargetFPS to refresh rate")
        }

        gSavedSettings.getControl("RenderVolumeLODFactor")?.addCommitListener { _, newVal, _ ->
            TODO("GPU: VOVolume.sLODFactor = clamp(newVal.asReal().toFloat(), 0.01f, MAX_LOD_FACTOR); sDistanceFactor = 1 - sLODFactor * 0.1f")
        }

        gSavedSettings.getControl("RenderAvatarLODFactor")?.addCommitListener { _, newVal, _ ->
            TODO("GPU: VOAvatar.sLODFactor = clamp(newVal.asReal().toFloat(), 0f, MAX_AVATAR_LOD_FACTOR)")
        }

        gSavedSettings.getControl("RenderAvatarPhysicsLODFactor")?.addCommitListener { _, newVal, _ ->
            TODO("GPU: VOAvatar.sPhysicsLODFactor = clamp(newVal.asReal().toFloat(), 0f, MAX_AVATAR_LOD_FACTOR)")
        }

        gSavedSettings.getControl("RenderTerrainLODFactor")?.addCommitListener { _, newVal, _ ->
            TODO("GPU: VOSurfacePatch.sLODFactor = newVal.asReal().toFloat() squared")
        }

        gSavedSettings.getControl("RenderTreeLODFactor")?.addCommitListener { _, newVal, _ ->
            TODO("GPU: VOTree.sTreeFactor = newVal.asReal().toFloat()")
        }

        gSavedSettings.getControl("RenderFlexTimeFactor")?.addCommitListener { _, newVal, _ ->
            TODO("GPU: VolumeImplFlexible.sUpdateFactor = newVal.asReal().toFloat()")
        }

        gSavedSettings.getControl("RenderGamma")?.addCommitListener { _, newVal, _ ->
            var gamma = newVal.asReal().toFloat()
            if (gamma == 0f) gamma = 1f
            TODO("GPU: viewerWindow.window.setGamma(gamma)")
        }

        gSavedSettings.getControl("RenderFogRatio")?.addCommitListener { _, newVal, _ ->
            TODO("GPU: gSky.setFogRatio(clamp(newVal.asReal().toFloat(), MIN_USER_FOG_RATIO, MAX_USER_FOG_RATIO))")
        }

        gSavedSettings.getControl("RenderMaxPartCount")?.addCommitListener { _, newVal, _ ->
            TODO("GPU: ViewerPartSim.setMaxPartCount(newVal.asInt())")
        }

        gSavedSettings.getControl("ChatFontSize")?.addCommitListener { _, newVal, _ ->
            TODO("APR: use JVM equivalent — gConsole.setFontSize(newVal.asInt())")
        }

        gSavedSettings.getControl("ChatPersistTime")?.addCommitListener { _, newVal, _ ->
            TODO("APR: use JVM equivalent — gConsole.setLinePersistTime(newVal.asReal().toFloat())")
        }

        gSavedSettings.getControl("ConsoleMaxLines")?.addCommitListener { _, newVal, _ ->
            TODO("APR: use JVM equivalent — gConsole.setMaxLines(newVal.asInt())")
        }

        gSavedSettings.getControl("AudioLevelMaster")?.addCommitListener { _, _, _ ->
            TODO("APR: use JVM equivalent — audio_update_volume(true)")
        }

        gSavedSettings.getControl("JoystickEnabled")?.addCommitListener { _, _, _ ->
            TODO("APR: use JVM equivalent — ViewerJoystick.getInstance().setCameraNeedsUpdate(true)")
        }

        gSavedSettings.getControl("UseOcclusion")?.addCommitListener { _, newVal, _ ->
            TODO("GPU: Pipeline.sUseOcclusion = if (newVal.asBoolean() && featureAvailable && !wireframe) 2 else 0")
        }

        gSavedSettings.getControl("RenderUseStreamVBO")?.addCommitListener { _, newVal, _ ->
            TODO("GPU: Pipeline.sForceOldBakedUpload = newVal.asBoolean()")
        }

        gSavedSettings.getControl("WLSkyDetail")?.addCommitListener { _, _, _ ->
            TODO("GPU: gSky.mVOWLSkyp.updateGeometry(drawable)")
        }

        gSavedSettings.getControl("OctreeMaxNodeCapacity")?.addCommitListener { _, _, _ ->
            TODO("GPU: re-partition octree with new capacity")
        }

        gSavedSettings.getControl("RenderDynamicLOD")?.addCommitListener { _, newVal, _ ->
            TODO("GPU: Pipeline.sDynamicLOD = newVal.asBoolean()")
        }

        gSavedSettings.getControl("RenderReflectionProbeDetail")?.addCommitListener { _, _, _ ->
            TODO("GPU: reflectionMapManager.refreshSettings(); reset(); release/create GL buffers; setShaders()")
        }

        gSavedSettings.getControl("RenderReflectionProbeCount")?.addCommitListener { _, _, _ ->
            TODO("GPU: reflectionMapManager.refreshSettings()")
        }

        gSavedSettings.getControl("RenderHeroProbeResolution")?.addCommitListener { _, _, _ ->
            TODO("GPU: heroProbeManager.reset(); release/create GL buffers")
        }

        gSavedSettings.getControl("RenderDebugPipeline")?.addCommitListener { _, newVal, _ ->
            TODO("GPU: gDebugPipeline = newVal.asBoolean()")
        }

        gSavedSettings.getControl("RenderResolutionDivisor")?.addCommitListener { _, _, _ ->
            TODO("GPU: gResizeScreenTexture = true")
        }

        gSavedSettings.getControl("DebugViews")?.addCommitListener { _, newVal, _ ->
            TODO("GPU: LLView.sDebugRects = newVal.asBoolean()")
        }

        gSavedSettings.getControl("LogFile")?.addCommitListener { _, newVal, _ ->
            TODO("APR: use JVM equivalent — reroute log output to newVal.asString()")
        }

        gSavedSettings.getControl("HideGroupTitle")?.addCommitListener { _, newVal, _ ->
            TODO("APR: use JVM equivalent — gAgent.setHideGroupTitle(newVal)")
        }

        gSavedSettings.getControl("EffectColor")?.addCommitListener { _, newVal, _ ->
            TODO("APR: use JVM equivalent — gAgent.setEffectColor(Color4(newVal))")
        }

        gSavedSettings.getControl("HighResSnapshot")?.addCommitListener { _, newVal, _ ->
            if (newVal.asBoolean()) gSavedSettings.setBool("RenderUIInSnapshot", false)
        }

        gSavedSettings.getControl("EnableVoiceChat")?.addCommitListener { _, _, _ ->
            TODO("APR: use JVM equivalent — VoiceClient.getInstance().updateSettings()")
        }

        gSavedSettings.getControl("VelocityInterpolate")?.addCommitListener { _, newVal, _ ->
            TODO("APR: use JVM equivalent — send VelocityInterpolateOn/Off UDP message")
        }

        gSavedSettings.getControl("ForceShowGrid")?.addCommitListener { _, _, _ ->
            TODO("APR: use JVM equivalent — FSPanelLogin.updateLocationSelectorsVisibility()")
        }

        gSavedSettings.getControl("LoginLocation")?.addCommitListener { _, newVal, _ ->
            TODO("APR: use JVM equivalent — LLStartUp.setStartSLURL(LLSLURL(newVal.asString()))")
        }

        gSavedSettings.getControl("SpellCheck")?.addCommitListener { _, _, _ ->
            TODO("APR: use JVM equivalent — reinitialise spell checker with current SpellCheckDictionary setting")
        }

        gSavedSettings.getControl("ShowNavbarNavigationPanel")?.addCommitListener { _, newVal, _ ->
            gSavedSettings.setBool("FSInternalShowNavbarNavigationPanel", newVal.asBoolean())
        }

        gSavedSettings.getControl("ShowNavbarFavoritesPanel")?.addCommitListener { _, newVal, _ ->
            gSavedSettings.setBool("FSInternalShowNavbarFavoritesPanel", newVal.asBoolean())
        }

        gSavedSettings.getControl("NaclAntiSpamGlobalQueue")?.addCommitListener { _, newVal, _ ->
            TODO("APR: use JVM equivalent — NACLAntiSpamRegistry.instance().setGlobalQueue(newVal.asBoolean())")
        }

        gSavedSettings.getControl("NaclAntiSpamTime")?.addCommitListener { _, newVal, _ ->
            TODO("APR: use JVM equivalent — NACLAntiSpamRegistry.instance().setAllQueueTimes(newVal.asInt())")
        }

        gSavedSettings.getControl("NaclAntiSpamAmount")?.addCommitListener { _, newVal, _ ->
            TODO("APR: use JVM equivalent — NACLAntiSpamRegistry.instance().setAllQueueAmounts(newVal.asInt())")
        }

        gSavedPerAccountSettings.getControl("GlobalOnlineStatusToggle")?.addCommitListener { _, newVal, _ ->
            TODO("APR: use JVM equivalent — show ConfirmGlobalOnlineStatusToggle notification")
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
