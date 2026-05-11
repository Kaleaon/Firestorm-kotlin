package com.firestorm.newview

interface CapabilityProvider {

    fun getCapability(name: String): String

    fun getHost(): String

    fun getDescription(): String
}
