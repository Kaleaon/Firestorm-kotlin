package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llrender.Texture

open class ViewerTexture(val id: LLUUID) : Texture(id) {

    enum class FetchState { UNLOADED, LOADING, LOADED, FAILED }

    var fetchPriority: Float = 0f
    var fetchState: FetchState = FetchState.UNLOADED
    var loadedCallback: ((ViewerTexture) -> Unit)? = null

    fun isFullyLoaded(): Boolean = fetchState == FetchState.LOADED
}

class ViewerFetchedTexture(id: LLUUID) : ViewerTexture(id) {

    var url: String = ""

    fun scheduleCreateTexture() {}

    fun cancelFetch() {}
}

object TextureList {

    val textures: MutableMap<LLUUID, ViewerTexture> = mutableMapOf()
    var numImagesDecoded: Int = 0

    fun getImage(id: LLUUID, texType: Int = 0, useMipMaps: Boolean = true): ViewerTexture =
        textures.getOrPut(id) { ViewerFetchedTexture(id) }

    fun findImage(id: LLUUID): ViewerTexture? = textures[id]

    fun removeImage(tex: ViewerTexture) {
        textures.remove(tex.id)
    }

    fun updateImages(maxTime: Float) {}

    fun decodeAllImages(maxTime: Float) {}

    fun shutdown() {
        textures.clear()
        numImagesDecoded = 0
    }
}
