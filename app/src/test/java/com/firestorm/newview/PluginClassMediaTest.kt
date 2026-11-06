package com.firestorm.newview

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer

class PluginClassMediaTest {

    private class TestMediaOwner : PluginClassMediaOwner {
        val events = mutableListOf<MediaEvent>()

        override fun handleMediaEvent(plugin: PluginClassMedia, event: MediaEvent) {
            events.add(event)
        }
    }

    private lateinit var owner: TestMediaOwner
    private lateinit var media: PluginClassMedia

    @Before
    fun setUp() {
        owner = TestMediaOwner()
        media = PluginClassMedia(owner)
    }

    @Test
    fun testInProcessEngineInitialization() {
        assertTrue("In-process native engine must initialize upon creation", media.isInitialized)
        assertFalse("Native engine must operate in-process without external helper process", media.isExternalProcess)
        assertEquals("Initial media status should be MEDIA_NONE", MediaStatus.MEDIA_NONE, media.status)
    }

    @Test
    fun testDirectHardwareTextureBindingAndRelease() {
        media.setSize(500, 500)
        assertEquals("Media width must match set size", 500, media.mediaWidth)
        assertEquals("Texture width must round up to next power of 2", 512, media.textureWidth)

        // Bind OpenGL texture handle
        val bound = media.bindTexture(1001)
        assertTrue("Texture binding should succeed for valid texture ID", bound)
        assertEquals("GL texture ID must match bound ID", 1001, media.glTextureId)
        assertTrue("Texture should be marked as bound", media.isTextureBound)

        // Lock frame buffer direct memory
        val buffer: ByteBuffer? = media.lockFrameBuffer()
        assertNotNull("Direct frame buffer memory must be allocated and accessible", buffer)
        assertTrue("Direct frame buffer must be direct ByteBuffer", buffer!!.isDirect)
        media.unlockFrameBuffer()

        // Upload frame directly to hardware texture
        val uploaded = media.uploadFrameToTexture()
        assertTrue("Direct frame upload to texture handle must succeed", uploaded)

        // Release texture
        media.releaseTexture()
        assertEquals("GL texture ID must be 0 after release", 0, media.glTextureId)
        assertFalse("Texture must no longer be bound after release", media.isTextureBound)
        assertNull("Frame buffer direct memory must be freed on release", media.lockFrameBuffer())
    }

    @Test
    fun testCleanResourceReleaseOnHiddenPriority() {
        media.setSize(256, 256)
        media.bindTexture(2002)
        assertTrue("Texture must be bound prior to priority change", media.isTextureBound)

        // Set priority to HIDDEN
        media.setPriority(PluginPriority.HIDDEN)
        assertFalse("Texture must release cleanly when priority is set to HIDDEN", media.isTextureBound)
        assertEquals("GL texture ID must reset to 0 when hidden", 0, media.glTextureId)
    }

    @Test
    fun testDirectInputEventForwarding() {
        media.setSize(800, 600)

        // Mouse events
        media.mouseEvent(MouseEventType.DOWN, 0, 100, 200, 0)
        media.mouseEvent(MouseEventType.MOVE, 0, 105, 205, 0)
        media.mouseEvent(MouseEventType.UP, 0, 105, 205, 0)

        // Key events
        val keyConsumed = media.keyEvent(KeyEventType.DOWN, 65, 0)
        assertTrue("Key event dispatching must return boolean indicating handling", keyConsumed)

        // Scroll events
        media.scrollEvent(100, 200, 0, 5, 0)

        // Text input
        val textConsumed = media.textInput("Hello Second Life", 0)
        assertTrue("Text input event dispatching must succeed", textConsumed)
    }

    @Test
    fun testMediaStateManagementAndNavigationCallbacks() {
        media.setSize(640, 480)
        assertTrue("Owner should receive SIZE_CHANGED event", owner.events.contains(MediaEvent.SIZE_CHANGED))

        // Navigate URI
        val testUri = "https://secondlife.com/welcome"
        media.loadUri(testUri)

        assertEquals("navigateUri must populate correctly", testUri, media.navigateUri)
        assertEquals("location must match navigateUri", testUri, media.location)
        assertEquals("Media status must transition to MEDIA_LOADED after navigation", MediaStatus.MEDIA_LOADED, media.status)
        assertEquals("Progress percent must update to 100 on load completion", 100, media.progressPercent)

        // Verify owner received expected callbacks
        assertTrue("Owner must receive NAVIGATE_BEGIN event", owner.events.contains(MediaEvent.NAVIGATE_BEGIN))
        assertTrue("Owner must receive PROGRESS_UPDATED event", owner.events.contains(MediaEvent.PROGRESS_UPDATED))
        assertTrue("Owner must receive NAVIGATE_COMPLETE event", owner.events.contains(MediaEvent.NAVIGATE_COMPLETE))
        assertTrue("Owner must receive CONTENT_UPDATED event", owner.events.contains(MediaEvent.CONTENT_UPDATED))
    }

    @Test
    fun testResetAndDestroyLifecycle() {
        media.setSize(300, 300)
        media.bindTexture(3003)
        media.loadUri("https://example.com")

        media.reset()
        assertEquals("Status must reset to MEDIA_NONE", MediaStatus.MEDIA_NONE, media.status)
        assertEquals("navigateUri must reset", "", media.navigateUri)
        assertFalse("Texture must unbind on reset", media.isTextureBound)

        media.destroy()
        assertFalse("Engine should no longer be initialized after destroy", media.isInitialized)
    }
}
