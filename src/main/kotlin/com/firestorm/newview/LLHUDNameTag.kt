package com.firestorm.newview

private const val SPRING_STRENGTH = 0.7f
private const val HORIZONTAL_PADDING = 16f
private const val VERTICAL_PADDING = 12f
private const val LINE_PADDING = 3f
private const val BUFFER_SIZE = 2f
private const val NUM_OVERLAP_ITERATIONS = 10
private const val POSITION_DAMPING_TC = 0.2f
private const val MAX_STABLE_CAMERA_VELOCITY = 0.1f
private const val LOD_0_SCREEN_COVERAGE = 0.15f
private const val LOD_1_SCREEN_COVERAGE = 0.30f
private const val LOD_2_SCREEN_COVERAGE = 0.40f

class LLHUDNameTag(type: UByte) : LLHUDObject(type) {

    enum class ETextAlignment { ALIGN_TEXT_LEFT, ALIGN_TEXT_CENTER }
    enum class EVertAlignment { ALIGN_VERT_TOP, ALIGN_VERT_CENTER }

    inner class LLHUDTextSegment(
        private val mText: String,
        val mStyle: Int,
        var mColor: LLColor4,
        val mFont: LLFontGL?
    ) {
        private val mFontWidthMap: MutableMap<LLFontGL, Float> = mutableMapOf()

        fun getWidth(font: LLFontGL): Float {
            return mFontWidthMap.getOrPut(font) {
                TODO("GPU: font.getWidthF32(mText)")
            }
        }

        fun getText(): String = mText
        fun clearFontWidthMap() { mFontWidthMap.clear() }
    }

    companion object {
        const val NAMETAG_MAX_WIDTH = 298f
        const val HUD_TEXT_MAX_WIDTH = 190f

        var sDisplayText: Boolean = true
        val sTextObjects: MutableSet<LLHUDNameTag> = mutableSetOf()
        val sVisibleTextObjects: MutableList<LLHUDNameTag> = mutableListOf()

        fun updateAll() {
            TODO("GPU: profile zone UI")
            sVisibleTextObjects.clear()

            for (textp in sTextObjects) {
                textp.mTargetPositionOffset = LLVector2.zero()
                textp.updateSize()
                textp.updateVisibility()
            }

            sVisibleTextObjects.sortWith(Comparator { lhs, rhs ->
                rhs.getDistance().compareTo(lhs.getDistance())
            })

            val screenArea: Float = TODO("GPU: gViewerWindow.getWindowWidthScaled() * gViewerWindow.getWindowHeightScaled()")
            var currentScreenArea = 0f

            for (textp in sVisibleTextObjects.reversed()) {
                val coverage = currentScreenArea / screenArea
                when {
                    coverage > LOD_2_SCREEN_COVERAGE -> textp.setLOD(3)
                    coverage > LOD_1_SCREEN_COVERAGE -> textp.setLOD(2)
                    coverage > LOD_0_SCREEN_COVERAGE -> textp.setLOD(1)
                    else -> textp.setLOD(0)
                }
                textp.updateSize()
                textp.mTargetPositionOffset = textp.updateScreenPos(LLVector2.zero())
                currentScreenArea += textp.mSoftScreenRect.getWidth() * textp.mSoftScreenRect.getHeight()
            }

            val cameraVel: Float = TODO("APR: use JVM equivalent - LLTrace frame recording camera velocity per second")
            if (cameraVel > MAX_STABLE_CAMERA_VELOCITY) {
                return
            }

            for (i in 0 until NUM_OVERLAP_ITERATIONS) {
                for (srcIdx in sVisibleTextObjects.indices) {
                    val srcTextp = sVisibleTextObjects[srcIdx]
                    for (dstIdx in srcIdx + 1 until sVisibleTextObjects.size) {
                        val dstTextp = sVisibleTextObjects[dstIdx]
                        if (srcTextp.mSoftScreenRect.overlaps(dstTextp.mSoftScreenRect)) {
                            val intersectRect = srcTextp.mSoftScreenRect.intersectWith(dstTextp.mSoftScreenRect)
                                .stretch(-BUFFER_SIZE * 0.5f)

                            val srcCenterX = srcTextp.mSoftScreenRect.getCenterX()
                            val srcCenterY = srcTextp.mSoftScreenRect.getCenterY()
                            val dstCenterX = dstTextp.mSoftScreenRect.getCenterX()
                            val dstCenterY = dstTextp.mSoftScreenRect.getCenterY()
                            val intersectCenterX = intersectRect.getCenterX()
                            val intersectCenterY = intersectRect.getCenterY()

                            var force = LLVector2(dstCenterX - srcCenterX, dstCenterY - srcCenterY)
                            force.normVec()

                            val srcForce = force * -1f
                            val dstForce = force

                            val srcMult = dstTextp.mMass / (dstTextp.mMass + srcTextp.mMass)
                            val dstMult = 1f - srcMult
                            val srcAspectRatio = srcTextp.mSoftScreenRect.getWidth() / srcTextp.mSoftScreenRect.getHeight()
                            val dstAspectRatio = dstTextp.mSoftScreenRect.getWidth() / dstTextp.mSoftScreenRect.getHeight()

                            val adjustedSrcForce = LLVector2(
                                srcForce.x * minOf(intersectRect.getWidth() * srcMult, intersectRect.getHeight() * SPRING_STRENGTH),
                                (srcForce.y * srcAspectRatio).let { sy ->
                                    val normY = if (srcForce.lengthSquared() > 0f) sy / srcForce.length() else sy
                                    normY * minOf(intersectRect.getHeight() * srcMult, intersectRect.getWidth() * SPRING_STRENGTH)
                                }
                            )
                            val adjustedDstForce = LLVector2(
                                dstForce.x * minOf(intersectRect.getWidth() * dstMult, intersectRect.getHeight() * SPRING_STRENGTH),
                                (dstForce.y * dstAspectRatio).let { dy ->
                                    val normY = if (dstForce.lengthSquared() > 0f) dy / dstForce.length() else dy
                                    normY * minOf(intersectRect.getHeight() * dstMult, intersectRect.getWidth() * SPRING_STRENGTH)
                                }
                            )

                            srcTextp.mTargetPositionOffset += adjustedSrcForce
                            dstTextp.mTargetPositionOffset += adjustedDstForce
                            srcTextp.mTargetPositionOffset = srcTextp.updateScreenPos(srcTextp.mTargetPositionOffset)
                            dstTextp.mTargetPositionOffset = dstTextp.updateScreenPos(dstTextp.mTargetPositionOffset)
                        }
                    }
                }
            }

            for (textp in sVisibleTextObjects) {
                val interpolant: Float = TODO("APR: use JVM equivalent - LLSmoothInterpolation.getInterpolant(POSITION_DAMPING_TC)")
                textp.mPositionOffset = lerp(textp.mPositionOffset, textp.mTargetPositionOffset, interpolant)
            }
        }

        fun shiftAll(offset: LLVector3) {
            for (textp in sTextObjects) {
                textp.shift(offset)
            }
        }

        fun addPickable(pickList: MutableSet<LLViewerObject?>) {
            for (textp in sVisibleTextObjects) {
                pickList.add(textp.mSourceObject)
            }
        }

        fun reshape() {
            for (textp in sTextObjects) {
                textp.mTextSegments.forEach { it.clearFontWidthMap() }
                textp.mLabelSegments.forEach { it.clearFontWidthMap() }
            }
        }

        fun setDisplayText(flag: Boolean) {
            sDisplayText = flag
        }

        private fun lerp(a: LLVector2, b: LLVector2, t: Float): LLVector2 =
            LLVector2(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
    }

    private var mDoFade: Boolean = true
    private var mFadeRange: Float = 4f
    private var mFadeDistance: Float = 8f
    private var mLastDistance: Float = 0f
    private var mZCompare: Boolean = true
    private var mVisibleOffScreen: Boolean = false
    private var mOffscreen: Boolean = false
    private var mColor: LLColor4 = LLColor4(1f, 1f, 1f, 1f)
    private var mWidth: Float = 0f
    private var mHeight: Float = 0f
    private var mFontp: LLFontGL? = TODO("GPU: LLFontGL.getFontSansSerifSmall()")
    private var mBoldFontp: LLFontGL? = TODO("GPU: LLFontGL.getFontSansSerifBold()")
    private var mSoftScreenRect: LLRectf = LLRectf()
    private var mPositionAgent: LLVector3 = LLVector3()
    private var mPositionOffset: LLVector2 = LLVector2()
    var mTargetPositionOffset: LLVector2 = LLVector2()
    private var mMass: Float = 10f
    private var mMaxLines: Int = 10
    private var mOffsetY: Int = 0
    private var mRadius: Float = 0.1f
    val mTextSegments: MutableList<LLHUDTextSegment> = mutableListOf()
    val mLabelSegments: MutableList<LLHUDTextSegment> = mutableListOf()
    private var mTextAlignment: ETextAlignment = ETextAlignment.ALIGN_TEXT_CENTER
    private var mVertAlignment: EVertAlignment = EVertAlignment.ALIGN_VERT_CENTER
    private var mLOD: Int = 0
    private var mHidden: Boolean = false
    private var mRoundedRectImgp: LLUIImage? = TODO("GPU: LLUI.getUIImage(\"Rounded_Rect\")")
    private var mRoundedRectTopImgp: LLUIImage? = TODO("GPU: LLUI.getUIImage(\"Rounded_Rect_Top\")")

    init {
        sTextObjects.add(this)
    }

    override fun getDistance(): Float = mLastDistance
    fun getLOD(): Int = mLOD
    override fun getVisible(): Boolean = mVisible
    fun getHidden(): Boolean = mHidden
    fun setHidden(hide: Boolean) { mHidden = hide }
    fun setVisibleOffScreen(visible: Boolean) { mVisibleOffScreen = visible }
    fun setMaxLines(maxLines: Int) { mMaxLines = maxLines }
    fun setFadeDistance(fadeDistance: Float, fadeRange: Float) {
        mFadeDistance = fadeDistance
        mFadeRange = fadeRange
    }
    fun setTextAlignment(alignment: ETextAlignment) { mTextAlignment = alignment }
    fun setVertAlignment(alignment: EVertAlignment) { mVertAlignment = alignment }

    fun setString(textUtf8: String) {
        mTextSegments.clear()
        addLine(textUtf8, mColor)
    }

    fun clearString() {
        mTextSegments.clear()
    }

    fun addLine(
        textUtf8: String,
        color: LLColor4,
        style: Int = LLFontGL.NORMAL,
        font: LLFontGL? = null,
        useEllipses: Boolean = false,
        maxPixels: Float = HUD_TEXT_MAX_WIDTH
    ) {
        val wline = textUtf8
        if (wline.isEmpty()) return

        val resolvedFont = font ?: mFontp ?: return
        val clampedMax = minOf(maxPixels, NAMETAG_MAX_WIDTH)

        val lines = wline.split("\r\n", "\r", "\n")
        for (line in lines) {
            if (useEllipses) {
                // Fits text into a single segment; if it overflows, truncate and append "...".
                var lineLength = 0
                do {
                    val segLen: Int = TODO("GPU: resolvedFont.maxDrawableChars(line.substring(lineLength), clampedMax, wline.length, LLFontGL.ANYWHERE)")
                    if (segLen + lineLength < wline.length) {
                        val ellipsisWidth: Float = TODO("GPU: resolvedFont.getWidthF32(\"....\")")
                        val truncLen: Int = TODO("GPU: resolvedFont.maxDrawableChars(line.substring(lineLength), clampedMax - ellipsisWidth, wline.length, LLFontGL.ANYWHERE)")
                        mTextSegments.add(LLHUDTextSegment(line.substring(lineLength, lineLength + truncLen) + "...", style, color, resolvedFont))
                        lineLength = line.length
                    } else {
                        mTextSegments.add(LLHUDTextSegment(line.substring(lineLength, lineLength + segLen), style, color, resolvedFont))
                        lineLength += segLen
                    }
                } while (lineLength < line.length)
            } else {
                var lineLength = 0
                do {
                    val segLen: Int = TODO("GPU: resolvedFont.maxDrawableChars(line.substring(lineLength), clampedMax, wline.length, LLFontGL.WORD_BOUNDARY_IF_POSSIBLE)")
                    mTextSegments.add(LLHUDTextSegment(line.substring(lineLength, lineLength + segLen), style, color, resolvedFont))
                    lineLength += segLen
                } while (lineLength < line.length)
            }
        }
    }

    fun setLabel(labelUtf8: String) {
        mLabelSegments.clear()
        addLabel(labelUtf8)
    }

    fun addLabel(labelUtf8: String, maxPixels: Float = HUD_TEXT_MAX_WIDTH) {
        val wstr = labelUtf8
        if (wstr.isEmpty()) return

        val clampedMax = minOf(maxPixels, NAMETAG_MAX_WIDTH)
        val font = mFontp ?: return

        val lines = wstr.split("\r\n", "\r", "\n")
        for (line in lines) {
            var lineLength = 0
            do {
                val segLen: Int = TODO("GPU: font.maxDrawableChars(line.substring(lineLength), clampedMax, wstr.length, LLFontGL.WORD_BOUNDARY_IF_POSSIBLE)")
                mLabelSegments.add(LLHUDTextSegment(line.substring(lineLength, lineLength + segLen), LLFontGL.NORMAL, mColor, font))
                lineLength += segLen
            } while (lineLength < line.length)
        }
    }

    fun setFont(font: LLFontGL) { mFontp = font }

    fun setColor(color: LLColor4) {
        mColor = color
        for (seg in mTextSegments) {
            seg.mColor = color
        }
    }

    fun setAlpha(alpha: Float) {
        mColor = mColor.copy(a = alpha)
        for (seg in mTextSegments) {
            seg.mColor = seg.mColor.copy(a = alpha)
        }
    }

    fun setZCompare(zcompare: Boolean) { mZCompare = zcompare }
    fun setDoFade(doFade: Boolean) { mDoFade = doFade }

    override fun markDead() {
        sTextObjects.remove(this)
        super.markDead()
    }

    fun shift(offset: LLVector3) {
        mPositionAgent += offset
    }

    fun getWorldHeight(): Float {
        val heightMeters: Float = TODO("GPU: mLastDistance * tan(camera.getView() / 2f)")
        val heightPixels: Float = TODO("GPU: camera.getViewHeightInPixels() / 2f")
        val metersPerPixel = heightMeters / heightPixels
        val displayScaleY: Float = TODO("GPU: gViewerWindow.getDisplayScale().y")
        return mHeight * metersPerPixel * displayScaleY
    }

    fun lineSegmentIntersect(start: LLVector4a, end: LLVector4a, intersection: LLVector4a, debugRender: Boolean = false): Boolean {
        if (!mVisible || mHidden) return false
        if (mSourceObject == null || mSourceObject!!.mDrawable == null) return false

        var alphaFactor = 1f
        var textColor = mColor
        if (mDoFade && mLastDistance > mFadeDistance) {
            alphaFactor = maxOf(0f, 1f - (mLastDistance - mFadeDistance) / mFadeRange)
            textColor = textColor.copy(a = textColor.a * alphaFactor)
        }
        if (textColor.a < 0.01f) return false

        mOffsetY = (mHeight * if (mVertAlignment == EVertAlignment.ALIGN_VERT_CENTER) 0.5f else 1f).toInt()

        TODO("GPU: ray-quad intersection test using camera pixel vectors, mPositionAgent, mWidth, mHeight, screen offset")
    }

    fun updateVisibility() {
        mSourceObject?.updateText()
        mPositionAgent = TODO("APR: use JVM equivalent - gAgent.getPosAgentFromGlobal(mPositionGlobal)")

        if (mSourceObject == null) {
            mVisible = true
            sVisibleTextObjects.add(this)
            return
        }

        if (mSourceObject!!.isDead()) {
            mVisible = false
            return
        }

        val vecFromCamera: LLVector3 = TODO("GPU: mPositionAgent - camera.getOrigin()")
        val dirFromCamera: LLVector3 = TODO("GPU: vecFromCamera.normalized()")

        if (TODO<Float>("GPU: dirFromCamera dot camera.getAtAxis()") <= 0f) {
            mVisible = false
            return
        }

        val nearPlusRadius: Float = TODO("GPU: camera.getNear() + 0.1f + mSourceObject.getVObjRadius()")
        if (TODO<Float>("GPU: vecFromCamera dot camera.getAtAxis()") <= nearPlusRadius) {
            mPositionAgent = TODO("GPU: camera.getOrigin() + vecFromCamera * ((camera.getNear() + 0.1f) / (vecFromCamera dot camera.getAtAxis()))")
        } else {
            mPositionAgent = TODO("GPU: mPositionAgent - dirFromCamera * mSourceObject.getVObjRadius()")
        }

        mLastDistance = TODO("GPU: (mPositionAgent - camera.getOrigin()).magVec()")

        if (mLOD >= 3 || mTextSegments.isEmpty() || (mDoFade && mLastDistance > mFadeDistance + mFadeRange)) {
            mVisible = false
            return
        }

        val renderPosition: LLVector3 = TODO("GPU: mPositionAgent + xPixelVec * mPositionOffset.x + yPixelVec * mPositionOffset.y")

        mOffscreen = false
        val inFrustum: Boolean = TODO("GPU: camera.sphereInFrustum(renderPosition, mRadius)")
        if (!inFrustum) {
            if (!mVisibleOffScreen) {
                mVisible = false
                return
            }
            mOffscreen = true
        }

        mVisible = true
        sVisibleTextObjects.add(this)
    }

    fun updateScreenPos(offset: LLVector2): LLVector2 {
        TODO("GPU: project mPositionAgent + offset through camera to screen; clamp to world view rect edges if mVisibleOffScreen; update mSoftScreenRect; return adjusted offset")
    }

    fun updateSize() {
        var height = 0f
        var width = 0f

        val maxLines = getMaxLines()
        val startSegment = if (maxLines < 0) 0 else maxOf(0, mTextSegments.size - maxLines)

        for (seg in mTextSegments.drop(startSegment)) {
            val font = seg.mFont ?: mFontp ?: continue
            height += TODO<Float>("GPU: font.getLineHeight()")
            height += LINE_PADDING
            width = maxOf(width, minOf(seg.getWidth(font), NAMETAG_MAX_WIDTH))
        }

        if (height > 0f) {
            height -= LINE_PADDING
        }

        for (seg in mLabelSegments) {
            val font = mFontp ?: continue
            height += TODO<Float>("GPU: font.getLineHeight()")
            width = maxOf(width, minOf(seg.getWidth(font), NAMETAG_MAX_WIDTH))
        }

        if (width == 0f) return

        mWidth = width + HORIZONTAL_PADDING
        mHeight = height + VERTICAL_PADDING
    }

    private fun setLOD(lod: Int) { mLOD = lod }

    private fun getMaxLines(): Int {
        return when (mLOD) {
            0 -> mMaxLines
            1 -> if (mMaxLines > 0) mMaxLines / 2 else 5
            2 -> if (mMaxLines > 0) mMaxLines / 3 else 2
            else -> 0
        }
    }

    override fun render() {
        if (sDisplayText) {
            TODO("GPU: LLGLDepthTest gls_depth(GL_TRUE, GL_FALSE)")
            renderText()
        }
    }

    private fun renderText() {
        if (!mVisible || mHidden) return

        TODO("GPU: gGL.getTexUnit(0).enable(LLTexUnit.TT_TEXTURE)")

        var alphaFactor = 1f
        var textColor = mColor
        if (mDoFade && mLastDistance > mFadeDistance) {
            alphaFactor = maxOf(0f, 1f - (mLastDistance - mFadeDistance) / mFadeRange)
            textColor = textColor.copy(a = textColor.a * alphaFactor)
        }
        if (textColor.a < 0.01f) return

        mOffsetY = (mHeight * if (mVertAlignment == EVertAlignment.ALIGN_VERT_CENTER) 0.5f else 1f).toInt()

        val bubbleOpacity: Float = TODO("APR: use JVM equivalent - cached gSavedSettings.getF32(\"ChatBubbleOpacity\")")
        val nametagBgColor: LLColor4 = TODO("GPU: LLUIColorTable.instance().getColor(\"NameTagBackground\")")
        val colorAlpha = bubbleOpacity * alphaFactor
        val bgColor = nametagBgColor.copy(a = colorAlpha)

        TODO("GPU: camera.getPixelVectors(mPositionAgent, yPixelVec, xPixelVec)")
        TODO("GPU: compute width_vec, height_vec, mRadius, screen_pos, screen_offset, render_position")
        TODO("GPU: LLGLDepthTest; mRoundedRectImgp.draw3D(render_position, xPixelVec, yPixelVec, screenRect, bgColor)")

        if (mLabelSegments.isNotEmpty()) {
            TODO("GPU: compute label_height, draw mRoundedRectTopImgp for label background area")
        }

        var yOffset = mOffsetY.toFloat()

        for (seg in mLabelSegments) {
            val font = if (seg.mStyle == LLFontGL.BOLD) mBoldFontp else mFontp
            TODO("GPU: yOffset -= font.getLineHeight(); compute xOffset; hud_render_text for label segment")
        }

        val maxLines = getMaxLines()
        val startSegment = if (maxLines < 0) 0 else maxOf(0, mTextSegments.size - maxLines)

        for (seg in mTextSegments.drop(startSegment)) {
            val font = seg.mFont ?: mFontp ?: continue
            val segColor = seg.mColor.copy(a = seg.mColor.a * alphaFactor)
            TODO("GPU: yOffset -= font.getLineHeight() + LINE_PADDING; compute xOffset; hud_render_text for text segment with DROP_SHADOW")
        }

        TODO("GPU: gGL.color4f(1f, 1f, 1f, 1f)")
    }
}
