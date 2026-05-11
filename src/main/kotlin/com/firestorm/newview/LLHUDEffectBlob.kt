package com.firestorm.newview

class LLHUDEffectBlob(type: UByte) : LLHUDEffect(type) {

    var pixelSize: Int = 10
    private val timer: Long = System.currentTimeMillis()
    private var image: Any? = null

    init {
        image = loadUiImage("Camera_Drag_Dot")
    }

    override fun markDead() {
        image = null
        super.markDead()
    }

    fun setPixelSize(pixels: Int) { pixelSize = pixels }

    override fun render() {
        val time = (System.currentTimeMillis() - timer) / 1000f
        if (mDuration < time) {
            markDead()
            return
        }

        TODO("GPU: " +
            "1. Convert mPositionGlobal to agent-space position. " +
            "2. Obtain per-pixel up/right vectors from viewer camera. " +
            "3. Bind image texture. " +
            "4. Set color alpha = clamp_rescale(time, 0, mDuration, 255, 0). " +
            "5. Draw two textured triangles (quad) scaled by pixelSize in pixel_up/pixel_right directions."
        )
    }

    override fun renderForTimer() {
        // Intentionally empty: blob does not animate during timer-only render passes.
    }

    private fun loadUiImage(name: String): Any? {
        TODO("APR: use JVM equivalent — load UI image resource by name from asset system")
    }
}
