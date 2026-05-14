/**
 * TextureEntry.kt
 * Kotlin port of lltextureentry.h / lltextureentry.cpp
 *
 * Per-face texture data carried in SL object updates.
 * Network codec logic is stubbed with TODO().
 */

package com.firestorm.llprimitive

import com.firestorm.llcommon.LLUUID
import com.firestorm.llcommon.LLSD
import com.firestorm.llmath.Color4
import java.nio.ByteBuffer
import java.nio.ByteOrder

// ---------------------------------------------------------------------------
// Change flags returned by setters (TEM_CHANGE_*)
// ---------------------------------------------------------------------------

const val TEM_CHANGE_NONE: Int    = 0x0
const val TEM_CHANGE_COLOR: Int   = 0x1
const val TEM_CHANGE_TEXTURE: Int = 0x2
const val TEM_CHANGE_MEDIA: Int   = 0x4
const val TEM_INVALID: Int        = 0x8

// ---------------------------------------------------------------------------
// Bit layout for the bump/shiny/fullbright packed byte:
// +----------+
// | SS FB BBBBB |  S=Shiny(2b), F=Fullbright(1b), B=Bumpmap(5b)
// | 76 5  43210 |
// +----------+
// ---------------------------------------------------------------------------

private const val BUMP_MASK: Int        = 0x1F
private const val FULLBRIGHT_SHIFT: Int = 5
private const val FULLBRIGHT_MASK: Int  = 0x01
private const val SHINY_SHIFT: Int      = 6
private const val SHINY_MASK: Int       = 0x03
private const val BUMP_SHINY_MASK: Int  = 0xC0 or 0x1F

// ---------------------------------------------------------------------------
// Bit layout for the media / texgen packed byte:
// +----------+
// | .....TTM |  T=TexGen(2b), M=Media(1b)
// | 76543210 |
// +----------+
// ---------------------------------------------------------------------------

private const val MEDIA_MASK: Int    = 0x01
private const val TEX_GEN_MASK: Int  = 0x06
private const val TEX_GEN_SHIFT: Int = 1

// ---------------------------------------------------------------------------
// TexGen — texture coordinate generation mode
// ---------------------------------------------------------------------------

enum class TexGen(val bits: UByte) {
    DEFAULT(0x00u),
    PLANAR(0x02u),
    SPHERICAL(0x04u),
    CYLINDRICAL(0x06u);

    companion object {
        fun fromBits(packed: UByte): TexGen {
            val masked = packed and TEX_GEN_MASK.toUByte()
            return entries.firstOrNull { it.bits == masked } ?: DEFAULT
        }
    }
}

// ---------------------------------------------------------------------------
// TextureEntryFace — face-specific overrides (used internally by the list)
// ---------------------------------------------------------------------------

/**
 * Holds the per-face fields that may differ from the "default" face entry in a
 * packed texture-entry message.  A null field means "use the default value from
 * the first entry."
 */
data class TextureEntryFace(
    var textureId: LLUUID? = null,
    var color: Color4?     = null,
    var scaleS: Float?     = null,
    var scaleT: Float?     = null,
    var offsetS: Float?    = null,
    var offsetT: Float?    = null,
    var rotation: Float?   = null,
    var glow: Float?       = null,
    var bumpPacked: UByte? = null,
    var mediaFlags: UByte? = null,
)

// ---------------------------------------------------------------------------
// TextureEntry — per-face texture data
// ---------------------------------------------------------------------------

/**
 * Full texture-entry record for a single face of a prim.
 *
 * Mirrors `LLTextureEntry` from the viewer.  The LLSD keys used in
 * [toSD] / [fromSD] match those produced by the viewer's `asLLSD()`.
 */
data class TextureEntry(
    var textureId: LLUUID = LLUUID.NULL,
    var color: Color4     = Color4(1f, 1f, 1f, 1f),
    var scaleS: Float     = 1f,
    var scaleT: Float     = 1f,
    var offsetS: Float    = 0f,
    var offsetT: Float    = 0f,
    var rotation: Float   = 0f,
    var glow: Float       = 0f,
    /**
     * Packed byte:
     *   bits [4:0] = bumpmap index
     *   bit  [5]   = fullbright flag
     *   bits [7:6] = shininess
     */
    var bumpPacked: UByte = 0u,
    /**
     * Packed byte:
     *   bit  [0]   = media (web-page/movie) flag
     *   bits [2:1] = TexGen mode
     */
    var mediaFlags: UByte = 0u,
) {
    // ---- Decoded accessors for bump/shiny/fullbright -------------------

    var bumpmap: UByte
        get() = (bumpPacked.toInt() and BUMP_MASK).toUByte()
        set(v) {
            bumpPacked = ((bumpPacked.toInt() and BUMP_MASK.inv()) or
                (v.toInt() and BUMP_MASK)).toUByte()
        }

    var fullbright: Boolean
        get() = ((bumpPacked.toInt() ushr FULLBRIGHT_SHIFT) and FULLBRIGHT_MASK) != 0
        set(v) {
            val bit = FULLBRIGHT_MASK shl FULLBRIGHT_SHIFT
            bumpPacked = if (v) (bumpPacked.toInt() or bit).toUByte()
                         else   (bumpPacked.toInt() and bit.inv()).toUByte()
        }

    var shinyness: UByte
        get() = ((bumpPacked.toInt() ushr SHINY_SHIFT) and SHINY_MASK).toUByte()
        set(v) {
            bumpPacked = ((bumpPacked.toInt() and (SHINY_MASK shl SHINY_SHIFT).inv()) or
                ((v.toInt() and SHINY_MASK) shl SHINY_SHIFT)).toUByte()
        }

    /** Bumpmap + shiny bits together, masking out fullbright. */
    val bumpShiny: UByte get() = (bumpPacked.toInt() and BUMP_SHINY_MASK).toUByte()

    // ---- Decoded accessors for media / texgen -------------------------

    var hasMedia: Boolean
        get() = (mediaFlags.toInt() and MEDIA_MASK) != 0
        set(v) {
            mediaFlags = if (v) (mediaFlags.toInt() or MEDIA_MASK).toUByte()
                         else   (mediaFlags.toInt() and MEDIA_MASK.inv()).toUByte()
        }

    var texGen: TexGen
        get() = TexGen.fromBits(mediaFlags)
        set(v) {
            mediaFlags = ((mediaFlags.toInt() and TEX_GEN_MASK.inv()) or
                v.bits.toInt()).toUByte()
        }

    // ---- LLSD serialization -------------------------------------------

    /** Serialize to LLSD using the standard viewer field names. */
    fun toSD(): LLSD = LLSD.map(
        "imageid"  to LLSD.uuid(textureId),
        "colors"   to LLSD.array(
            LLSD.real(color.r.toDouble()),
            LLSD.real(color.g.toDouble()),
            LLSD.real(color.b.toDouble()),
            LLSD.real(color.a.toDouble()),
        ),
        "scales"   to LLSD.real(scaleS.toDouble()),
        "scalet"   to LLSD.real(scaleT.toDouble()),
        "offsets"  to LLSD.real(offsetS.toDouble()),
        "offsett"  to LLSD.real(offsetT.toDouble()),
        "imagerot" to LLSD.real(rotation.toDouble()),
        "bump"     to LLSD.integer(bumpPacked.toInt()),
        "texgen"   to LLSD.integer(mediaFlags.toInt()),
        "glow"     to LLSD.real(glow.toDouble()),
    )

    companion object {
        val NULL = TextureEntry()

        // LLSD key constants (mirroring viewer static const char*)
        const val OBJECT_ID_KEY: String         = "object_id"
        const val OBJECT_MEDIA_DATA_KEY: String = "object_media_data"
        const val MEDIA_VERSION_KEY: String     = "media_version"
        const val TEXTURE_INDEX_KEY: String     = "texture_index"
        const val TEXTURE_MEDIA_DATA_KEY: String = "texture_media_data"

        /** Deserialize from LLSD. */
        fun fromSD(sd: LLSD): TextureEntry = TextureEntry(
            textureId  = sd["imageid"].asUUID(),
            color      = sd["colors"].let { c ->
                Color4(c[0].asFloat(), c[1].asFloat(), c[2].asFloat(), c[3].asFloat())
            },
            scaleS     = sd["scales"].asFloat(),
            scaleT     = sd["scalet"].asFloat(),
            offsetS    = sd["offsets"].asFloat(),
            offsetT    = sd["offsett"].asFloat(),
            rotation   = sd["imagerot"].asFloat(),
            glow       = sd["glow"].asFloat(),
            bumpPacked = sd["bump"].asInt().toUByte(),
            mediaFlags = sd["texgen"].asInt().toUByte(),
        )

        /**
         * Deserialise from a raw packed binary buffer (little-endian).
         * Layout: UUID (16 bytes), RGBA (4 × U8), scaleS/T, offsetS/T,
         * rotation (all F32 LE), bump (U8), mediaFlags (U8), glow (U8/255).
         */
        fun unpack(buf: ByteBuffer): TextureEntry {
            buf.order(ByteOrder.LITTLE_ENDIAN)
            val uuidBytes = ByteArray(16).also { buf.get(it) }
            val id = LLUUID.fromBytes(uuidBytes)
            val r  = buf.get().toInt() and 0xFF
            val g  = buf.get().toInt() and 0xFF
            val b  = buf.get().toInt() and 0xFF
            val a  = buf.get().toInt() and 0xFF
            return TextureEntry(
                textureId  = id,
                color      = Color4(r / 255f, g / 255f, b / 255f, a / 255f),
                scaleS     = buf.float,
                scaleT     = buf.float,
                offsetS    = buf.float,
                offsetT    = buf.float,
                rotation   = buf.float,
                bumpPacked = buf.get().toUByte(),
                mediaFlags = buf.get().toUByte(),
                glow       = (buf.get().toInt() and 0xFF) / 255f,
            )
        }

        /** Emit a media version string touched by the given agent. */
        fun touchMediaVersionString(inVersion: String, agentId: LLUUID): String {
            System.err.println("TextureEntry: touchMediaVersionString not yet implemented")
            return ""
        }

        /** Parse the version number from a media-version string. */
        fun getVersionFromMediaVersionString(versionString: String): UInt {
            System.err.println("TextureEntry: getVersionFromMediaVersionString not yet implemented")
            return 0u
        }

        /** Parse the agent UUID from a media-version string. */
        fun getAgentIDFromMediaVersionString(versionString: String): LLUUID {
            System.err.println("TextureEntry: getAgentIDFromMediaVersionString not yet implemented")
            return LLUUID.NULL
        }

        /** Return whether a string is a valid media-version string. */
        fun isMediaVersionString(versionString: String): Boolean {
            System.err.println("TextureEntry: isMediaVersionString not yet implemented")
            return false
        }
    }

    /**
     * Pack this entry into [buf] in the compact binary format expected by
     * the SL messaging protocol.
     */
    fun pack(buf: ByteBuffer) {
        buf.order(ByteOrder.LITTLE_ENDIAN)
        buf.put(textureId.toBytes())
        buf.put((color.r * 255f + 0.5f).toInt().coerceIn(0, 255).toByte())
        buf.put((color.g * 255f + 0.5f).toInt().coerceIn(0, 255).toByte())
        buf.put((color.b * 255f + 0.5f).toInt().coerceIn(0, 255).toByte())
        buf.put((color.a * 255f + 0.5f).toInt().coerceIn(0, 255).toByte())
        buf.putFloat(scaleS)
        buf.putFloat(scaleT)
        buf.putFloat(offsetS)
        buf.putFloat(offsetT)
        buf.putFloat(rotation)
        buf.put(bumpPacked.toByte())
        buf.put(mediaFlags.toByte())
        buf.put((glow * 255f + 0.5f).toInt().coerceIn(0, 255).toByte())
    }
}

// ---------------------------------------------------------------------------
// TextureList — ordered list of TextureEntry (one per prim face)
// ---------------------------------------------------------------------------

class TextureList(capacity: Int = 0) {
    private val entries: MutableList<TextureEntry> = ArrayList(capacity)

    val size: Int get() = entries.size

    operator fun get(index: Int): TextureEntry = entries[index]
    operator fun set(index: Int, te: TextureEntry) { entries[index] = te }

    fun setSize(n: Int) {
        while (entries.size < n) entries.add(TextureEntry())
        while (entries.size > n) entries.removeAt(entries.size - 1)
    }

    fun setAllIds(id: LLUUID) = entries.forEach { it.textureId = id }

    fun copy(other: TextureList) {
        entries.clear()
        other.entries.mapTo(entries) { it.copy() }
    }

    fun toList(): List<TextureEntry> = entries.toList()
}
