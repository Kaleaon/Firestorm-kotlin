package com.firestorm.newview

import kotlin.math.*

const val NO_EDGE: UByte    = 0x00u
const val EAST_EDGE: UByte  = 0x01u
const val NORTH_EDGE: UByte = 0x02u
const val WEST_EDGE: UByte  = 0x04u
const val SOUTH_EDGE: UByte = 0x08u

const val ONE_MORE_THAN_NEIGHBOR = 1
const val EQUAL_TO_NEIGHBOR      = 0
const val ONE_LESS_THAN_NEIGHBOR = -1

const val ABOVE_WATERLINE_ALPHA = 32

const val DEFAULT_WATER_HEIGHT = 20.0f

// Cardinal + diagonal direction indices matching C++ gDirOpposite / gDirAdjacent convention.
const val EAST      = 0
const val NORTH     = 1
const val WEST      = 2
const val SOUTH     = 3
const val NORTHEAST = 4
const val NORTHWEST = 5
const val SOUTHWEST = 6
const val SOUTHEAST = 7
const val MIDDLE    = 8

data class Vector3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vector3)  = Vector3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vector3) = Vector3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float)   = Vector3(x * s, y * s, z * s)
    fun length()                   = sqrt(x * x + y * y + z * z)
    fun lengthSquared()            = x * x + y * y + z * z
    fun normalize(): Vector3 {
        val l = length()
        return if (l > 0f) Vector3(x / l, y / l, z / l) else this
    }
    fun cross(o: Vector3) = Vector3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    companion object { val Z_AXIS = Vector3(0f, 0f, 1f) }
}

data class Vector3d(val x: Double, val y: Double, val z: Double) {
    operator fun minus(o: Vector3d) = Vector3d(x - o.x, y - o.y, z - o.z)
    fun toVector3() = Vector3(x.toFloat(), y.toFloat(), z.toFloat())
}

data class Vector2(val x: Float, val y: Float)

// Opaque stand-ins for renderer/region types that live outside this module.
class ViewerRegion {
    var handle: Long = 0L
    fun width(): Float = 256f
    fun getOriginAgent(): Vector3 = TODO("APR: use JVM equivalent")
    fun getCompositionXY(x: Int, y: Int): Float = TODO("GPU: composition lookup")
    fun getPosRegionFromGlobal(v: Vector3d): Vector3 = TODO("APR: use JVM equivalent")
    fun getPosGlobalFromRegion(v: Vector3): Vector3d = TODO("APR: use JVM equivalent")
    fun getComposition(): Any = TODO("APR: use JVM equivalent")
    fun dirtyHeights() { TODO("APR: use JVM equivalent") }
    fun calculateCenterGlobal() { TODO("APR: use JVM equivalent") }
    fun updateReflectionProbes(didUpdate: Boolean) { TODO("APR: use JVM equivalent") }
    fun getLandHeightRegion(pos: Vector3): Float = TODO("APR: use JVM equivalent")
    fun getLand(): Surface = TODO("APR: use JVM equivalent")
}

// Minimal patch-vertex-array stand-in.
class PatchVertexArray {
    var patchWidth: UInt = 0u
    val renderLevelp: IntArray  = IntArray(32)
    val renderStridep: IntArray = IntArray(32)
    fun create(gridsPerEdge: Int, gridsPerPatchEdge: Int, regionScale: Float) {
        TODO("APR: use JVM equivalent")
    }
}

// Minimal texture placeholder.
class ViewerTexture {
    fun hasGLTexture(): Boolean = TODO("GPU: texture query")
}

// Minimal water-object placeholder.
class VOWater {
    var drawable: Any? = null
    fun getPositionGlobal(): Vector3d = TODO("APR: use JVM equivalent")
    fun getPositionRegion(): Vector3 = TODO("APR: use JVM equivalent")
    fun setPositionGlobal(v: Vector3d) { TODO("APR: use JVM equivalent") }
    fun setPositionRegion(v: Vector3) { TODO("APR: use JVM equivalent") }
}

// Bit-pack / group-header stand-ins for DCT decompression.
class BitPack
class GroupHeader(var patchSize: Int = 0, var stride: Int = 0)

open class Surface(val type: UInt, var region: ViewerRegion? = null) {

    companion object {
        var textureSize: Int = 256

        fun initClasses() {}

        fun setTextureSize(size: Int) { textureSize = size }
    }

    var gridsPerEdge: Int = 0
        private set
    var ooGridsPerEdge: Float = 0f
        private set
    var patchesPerEdge: Int = 0
        private set
    var numberOfPatches: Int = 0
        private set

    val neighbors: Array<Surface?> = arrayOfNulls(8)

    var detailTextureScale: Float = 0f

    private var originGlobal = Vector3d(0.0, 0.0, 0.0)

    // Flat array of SurfacePatch objects, indexed [j*patchesPerEdge + i].
    private var patchList: Array<SurfacePatch> = emptyArray()

    private var surfaceZ: FloatArray = floatArrayOf()
    private var norm: Array<Vector3> = emptyArray()

    private val dirtyPatchList: MutableSet<SurfacePatch> = mutableSetOf()

    private var sTexture: ViewerTexture? = null
    private var waterObj: VOWater? = null

    private var visiblePatchCount: Int = 0
    private var gridsPerPatchEdge: UInt = 0u
    private var metersPerGrid: Float = 1f
    private var metersPerEdge: Float = 1f
    internal val pvArray = PatchVertexArray()

    var hasZData: Boolean = false
        internal set
    var minZ: Float = 10000f
    var maxZ: Float = -10000f

    private var surfacePatchUpdateCount: Int = 0

    fun create(
        gridWidth: Int,
        patchWidth: Int,
        originGlobal: Vector3d,
        width: Float
    ) {
        gridsPerEdge = gridWidth + 1
        ooGridsPerEdge = 1f / gridsPerEdge
        gridsPerPatchEdge = patchWidth.toUInt()
        patchesPerEdge = (gridsPerEdge - 1) / patchWidth
        numberOfPatches = patchesPerEdge * patchesPerEdge
        metersPerGrid = width / (gridsPerEdge - 1).toFloat()
        metersPerEdge = metersPerGrid * (gridsPerEdge - 1)

        // Aurora Sim: texture size follows region width, clamped to next power-of-2 ≤ 1024.
        var sz = width.toInt()
        if (sz and (sz - 1) != 0) {
            sz = 1 shl ceil(log2(sz.toFloat())).toInt()
        }
        textureSize = minOf(sz, 1024)

        this.originGlobal = originGlobal
        pvArray.create(gridsPerEdge, patchWidth, TODO("APR: use JVM equivalent"))

        val total = gridsPerEdge * gridsPerEdge
        surfaceZ = FloatArray(total) { 0f }
        norm = Array(total) { Vector3.Z_AXIS }

        visiblePatchCount = 0
        initTextures()
        createPatchData()
    }

    fun setRegion(regionp: ViewerRegion?) {
        region = regionp
        waterObj = null
    }

    fun setOriginGlobal(newOrigin: Vector3d) {
        originGlobal = newOrigin
        for (j in 0 until patchesPerEdge) {
            for (i in 0 until patchesPerEdge) {
                val patch = getPatch(i, j)
                val updated = Vector3d(
                    originGlobal.x + i * metersPerGrid * gridsPerPatchEdge.toInt(),
                    originGlobal.y + j * metersPerGrid * gridsPerPatchEdge.toInt(),
                    patch.getOriginGlobal().z
                )
                patch.setOriginGlobal(updated)
            }
        }
        val wo = waterObj
        if (wo != null && wo.drawable != null) {
            val hw = (region?.width() ?: 256f) / 2.0
            val x = newOrigin.x + hw
            val y = newOrigin.y + hw
            val z = wo.getPositionGlobal().z
            wo.setPositionGlobal(Vector3d(x, y, z))
        }
    }

    fun connectNeighbor(neighbor: Surface, direction: Int) {
        val neighborPpe = neighbor.patchesPerEdge
        neighbors[direction] = neighbor
        neighbor.neighbors[dirOpposite[direction]] = this

        val ppe = IntArray(2)
        val ownOff = IntArray(2)
        val nbrOff = IntArray(2)

        ppe[0] = minOf(patchesPerEdge, neighborPpe)
        ppe[1] = ppe[0]

        val ownHandle  = region?.handle  ?: 0L
        val nbrHandle  = neighbor.region?.handle ?: 0L
        val ownX = (ownHandle ushr 32).toInt() * 256
        val ownY = (ownHandle and 0xFFFF_FFFFL).toInt() * 256
        val nbrX = (nbrHandle ushr 32).toInt() * 256
        val nbrY = (nbrHandle and 0xFFFF_FFFFL).toInt() * 256
        val gppe = gridsPerPatchEdge.toInt()

        if (ownY >= nbrY) { nbrOff[1] = (ownY - nbrY) / gppe; ppe[1] = minOf(patchesPerEdge, neighborPpe - nbrOff[1]) }
        else              { ownOff[1] = (nbrY - ownY) / gppe;  ppe[1] = minOf(patchesPerEdge - ownOff[1], neighborPpe) }
        if (ownX >= nbrX) { nbrOff[0] = (ownX - nbrX) / gppe; ppe[0] = minOf(patchesPerEdge, neighborPpe - nbrOff[0]) }
        else              { ownOff[0] = (nbrX - ownX) / gppe;  ppe[0] = minOf(patchesPerEdge - ownOff[0], neighborPpe) }

        when (direction) {
            NORTHEAST -> {
                val p  = getPatch(patchesPerEdge - 1, patchesPerEdge - 1)
                val np = neighbor.getPatch(nbrOff[0], nbrOff[1])
                p.connectNeighbor(np, direction)
                np.connectNeighbor(p, dirOpposite[direction])
                p.updateNorthEdge(); p.dirtyZ()
            }
            NORTHWEST -> {
                val off = patchesPerEdge + nbrOff[1] - ownOff[1]
                val p  = getPatch(0, patchesPerEdge - 1)
                val np = neighbor.getPatch(nbrOff[0] - 1, off)
                p.connectNeighbor(np, direction)
                np.connectNeighbor(p, dirOpposite[direction])
            }
            SOUTHWEST -> {
                val p  = getPatch(0, 0)
                val np = neighbor.getPatch(nbrOff[0] - 1, nbrOff[1] - 1)
                p.connectNeighbor(np, direction)
                np.connectNeighbor(p, dirOpposite[direction])
                np.updateEastEdge(); np.dirtyZ()
            }
            SOUTHEAST -> {
                val off = patchesPerEdge + nbrOff[0] - ownOff[0]
                val p  = getPatch(patchesPerEdge - 1, 0)
                val np = neighbor.getPatch(off, nbrOff[1] - 1)
                p.connectNeighbor(np, direction)
                np.connectNeighbor(p, dirOpposite[direction])
            }
            EAST -> {
                for (i in 0 until ppe[1]) {
                    val p  = getPatch(patchesPerEdge - 1, i + ownOff[1])
                    val np = neighbor.getPatch(0, i + nbrOff[1])
                    p.connectNeighbor(np, direction)
                    np.connectNeighbor(p, dirOpposite[direction])
                    p.updateEastEdge(); p.dirtyZ()
                }
                for (i in 0 until ppe[1] - 1) {
                    val p  = getPatch(patchesPerEdge - 1, i + ownOff[1])
                    val np = neighbor.getPatch(0, i + 1 + nbrOff[1])
                    p.connectNeighbor(np, NORTHEAST); np.connectNeighbor(p, SOUTHWEST)
                }
                for (i in 1 until ppe[1]) {
                    val p  = getPatch(patchesPerEdge - 1, i + ownOff[1])
                    val np = neighbor.getPatch(0, i - 1 + nbrOff[1])
                    p.connectNeighbor(np, SOUTHEAST); np.connectNeighbor(p, NORTHWEST)
                }
            }
            NORTH -> {
                for (i in 0 until ppe[0]) {
                    val p  = getPatch(i + ownOff[0], patchesPerEdge - 1)
                    val np = neighbor.getPatch(i + nbrOff[0], 0)
                    p.connectNeighbor(np, direction)
                    np.connectNeighbor(p, dirOpposite[direction])
                    p.updateNorthEdge(); p.dirtyZ()
                }
                for (i in 0 until ppe[0] - 1) {
                    val p  = getPatch(i + ownOff[0], patchesPerEdge - 1)
                    val np = neighbor.getPatch(i + 1 + nbrOff[0], 0)
                    p.connectNeighbor(np, NORTHEAST); np.connectNeighbor(p, SOUTHWEST)
                }
                for (i in 1 until ppe[0]) {
                    val p  = getPatch(i + ownOff[0], patchesPerEdge - 1)
                    val np = neighbor.getPatch(i - 1 + nbrOff[0], 0)
                    p.connectNeighbor(np, NORTHWEST); np.connectNeighbor(p, SOUTHEAST)
                }
            }
            WEST -> {
                for (i in 0 until ppe[1]) {
                    val p  = getPatch(0, i + ownOff[1])
                    val np = neighbor.getPatch(neighborPpe - 1, i + nbrOff[1])
                    p.connectNeighbor(np, direction)
                    np.connectNeighbor(p, dirOpposite[direction])
                    np.updateEastEdge(); np.dirtyZ()
                }
                for (i in 1 until ppe[1]) {
                    val p  = getPatch(0, i + ownOff[1])
                    val np = neighbor.getPatch(neighborPpe - 1, i - 1 + nbrOff[1])
                    p.connectNeighbor(np, SOUTHWEST); np.connectNeighbor(p, NORTHEAST)
                }
                for (i in 0 until ppe[1] - 1) {
                    val p  = getPatch(0, i + ownOff[1])
                    val np = neighbor.getPatch(neighborPpe - 1, i + 1 + nbrOff[1])
                    p.connectNeighbor(np, NORTHWEST); np.connectNeighbor(p, SOUTHEAST)
                }
            }
            SOUTH -> {
                for (i in 0 until ppe[0]) {
                    val p  = getPatch(i + ownOff[0], 0)
                    val np = neighbor.getPatch(i + nbrOff[0], neighborPpe - 1)
                    p.connectNeighbor(np, direction)
                    np.connectNeighbor(p, dirOpposite[direction])
                    np.updateNorthEdge(); np.dirtyZ()
                }
                for (i in 1 until ppe[0]) {
                    val p  = getPatch(i + ownOff[0], 0)
                    val np = neighbor.getPatch(i - 1 + nbrOff[0], neighborPpe - 1)
                    p.connectNeighbor(np, SOUTHWEST); np.connectNeighbor(p, NORTHEAST)
                }
                for (i in 0 until ppe[0] - 1) {
                    val p  = getPatch(i + ownOff[0], 0)
                    val np = neighbor.getPatch(i + 1 + nbrOff[0], neighborPpe - 1)
                    p.connectNeighbor(np, SOUTHEAST); np.connectNeighbor(p, NORTHWEST)
                }
            }
        }
    }

    fun disconnectNeighbor(surface: Surface) {
        for (i in 0..7) {
            if (neighbors[i] === surface) neighbors[i] = null
        }
        for (patch in patchList) patch.disconnectNeighbor(surface)
    }

    fun disconnectAllNeighbors() {
        for (i in 0..7) {
            neighbors[i]?.disconnectNeighbor(this)
            neighbors[i] = null
        }
    }

    fun rebuildWater() {
        TODO("APR: use JVM equivalent")
    }

    open fun decompressDCTPatch(bitpack: BitPack, gopp: GroupHeader, bLargePatch: Boolean) {
        TODO("APR: use JVM equivalent")
    }

    open fun updatePatchVisibilities() {
        TODO("APR: use JVM equivalent")
    }

    fun getZ(k: Int): Float = surfaceZ[k]
    fun getZ(i: Int, j: Int): Float = surfaceZ[i + j * gridsPerEdge]

    fun getOriginAgent(): Vector3 = originGlobal.toVector3()
    fun getOriginGlobal(): Vector3d = originGlobal

    fun getMetersPerGrid(): Float = metersPerGrid
    fun getGridsPerEdge(): Int = gridsPerEdge
    fun getPatchesPerEdge(): Int = patchesPerEdge
    fun getGridsPerPatchEdge(): Int = gridsPerPatchEdge.toInt()

    fun getRenderStride(renderLevel: UInt): UInt = pvArray.renderStridep[renderLevel.toInt()].toUInt()
    fun getRenderLevel(renderStride: UInt): UInt = pvArray.renderLevelp[renderStride.toInt()].toUInt()

    fun resolveHeightRegion(x: Float, y: Float): Float {
        if (x < 0f || x > metersPerEdge || y < 0f || y > metersPerEdge) return 0f
        val oom = 1f / metersPerGrid
        val left   = floor(x * oom).toInt()
        val bottom = floor(y * oom).toInt()
        val right  = if (left   + 1 < gridsPerEdge - 1) left   + 1 else left
        val top    = if (bottom + 1 < gridsPerEdge - 1) bottom + 1 else bottom
        val lb = getZ(left, bottom); val rb = getZ(right, bottom)
        val lt = getZ(left, top);    val rt = getZ(right, top)
        var dx = x - left   * metersPerGrid
        var dy = y - bottom * metersPerGrid
        if (dy > dx) { dy *= lt - lb; dx *= rt - lt }
        else         { dx *= rb - lb; dy *= rt - rb }
        return lb + (dx + dy) * oom
    }

    fun resolveHeightRegion(pos: Vector3): Float = resolveHeightRegion(pos.x, pos.y)

    fun resolveHeightGlobal(posGlobal: Vector3d): Float {
        val r = region ?: return 0f
        return resolveHeightRegion(r.getPosRegionFromGlobal(posGlobal))
    }

    fun resolveNormalGlobal(posGlobal: Vector3d): Vector3 {
        if (surfaceZ.isEmpty()) return Vector3.Z_AXIS
        val oom = 1f / metersPerGrid
        if (posGlobal.x < originGlobal.x || posGlobal.x >= originGlobal.x + metersPerEdge ||
            posGlobal.y < originGlobal.y || posGlobal.y >= originGlobal.y + metersPerEdge) {
            return Vector3.Z_AXIS
        }
        val i = ((posGlobal.x - originGlobal.x) * oom).toInt()
        val j = ((posGlobal.y - originGlobal.y) * oom).toInt()
        val k = i + j * gridsPerEdge
        val dx = (posGlobal.x - i * metersPerGrid - originGlobal.x).toFloat()
        val dy = (posGlobal.y - j * metersPerGrid - originGlobal.y).toFloat()
        val dzx: Float; val dzy: Float
        val normal: Vector3
        if (dy > dx) {
            dzx = surfaceZ[k + 1 + gridsPerEdge] - surfaceZ[k + gridsPerEdge]
            dzy = surfaceZ[k] - surfaceZ[k + gridsPerEdge]
            normal = Vector3(-dzx, dzy, 1f)
        } else {
            dzx = surfaceZ[k] - surfaceZ[k + 1]
            dzy = surfaceZ[k + 1 + gridsPerEdge] - surfaceZ[k + 1]
            normal = Vector3(dzx, -dzy, 1f)
        }
        return normal.normalize()
    }

    fun resolvePatchRegion(x: Float, y: Float): SurfacePatch? {
        if (numberOfPatches == 0) return null
        val i = when {
            x < 0f             -> 0
            x >= metersPerEdge -> patchesPerEdge - 1
            else               -> (x / (metersPerGrid * gridsPerPatchEdge.toInt())).toInt()
        }
        val j = when {
            y < 0f             -> 0
            y >= metersPerEdge -> patchesPerEdge - 1
            else               -> (y / (metersPerGrid * gridsPerPatchEdge.toInt())).toInt()
        }
        val index = (i + j * patchesPerEdge).coerceIn(0, numberOfPatches - 1)
        return patchList[index]
    }

    fun resolvePatchRegion(posRegion: Vector3): SurfacePatch? =
        resolvePatchRegion(posRegion.x, posRegion.y)

    fun resolvePatchGlobal(posGlobal: Vector3d): SurfacePatch? {
        val r = region ?: return null
        return resolvePatchRegion(r.getPosRegionFromGlobal(posGlobal))
    }

    fun getPatch(x: Int, y: Int): SurfacePatch {
        require(x in 0 until patchesPerEdge) { "Patch x=$x out of bounds" }
        require(y in 0 until patchesPerEdge) { "Patch y=$y out of bounds" }
        return patchList[x + y * patchesPerEdge]
    }

    fun idleUpdate(maxUpdateTime: Float): Boolean {
        if (dirtyPatchList.isNotEmpty()) {
            region?.dirtyHeights()
        }
        var didUpdate = false
        val iter = dirtyPatchList.iterator()
        while (iter.hasNext()) {
            val patch = iter.next()
            patch.updateNormals()
            patch.updateVerticalStats()
            if (patch.updateTexture()) {
                didUpdate = true
                patch.clearDirty()
                iter.remove()
            }
        }
        region?.updateReflectionProbes(didUpdate)
        return didUpdate
    }

    fun containsPosition(position: Vector3): Boolean =
        position.x in 0f..metersPerEdge && position.y in 0f..metersPerEdge

    fun moveZ(x: Int, y: Int, delta: Float) {
        surfaceZ[x + y * gridsPerEdge] += delta
    }

    fun getMinZ(): Float = minZ
    fun getMaxZ(): Float = maxZ

    fun setWaterHeight(height: Float) {
        val wo = waterObj ?: return
        val pos = wo.getPositionRegion()
        wo.setPositionRegion(Vector3(pos.x, pos.y, height))
    }

    fun getWaterHeight(): Float = waterObj?.getPositionRegion()?.z ?: DEFAULT_WATER_HEIGHT

    fun getSTexture(): ViewerTexture? {
        val t = sTexture
        if (t != null && !t.hasGLTexture()) createSTexture()
        return sTexture
    }

    fun dirtyAllPatches() { for (p in patchList) p.dirtyZ() }

    fun dirtySurfacePatch(patch: SurfacePatch) { dirtyPatchList.add(patch) }

    fun getWaterObj(): VOWater? = waterObj

    fun getNeighboringRegions(): List<ViewerRegion> =
        neighbors.filterNotNull().mapNotNull { it.region }

    fun getNeighboringRegionsStatus(): List<Int> =
        neighbors.indices.filter { neighbors[it] != null }

    override fun toString(): String = buildString {
        append("{\n  gridsPerEdge = ${gridsPerEdge - 1} + 1\n")
        append("  gridsPerPatchEdge = $gridsPerPatchEdge\n")
        append("  patchesPerEdge = $patchesPerEdge\n")
        append("  originGlobal = $originGlobal\n")
        append("  metersPerGrid = $metersPerGrid\n")
        append("  visiblePatchCount = $visiblePatchCount\n}")
    }

    // --- private helpers ---

    private fun createSTexture() {
        TODO("GPU: create/request surface texture tile")
    }

    private fun initTextures() {
        createSTexture()
        TODO("APR: create water object if RenderWater is enabled")
    }

    private fun createPatchData() {
        patchList = Array(numberOfPatches) { SurfacePatch() }
        visiblePatchCount = numberOfPatches

        for (j in 0 until patchesPerEdge) {
            for (i in 0 until patchesPerEdge) {
                val patch = getPatch(i, j)
                patch.setSurface(this)
            }
        }

        for (j in 0 until patchesPerEdge) {
            for (i in 0 until patchesPerEdge) {
                val patch = getPatch(i, j)
                patch.hasReceivedData = false
                patch.sTexUpdate = true

                val dataOffset = i * gridsPerPatchEdge.toInt() + j * gridsPerPatchEdge.toInt() * gridsPerEdge
                patch.setDataOffset(dataOffset, surfaceZ, norm)

                patch.setNeighborPatch(EAST,      if (i < patchesPerEdge - 1) getPatch(i + 1, j) else null)
                patch.setNeighborPatch(NORTH,     if (j < patchesPerEdge - 1) getPatch(i, j + 1) else null)
                patch.setNeighborPatch(WEST,      if (i > 0) getPatch(i - 1, j) else null)
                patch.setNeighborPatch(SOUTH,     if (j > 0) getPatch(i, j - 1) else null)
                patch.setNeighborPatch(NORTHEAST, if (i < patchesPerEdge - 1 && j < patchesPerEdge - 1) getPatch(i + 1, j + 1) else null)
                patch.setNeighborPatch(NORTHWEST, if (i > 0 && j < patchesPerEdge - 1) getPatch(i - 1, j + 1) else null)
                patch.setNeighborPatch(SOUTHWEST, if (i > 0 && j > 0) getPatch(i - 1, j - 1) else null)
                patch.setNeighborPatch(SOUTHEAST, if (i < patchesPerEdge - 1 && j > 0) getPatch(i + 1, j - 1) else null)

                val og = Vector3d(
                    originGlobal.x + i * metersPerGrid * gridsPerPatchEdge.toInt(),
                    originGlobal.x + j * metersPerGrid * gridsPerPatchEdge.toInt(),
                    0.0
                )
                patch.setOriginGlobal(og)
            }
        }
    }
}

// Opposite direction indices (mirrors gDirOpposite in C++).
private val dirOpposite = intArrayOf(WEST, SOUTH, EAST, NORTH, SOUTHWEST, SOUTHEAST, NORTHEAST, NORTHWEST)
