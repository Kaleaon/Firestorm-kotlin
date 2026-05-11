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
            TODO("GPU: GLTFSceneManager.instance().renderOpaque()")
        }

        TODO("GPU: gDeferredPBROpaqueProgram.bind(); pushGLTFBatches(mRenderType); GLTFSceneManager.instance().render(true, true); gDeferredPBROpaqueProgram.bind(true); pushRiggedGLTFBatches(mRenderType + 1)")
    }

    override fun getNumPostDeferredPasses(): Int = 1

    override fun renderPostDeferred(pass: Int) {
        if (LLPipeline.sRenderingHUDs) {
            TODO("GPU: gHUDPBROpaqueProgram.bind(); pushGLTFBatches(mRenderType)")
        } else if (mRenderType == LLPipeline.RENDER_TYPE_PASS_GLTF_PBR) {
            // Only render glow for the non-alpha-masked variant
            TODO("GPU: gGL.setColorMask(false, true); gPBRGlowProgram.bind(); pushGLTFBatches(PASS_GLTF_GLOW); gPBRGlowProgram.bind(true); pushRiggedGLTFBatches(PASS_GLTF_GLOW_RIGGED); gGL.setColorMask(true, false)")
        }
    }
}
