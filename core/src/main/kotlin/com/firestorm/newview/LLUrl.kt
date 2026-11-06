package com.firestorm.newview

open class LLURL {

    var mURI: String = ""
    var mAuthority: String = ""
    var mPath: String = ""
    var mFilename: String = ""
    var mExtension: String = ""
    var mTag: String = ""

    constructor() {
        init("")
    }

    constructor(url: LLURL) {
        if (url !== this) {
            init(url.getFQURL())
        } else {
            init("")
        }
    }

    constructor(url: String) {
        init(url)
    }

    open fun init(url: String) {
        mURI = ""
        mAuthority = ""
        mPath = ""
        mFilename = ""
        mExtension = ""
        mTag = ""

        var working = url

        val hashIdx = working.indexOf('#')
        if (hashIdx != -1) {
            mTag = working.substring(hashIdx + 1)
            working = working.substring(0, hashIdx)
        }

        val colonIdx = working.indexOf(':')
        if (colonIdx != -1) {
            mURI = working.substring(0, colonIdx)
            working = working.substring(colonIdx + 1)
        }

        if (working.startsWith("//")) {
            working = working.substring(2)
            val slashIdx = working.indexOf('/')
            if (slashIdx != -1) {
                mAuthority = working.substring(0, slashIdx)
                working = working.substring(slashIdx)
            } else {
                mAuthority = working
                working = ""
            }
        }

        val dotIdx = working.lastIndexOf('.')
        if (dotIdx != -1) {
            mExtension = working.substring(dotIdx + 1)
            working = working.substring(0, dotIdx)
        }

        val lastSlash = working.lastIndexOf('/')
        if (lastSlash != -1) {
            mFilename = working.substring(lastSlash + 1)
            mPath = working.substring(0, lastSlash + 1)
        } else {
            mFilename = working
            mPath = ""
        }
    }

    open fun cleanup() {}

    override fun equals(other: Any?): Boolean {
        if (other !is LLURL) return false
        return mURI == other.mURI &&
            mAuthority == other.mAuthority &&
            mPath == other.mPath &&
            mFilename == other.mFilename &&
            mExtension == other.mExtension &&
            mTag == other.mTag
    }

    override fun hashCode(): Int =
        arrayOf(mURI, mAuthority, mPath, mFilename, mExtension, mTag).contentHashCode()

    open fun getFQURL(): String {
        val sb = StringBuilder()
        if (mURI.isNotEmpty()) {
            sb.append(mURI).append(':')
            if (mAuthority.isNotEmpty()) sb.append("//")
        }
        if (mAuthority.isNotEmpty()) sb.append(mAuthority)
        sb.append(mPath)
        sb.append(mFilename)
        if (mExtension.isNotEmpty()) sb.append('.').append(mExtension)
        if (mTag.isNotEmpty()) sb.append('#').append(mTag)
        return sb.toString()
    }

    open fun getFullPath(): String = mPath + mFilename + "." + mExtension

    open fun getAuthority(): String = mAuthority

    open fun updateRelativePath(url: LLURL): String {
        if (mPath.startsWith('/')) return mPath

        val baseParts = url.mPath.trimEnd('/').split('/').toMutableList()
        val relParts = mPath.trimEnd('/').split('/')

        for (part in relParts) {
            when (part) {
                "." -> Unit
                ".." -> if (baseParts.isNotEmpty()) baseParts.removeLast() else baseParts.add("..")
                else -> baseParts.add(part)
            }
        }

        mPath = if (baseParts.isEmpty()) "" else baseParts.joinToString("/") + "/"
        return mPath
    }

    open fun isExtension(compare: String): Boolean = mExtension == compare

    fun assign(rhs: LLURL): LLURL {
        if (rhs !== this) init(rhs.getFQURL())
        return this
    }
}
