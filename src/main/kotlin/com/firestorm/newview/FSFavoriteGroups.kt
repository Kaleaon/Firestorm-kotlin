package com.firestorm.newview

import java.util.UUID

object FSFavoriteGroups {

    private val favoriteGroups: MutableSet<UUID> = mutableSetOf()
    private val favoritesChangedListeners: MutableList<() -> Unit> = mutableListOf()

    fun setFavoritesChangedCallback(cb: () -> Unit): () -> Unit {
        favoritesChangedListeners.add(cb)
        return { favoritesChangedListeners.remove(cb) }
    }

    fun isFavorite(groupId: UUID): Boolean = favoriteGroups.contains(groupId)

    fun addFavorite(groupId: UUID) {
        if (groupId == UUID(0L, 0L)) return
        if (favoriteGroups.add(groupId)) {
            saveFavorites()
            favoritesChangedListeners.forEach { it() }
        }
    }

    fun removeFavorite(groupId: UUID) {
        if (favoriteGroups.remove(groupId)) {
            saveFavorites()
            favoritesChangedListeners.forEach { it() }
        }
    }

    fun toggleFavorite(groupId: UUID) {
        if (isFavorite(groupId)) removeFavorite(groupId) else addFavorite(groupId)
    }

    fun getFavorites(): Set<UUID> = favoriteGroups

    fun hasFavorites(): Boolean = favoriteGroups.isNotEmpty()

    fun loadFavorites() {
        favoriteGroups.clear()
        TODO("APR: use JVM equivalent — read FSFavoriteGroups array from per-account settings LLSD and populate favoriteGroups")
    }

    fun saveFavorites() {
        TODO("APR: use JVM equivalent — prune groups no longer joined via agent membership check, then persist favoriteGroups to per-account settings LLSD")
    }
}
