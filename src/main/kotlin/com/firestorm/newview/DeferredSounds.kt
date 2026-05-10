package com.firestorm.newview

object DeferredSounds {

    private val soundVector: MutableList<SoundData> = mutableListOf()

    fun deferSound(sound: SoundData) {
        soundVector.add(sound)
    }

    fun playDeferredSounds() {
        while (soundVector.isNotEmpty()) {
            AudioEngine.instance?.triggerSound(soundVector.last())
            soundVector.removeAt(soundVector.lastIndex)
        }
    }
}
