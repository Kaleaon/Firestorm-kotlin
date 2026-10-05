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

        // no-op
    }

    override fun renderForTimer() {
        // Intentionally empty: blob does not animate during timer-only render passes.
    }

    private fun loadUiImage(name: String): Any? {
        return null
    }
}
