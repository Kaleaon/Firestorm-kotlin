package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import com.firestorm.llinventory.*

const val SKY_BOX_MULT: Float = 16.0f
const val HEAVENLY_BODY_FACTOR: Float = 0.1f
const val SKYTEX_COMPONENTS: Int = 4
const val SKYTEX_RESOLUTION: Int = 64

class SkyTex {
    companion object {
        var current: Int = 0
            private set

        fun getResolution(): Int = SKYTEX_RESOLUTION
        fun getCurrent(): Int = current
        fun stepCurrent(): Int { current = (current + 1) and 1; return current }
        fun getNext(): Int = (current + 1) and 1
        fun getWhich(curr: Boolean): Int = if (curr) current else getNext()
    }

    var isShiny: Boolean = false
        private set

    private val skyData: Array<Color4> = Array(SKYTEX_RESOLUTION * SKYTEX_RESOLUTION) { Color4.BLACK }
    private val skyDirs: Array<Vector3> = Array(SKYTEX_RESOLUTION * SKYTEX_RESOLUTION) { Vector3.ZERO }

    fun init(isShiny: Boolean) {
        this.isShiny = isShiny
        initEmpty(0)
        initEmpty(1)
    }

    fun setDir(dir: Vector3, i: Int, j: Int) {
        skyDirs[i * SKYTEX_RESOLUTION + j] = dir
    }

    fun getDir(i: Int, j: Int): Vector3 = skyDirs[i * SKYTEX_RESOLUTION + j]

    fun setPixel(col: Color4, i: Int, j: Int) {
        skyData[i * SKYTEX_RESOLUTION + j] = col
    }

    fun bindTexture(curr: Boolean = true): Unit = TODO("GPU: bindTexture index=${getWhich(curr)}")
    fun initEmpty(tex: Int): Unit = TODO("GPU: initEmpty tex=$tex")
    fun create(): Unit = TODO("GPU: create sky GL image")
    fun createGLImage(which: Int): Unit = TODO("GPU: createGLImage which=$which")
    fun cleanupGL(): Unit = TODO("GPU: cleanupGL")
    fun restoreGL(): Unit = TODO("GPU: restoreGL")
}

class HeavenBody(diskRadiusInit: Float) {
    companion object {
        var interpVal: Float = 0f
            private set

        fun setInterpVal(v: Float) { interpVal = v }
    }

    private var directionCached: Vector3 = Vector3.ZERO
    private var color: Color4 = Color4.BLACK
    private var colorCached: Color4 = Color4.BLACK
    var intensity: Float = 0f
    var direction: Vector3 = Vector3.ZERO
    var rotation: Quaternion = Quaternion.IDENTITY
    var angularVelocity: Vector3 = Vector3.ZERO
    var diskRadius: Float = diskRadiusInit
    var draw: Boolean = false
    var horizonVisibility: Float = 1f
    var visibility: Float = 1f
    var visible: Boolean = false
    private val quadCorners: Array<Vector3> = Array(4) { Vector3.ZERO }
    private var origin: Vector3 = Vector3.ZERO

    fun getDirectionCached(): Vector3 = directionCached
    fun renewDirection() { directionCached = direction }

    fun getColorCached(): Color4 = colorCached
    fun setColorCached(c: Color4) { colorCached = c }
    fun getColor(): Color4 = color
    fun setColor(c: Color4) { color = c }
    fun renewColor() { colorCached = color }

    fun getInterpColor(): Color4 = TODO("lerp color")

    fun corner(n: Int): Vector3 = quadCorners[n]
    fun corners(): Array<Vector3> = quadCorners
}

open class VOSky(id: LLUUID, localId: UInt, pCode: UInt) : ViewerObject(id, localId, pCode) {

    enum class Face {
        SIDE0, SIDE1, SIDE2, SIDE3, SIDE4, SIDE5,
        SUN, MOON, BLOOM, REFLECTION;

        companion object { val COUNT = values().size }
    }

    companion object {
        private const val NUM_TILES_X = 8
        private const val NUM_TILES_Y = 4
        private const val NUM_TILES = NUM_TILES_X * NUM_TILES_Y
        private const val NUM_CUBEMAP_FACES = 6
        val TOTAL_TILES = NUM_CUBEMAP_FACES * NUM_TILES

        private const val SUN_DISK_RADIUS = 0.5f
        private const val MOON_DISK_RADIUS = SUN_DISK_RADIUS * 0.9f
        private const val SUN_INTENSITY = 1e5f

        private const val UPDATE_MIN_DELTA_THRESHOLD = 0.0005f

        var resolution: Int = 64
        var tileResX: Int = 0
        var tileResY: Int = 0
    }

    val faces: Array<Any?> = arrayOfNulls(Face.COUNT)
    var bumpSunDir: Vector3 = Vector3.ZERO

    val sun: HeavenBody = HeavenBody(SUN_DISK_RADIUS)
    val moon: HeavenBody = HeavenBody(MOON_DISK_RADIUS)

    private val skyTex: Array<SkyTex> = Array(6) { SkyTex() }
    private val shinyTex: Array<SkyTex> = Array(6) { SkyTex() }

    private var sunDefaultPosition: Vector3 = Vector3.ZERO
    private var sunAngVel: Vector3 = Vector3.ZERO
    private var atmHeight: Float = 0f
    private var earthCenter: Vector3 = Vector3.ZERO
    private var cameraPosAgent: Vector3 = Vector3.ZERO
    private var brightnessScale: Float = 0f
    private var brightestPoint: Color4 = Color4.BLACK
    private var weatherChange: Boolean = false
    private var cloudDensity: Float = 0f
    private var wind: Float = 0f

    private var initialized: Boolean = false
    private var forceUpdate: Boolean = false
    private var needUpdate: Boolean = false
    private var cubeMapUpdateStage: Int = -1

    private var ambientScale: Float = 0f
    private var interpVal: Float = 0f
    private var worldScale: Float = 1f
    private var drawRefl: Int = 0
    private var heavenlyBodyUpdated: Boolean = false

    private var sunScale: Float = 1f
    private var moonScale: Float = 1f

    private var sunTextureId: Array<LLUUID> = Array(2) { LLUUID.NULL }
    private var moonTextureId: Array<LLUUID> = Array(2) { LLUUID.NULL }
    private var cloudNoiseTextureId: Array<LLUUID> = Array(2) { LLUUID.NULL }
    private var bloomTextureId: Array<LLUUID> = Array(2) { LLUUID.NULL }
    private var rainbowMapId: LLUUID = LLUUID.NULL
    private var haloMapId: LLUUID = LLUUID.NULL

    fun init() = TODO("init sky")
    fun initCubeMap(): Unit = TODO("GPU: initCubeMap")
    fun cleanupGL(): Unit = TODO("GPU: cleanupGL")
    fun restoreGL(): Unit = TODO("GPU: restoreGL")

    fun calc() = TODO("calc sky atmosphere")
    fun cacheEnvironment(psky: Any?, atmosphericsVars: Any?) = TODO("cacheEnvironment")

    fun idleUpdate(agent: Any?, time: Double) = TODO("idleUpdate")
    fun updateSky(): Boolean = TODO("updateSky")
    fun updateTextures(): Unit = TODO("updateTextures")
    fun createDrawable(pipeline: Any?): Any? = TODO("GPU: createDrawable")
    fun updateGeometry(drawable: Drawable): Boolean = TODO("GPU: updateGeometry")

    fun getInterpVal(): Float = interpVal

    fun getWorldScale(): Float = worldScale
    fun setWorldScale(s: Float) { worldScale = s }

    fun updateFog(distance: Float): Unit = TODO("updateFog")
    fun setFogRatio(fogRatio: Float): Unit = TODO("setFogRatio")
    fun getFogRatio(): Float = TODO("getFogRatio")

    fun getSkyFogColor(): Color4 = TODO("getSkyFogColor")
    fun getGLFogColor(): Color4 = TODO("getGLFogColor")

    fun setCloudDensity(density: Float) { cloudDensity = density }
    fun setWind(wind: Vector3) { this.wind = wind.length() }

    fun getCameraPosAgent(): Vector3 = cameraPosAgent
    fun getEarthCenter(): Vector3 = earthCenter

    fun getDrawRefl(): Int = drawRefl
    fun setDrawRefl(r: Int) { drawRefl = r }

    fun setSunAndMoonDirectionsCFR(sunDir: Vector3, moonDir: Vector3) {
        sun.direction = sunDir
        moon.direction = moonDir
    }

    fun setSunDirectionCFR(sunDirection: Vector3) { sun.direction = sunDirection }
    fun setMoonDirectionCFR(moonDirection: Vector3) { moon.direction = moonDirection }

    fun updateHeavenlyBodyGeometry(
        drawable: Drawable, scale: Float, side: Int,
        hb: HeavenBody, up: Vector3, right: Vector3
    ): Boolean = TODO("GPU: updateHeavenlyBodyGeometry")

    fun updateReflectionGeometry(drawable: Drawable, h: Float, hb: HeavenBody): Unit =
        TODO("GPU: updateReflectionGeometry")

    fun setSunTextures(sunTexture: LLUUID, sunTextureNext: LLUUID) {
        sunTextureId[0] = sunTexture; sunTextureId[1] = sunTextureNext
    }
    fun setMoonTextures(moonTexture: LLUUID, moonTextureNext: LLUUID) {
        moonTextureId[0] = moonTexture; moonTextureId[1] = moonTextureNext
    }
    fun setCloudNoiseTextures(cloudNoiseTexture: LLUUID, cloudNoiseTextureNext: LLUUID) {
        cloudNoiseTextureId[0] = cloudNoiseTexture; cloudNoiseTextureId[1] = cloudNoiseTextureNext
    }
    fun setBloomTextures(bloomTexture: LLUUID, bloomTextureNext: LLUUID) {
        bloomTextureId[0] = bloomTexture; bloomTextureId[1] = bloomTextureNext
    }

    fun setSunScale(scale: Float) { sunScale = scale }
    fun setMoonScale(scale: Float) { moonScale = scale }
    fun forceSkyUpdate() { forceUpdate = true }

    private fun updateDirections(psky: Any?) = TODO("updateDirections")
}
