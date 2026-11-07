package com.firestorm.llui

import com.firestorm.llmath.Color4
import com.firestorm.llmath.Rect
import kotlin.math.sin
import kotlin.math.roundToInt

class ProgressBar(
    name: String,
    rect: Rect = Rect(),
    imageBar: UIImage? = null,
    imageFill: UIImage? = null,
    colorBar: UIColor = UIColor(Color4.WHITE),
    colorBackground: UIColor = UIColor(Color4.BLACK)
) : UICtrl(name, rect) {

    private var percentDone: Float = 0f

    private var imageBar: UIImage? = imageBar
    private var imageFill: UIImage? = imageFill
    private var colorBar: UIColor = colorBar
    private var colorBackground: UIColor = colorBackground

    fun setValue(value: Double) {
        percentDone = value.toFloat().coerceIn(0f, 100f)
    }

    override fun draw() {
        val alpha = drawContextAlpha

        imageBar?.let { bar ->
            val c = colorBackground.get()
            val bgColor = Color4(c.r, c.g, c.b, alpha)
            bar.draw(localRect(), bgColor)
        }

        imageFill?.let { fill ->
            val elapsedSec = currentTimeSeconds()
            val pulsedAlpha = alpha * (0.5f + 0.25f * (1f + sin(3f * elapsedSec).toFloat()))
            val c = colorBar.get()
            val barColor = Color4(c.r, c.g, c.b, c.a * pulsedAlpha)
            val local = localRect()
            val progressRect = Rect(local.left, local.top, local.left + (rect.width * (percentDone / 100f)).toInt(), local.bottom)
            fill.draw(progressRect, barColor)
        }
    }

    private fun currentTimeSeconds(): Float {
        return (System.currentTimeMillis() % 1_000_000L) / 1000f
    }
}
