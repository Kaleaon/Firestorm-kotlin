package com.firestorm.newview

import com.firestorm.llcharacter.MotionController
import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Vector3
import com.firestorm.llmath.Vector3d
import com.firestorm.llmath.Vector4
import com.firestorm.llmessage.AvatarName
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Agent animation UUIDs (mirrors llvoavatar.cpp global constants)
// ---------------------------------------------------------------------------

object AgentAnims {
    val BODY_NOISE       = LLUUID("9aa8b0a6-0c6f-9518-c7c3-4f41f2c001ad")
    val BREATHE_ROT      = LLUUID("4c5a103e-b830-2f1c-16bc-224aa0ad5bc8")
    val PHYSICS_MOTION   = LLUUID("7360e029-3cb8-ebc4-863e-212df440d987")
    val EDITING          = LLUUID("2a8eba1d-a7f8-5596-d44a-b4977bf8c8bb")
    val EYE              = LLUUID("5c780ea8-1cd1-c463-a128-48c023f6fbea")
    val FLY_ADJUST       = LLUUID("db95561f-f1b0-9f9a-7224-b12f71af126e")
    val HAND_MOTION      = LLUUID("ce986325-0ba7-6e6e-cc24-b17c4b795578")
    val HEAD_ROT         = LLUUID("e6e8d1dd-e643-fff7-b238-c6b4b056a68d")
    val PELVIS_FIX       = LLUUID("0c5dd2a2-514d-8893-d44d-05beffad208b")
    val TARGET           = LLUUID("0e4896cb-fba4-926c-f355-8720189d5b55")
    val WALK_ADJUST      = LLUUID("829bc85b-02fc-ec41-be2e-74cc6dd7215d")
}

// ---------------------------------------------------------------------------
// Physics constants (from llvoavatar.cpp)
// ---------------------------------------------------------------------------

private const val DELTA_TIME_MIN             = 0.01f
private const val DELTA_TIME_MAX             = 0.2f
private const val PELVIS_LAG_FLYING          = 0.22f
private const val PELVIS_LAG_WALKING         = 0.4f
private const val PELVIS_LAG_MOUSELOOK       = 0.15f
private const val TORSO_NOISE_AMOUNT         = 1.0f
private const val TORSO_NOISE_SPEED          = 0.2f
private const val BREATHE_ROT_STRENGTH       = 0.05f
private const val APPEARANCE_MORPH_TIME      = 0.65f
private const val CHAT_FADE_TIME             = 8.0f
private const val BUBBLE_CHAT_TIME           = CHAT_FADE_TIME * 3.0f
private const val MAX_AVATAR_LOD_FACTOR      = 1.0f
private const val MAX_HOVER_Z                = 3.0f
private const val MIN_HOVER_Z                = -3.0f
private const val FIRST_CLOUD_MIN_DELAY      = 3.0f
private const val FIRST_CLOUD_MAX_DELAY      = 15.0f
private const val TEX_IMAGE_SIZE_OTHER       = 128    // 512/4

// ---------------------------------------------------------------------------
// Enumerations
// ---------------------------------------------------------------------------

enum class VisualMuteSettings {
    AV_RENDER_NORMALLY,
    AV_DO_NOT_RENDER,
    AV_ALWAYS_RENDER
}

enum class AvatarOverallAppearance {
    NORMAL,
    JELLYDOLL,
    INVISIBLE
}

enum class RenderComplexityMode {
    LIMIT_BY_COMPLEXITY,
    ALWAYS_SHOW_FRIENDS,
    ONLY_SHOW_FRIENDS
}

enum class RenderName {
    NEVER,
    ALWAYS,
    FADE
}

// ---------------------------------------------------------------------------
// LLVOAvatar → VOAvatar
// ---------------------------------------------------------------------------

open class VOAvatar(id: LLUUID, localId: UInt, regionHandle: ULong = 0uL)
    : ViewerObject(id, localId, 0u) {

    // ---- name / display -------------------------------------------------------

    var avatarName: AvatarName? = null
    open var isSelf: Boolean = false

    private var nameIsSet: Boolean = false
    var nameFirstname: String = ""
    var nameLastname: String = ""
    var title: String = ""
    var nameAlpha: Float = 0.0f
    var nameAway: Boolean = false
    var nameDoNotDisturb: Boolean = false
    var nameAutoResponse: Boolean = false
    var nameIsTyping: Boolean = false
    var nameMute: Boolean = false
    var nameAppearance: Boolean = false
    var nameFriend: Boolean = false
    var nameCloud: Boolean = false
    var renderGroupTitles: Boolean = true
    var distanceString: String = ""

    // Firestorm: ARC shown in nametag for jelly-dolled avatars
    var nameArc: UInt = 0u
    var nameArcColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)  // RGBA white

    // ---- state flags ----------------------------------------------------------

    var isSitting: Boolean = false
        private set
    var inAir: Boolean = false
    var isTyping: Boolean = false
        private set
    var belowWater: Boolean = false

    var isControlAvatar: Boolean = false
    var isUIAvatar: Boolean = false
    var enableDefaultMotions: Boolean = true

    // ---- rendering / complexity -----------------------------------------------

    var overallAppearance: AvatarOverallAppearance = AvatarOverallAppearance.INVISIBLE
        private set

    private var visuallyMuteSetting: VisualMuteSettings = VisualMuteSettings.AV_RENDER_NORMALLY
    private var visualComplexity: UInt = VISUAL_COMPLEXITY_UNKNOWN
    private var visualComplexityStale: Boolean = true
    var reportedVisualComplexity: UInt = VISUAL_COMPLEXITY_UNKNOWN
    var attachmentSurfaceArea: Float = 0.0f
    var attachmentVisibleTriangleCount: UInt = 0u
    var attachmentEstTriangleCount: Float = 0.0f

    var specialRenderMode: Int = 0
    private var needsSkin: Boolean = false
    private var lastSkinTime: Float = 0.0f

    private var gpuRenderTime: Float = 0.0f
    private var cpuRenderTime: Float = 0.0f
    private var gpuProfilePending: Boolean = false

    var mutedAvColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)  // RGBA white

    // ---- loading state --------------------------------------------------------

    var fullyLoaded: Boolean = false
        private set
    private var previousFullyLoaded: Boolean = false
    private var fullyLoadedInitialized: Boolean = false
    private var fullyLoadedFrameCounter: Int = 0
    var firstFullyVisible: Boolean = true
    var waitingForMeshes: Boolean = false
    var firstDecloudTime: Float = -1.0f

    private var tooSlow: Boolean = false
    private var tooSlowWithoutShadows: Boolean = false

    var lastRezzedStatus: Int = -1
    var lastUpdateRequestCOFVersion: Int = -1
    var lastUpdateReceivedCOFVersion: Int = -1

    // ---- impostor -------------------------------------------------------------

    var needsImpostorUpdate: Boolean = true
    var lastImpostorUpdateReason: Int = 0
    var lastImpostorUpdateFrameTime: Float = 0.0f
    var isAnimesh: Boolean = false
    private var impostorOffset: Vector3 = Vector3.ZERO
    private var impostorDim: Vector3 = Vector3.ZERO     // x,y used
    private var impostorAngle: Vector3 = Vector3.ZERO
    private var impostorDistance: Float = 0.0f
    private var impostorPixelArea: Float = 0.0f
    private var lastAnimExtents: Array<Vector3> = arrayOf(Vector3.ZERO, Vector3.ZERO)
    private var lastAnimBasePos: Vector3 = Vector3.ZERO
    private var needsExtentUpdate: Boolean = true
    private var needsAnimUpdate: Boolean = true

    // ---- skeleton / pose ------------------------------------------------------

    val skeleton: MutableList<Any?> = mutableListOf()   // LLJoint equivalents
    var lastSkeletonSerialNum: Int = 0
    var curRootToHeadOffset: Vector3 = Vector3.ZERO
    var targetRootToHeadOffset: Vector3 = Vector3.ZERO

    // ---- animations -----------------------------------------------------------

    val signaledAnimations: MutableMap<LLUUID, Int> = mutableMapOf()
    val playingAnimations:  MutableMap<LLUUID, Int> = mutableMapOf()
    val animationSources:   MutableMap<LLUUID, MutableList<LLUUID>> = mutableMapOf()

    val motionController: MotionController = MotionController()

    private var timeLast: Float = 0.0f
    private var speed: Float = 0.0f
    private var speedAccum: Float = 0.0f
    private var turning: Boolean = false

    // ---- attachment points ----------------------------------------------------

    val attachmentPoints: MutableMap<Int, Any?> = mutableMapOf()    // S32 → ViewerJointAttachment
    val pendingAttachment: MutableList<ViewerObject> = mutableListOf()
    val simAttachments: MutableMap<LLUUID, Int> = mutableMapOf()
    var lastCloudAttachmentCount: Int = -1

    // ---- physics / wind -------------------------------------------------------

    var windVec: Vector4 = Vector4.ZERO
    var ripplePhase: Float = 0.0f
    var footPlane: Vector4 = Vector4.ZERO
    private var windFreq: Float = 0.0f
    private var rippleTimeLast: Float = 0.0f
    private var rippleAccel: Vector3 = Vector3.ZERO
    private var lastVel: Vector3 = Vector3.ZERO

    // ---- visibility -----------------------------------------------------------

    private var visible: Boolean = false
    private var culled: Boolean = false
    var visibilityRank: UInt = 0u
    private var visibilityPreference: Float = 0.0f

    // ---- appearance morphing --------------------------------------------------

    var appearanceAnimating: Boolean = false
    private var lastAppearanceBlendTime: Float = 0.0f
    var isEditingAppearance: Boolean = false
    var useLocalAppearance: Boolean = false
    var useServerBakes: Boolean = false

    // ---- meshes ---------------------------------------------------------------

    var dirtyMesh: Int = 2   // 0=clean, 1=morphed, 2=LOD
    var meshTexturesDirty: Boolean = false
    var meshValid: Boolean = false
    private var updatePeriod: Int = 1

    // ---- chat / lip sync ------------------------------------------------------

    var visibleChat: Boolean = false
    var visibleTyping: Boolean = false
    var lipSyncActive: Boolean = false
    var currentGesticulationLevel: Int = 0

    private var stepOnLand: Boolean = true
    private var stepMaterial: UByte = 0u
    private var stepObjectVelocity: Vector3 = Vector3.ZERO

    private var inAirTime: Long = 0L   // ms since entered air

    // ---- mutable caches -------------------------------------------------------

    private var cachedInMuteList: Boolean = false
    private var cachedMuteListUpdateTime: Double = 0.0
    private var cachedInBuddyList: Boolean = false
    private var cachedBuddyListUpdateTime: Double = 0.0

    // ---- jelly anims ----------------------------------------------------------

    val jellyAnims: MutableSet<LLUUID> = mutableSetOf()

    // ---- matrix palette cache -------------------------------------------------

    data class MatrixPaletteEntry(val frame: UInt, val palette: MutableList<FloatArray> = mutableListOf())
    val matrixPaletteCache: MutableMap<ULong, MatrixPaletteEntry> = mutableMapOf()

    // ---- phase tracking -------------------------------------------------------

    val phases: MutableMap<String, Long> = mutableMapOf()   // phase name → start ms

    // ---------------------------------------------------------------------------
    // ViewerObject overrides
    // ---------------------------------------------------------------------------

    override fun isAvatar(): Boolean = true

    open fun isBuddy(): Boolean = false
    open fun isImpostor(): Boolean = false

    open fun isControlAvatar(): Boolean = isControlAvatar
    open fun isUIAvatar(): Boolean = isUIAvatar

    open fun getAttachedAvatar(): VOAvatar? = null

    // ---------------------------------------------------------------------------
    // Initialization
    // ---------------------------------------------------------------------------

    open fun markDead() {
        TODO("APR: markDead — clean up name text, voice visualizer, callbacks")
    }

    open fun initInstance() {
        dirtyMesh = 2
        needsImpostorUpdate = true
        needsAnimUpdate = true
        needsExtentUpdate = true
        signaledAnimations.clear()
        playingAnimations.clear()
    }

    open fun updateGL() {
        TODO("GPU: updateGL — upload pending texture data to GPU")
    }

    // ---------------------------------------------------------------------------
    // Character interface
    // ---------------------------------------------------------------------------

    open fun getCharacterPosition(): Vector3 = TODO("getCharacterPosition")
    open fun getCharacterRotation(): FloatArray = TODO("getCharacterRotation: quaternion")
    open fun getCharacterVelocity(): Vector3 = TODO("getCharacterVelocity")
    open fun getCharacterAngularVelocity(): Vector3 = TODO("getCharacterAngularVelocity")

    open fun getTimeDilation(): Float = TODO("getTimeDilation: from region")
    open fun getPixelArea(): Float = TODO("getPixelArea")
    open fun getPosGlobalFromAgent(pos: Vector3): Vector3d = TODO("getPosGlobalFromAgent")
    open fun getPosAgentFromGlobal(posGlobal: Vector3d): Vector3 = TODO("getPosAgentFromGlobal")

    // ---------------------------------------------------------------------------
    // Full name
    // ---------------------------------------------------------------------------

    open fun getFullname(): String {
        val first = nameFirstname.ifEmpty { avatarName?.getFirstName() ?: "" }
        val last  = nameLastname.ifEmpty  { avatarName?.getLastName()  ?: "" }
        return if (last.isEmpty()) first else "$first $last"
    }

    fun avString(): String {
        val status = rezStatusToString(getRezzedStatus())
        return " Avatar '${getFullname()}' $status "
    }

    fun getDebugName(): String = getFullname()

    // ---------------------------------------------------------------------------
    // LOD
    // ---------------------------------------------------------------------------

    open fun updateLOD(): Boolean = TODO("GPU: updateLOD — pick mesh LOD based on pixel area")
    fun updateJointLODs(): Boolean = TODO("GPU: updateJointLODs")
    fun updateLODRiggedAttachments() { TODO("GPU: updateLODRiggedAttachments") }
    fun setCorrectedPixelArea(area: Float) { TODO("setCorrectedPixelArea area=$area") }

    // ---------------------------------------------------------------------------
    // Per-frame update
    // ---------------------------------------------------------------------------

    open fun idleUpdate(dt: Float) {
        TODO("APR: idleUpdate — orchestrate all per-frame avatar work")
    }

    open fun updateCharacter(dt: Float) {
        TODO("APR: updateCharacter — physics, orientation, root position")
    }

    fun computeUpdatePeriod() { TODO("computeUpdatePeriod") }
    fun updateOrientation(speed: Float, deltaTime: Float) { TODO("updateOrientation") }
    fun updateTimeStep() { TODO("updateTimeStep") }
    fun updateRootPositionAndRotation(speed: Float, wasSitGroundConstrained: Boolean) {
        TODO("updateRootPositionAndRotation")
    }
    fun idleUpdateMisc(detailedUpdate: Boolean) { TODO("idleUpdateMisc") }
    open fun idleUpdateAppearanceAnimation() { TODO("idleUpdateAppearanceAnimation") }
    fun idleUpdateLipSync(voiceEnabled: Boolean) { TODO("idleUpdateLipSync") }
    fun idleUpdateLoadingEffect() { TODO("idleUpdateLoadingEffect") }
    fun idleUpdateWindEffect() { TODO("idleUpdateWindEffect") }
    fun idleUpdateNameTag(rootPosLast: Vector3) { TODO("idleUpdateNameTag") }
    fun idleUpdateNameTagText(newName: Boolean) { TODO("idleUpdateNameTagText") }
    fun idleUpdateNameTagAlpha(newName: Boolean, alpha: Float) { TODO("idleUpdateNameTagAlpha") }
    fun getNameTagColor(): FloatArray = TODO("getNameTagColor")
    fun clearNameTag() { TODO("clearNameTag") }
    fun addNameTagLine(line: String, color: FloatArray, style: Int, font: Any?, useEllipses: Boolean = false, isName: Boolean = false) {
        TODO("addNameTagLine")
    }
    fun idleUpdateRenderComplexity() { TODO("idleUpdateRenderComplexity") }
    fun idleUpdateDebugInfo() { TODO("idleUpdateDebugInfo") }
    fun idleUpdateBelowWater() { TODO("idleUpdateBelowWater") }
    fun idleUpdateVoiceVisualizer(voiceEnabled: Boolean, position: Vector3) { TODO("idleUpdateVoiceVisualizer") }

    // ---------------------------------------------------------------------------
    // Appearance
    // ---------------------------------------------------------------------------

    fun startAppearanceAnimation() { appearanceAnimating = true }
    fun hideHair() { TODO("hideHair") }
    fun hideSkirt() { TODO("hideSkirt") }
    fun bodySizeChanged() { TODO("bodySizeChanged") }
    fun getIsAppearanceAnimating(): Boolean = appearanceAnimating

    fun isUsingLocalAppearance(): Boolean = useLocalAppearance
    fun isEditingAppearance(): Boolean = isEditingAppearance

    fun isUsingServerBakes(): Boolean = useServerBakes
    fun setIsUsingServerBakes(v: Boolean) { useServerBakes = v }

    // ---------------------------------------------------------------------------
    // Overall appearance / jelly doll
    // ---------------------------------------------------------------------------

    fun setOverallAppearanceNormal()    { overallAppearance = AvatarOverallAppearance.NORMAL;    updateOverallAppearanceAnimations() }
    fun setOverallAppearanceJellyDoll() { overallAppearance = AvatarOverallAppearance.JELLYDOLL; updateOverallAppearanceAnimations() }
    fun setOverallAppearanceInvisible() { overallAppearance = AvatarOverallAppearance.INVISIBLE }

    fun updateOverallAppearance() { TODO("updateOverallAppearance") }
    fun updateOverallAppearanceAnimations() { TODO("updateOverallAppearanceAnimations") }

    // ---------------------------------------------------------------------------
    // Visual mute
    // ---------------------------------------------------------------------------

    fun setVisualMuteSettings(setting: VisualMuteSettings) {
        visuallyMuteSetting = setting
        visualComplexityStale = true
    }

    fun isVisuallyMuted(): Boolean = TODO("isVisuallyMuted: checks mute list + complexity + setting")
    fun isInMuteList(): Boolean {
        TODO("APR: isInMuteList — check LLMuteList with cache")
    }
    fun isRlvSilhouette(): Boolean = TODO("isRlvSilhouette")

    // ---------------------------------------------------------------------------
    // Visual complexity
    // ---------------------------------------------------------------------------

    fun getVisualComplexity(): UInt = visualComplexity
    fun updateVisualComplexity() { visualComplexityStale = true }
    fun calculateUpdateRenderComplexity() { TODO("calculateUpdateRenderComplexity") }
    fun calcMutedAvColor() { TODO("calcMutedAvColor") }

    fun getGPURenderTime(): Float = gpuRenderTime
    fun getCPURenderTime(): Float = cpuRenderTime

    fun placeProfileQuery() { TODO("GPU: placeProfileQuery — insert GPU timer query") }
    fun readProfileQuery(retries: Int) { TODO("GPU: readProfileQuery") }

    // ---------------------------------------------------------------------------
    // Impostors
    // ---------------------------------------------------------------------------

    open fun shouldImpostor(rankFactor: Float = 1.0f): Boolean = TODO("shouldImpostor")
    fun needsImpostorUpdate(): Boolean = needsImpostorUpdate
    fun getImpostorOffset(): Vector3 = impostorOffset
    fun getImpostorDim(): FloatArray = floatArrayOf(impostorDim.x, impostorDim.y)
    fun setImpostorDim(w: Float, h: Float) { impostorDim = Vector3(w, h, 0f) }
    fun cacheImpostorValues() { TODO("cacheImpostorValues") }
    fun getImpostorValues(extents: Array<Vector3>, angle: FloatArray, distance: FloatArray) { TODO("getImpostorValues") }
    fun setNeedsExtentUpdate(v: Boolean) { needsExtentUpdate = v }
    fun getLastAnimExtents(): Array<Vector3> = lastAnimExtents

    // ---------------------------------------------------------------------------
    // Rendering
    // ---------------------------------------------------------------------------

    open fun renderImpostor(color: FloatArray = floatArrayOf(1f,1f,1f,1f), diffuseChannel: Int = 0): UInt {
        TODO("GPU: renderImpostor — blit impostor texture to screen")
    }

    fun renderRigid(): UInt = TODO("GPU: renderRigid")
    fun renderSkinned(): UInt = TODO("GPU: renderSkinned")
    fun renderTransparent(firstPass: Boolean): UInt = TODO("GPU: renderTransparent")
    fun renderCollisionVolumes() { TODO("GPU: renderCollisionVolumes") }
    open fun renderJoints() { TODO("GPU: renderJoints") }
    fun renderBones(selectedJoint: String = "") { TODO("GPU: renderBones") }
    fun renderOnlySelectedBones(selectedJoints: List<String>) { TODO("GPU: renderOnlySelectedBones") }
    fun renderBoxAroundJointAttachments(joint: Any?) { TODO("GPU: renderBoxAroundJointAttachments") }

    open fun shouldRenderRigged(): Boolean = TODO("shouldRenderRigged")

    fun updateMeshTextures() { TODO("GPU: updateMeshTextures") }
    fun updateMeshData() { TODO("GPU: updateMeshData") }
    fun updateMeshVisibility() { TODO("GPU: updateMeshVisibility") }
    fun dirtyMesh() { if (dirtyMesh < 1) dirtyMesh = 1 }
    open fun restoreMeshData() { TODO("GPU: restoreMeshData") }
    fun releaseMeshData() { TODO("GPU: releaseMeshData") }

    // ---------------------------------------------------------------------------
    // Textures / baking
    // ---------------------------------------------------------------------------

    fun updateTextures() { TODO("GPU: updateTextures — set LOD and request loads") }
    fun releaseOldTextures() { TODO("GPU: releaseOldTextures") }
    open fun updateVisualParams() { TODO("updateVisualParams") }

    open fun isTextureDefined(textureIndex: Int, index: UInt = 0u): Boolean = TODO("isTextureDefined")
    open fun isTextureVisible(textureIndex: Int, index: UInt = 0u): Boolean = TODO("isTextureVisible")

    fun isFullyBaked(): Boolean = TODO("isFullyBaked")
    fun isFullyTextured(): Boolean = TODO("GPU: isFullyTextured — check mesh composites")
    fun allBakedTexturesCompletelyDownloaded(): Boolean = TODO("allBakedTexturesCompletelyDownloaded")
    fun allLocalTexturesCompletelyDownloaded(): Boolean = TODO("allLocalTexturesCompletelyDownloaded")

    fun hasGray(): Boolean = !getHasMissingParts() && !isFullyTextured()
    open fun getHasMissingParts(): Boolean = false

    fun getRezzedStatus(): Int {
        if (getHasMissingParts()) return 0
        val textured = isFullyTextured()
        val allBaked = allBakedTexturesCompletelyDownloaded()
        if (textured && allBaked && getAttachmentCount() == simAttachments.size) return 4
        if (textured && allBaked) return 3
        if (textured) return 2
        return 1
    }

    fun updateRezzedStatusTimers(status: Int) { TODO("updateRezzedStatusTimers status=$status") }
    fun getNumBakes(): Int = TODO("getNumBakes")

    // ---------------------------------------------------------------------------
    // Loading state
    // ---------------------------------------------------------------------------

    fun isFullyLoaded(): Boolean = fullyLoaded
    fun hasFirstFullAttachmentData(): Boolean = TODO("hasFirstFullAttachmentData")

    fun isTooSlow(): Boolean = tooSlow
    fun isTooSlowWithoutShadows(): Boolean = tooSlowWithoutShadows
    fun updateTooSlow() { TODO("updateTooSlow") }
    open fun isTooComplex(): Boolean = TODO("isTooComplex")

    fun visualParamWeightsAreDefault(): Boolean = TODO("visualParamWeightsAreDefault")
    fun isVisible(): Boolean = visible
    fun setVisibilityRank(rank: UInt) { visibilityRank = rank }
    fun getVisibilityRank(): UInt = visibilityRank

    // ---------------------------------------------------------------------------
    // Sitting
    // ---------------------------------------------------------------------------

    fun sitDown(sitting: Boolean) {
        isSitting = sitting
    }

    fun isSitting(): Boolean = isSitting

    fun sitOnObject(sitObject: ViewerObject) { TODO("APR: sitOnObject") }
    fun getOffObject() { TODO("APR: getOffObject") }
    fun revokePermissionsOnObject(sitObject: ViewerObject) { TODO("APR: revokePermissionsOnObject") }

    // ---------------------------------------------------------------------------
    // Animations
    // ---------------------------------------------------------------------------

    open fun startMotion(id: LLUUID, timeOffset: Float = 0.0f): Boolean {
        return motionController.startMotion(id, timeOffset > 0.0f)
    }

    open fun stopMotion(id: LLUUID, stopImmediate: Boolean = false): Boolean {
        return motionController.stopMotion(id, stopImmediate)
    }

    fun remapMotionID(id: LLUUID): LLUUID = id
    fun hasMotionFromSource(sourceId: LLUUID): Boolean = TODO("hasMotionFromSource")
    fun stopMotionFromSource(sourceId: LLUUID) { TODO("stopMotionFromSource") }
    fun requestStopMotion(motion: Any?) { TODO("requestStopMotion") }
    fun findMotion(id: LLUUID): Any? = TODO("findMotion")

    fun startDefaultMotions() {
        startMotion(AgentAnims.HEAD_ROT)
        startMotion(AgentAnims.EYE)
        startMotion(AgentAnims.BODY_NOISE)
        startMotion(AgentAnims.BREATHE_ROT)
        startMotion(AgentAnims.PHYSICS_MOTION)
        startMotion(AgentAnims.HAND_MOTION)
        startMotion(AgentAnims.PELVIS_FIX)
        processAnimationStateChanges()
    }

    fun dumpAnimationState() { TODO("dumpAnimationState") }

    fun isAnyAnimationSignaled(animArray: Array<LLUUID>): Boolean =
        animArray.any { signaledAnimations.containsKey(it) }

    fun processAnimationStateChanges() { TODO("APR: processAnimationStateChanges") }

    // ---------------------------------------------------------------------------
    // Chat
    // ---------------------------------------------------------------------------

    fun addChat(message: String, from: String = "") { TODO("addChat") }
    fun clearChat() { TODO("clearChat") }

    fun startTyping() {
        isTyping = true
    }

    fun stopTyping() {
        isTyping = false
    }

    // ---------------------------------------------------------------------------
    // Attachments
    // ---------------------------------------------------------------------------

    fun getAttachments(): List<ViewerObject> = children.filter { it.isAttachment() }

    open fun attachObject(obj: ViewerObject, attachPt: Int): Any? {
        attachmentPoints[attachPt] = obj
        return null
    }

    open fun detachObject(obj: ViewerObject): Boolean {
        val key = attachmentPoints.entries.firstOrNull { it.value == obj }?.key ?: return false
        attachmentPoints.remove(key)
        return true
    }

    fun getAttachmentCount(): Int = attachmentPoints.size
    fun hasHUDAttachment(): Boolean = TODO("hasHUDAttachment")
    fun resetHUDAttachments() { TODO("resetHUDAttachments") }
    fun getMaxAttachments(): Int = TODO("getMaxAttachments")
    fun canAttachMoreObjects(n: UInt = 1u): Boolean = TODO("canAttachMoreObjects n=$n")
    fun getMaxAnimatedObjectAttachments(): Int = TODO("getMaxAnimatedObjectAttachments")
    fun canAttachMoreAnimatedObjects(n: UInt = 1u): Boolean = TODO("canAttachMoreAnimatedObjects n=$n")
    fun hasPendingAttachedMeshes(): Boolean = pendingAttachment.isNotEmpty()
    fun clampAttachmentPositions() { TODO("clampAttachmentPositions") }

    fun addAttachmentOverridesForObject(vo: ViewerObject, recursive: Boolean = true) {
        TODO("addAttachmentOverridesForObject")
    }
    fun removeAttachmentOverridesForObject(meshId: LLUUID) { TODO("removeAttachmentOverridesForObject") }
    fun clearAttachmentOverrides() { TODO("clearAttachmentOverrides") }
    fun rebuildAttachmentOverrides() { TODO("rebuildAttachmentOverrides") }
    fun updateAttachmentOverrides() { TODO("updateAttachmentOverrides") }
    fun notifyAttachmentMeshLoaded() { TODO("notifyAttachmentMeshLoaded") }
    fun jointIsRiggedTo(joint: Any?): Boolean = TODO("jointIsRiggedTo")
    fun onActiveOverrideMeshesChanged() { TODO("onActiveOverrideMeshesChanged") }

    val activeOverrideMeshes: MutableSet<LLUUID> = mutableSetOf()

    // ---------------------------------------------------------------------------
    // Skeleton
    // ---------------------------------------------------------------------------

    open fun buildCharacter() { TODO("buildCharacter") }
    fun resetVisualParams() { TODO("resetVisualParams") }
    fun applyDefaultParams() { TODO("applyDefaultParams") }
    fun resetSkeleton(resetAnimations: Boolean) { TODO("resetSkeleton resetAnimations=$resetAnimations") }
    fun updateHeadOffset() { TODO("updateHeadOffset") }
    fun postPelvisSetRecalc() { TODO("postPelvisSetRecalc") }
    fun initAllJoints() { TODO("initAllJoints") }
    fun initAttachmentPoints(ignoreHudJoints: Boolean = false) { TODO("initAttachmentPoints") }

    // ---------------------------------------------------------------------------
    // Physics / height resolution
    // ---------------------------------------------------------------------------

    fun resolveHeightGlobal(inPos: Vector3d, outPos: Vector3d, outNorm: Vector3) { TODO("resolveHeightGlobal") }
    fun resolveHeightAgent(inPos: Vector3, outPos: Vector3, outNorm: Vector3) { TODO("resolveHeightAgent") }
    fun slamPosition() { TODO("slamPosition") }

    // ---------------------------------------------------------------------------
    // Hierarchy (parent/child)
    // ---------------------------------------------------------------------------

    open fun setParent(parent: ViewerObject?): Boolean = TODO("setParent")
    override fun addChild(child: ViewerObject) { TODO("addChild") }
    override fun removeChild(child: ViewerObject) { TODO("removeChild") }

    // ---------------------------------------------------------------------------
    // Visibility / culling
    // ---------------------------------------------------------------------------

    fun isCulled(): Boolean = culled

    // ---------------------------------------------------------------------------
    // Shadows
    // ---------------------------------------------------------------------------

    fun updateShadowFaces() { TODO("GPU: updateShadowFaces") }

    // ---------------------------------------------------------------------------
    // Rigging
    // ---------------------------------------------------------------------------

    fun updateRiggingInfo() { TODO("GPU: updateRiggingInfo") }
    var lastRiggingInfoKey: Long = 0L

    fun getAssociatedVolumes(volumes: MutableList<Any?>) { TODO("getAssociatedVolumes") }

    // ---------------------------------------------------------------------------
    // Debug / diagnostics
    // ---------------------------------------------------------------------------

    fun addDebugText(text: String) { TODO("addDebugText text=$text") }
    open fun updateDebugText() { TODO("updateDebugText") }
    fun dumpBakedStatus() { TODO("dumpBakedStatus") }
    fun dumpAvatarTEs(context: String) { TODO("dumpAvatarTEs context=$context") }
    fun getSortedJointNames(jointType: Int, result: MutableList<String>) { TODO("getSortedJointNames") }
    fun debugAvatarRezTime(notificationName: String, comment: String = "") { TODO("debugAvatarRezTime") }
    fun debugGetExistenceTimeElapsed(): Float = TODO("debugGetExistenceTimeElapsed")

    fun startPhase(phaseName: String) { phases[phaseName] = System.currentTimeMillis() }
    fun stopPhase(phaseName: String, errCheck: Boolean = true) { phases.remove(phaseName) }
    fun clearPhases() { phases.clear() }
    fun logPendingPhases() { TODO("logPendingPhases") }

    // ---------------------------------------------------------------------------
    // Morph masks / composite
    // ---------------------------------------------------------------------------

    open fun applyMorphMask(texData: ByteArray, width: Int, height: Int, numComponents: Int, bakedIndex: Int) {
        TODO("applyMorphMask")
    }
    fun morphMaskNeedsUpdate(bakedIndex: Int): Boolean = TODO("morphMaskNeedsUpdate")
    fun onGlobalColorChanged(globalColor: Any?, uploadBake: Boolean) { TODO("onGlobalColorChanged") }
    open fun invalidateComposite(layerSet: Any?, uploadResult: Boolean) { TODO("invalidateComposite") }
    open fun invalidateAll() { TODO("invalidateAll") }
    open fun setCompositeUpdatesEnabled(b: Boolean) {}
    open fun isCompositeUpdateEnabled(index: UInt): Boolean = false

    // ---------------------------------------------------------------------------
    // Messaging
    // ---------------------------------------------------------------------------

    fun onFirstTEMessageReceived() { TODO("APR: onFirstTEMessageReceived") }
    fun processAvatarAppearance(msg: Any?) { TODO("APR: processAvatarAppearance") }

    // ---------------------------------------------------------------------------
    // Region / tex image size
    // ---------------------------------------------------------------------------

    fun getObjectHost(): Any? = TODO("APR: getObjectHost")
    open fun getTexImageSize(): Int = TEX_IMAGE_SIZE_OTHER

    // ---------------------------------------------------------------------------
    // Companion object (static members)
    // ---------------------------------------------------------------------------

    companion object {
        val VISUAL_COMPLEXITY_UNKNOWN: UInt = 0u
        val NON_IMPOSTORS_MAX_SLIDER: UInt = 30u

        var sRenderName: Int = RenderName.ALWAYS.ordinal
        var sRenderGroupTitles: Boolean = true
        var sMaxNonImpostors: UInt = 12u
        var sLimitNonImpostors: Boolean = false
        var sRenderDistance: Float = 256.0f
        var sShowAnimationDebug: Boolean = false
        var sShowCollisionVolumes: Boolean = false
        var sVisibleInFirstPerson: Boolean = false
        var sNumLODChangesThisFrame: Int = 0
        var sNumVisibleChatBubbles: Int = 0
        var sDebugInvisible: Boolean = false
        var sShowAttachmentPoints: Boolean = false
        var sLODFactor: Float = 1.0f
        var sPhysicsLODFactor: Float = 1.0f
        var sJointDebug: Boolean = false
        var sNumVisibleAvatars: Int = 0
        var sAvatarsNearby: Int = 0

        var sUnbakedTime: Float = 0.0f
        var sUnbakedUpdateTime: Float = 0.0f
        var sGreyTime: Float = 0.0f
        var sGreyUpdateTime: Float = 0.0f

        val sAVsIgnoringARTLimit: MutableList<LLUUID> = mutableListOf()

        fun updateImpostorRendering(newMaxNonImpostors: UInt) {
            sMaxNonImpostors = newMaxNonImpostors
            sLimitNonImpostors = newMaxNonImpostors > 0u
        }

        fun invalidateNameTag(agentId: LLUUID) { TODO("invalidateNameTag agentId=$agentId") }
        fun invalidateNameTags() { TODO("invalidateNameTags") }

        fun areAllNearbyInstancesBaked(greyAvatars: IntArray): Boolean {
            TODO("areAllNearbyInstancesBaked")
        }

        fun cullAvatarsByPixelArea() { TODO("GPU: cullAvatarsByPixelArea") }
        fun updateNearbyAvatarCount() { TODO("updateNearbyAvatarCount") }
        fun updateImpostors() { TODO("GPU: updateImpostors") }
        fun resetImpostors() { TODO("GPU: resetImpostors") }

        fun deleteCachedImages(clearAll: Boolean = true) { TODO("GPU: deleteCachedImages clearAll=$clearAll") }
        fun destroyGL() { TODO("GPU: destroyGL") }
        fun restoreGL() { TODO("GPU: restoreGL") }

        fun findAvatarFromAttachment(obj: ViewerObject): VOAvatar? = TODO("findAvatarFromAttachment")

        fun rezStatusToString(status: Int): String = when (status) {
            0 -> "cloud"
            1 -> "gray"
            2 -> "textured"
            3 -> "textured+baked"
            4 -> "full"
            else -> "unknown"
        }

        fun isIndexLocalTexture(index: Int): Boolean = TODO("isIndexLocalTexture")
        fun isIndexBakedTexture(index: Int): Boolean = TODO("isIndexBakedTexture")

        fun logPendingPhasesAllAvatars() { TODO("logPendingPhasesAllAvatars") }
        fun initClass() { TODO("APR: initClass — register avatar class") }
        fun cleanupClass() { TODO("APR: cleanupClass") }
        fun initCloud() { TODO("APR: initCloud — initialise cloud particle system") }

        fun getRiggedMeshID(vo: ViewerObject, meshId: LLUUID): Boolean = TODO("getRiggedMeshID")
        fun getAnimLabels(labels: MutableList<String>) { TODO("getAnimLabels") }
        fun getAnimNames(names: MutableList<String>) { TODO("getAnimNames") }
    }
}
