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
        System.err.println("VOAvatar: markDead not yet implemented")
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
        System.err.println("VOAvatar: updateGL not yet implemented")
    }

    // ---------------------------------------------------------------------------
    // Character interface
    // ---------------------------------------------------------------------------

    open fun getCharacterPosition(): Vector3 = Vector3.ZERO
    open fun getCharacterRotation(): FloatArray = floatArrayOf(0f, 0f, 0f, 1f)
    open fun getCharacterVelocity(): Vector3 = Vector3.ZERO
    open fun getCharacterAngularVelocity(): Vector3 = Vector3.ZERO

    open fun getTimeDilation(): Float = 1.0f
    open fun getPixelArea(): Float = 0f
    open fun getPosGlobalFromAgent(pos: Vector3): Vector3d = Vector3d.ZERO
    open fun getPosAgentFromGlobal(posGlobal: Vector3d): Vector3 = Vector3.ZERO

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

    open fun updateLOD(): Boolean = false
    fun updateJointLODs(): Boolean = false
    fun updateLODRiggedAttachments() { System.err.println("VOAvatar: updateLODRiggedAttachments not yet implemented") }
    fun setCorrectedPixelArea(area: Float) { System.err.println("VOAvatar: setCorrectedPixelArea not yet implemented") }

    // ---------------------------------------------------------------------------
    // Per-frame update
    // ---------------------------------------------------------------------------

    open fun idleUpdate(dt: Float) {
        System.err.println("VOAvatar: idleUpdate not yet implemented")
    }

    open fun updateCharacter(dt: Float) {
        System.err.println("VOAvatar: updateCharacter not yet implemented")
    }

    fun computeUpdatePeriod() { System.err.println("VOAvatar: computeUpdatePeriod not yet implemented") }
    fun updateOrientation(speed: Float, deltaTime: Float) { System.err.println("VOAvatar: updateOrientation not yet implemented") }
    fun updateTimeStep() { System.err.println("VOAvatar: updateTimeStep not yet implemented") }
    fun updateRootPositionAndRotation(speed: Float, wasSitGroundConstrained: Boolean) {
        System.err.println("VOAvatar: updateRootPositionAndRotation not yet implemented")
    }
    fun idleUpdateMisc(detailedUpdate: Boolean) { System.err.println("VOAvatar: idleUpdateMisc not yet implemented") }
    open fun idleUpdateAppearanceAnimation() { System.err.println("VOAvatar: idleUpdateAppearanceAnimation not yet implemented") }
    fun idleUpdateLipSync(voiceEnabled: Boolean) { System.err.println("VOAvatar: idleUpdateLipSync not yet implemented") }
    fun idleUpdateLoadingEffect() { System.err.println("VOAvatar: idleUpdateLoadingEffect not yet implemented") }
    fun idleUpdateWindEffect() { System.err.println("VOAvatar: idleUpdateWindEffect not yet implemented") }
    fun idleUpdateNameTag(rootPosLast: Vector3) { System.err.println("VOAvatar: idleUpdateNameTag not yet implemented") }
    fun idleUpdateNameTagText(newName: Boolean) { System.err.println("VOAvatar: idleUpdateNameTagText not yet implemented") }
    fun idleUpdateNameTagAlpha(newName: Boolean, alpha: Float) { System.err.println("VOAvatar: idleUpdateNameTagAlpha not yet implemented") }
    fun getNameTagColor(): FloatArray = floatArrayOf(1f, 1f, 1f, 1f)
    fun clearNameTag() { System.err.println("VOAvatar: clearNameTag not yet implemented") }
    fun addNameTagLine(line: String, color: FloatArray, style: Int, font: Any?, useEllipses: Boolean = false, isName: Boolean = false) {
        System.err.println("VOAvatar: addNameTagLine not yet implemented")
    }
    fun idleUpdateRenderComplexity() { System.err.println("VOAvatar: idleUpdateRenderComplexity not yet implemented") }
    fun idleUpdateDebugInfo() { System.err.println("VOAvatar: idleUpdateDebugInfo not yet implemented") }
    fun idleUpdateBelowWater() { System.err.println("VOAvatar: idleUpdateBelowWater not yet implemented") }
    fun idleUpdateVoiceVisualizer(voiceEnabled: Boolean, position: Vector3) { System.err.println("VOAvatar: idleUpdateVoiceVisualizer not yet implemented") }

    // ---------------------------------------------------------------------------
    // Appearance
    // ---------------------------------------------------------------------------

    fun startAppearanceAnimation() { appearanceAnimating = true }
    fun hideHair() { System.err.println("VOAvatar: hideHair not yet implemented") }
    fun hideSkirt() { System.err.println("VOAvatar: hideSkirt not yet implemented") }
    fun bodySizeChanged() { System.err.println("VOAvatar: bodySizeChanged not yet implemented") }
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

    fun updateOverallAppearance() { System.err.println("VOAvatar: updateOverallAppearance not yet implemented") }
    fun updateOverallAppearanceAnimations() { System.err.println("VOAvatar: updateOverallAppearanceAnimations not yet implemented") }

    // ---------------------------------------------------------------------------
    // Visual mute
    // ---------------------------------------------------------------------------

    fun setVisualMuteSettings(setting: VisualMuteSettings) {
        visuallyMuteSetting = setting
        visualComplexityStale = true
    }

    fun isVisuallyMuted(): Boolean = false
    fun isInMuteList(): Boolean {
        System.err.println("VOAvatar: isInMuteList not yet implemented")
        return false
    }
    fun isRlvSilhouette(): Boolean = false

    // ---------------------------------------------------------------------------
    // Visual complexity
    // ---------------------------------------------------------------------------

    fun getVisualComplexity(): UInt = visualComplexity
    fun updateVisualComplexity() { visualComplexityStale = true }
    fun calculateUpdateRenderComplexity() { System.err.println("VOAvatar: calculateUpdateRenderComplexity not yet implemented") }
    fun calcMutedAvColor() { System.err.println("VOAvatar: calcMutedAvColor not yet implemented") }

    fun getGPURenderTime(): Float = gpuRenderTime
    fun getCPURenderTime(): Float = cpuRenderTime

    fun placeProfileQuery() { System.err.println("VOAvatar: placeProfileQuery not yet implemented") }
    fun readProfileQuery(retries: Int) { System.err.println("VOAvatar: readProfileQuery not yet implemented") }

    // ---------------------------------------------------------------------------
    // Impostors
    // ---------------------------------------------------------------------------

    open fun shouldImpostor(rankFactor: Float = 1.0f): Boolean = false
    fun needsImpostorUpdate(): Boolean = needsImpostorUpdate
    fun getImpostorOffset(): Vector3 = impostorOffset
    fun getImpostorDim(): FloatArray = floatArrayOf(impostorDim.x, impostorDim.y)
    fun setImpostorDim(w: Float, h: Float) { impostorDim = Vector3(w, h, 0f) }
    fun cacheImpostorValues() { System.err.println("VOAvatar: cacheImpostorValues not yet implemented") }
    fun getImpostorValues(extents: Array<Vector3>, angle: FloatArray, distance: FloatArray) { System.err.println("VOAvatar: getImpostorValues not yet implemented") }
    fun setNeedsExtentUpdate(v: Boolean) { needsExtentUpdate = v }
    fun getLastAnimExtents(): Array<Vector3> = lastAnimExtents

    // ---------------------------------------------------------------------------
    // Rendering
    // ---------------------------------------------------------------------------

    open fun renderImpostor(color: FloatArray = floatArrayOf(1f,1f,1f,1f), diffuseChannel: Int = 0): UInt {
        return 0u
    }

    fun renderRigid(): UInt = 0u
    fun renderSkinned(): UInt = 0u
    fun renderTransparent(firstPass: Boolean): UInt = 0u
    fun renderCollisionVolumes() { System.err.println("VOAvatar: renderCollisionVolumes not yet implemented") }
    open fun renderJoints() { System.err.println("VOAvatar: renderJoints not yet implemented") }
    fun renderBones(selectedJoint: String = "") { System.err.println("VOAvatar: renderBones not yet implemented") }
    fun renderOnlySelectedBones(selectedJoints: List<String>) { System.err.println("VOAvatar: renderOnlySelectedBones not yet implemented") }
    fun renderBoxAroundJointAttachments(joint: Any?) { System.err.println("VOAvatar: renderBoxAroundJointAttachments not yet implemented") }

    open fun shouldRenderRigged(): Boolean = false

    fun updateMeshTextures() { System.err.println("VOAvatar: updateMeshTextures not yet implemented") }
    fun updateMeshData() { System.err.println("VOAvatar: updateMeshData not yet implemented") }
    fun updateMeshVisibility() { System.err.println("VOAvatar: updateMeshVisibility not yet implemented") }
    fun dirtyMesh() { if (dirtyMesh < 1) dirtyMesh = 1 }
    open fun restoreMeshData() { System.err.println("VOAvatar: restoreMeshData not yet implemented") }
    fun releaseMeshData() { System.err.println("VOAvatar: releaseMeshData not yet implemented") }

    // ---------------------------------------------------------------------------
    // Textures / baking
    // ---------------------------------------------------------------------------

    fun updateTextures() { System.err.println("VOAvatar: updateTextures not yet implemented") }
    fun releaseOldTextures() { System.err.println("VOAvatar: releaseOldTextures not yet implemented") }
    open fun updateVisualParams() { System.err.println("VOAvatar: updateVisualParams not yet implemented") }

    open fun isTextureDefined(textureIndex: Int, index: UInt = 0u): Boolean = false
    open fun isTextureVisible(textureIndex: Int, index: UInt = 0u): Boolean = false

    fun isFullyBaked(): Boolean = false
    fun isFullyTextured(): Boolean = false
    fun allBakedTexturesCompletelyDownloaded(): Boolean = false
    fun allLocalTexturesCompletelyDownloaded(): Boolean = false

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

    fun updateRezzedStatusTimers(status: Int) { System.err.println("VOAvatar: updateRezzedStatusTimers not yet implemented") }
    fun getNumBakes(): Int = 0

    // ---------------------------------------------------------------------------
    // Loading state
    // ---------------------------------------------------------------------------

    fun isFullyLoaded(): Boolean = fullyLoaded
    fun hasFirstFullAttachmentData(): Boolean = false

    fun isTooSlow(): Boolean = tooSlow
    fun isTooSlowWithoutShadows(): Boolean = tooSlowWithoutShadows
    fun updateTooSlow() { System.err.println("VOAvatar: updateTooSlow not yet implemented") }
    open fun isTooComplex(): Boolean = false

    fun visualParamWeightsAreDefault(): Boolean = false
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

    fun sitOnObject(sitObject: ViewerObject) { System.err.println("VOAvatar: sitOnObject not yet implemented") }
    fun getOffObject() { System.err.println("VOAvatar: getOffObject not yet implemented") }
    fun revokePermissionsOnObject(sitObject: ViewerObject) { System.err.println("VOAvatar: revokePermissionsOnObject not yet implemented") }

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
    fun hasMotionFromSource(sourceId: LLUUID): Boolean = false
    fun stopMotionFromSource(sourceId: LLUUID) { System.err.println("VOAvatar: stopMotionFromSource not yet implemented") }
    fun requestStopMotion(motion: Any?) { System.err.println("VOAvatar: requestStopMotion not yet implemented") }
    fun findMotion(id: LLUUID): Any? = null

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

    fun dumpAnimationState() { System.err.println("VOAvatar: dumpAnimationState not yet implemented") }

    fun isAnyAnimationSignaled(animArray: Array<LLUUID>): Boolean =
        animArray.any { signaledAnimations.containsKey(it) }

    fun processAnimationStateChanges() { System.err.println("VOAvatar: processAnimationStateChanges not yet implemented") }

    // ---------------------------------------------------------------------------
    // Chat
    // ---------------------------------------------------------------------------

    fun addChat(message: String, from: String = "") { System.err.println("VOAvatar: addChat not yet implemented") }
    fun clearChat() { System.err.println("VOAvatar: clearChat not yet implemented") }

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
    fun hasHUDAttachment(): Boolean = false
    fun resetHUDAttachments() { System.err.println("VOAvatar: resetHUDAttachments not yet implemented") }
    fun getMaxAttachments(): Int = 0
    fun canAttachMoreObjects(n: UInt = 1u): Boolean = false
    fun getMaxAnimatedObjectAttachments(): Int = 0
    fun canAttachMoreAnimatedObjects(n: UInt = 1u): Boolean = false
    fun hasPendingAttachedMeshes(): Boolean = pendingAttachment.isNotEmpty()
    fun clampAttachmentPositions() { System.err.println("VOAvatar: clampAttachmentPositions not yet implemented") }

    fun addAttachmentOverridesForObject(vo: ViewerObject, recursive: Boolean = true) {
        System.err.println("VOAvatar: addAttachmentOverridesForObject not yet implemented")
    }
    fun removeAttachmentOverridesForObject(meshId: LLUUID) { System.err.println("VOAvatar: removeAttachmentOverridesForObject not yet implemented") }
    fun clearAttachmentOverrides() { System.err.println("VOAvatar: clearAttachmentOverrides not yet implemented") }
    fun rebuildAttachmentOverrides() { System.err.println("VOAvatar: rebuildAttachmentOverrides not yet implemented") }
    fun updateAttachmentOverrides() { System.err.println("VOAvatar: updateAttachmentOverrides not yet implemented") }
    fun notifyAttachmentMeshLoaded() { System.err.println("VOAvatar: notifyAttachmentMeshLoaded not yet implemented") }
    fun jointIsRiggedTo(joint: Any?): Boolean = false
    fun onActiveOverrideMeshesChanged() { System.err.println("VOAvatar: onActiveOverrideMeshesChanged not yet implemented") }

    val activeOverrideMeshes: MutableSet<LLUUID> = mutableSetOf()

    // ---------------------------------------------------------------------------
    // Skeleton
    // ---------------------------------------------------------------------------

    open fun buildCharacter() { System.err.println("VOAvatar: buildCharacter not yet implemented") }
    fun resetVisualParams() { System.err.println("VOAvatar: resetVisualParams not yet implemented") }
    fun applyDefaultParams() { System.err.println("VOAvatar: applyDefaultParams not yet implemented") }
    fun resetSkeleton(resetAnimations: Boolean) { System.err.println("VOAvatar: resetSkeleton not yet implemented") }
    fun updateHeadOffset() { System.err.println("VOAvatar: updateHeadOffset not yet implemented") }
    fun postPelvisSetRecalc() { System.err.println("VOAvatar: postPelvisSetRecalc not yet implemented") }
    fun initAllJoints() { System.err.println("VOAvatar: initAllJoints not yet implemented") }
    fun initAttachmentPoints(ignoreHudJoints: Boolean = false) { System.err.println("VOAvatar: initAttachmentPoints not yet implemented") }

    // ---------------------------------------------------------------------------
    // Physics / height resolution
    // ---------------------------------------------------------------------------

    fun resolveHeightGlobal(inPos: Vector3d, outPos: Vector3d, outNorm: Vector3) { System.err.println("VOAvatar: resolveHeightGlobal not yet implemented") }
    fun resolveHeightAgent(inPos: Vector3, outPos: Vector3, outNorm: Vector3) { System.err.println("VOAvatar: resolveHeightAgent not yet implemented") }
    fun slamPosition() { System.err.println("VOAvatar: slamPosition not yet implemented") }

    // ---------------------------------------------------------------------------
    // Hierarchy (parent/child)
    // ---------------------------------------------------------------------------

    open fun setParent(parent: ViewerObject?): Boolean = false
    override fun addChild(child: ViewerObject) { System.err.println("VOAvatar: addChild not yet implemented") }
    override fun removeChild(child: ViewerObject) { System.err.println("VOAvatar: removeChild not yet implemented") }

    // ---------------------------------------------------------------------------
    // Visibility / culling
    // ---------------------------------------------------------------------------

    fun isCulled(): Boolean = culled

    // ---------------------------------------------------------------------------
    // Shadows
    // ---------------------------------------------------------------------------

    fun updateShadowFaces() { System.err.println("VOAvatar: updateShadowFaces not yet implemented") }

    // ---------------------------------------------------------------------------
    // Rigging
    // ---------------------------------------------------------------------------

    fun updateRiggingInfo() { System.err.println("VOAvatar: updateRiggingInfo not yet implemented") }
    var lastRiggingInfoKey: Long = 0L

    fun getAssociatedVolumes(volumes: MutableList<Any?>) { System.err.println("VOAvatar: getAssociatedVolumes not yet implemented") }

    // ---------------------------------------------------------------------------
    // Debug / diagnostics
    // ---------------------------------------------------------------------------

    fun addDebugText(text: String) { System.err.println("VOAvatar: addDebugText not yet implemented") }
    open fun updateDebugText() { System.err.println("VOAvatar: updateDebugText not yet implemented") }
    fun dumpBakedStatus() { System.err.println("VOAvatar: dumpBakedStatus not yet implemented") }
    fun dumpAvatarTEs(context: String) { System.err.println("VOAvatar: dumpAvatarTEs not yet implemented") }
    fun getSortedJointNames(jointType: Int, result: MutableList<String>) { System.err.println("VOAvatar: getSortedJointNames not yet implemented") }
    fun debugAvatarRezTime(notificationName: String, comment: String = "") { System.err.println("VOAvatar: debugAvatarRezTime not yet implemented") }
    fun debugGetExistenceTimeElapsed(): Float = 0f

    fun startPhase(phaseName: String) { phases[phaseName] = System.currentTimeMillis() }
    fun stopPhase(phaseName: String, errCheck: Boolean = true) { phases.remove(phaseName) }
    fun clearPhases() { phases.clear() }
    fun logPendingPhases() { System.err.println("VOAvatar: logPendingPhases not yet implemented") }

    // ---------------------------------------------------------------------------
    // Morph masks / composite
    // ---------------------------------------------------------------------------

    open fun applyMorphMask(texData: ByteArray, width: Int, height: Int, numComponents: Int, bakedIndex: Int) {
        System.err.println("VOAvatar: applyMorphMask not yet implemented")
    }
    fun morphMaskNeedsUpdate(bakedIndex: Int): Boolean = false
    fun onGlobalColorChanged(globalColor: Any?, uploadBake: Boolean) { System.err.println("VOAvatar: onGlobalColorChanged not yet implemented") }
    open fun invalidateComposite(layerSet: Any?, uploadResult: Boolean) { System.err.println("VOAvatar: invalidateComposite not yet implemented") }
    open fun invalidateAll() { System.err.println("VOAvatar: invalidateAll not yet implemented") }
    open fun setCompositeUpdatesEnabled(b: Boolean) {}
    open fun isCompositeUpdateEnabled(index: UInt): Boolean = false

    // ---------------------------------------------------------------------------
    // Messaging
    // ---------------------------------------------------------------------------

    fun onFirstTEMessageReceived() { System.err.println("VOAvatar: onFirstTEMessageReceived not yet implemented") }
    fun processAvatarAppearance(msg: Any?) { System.err.println("VOAvatar: processAvatarAppearance not yet implemented") }

    // ---------------------------------------------------------------------------
    // Region / tex image size
    // ---------------------------------------------------------------------------

    fun getObjectHost(): Any? = null
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

        fun invalidateNameTag(agentId: LLUUID) { System.err.println("VOAvatar: invalidateNameTag not yet implemented") }
        fun invalidateNameTags() { System.err.println("VOAvatar: invalidateNameTags not yet implemented") }

        fun areAllNearbyInstancesBaked(greyAvatars: IntArray): Boolean {
            return false
        }

        fun cullAvatarsByPixelArea() { System.err.println("VOAvatar: cullAvatarsByPixelArea not yet implemented") }
        fun updateNearbyAvatarCount() { System.err.println("VOAvatar: updateNearbyAvatarCount not yet implemented") }
        fun updateImpostors() { System.err.println("VOAvatar: updateImpostors not yet implemented") }
        fun resetImpostors() { System.err.println("VOAvatar: resetImpostors not yet implemented") }

        fun deleteCachedImages(clearAll: Boolean = true) { System.err.println("VOAvatar: deleteCachedImages not yet implemented") }
        fun destroyGL() { System.err.println("VOAvatar: destroyGL not yet implemented") }
        fun restoreGL() { System.err.println("VOAvatar: restoreGL not yet implemented") }

        fun findAvatarFromAttachment(obj: ViewerObject): VOAvatar? = null

        fun rezStatusToString(status: Int): String = when (status) {
            0 -> "cloud"
            1 -> "gray"
            2 -> "textured"
            3 -> "textured+baked"
            4 -> "full"
            else -> "unknown"
        }

        fun isIndexLocalTexture(index: Int): Boolean = false
        fun isIndexBakedTexture(index: Int): Boolean = false

        fun logPendingPhasesAllAvatars() { System.err.println("VOAvatar: logPendingPhasesAllAvatars not yet implemented") }
        fun initClass() { System.err.println("VOAvatar: initClass not yet implemented") }
        fun cleanupClass() { System.err.println("VOAvatar: cleanupClass not yet implemented") }
        fun initCloud() { System.err.println("VOAvatar: initCloud not yet implemented") }

        fun getRiggedMeshID(vo: ViewerObject, meshId: LLUUID): Boolean = false
        fun getAnimLabels(labels: MutableList<String>) { System.err.println("VOAvatar: getAnimLabels not yet implemented") }
        fun getAnimNames(names: MutableList<String>) { System.err.println("VOAvatar: getAnimNames not yet implemented") }
    }
}
