package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Quaternion
import com.firestorm.llmath.Vector3
import com.firestorm.llmath.Vector3d

const val FLAGS_USE_PHYSICS: UInt           = 0x00000001u
const val FLAGS_PHANTOM: UInt               = 0x00000010u
const val FLAGS_SCRIPTED: UInt              = 0x00000200u
const val FLAGS_HANDLE_TOUCH: UInt          = 0x00000400u
const val FLAGS_TAKES_MONEY: UInt           = 0x00000800u
const val FLAGS_OBJECT_MODIFY: UInt         = 0x00004000u
const val FLAGS_OBJECT_COPY: UInt           = 0x00008000u
const val FLAGS_OBJECT_MOVE: UInt           = 0x00010000u
const val FLAGS_OBJECT_TRANSFER: UInt       = 0x00020000u
const val FLAGS_OBJECT_YOU_OWNER: UInt      = 0x00040000u
const val FLAGS_OBJECT_ANY_OWNER: UInt      = 0x00080000u
const val FLAGS_OBJECT_GROUP_OWNED: UInt    = 0x00100000u
const val FLAGS_OBJECT_OWNER_MODIFY: UInt   = 0x00200000u
const val FLAGS_ALLOWS_ENVIRONMENT_OVERRIDE: UInt = 0x01000000u
const val FLAGS_ALLOW_INVENTORY_DROP: UInt  = 0x10000000u
const val FLAGS_INVENTORY_EMPTY: UInt       = 0x20000000u
const val FLAGS_TEMPORARY_ON_REZ: UInt      = 0x02000000u
const val FLAGS_ANIM_SOURCE: UInt           = 0x04000000u
const val FLAGS_CAMERA_SOURCE: UInt         = 0x08000000u
const val FLAGS_CAMERA_DECOUPLED: UInt      = 0x00200000u
const val FLAGS_AFFECTS_NAVMESH: UInt       = 0x00000040u
const val FLAGS_CHARACTER: UInt             = 0x00000080u
const val FLAGS_VOLUME_DETECT: UInt         = 0x00000100u
const val FLAGS_INCLUDE_IN_SEARCH: UInt     = 0x00000020u

enum class ObjectUpdateType {
    FULL,
    TERSE_IMPROVED,
    FULL_COMPRESSED,
    FULL_CACHED,
    UNKNOWN
}

enum class PhysicsShapeType {
    PRIM,
    NONE,
    CONVEX_HULL
}

enum class VoType(val code: Int) {
    SURFACE_PATCH(0x30),
    WL_SKY(0x40),
    SKY(0x60),
    VOID_WATER(0x70),
    WATER(0x80),
    PART_GROUP(0xa0),
    HUD_PART_GROUP(0xc0),
}

enum class InventoryRequestState {
    STOPPED,
    WAIT,
    PENDING,
    XFER
}

data class MaterialExportInfo(val materialIndex: Int, val textureIndex: Int, val color: FloatArray)

open class ViewerObject(
    val id: LLUUID,
    val localId: UInt,
    var region: ViewerRegion?,
    val isGlobal: Boolean = false,
) {
    var ownerId: LLUUID = LLUUID.NULL
    var totalCRC: UInt = 0u
    var listIndex: Int = -1
    var regionIndex: UInt = 0u

    var position: Vector3 = Vector3.ZERO
    var rotation: Quaternion = Quaternion.IDENTITY
    var scale: Vector3 = Vector3.ALL_ONE
    var velocity: Vector3 = Vector3.ZERO
    var angularVelocity: Vector3 = Vector3.ZERO
    var acceleration: Vector3 = Vector3.ZERO

    var primCode: UByte = 0u
    var material: UByte = 0u
    var clickAction: UByte = 0u
    var attachmentState: UByte = 0u

    var flags: UInt = 0u

    var physicsShapeType: UByte = 0u
    var physicsGravity: Float = 1f
    var physicsFriction: Float = 0.6f
    var physicsDensity: Float = 1000f
    var physicsRestitution: Float = 0.5f

    var objectCost: Float = -1f
    var linksetCost: Float = -1f
    var physicsCost: Float = -1f
    var linksetPhysicsCost: Float = -1f

    var appAngle: Float = 0f
    var pixelArea: Float = 0f
    var numFaces: Int = 0
    var rotTime: Float = 0f

    var dead: Boolean = false
    var orphaned: Boolean = false
    var userSelected: Boolean = false
    var onActiveList: Boolean = false
    var onMap: Boolean = false
    var static: Boolean = false
    var seatCount: Int = 0
    var canSelect: Boolean = true
    var createSelected: Boolean = false
    var renderMedia: Boolean = false
    var shouldShrinkWrap: Boolean = false

    var mediaType: UByte = 0u
    var mediaUrl: String = ""
    var mediaPassedWhitelist: Boolean = false

    var hudText: String = ""
    var debugText: String = ""

    var attachmentItemId: LLUUID = LLUUID.NULL
    var lastUpdateType: ObjectUpdateType = ObjectUpdateType.UNKNOWN
    var lastUpdateCached: Boolean = false

    var isReflectionProbe: Boolean = false
    var isHeroProbe: Boolean = false
    var isGLTFAssetMissing: Boolean = false
    var gpuRenderTime: Float = -1f

    private val nameValuePairs: MutableMap<String, String> = mutableMapOf()
    val children: MutableList<ViewerObject> = mutableListOf()
    var parent: ViewerObject? = null
    var parentId: UInt = 0u

    val unselectedChildrenPositions: MutableList<Vector3> = mutableListOf()

    val savedGLTFMaterialIds: MutableList<LLUUID> = mutableListOf()

    private val inventoryCallbacks: MutableList<Any> = mutableListOf()
    private var inventoryDirty: Boolean = false
    private var inventorySerialNum: Short = 0
    private var invRequestState: InventoryRequestState = InventoryRequestState.STOPPED

    companion object {
        var velocityInterpolate: Boolean = true
        var pingInterpolate: Boolean = true
        var numZombieObjects: UInt = 0u
        var mapDebug: Boolean = true
        var pulseEnabled: Boolean = false
        var useSharedDrawables: Boolean = false

        var maxUpdateInterpolationTime: Double = 3.0
        var phaseOutUpdateInterpolationTime: Double = 2.0
        var maxRegionCrossingInterpolationTime: Double = 1.0

        var axisArrowLength: Int = 50

        fun initVOClasses() { TODO("APR: use JVM equivalent") }
        fun cleanupVOClasses() { TODO("APR: use JVM equivalent") }

        fun increaseArrowLength() { axisArrowLength += 1 }
        fun decreaseArrowLength() { axisArrowLength -= 1 }

        fun setPhaseOutUpdateInterpolationTime(value: Float) { phaseOutUpdateInterpolationTime = value.toDouble() }
        fun setMaxUpdateInterpolationTime(value: Float) { maxUpdateInterpolationTime = value.toDouble() }
        fun setMaxRegionCrossingInterpolationTime(value: Float) { maxRegionCrossingInterpolationTime = value.toDouble() }
        fun setVelocityInterpolate(value: Boolean) { velocityInterpolate = value }
        fun setPingInterpolate(value: Boolean) { pingInterpolate = value }
    }

    open fun markDead() {
        dead = true
        numZombieObjects++
    }

    fun isDead(): Boolean = dead
    fun isOrphaned(): Boolean = orphaned
    fun isOnActiveList(): Boolean = onActiveList
    fun setOnActiveList(on: Boolean) { onActiveList = on }

    open fun isAvatar(): Boolean = false
    open fun isAttachment(): Boolean = false
    open fun isHUDAttachment(): Boolean = false
    open fun isTempAttachment(): Boolean = (flags and FLAGS_TEMPORARY_ON_REZ) != 0u
    open fun isFlexible(): Boolean = false
    open fun isSculpted(): Boolean = false
    open fun isMesh(): Boolean = false
    open fun isRiggedMesh(): Boolean = false
    open fun hasLightTexture(): Boolean = false
    open fun isReflectionProbeObj(): Boolean = false
    open fun isAnimatedObject(): Boolean = false
    open fun isActive(): Boolean = false

    fun isPrimitive(): Boolean = !isAvatar()
    fun isSelected(): Boolean = userSelected
    open fun setSelected(sel: Boolean) { userSelected = sel }

    fun isAnySelected(): Boolean = userSelected || children.any { it.userSelected }

    fun flagUsePhysics(): Boolean = (flags and FLAGS_USE_PHYSICS) != 0u
    fun flagObjectAnyOwner(): Boolean = (flags and FLAGS_OBJECT_ANY_OWNER) != 0u
    fun flagObjectYouOwner(): Boolean = (flags and FLAGS_OBJECT_YOU_OWNER) != 0u
    fun flagObjectGroupOwned(): Boolean = (flags and FLAGS_OBJECT_GROUP_OWNED) != 0u
    fun flagObjectOwnerModify(): Boolean = (flags and FLAGS_OBJECT_OWNER_MODIFY) != 0u
    fun flagObjectModify(): Boolean = (flags and FLAGS_OBJECT_MODIFY) != 0u
    fun flagObjectCopy(): Boolean = (flags and FLAGS_OBJECT_COPY) != 0u
    fun flagObjectMove(): Boolean = (flags and FLAGS_OBJECT_MOVE) != 0u
    fun flagObjectTransfer(): Boolean = (flags and FLAGS_OBJECT_TRANSFER) != 0u
    fun flagObjectPermanent(): Boolean = (flags and FLAGS_AFFECTS_NAVMESH) != 0u
    fun flagCharacter(): Boolean = (flags and FLAGS_CHARACTER) != 0u
    fun flagVolumeDetect(): Boolean = (flags and FLAGS_VOLUME_DETECT) != 0u
    fun flagIncludeInSearch(): Boolean = (flags and FLAGS_INCLUDE_IN_SEARCH) != 0u
    fun flagScripted(): Boolean = (flags and FLAGS_SCRIPTED) != 0u
    fun flagHandleTouch(): Boolean = (flags and FLAGS_HANDLE_TOUCH) != 0u
    fun flagTakesMoney(): Boolean = (flags and FLAGS_TAKES_MONEY) != 0u
    fun flagPhantom(): Boolean = (flags and FLAGS_PHANTOM) != 0u
    fun flagInventoryEmpty(): Boolean = (flags and FLAGS_INVENTORY_EMPTY) != 0u
    fun flagAllowInventoryAdd(): Boolean = (flags and FLAGS_ALLOW_INVENTORY_DROP) != 0u
    fun flagTemporaryOnRez(): Boolean = (flags and FLAGS_TEMPORARY_ON_REZ) != 0u
    fun flagAnimSource(): Boolean = (flags and FLAGS_ANIM_SOURCE) != 0u
    fun flagCameraSource(): Boolean = (flags and FLAGS_CAMERA_SOURCE) != 0u

    fun getFlags(): UInt = flags

    fun loadFlags(f: UInt) { flags = f }

    fun setFlags(flag: UInt, state: Boolean): Boolean {
        val old = flags
        flags = if (state) flags or flag else flags and flag.inv()
        return flags != old
    }

    open fun getPositionGlobal(): Vector3d {
        val r = region ?: return Vector3d(position.x.toDouble(), position.y.toDouble(), position.z.toDouble())
        return r.localToGlobal(position)
    }

    open fun getPositionRegion(): Vector3 = position
    open fun getPositionAgent(): Vector3 = position
    open fun getPivotPositionAgent(): Vector3 = getPositionAgent()

    fun setPosition(pos: Vector3, damped: Boolean = false) {
        position = pos
        updateDrawable(damped)
    }

    fun setPositionGlobal(pos: Vector3d, damped: Boolean = false) {
        val r = region
        position = if (r != null) r.globalToLocal(pos)
                   else Vector3(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
        updateDrawable(damped)
    }

    fun setPositionRegion(pos: Vector3, damped: Boolean = false) {
        position = pos
        updateDrawable(damped)
    }

    fun setPositionAgent(pos: Vector3, damped: Boolean = false) {
        position = pos
        updateDrawable(damped)
    }

    fun setRotation(quat: Quaternion, damped: Boolean = false) {
        rotation = quat
        updateDrawable(damped)
    }

    open fun setScale(s: Vector3, damped: Boolean = false) {
        scale = s
        updateDrawable(damped)
    }

    open fun updateDrawable(forceDamped: Boolean) {
        TODO("GPU: schedule drawable rebuild")
    }

    open fun idleUpdate(time: Double) {}

    open fun updateGeometry(): Boolean { TODO("GPU: update vertex buffer geometry") }

    open fun updateLOD(): Boolean { TODO("GPU: recompute LOD") }

    open fun updateTextures() { TODO("GPU: rebind texture resources") }

    open fun markForUpdate() { TODO("GPU: mark drawable dirty") }

    open fun createDrawable() { TODO("GPU: allocate drawable in pipeline") }

    open fun getLOD(): Int = 3

    fun getMaxScale(): Float = maxOf(scale.x, scale.y, scale.z)
    fun getMidScale(): Float {
        val sorted = floatArrayOf(scale.x, scale.y, scale.z).also { it.sort() }
        return sorted[1]
    }
    fun getMinScale(): Float = minOf(scale.x, scale.y, scale.z)

    open fun getVObjRadius(): Float { TODO("GPU: query drawable radius") }

    fun getNumFaces(): Int = numFaces

    fun setRegion(r: ViewerRegion?) { region = r }
    open fun updateRegion(r: ViewerRegion?) { region = r }

    open fun setParent(p: ViewerObject?): Boolean {
        parent = p
        return true
    }
    open fun onReparent(oldParent: ViewerObject?, newParent: ViewerObject?) {}
    open fun afterReparent() {}

    open fun addChild(child: ViewerObject) {
        children.add(child)
        child.parent = this
    }

    open fun removeChild(child: ViewerObject) {
        children.remove(child)
        child.parent = null
    }

    fun numChildren(): Int = children.size

    fun isChild(child: ViewerObject): Boolean = children.any { it === child }

    fun addThisAndAllChildren(out: MutableList<ViewerObject>) {
        out.add(this)
        children.forEach { it.addThisAndAllChildren(out) }
    }

    fun addThisAndNonJointChildren(out: MutableList<ViewerObject>) {
        out.add(this)
        children.forEach { it.addThisAndNonJointChildren(out) }
    }

    fun getRootEdit(): ViewerObject {
        var root = this
        while (root.parent != null && root.parent?.isAvatar() == false) {
            root = root.parent!!
        }
        return root
    }

    fun getSubParent(): ViewerObject? = parent

    fun isSeat(): Boolean = seatCount > 0

    fun isParticleSource(): Boolean { TODO("check mPartSourcep") }

    fun addNVPair(data: String) {
        val parts = data.split(" ", limit = 2)
        if (parts.size == 2) nameValuePairs[parts[0]] = parts[1]
    }

    fun removeNVPair(name: String): Boolean = nameValuePairs.remove(name) != null

    fun getNVPair(name: String): String? = nameValuePairs[name]

    fun setAttachedSound(audioUuid: LLUUID, ownerId: LLUUID, gain: Float, soundFlags: UByte) {
        TODO("APR: use JVM equivalent")
    }

    fun clearAttachedSound() {}

    fun isAudioSource(): Boolean = false

    fun setDebugText(text: String) { debugText = text }
    fun appendDebugText(text: String) { debugText += text }

    fun setMediaType(mt: UByte) { mediaType = mt }
    fun getMediaType(): UByte = mediaType
    fun setMediaUrl(url: String) { mediaUrl = url }
    fun getMediaUrl(): String = mediaUrl
    fun setMediaPassedWhitelist(passed: Boolean) { mediaPassedWhitelist = passed }
    fun getMediaPassedWhitelist(): Boolean = mediaPassedWhitelist

    fun setClickAction(action: UByte) { clickAction = action }
    fun getClickAction(): UByte = clickAction

    fun setObjectCost(cost: Float) { objectCost = cost }
    fun getObjectCost(): Float = objectCost
    fun setLinksetCost(cost: Float) { linksetCost = cost }
    fun getLinksetCost(): Float = linksetCost
    fun setPhysicsCost(cost: Float) { physicsCost = cost }
    fun getPhysicsCost(): Float = physicsCost
    fun setLinksetPhysicsCost(cost: Float) { linksetPhysicsCost = cost }
    fun getLinksetPhysicsCost(): Float = linksetPhysicsCost

    fun setPhysicsShapeType(type: UByte) { physicsShapeType = type }
    fun getPhysicsShapeType(): UByte = physicsShapeType
    fun setPhysicsGravity(g: Float) { physicsGravity = g }
    fun setPhysicsFriction(f: Float) { physicsFriction = f }
    fun setPhysicsDensity(d: Float) { physicsDensity = d }
    fun setPhysicsRestitution(r: Float) { physicsRestitution = r }

    fun setIncludeInSearch(include: Boolean) {
        flags = if (include) flags or FLAGS_INCLUDE_IN_SEARCH else flags and FLAGS_INCLUDE_IN_SEARCH.inv()
    }

    fun getIncludeInSearch(): Boolean = flagIncludeInSearch()

    fun allowOpen(): Boolean = flagHandleTouch() || mediaType.toInt() != 0

    fun permModify(): Boolean = flagObjectModify()
    fun permCopy(): Boolean = flagObjectCopy()
    fun permMove(): Boolean = flagObjectMove()
    fun permTransfer(): Boolean = flagObjectTransfer()
    fun permYouOwner(): Boolean = flagObjectYouOwner()
    fun permAnyOwner(): Boolean = flagObjectAnyOwner()
    fun permGroupOwner(): Boolean = flagObjectGroupOwned()
    fun permOwnerModify(): Boolean = flagObjectOwnerModify()

    fun isReturnable(): Boolean { TODO("check parcel ownership vs agent") }

    fun crossesParcelBounds(): Boolean { TODO("check parcel bitmap") }

    fun isReachable(): Boolean { TODO("traverse neighbor region graph") }

    fun isPermanentEnforced(): Boolean = flagObjectPermanent()

    fun setDrawableState(state: UInt, recursive: Boolean = true) { TODO("GPU: set drawable state flag") }
    fun clearDrawableState(state: UInt, recursive: Boolean = true) { TODO("GPU: clear drawable state flag") }
    fun isDrawableState(state: UInt, recursive: Boolean = true): Boolean { TODO("GPU: query drawable state") }

    fun dirtySpatialGroup() { TODO("GPU: dirty spatial group containing this object") }
    open fun dirtyMesh() { TODO("GPU: mark mesh dirty") }

    fun shrinkWrap() { shouldShrinkWrap = true }

    fun isInventoryPending(): Boolean = invRequestState != InventoryRequestState.STOPPED
    fun isInventoryDirty(): Boolean = inventoryDirty
    fun dirtyInventory() { inventoryDirty = true }
    fun requestInventory() { TODO("APR: use JVM equivalent") }

    fun getInventorySerial(): Short = inventorySerialNum

    fun updateInventory(key: UByte, isNew: Boolean) { TODO("APR: use JVM equivalent") }

    fun saveGLTFMaterials() { TODO("snapshot current GLTF override materials") }
    fun clearSavedGLTFMaterials() { savedGLTFMaterialIds.clear() }

    fun saveUnselectedChildrenRotation(rotations: MutableList<Quaternion>) {
        children.filter { !it.isSelected() }.forEach { rotations.add(it.rotation) }
    }

    fun saveUnselectedChildrenPosition(positions: MutableList<Vector3>) {
        children.filter { !it.isSelected() }.forEach { positions.add(it.position) }
    }

    fun resetChildrenPosition(offset: Vector3, simplified: Boolean = false, skipAvatarChild: Boolean = false) {
        for (child in children) {
            if (skipAvatarChild && child.isAvatar()) continue
            child.position = child.position - offset
        }
    }

    fun applyAngularVelocity(dt: Float) {
        if (angularVelocity.length() < 0.0001f) return
        TODO("GPU: apply angular velocity rotation step")
    }

    open fun isOwnerInMuteList(itemId: LLUUID = LLUUID.NULL): Boolean { TODO("check mute list") }

    open fun updateRiggingInfo() {}

    fun setGLTFAsset(id: LLUUID) { TODO("GPU: load GLTF asset by id") }

    fun setGLTFNodeRotationAgent(nodeIndex: Int, rot: Quaternion) { TODO("GPU: set GLTF node rotation") }

    fun moveGLTFNode(nodeIndex: Int, offset: Vector3) { TODO("GPU: translate GLTF node in agent space") }

    open fun dump() {
        println("ViewerObject id=$id localId=$localId dead=$dead pos=$position")
    }
}

open class AlphaObject(
    id: LLUUID,
    localId: UInt,
    region: ViewerRegion?,
) : ViewerObject(id, localId, region) {
    var depth: Float = 0f

    open fun getPartSize(idx: Int): Float { TODO("GPU: query particle part size") }

    open fun getGeometry(idx: Int) { TODO("GPU: fill vertex/normal/texcoord/color buffers") }
}

open class StaticViewerObject(
    id: LLUUID,
    localId: UInt,
    region: ViewerRegion?,
    isGlobal: Boolean = false,
) : ViewerObject(id, localId, region, isGlobal) {
    override fun updateDrawable(forceDamped: Boolean) {
        TODO("GPU: update static drawable with force-damped=$forceDamped")
    }
}

class ViewerObjectMedia {
    var mediaUrl: String = ""
    var passedWhitelist: Boolean = false
    var mediaType: UByte = 0u
}
