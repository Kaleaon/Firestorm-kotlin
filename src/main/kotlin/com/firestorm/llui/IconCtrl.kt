package com.firestorm.llui

import com.firestorm.llmath.Color4
import com.firestorm.llmath.Rect
import java.util.UUID

open class IconCtrl(
    name: String,
    rect: Rect = Rect(),
    color: UIColor = UIColor(Color4.WHITE),
    image: UIImage? = null,
    val useDrawContextAlpha: Boolean = true,
    val interactable: Boolean = false,
    val minWidth: Int = 0,
    val minHeight: Int = 0,
    val maxWidth: Int = 0,
    val maxHeight: Int = 0
) : UICtrl(name, rect) {

    companion object {
        const val BOOST_ICON: Int = 8
    }

    var color: UIColor = color
    var image: UIImage? = image
        private set

    private var priority: Int = 0
    private var currentValue: Any? = image?.name

    init {
        image?.let { currentValue = it.name }
    }

    open fun setValue(value: Any?) {
        setValue(value, priority)
    }

    fun setValue(value: Any?, pri: Int) {
        val resolved = when {
            value is String && isUUID(value) -> UUID.fromString(value)
            else -> value
        }
        currentValue = resolved
        loadImage(resolved, pri)
    }

    private fun loadImage(value: Any?, pri: Int) {
        if (priority == BOOST_ICON && !visible) return

        image = when (value) {
            is UUID -> TODO("APR: use JVM equivalent — fetch UIImage by UUID $value with priority $pri")
            is String -> TODO("APR: use JVM equivalent — fetch UIImage by name '$value' with priority $pri")
            else -> null
        }

        val img = image ?: return
        if (minWidth > 0 && minHeight > 0) {
            val drawW = if (maxWidth > 0) minOf(maxOf(minWidth, img.width), maxWidth)
                        else maxOf(minWidth, img.width)
            val drawH = if (maxHeight > 0) minOf(maxOf(minHeight, img.height), maxHeight)
                        else maxOf(minHeight, img.height)
            TODO("GPU: setKnownDrawSize($drawW, $drawH) on underlying texture")
        }
    }

    override fun onVisibilityChange(newVisibility: Boolean) {
        super.onVisibilityChange(newVisibility)
        if (priority == BOOST_ICON) {
            if (newVisibility) {
                loadImage(currentValue, priority)
            } else {
                image = null
            }
        }
    }

    override fun draw() {
        val img = image
        if (img != null) {
            val alpha = if (useDrawContextAlpha) drawContextAlpha else currentTransparency()
            val c = color.get()
            val drawColor = Color4(c.r, c.g, c.b, c.a * alpha)
            img.draw(localRect(), drawColor)
        }
        super.draw()
    }

    override fun handleHover(x: Int, y: Int, mask: UInt): Boolean {
        if (interactable && enabled) {
            TODO("APR: use JVM equivalent — set cursor to hand/pointer")
        }
        return super.handleHover(x, y, mask)
    }

    fun getImageName(): String = when (val v = currentValue) {
        is String -> v
        else -> ""
    }

    fun setImage(img: UIImage?) { image = img }
    fun getImage(): UIImage? = image

    private fun isUUID(s: String): Boolean = runCatching { UUID.fromString(s); true }.getOrDefault(false)
}

open class UICtrl(
    name: String,
    rect: Rect = Rect()
) : View(name, rect) {

    var drawContextAlpha: Float = 1.0f
    protected val localRectCache: Rect = Rect()

    fun localRect(): Rect = Rect(0, rect.height, rect.width, 0)
    open fun currentTransparency(): Float = 1.0f
    open fun onVisibilityChange(newVisibility: Boolean) {}

    override fun draw() {
        if (!visible) return
        for (child in children) child.draw()
    }
}
