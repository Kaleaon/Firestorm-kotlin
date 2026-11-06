/**
 * ViewerJoint.kt
 * Kotlin port of llviewerjoint.h / llviewerjoint.cpp
 *
 * Original: Second Life Viewer Source Code
 * Copyright (C) 2010, Linden Research, Inc.
 * License: GNU Lesser General Public License v2.1
 */

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import com.firestorm.llcharacter.Joint

/**
 * A viewer-side joint that can be rendered as part of the avatar skeleton
 * hierarchy.  Corresponds to C++ LLViewerJoint (which extends LLAvatarJoint).
 *
 * Each joint may own a list of [ViewerJointMesh] segments that are drawn when
 * the joint is visible and passes the LOD threshold.  The [render] method
 * traverses the joint tree, applies per-joint LOD culling, and delegates
 * actual drawing to [drawShape].
 */
open class ViewerJoint : Joint {

    // -----------------------------------------------------------------------
    // Constructors – mirrors C++ default, joint-number, and named constructors
    // -----------------------------------------------------------------------

    constructor() : super()

    constructor(jointNum: Int) : super(jointNum)

    /** Named constructor — used for special joints like LLVOAvatarSelf::mScreenp. */
    constructor(name: String, parent: Joint? = null) : super(name, parent)

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /** Meshes owned by this joint. */
    val meshes: MutableList<ViewerJointMesh> = mutableListOf()

    /**
     * Pick name used for mouse-hit testing.
     * Corresponds to C++ LLJointPickName enum value stored on the joint.
     */
    var pickName: String = ""

    /**
     * Whether this joint has valid geometry and should be rendered.
     * Mirrors the C++ mValid flag on LLAvatarJoint.
     */
    var valid: Boolean = false

    /** True when this joint is part of the skeleton (not a floating attachment). */
    private var isSkeletonJoint: Boolean = false

    // -----------------------------------------------------------------------
    // Skeleton-joint flag
    // -----------------------------------------------------------------------

    fun setSkeletonJoint(b: Boolean) { isSkeletonJoint = b }
    fun getSkeletonJoint(): Boolean = isSkeletonJoint

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    /**
     * Render the full joint hierarchy rooted at this joint, applying LOD
     * culling to child joints.  Returns the number of triangles drawn.
     *
     * Corresponds to C++ LLViewerJoint::render().
     *
     * Transparent joints (hair/skirt) normally require three-pass rendering
     * to avoid z-fighting; the GPU details are stubbed with TODO.
     */
    open fun render(pixelArea: Float, firstPass: Boolean = true, isDummy: Boolean = false): UInt {
        if (!valid) return 0u

        var triangleCount = 0u

        when {
            isDummy -> {
                triangleCount += drawShape(pixelArea, firstPass, isDummy)
            }
            isTransparent() -> {
                // Three-pass transparent rendering (hair / skirt).
                // Full pipeline details deferred to GPU layer.
                // no-op
            }
            else -> {
                triangleCount += drawShape(pixelArea, firstPass)
            }
        }

        // Recurse into children, respecting LOD thresholds.
        for (child in children) {
            val childJoint = child as? ViewerJoint ?: continue
            val jointLod = childJoint.lod
            if (pixelArea >= jointLod || disableLod) {
                triangleCount += childJoint.render(pixelArea, firstPass = true, isDummy = isDummy)
                // C++ breaks after the first child that is NOT at the default
                // LOD level (the LOD chain stops).
                if (jointLod != DEFAULT_AVATAR_JOINT_LOD) break
            }
        }

        return triangleCount
    }

    /**
     * Draw the geometry attached to this joint.  Base implementation returns
     * zero (no geometry); subclasses override to issue actual draw calls.
     *
     * Corresponds to C++ LLViewerJoint::drawShape().
     */
    open fun drawShape(
        pixelArea: Float,
        firstPass: Boolean = true,
        isDummy: Boolean = false,
    ): UInt = 0u

    /**
     * Draw surface normals for debugging.
     * Corresponds to C++ LLViewerJoint::drawNormals() (default empty body).
     */
    open fun drawNormals(): Unit = Unit

    // -----------------------------------------------------------------------
    // Transparency helper — overridden in subclasses where needed
    // -----------------------------------------------------------------------

    /** Returns true if this joint's geometry should be rendered transparently. */
    open fun isTransparent(): Boolean = false

    // -----------------------------------------------------------------------
    // Companion
    // -----------------------------------------------------------------------

    companion object {
        /**
         * Sentinel LOD value meaning "always render" — matches C++
         * DEFAULT_AVATAR_JOINT_LOD (0.0f in llavatarjoint.h).
         */
        const val DEFAULT_AVATAR_JOINT_LOD: Float = 0.0f

        /**
         * When true, LOD thresholds are ignored and all joints are rendered
         * regardless of pixel area.  Mirrors C++ LLAvatarJoint::sDisableLOD.
         */
        var disableLod: Boolean = false

        /** Pixel-area threshold below which three-pass hair rendering collapses
         *  to a simpler two-pass approach.  Mirrors C++ MIN_PIXEL_AREA_3PASS_HAIR. */
        const val MIN_PIXEL_AREA_3PASS_HAIR: Int = 64 * 64
    }
}
