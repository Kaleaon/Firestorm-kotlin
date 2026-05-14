package com.firestorm.newview

class LLDrawPoolGLTFPBR(type: UInt = LLDrawPool.POOL_GLTF_PBR) : LLRenderPass(type) {

    var mRenderType: UInt = if (type == LLDrawPool.POOL_GLTF_PBR_ALPHA_MASK) {
        LLPipeline.RENDER_TYPE_PASS_GLTF_PBR_ALPHA_MASK
    } else {
        LLPipeline.RENDER_TYPE_PASS_GLTF_PBR
    }

    override fun getNumDeferredPasses(): Int = 1

    override fun renderDeferred(pass: Int) {
        check(!LLPipeline.sRenderingHUDs)

        if (mRenderType == LLPipeline.RENDER_TYPE_PASS_GLTF_PBR_ALPHA_MASK) {
            // no-op
        }

        // no-op
    }

    override fun getNumPostDeferredPasses(): Int = 1

    override fun renderPostDeferred(pass: Int) {
        if (LLPipeline.sRenderingHUDs) {
            // no-op
        } else if (mRenderType == LLPipeline.RENDER_TYPE_PASS_GLTF_PBR) {
            // Only render glow for the non-alpha-masked variant
            // no-op
        }
    }
}
