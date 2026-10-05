package com.firestorm.llrender

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LLGLStateTest {

    @Test
    fun `enable and disable capabilities`() {
        LLGLState.reset()
        assertFalse(LLGLState.isEnabled(GLCapability.BLEND))
        LLGLState.enable(GLCapability.BLEND)
        assertTrue(LLGLState.isEnabled(GLCapability.BLEND))
        assertTrue(LLGLState.isDirty(GLCapability.BLEND))
        LLGLState.disable(GLCapability.BLEND)
        assertFalse(LLGLState.isEnabled(GLCapability.BLEND))
    }

    @Test
    fun `clearDirty removes dirty flags`() {
        LLGLState.reset()
        LLGLState.enable(GLCapability.DEPTH_TEST)
        assertTrue(LLGLState.isDirty(GLCapability.DEPTH_TEST))
        LLGLState.clearDirty(GLCapability.DEPTH_TEST)
        assertFalse(LLGLState.isDirty(GLCapability.DEPTH_TEST))
    }
}

class LLGLSLShaderTest {

    @Test
    fun `compile and link produces linked shader`() {
        val shader = LLGLSLShader("test")
        assertFalse(shader.isLinked)
        val ok = shader.compile("void main(){}", "void main(){gl_FragColor=vec4(1.0);}")
        assertTrue(ok)
        assertTrue(shader.isLinked)
        assertTrue(shader.programId != 0)
    }

    @Test
    fun `compile blank source fails`() {
        val shader = LLGLSLShader("empty")
        val ok = shader.compile("", "")
        assertFalse(ok)
        assertFalse(shader.isLinked)
        assertTrue(shader.errors.isNotEmpty())
    }

    @Test
    fun `uniforms are stored and retrieved`() {
        val shader = LLGLSLShader("uniforms")
        shader.compile("v", "f")
        shader.uniform1f("alpha", 0.5f)
        shader.uniform1i("sampler", 2)
        assertEquals(0.5f, shader.getUniform<Float>("alpha"))
        assertEquals(2, shader.getUniform<Int>("sampler"))
    }

    @Test
    fun `bind and unbind tracks state`() {
        val shader = LLGLSLShader("bind_test")
        shader.compile("v", "f")
        shader.bind()
        assertTrue(shader.isBound)
        shader.unbind()
        assertFalse(shader.isBound)
    }
}

class LLRenderTest {

    @Test
    fun `begin and end accumulates vertices`() {
        val render = LLRender()
        render.begin(BufferType.TRIANGLES)
        assertTrue(render.isBuilding())
        render.vertex(0f, 0f, 0f)
        render.vertex(1f, 0f, 0f)
        render.vertex(0f, 1f, 0f)
        assertEquals(3, render.getPendingVertexCount())
        render.end()
        assertFalse(render.isBuilding())
        assertEquals(1, render.getBatchCount())
    }

    @Test
    fun `color and texCoord are applied to vertices`() {
        val render = LLRender()
        render.begin(BufferType.TRIANGLES)
        render.color(1f, 0f, 0f)
        render.texCoord(0.5f, 0.5f)
        render.vertex(1f, 2f, 3f)
        render.end()
        val batch = render.getFlushedBatches().first()
        val v = batch.second.first()
        assertEquals(1f, v.r)
        assertEquals(0f, v.g)
        assertEquals(0.5f, v.u)
    }

    @Test
    fun `flush clears all batches`() {
        val render = LLRender()
        render.begin(BufferType.LINES)
        render.vertex(0f, 0f)
        render.vertex(1f, 1f)
        render.end()
        assertTrue(render.getBatchCount() > 0)
        render.flush()
        assertEquals(0, render.getBatchCount())
    }
}

class LLRenderTargetTest {

    @Test
    fun `allocate creates valid FBO`() {
        val rt = LLRenderTarget()
        assertTrue(rt.allocate(256, 256, LLRenderTargetFormat.RGBA))
        assertTrue(rt.isAllocated)
        assertEquals(256, rt.width)
        assertEquals(256, rt.height)
        assertTrue(rt.fboId != 0)
    }

    @Test
    fun `bind and release lifecycle`() {
        val rt = LLRenderTarget()
        rt.allocate(128, 128, LLRenderTargetFormat.RGBA16F)
        rt.bindTarget()
        assertTrue(rt.isBound)
        rt.bindScreen()
        assertFalse(rt.isBound)
        rt.release()
        assertFalse(rt.isAllocated)
        assertEquals(0, rt.fboId)
    }
}

class LLFontGLTest {

    @Test
    fun `default font has positive metrics`() {
        val font = LLFontGL.DEFAULT
        assertTrue(font.ascender > 0f)
        assertTrue(font.descender > 0f)
        assertTrue(font.lineHeight >= font.ascender)
    }

    @Test
    fun `string width increases with length`() {
        val font = LLFontGL("test", 12f)
        val w1 = font.getStringWidth("hi")
        val w2 = font.getStringWidth("hello world")
        assertTrue(w2 > w1)
    }

    @Test
    fun `render returns character count`() {
        val font = LLFontGL("test", 12f)
        val count = font.render("abc", 0f, 0f)
        assertEquals(3, count)
    }
}

class LLShaderMgrTest {

    @Test
    fun `load and retrieve shader`() {
        LLShaderMgr.releaseAll()
        val s = LLShaderMgr.loadShader("myshader", "vertex src", "frag src")
        assertNotNull(s)
        assertTrue(s.isLinked)
        assertEquals(s, LLShaderMgr.getShader("myshader"))
    }

    @Test
    fun `release shader removes it from cache`() {
        LLShaderMgr.releaseAll()
        LLShaderMgr.loadShader("temp", "v", "f")
        LLShaderMgr.releaseShader("temp")
        assertNull(LLShaderMgr.getShader("temp"))
    }

    @Test
    fun `bind and unbind tracks current shader`() {
        LLShaderMgr.releaseAll()
        val s = LLShaderMgr.loadShader("active", "v", "f")!!
        LLShaderMgr.bindShader(s)
        assertEquals(s, LLShaderMgr.currentShader)
        LLShaderMgr.unbindShader()
        assertNull(LLShaderMgr.currentShader)
    }
}
