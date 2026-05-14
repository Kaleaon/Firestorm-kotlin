/**
 * MaterialTable.kt
 * Kotlin port of llmaterialtable.h / llmaterialtable.cpp
 *
 * Singleton table mapping prim material type codes to names, physics
 * properties, default textures, and sound UUIDs.
 *
 * Sound-matrix lookup and collision-sound helpers are stubbed with TODO()
 * because they depend on the sound-ID definitions from the viewer sound layer.
 */

package com.firestorm.llprimitive

import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// Material type codes (LL_MCODE_*)
// ---------------------------------------------------------------------------

/** Raw byte codes for each prim material type, as sent over the wire. */
object MaterialCode {
    const val STONE: UByte   = 0u
    const val METAL: UByte   = 1u
    const val GLASS: UByte   = 2u
    const val WOOD: UByte    = 3u
    const val FLESH: UByte   = 4u
    const val PLASTIC: UByte = 5u
    const val RUBBER: UByte  = 6u
    const val LIGHT: UByte   = 7u
    const val END: UByte     = 8u
    const val MASK: UByte    = 0x0Fu
}

// ---------------------------------------------------------------------------
// MaterialType — named enum matching C++ LL_MCODE_* constants
// ---------------------------------------------------------------------------

/** Enumerated material types, ordered to match their byte-code values. */
enum class MaterialType(val code: UByte) {
    STONE(MaterialCode.STONE),
    METAL(MaterialCode.METAL),
    GLASS(MaterialCode.GLASS),
    WOOD(MaterialCode.WOOD),
    FLESH(MaterialCode.FLESH),
    PLASTIC(MaterialCode.PLASTIC),
    RUBBER(MaterialCode.RUBBER),
    LIGHT(MaterialCode.LIGHT);

    companion object {
        private val byCode: Map<UByte, MaterialType> = entries.associateBy { it.code }
        fun fromCode(code: UByte): MaterialType? = byCode[code]
    }
}

// ---------------------------------------------------------------------------
// MaterialInfo — per-material properties
// ---------------------------------------------------------------------------

/**
 * All physics and audio properties for a single material type.
 *
 * Default values for [density], [friction], and [restitution] match the
 * `LLMaterialInfo::init()` function in the original C++ source.
 */
data class MaterialInfo(
    val type: MaterialType,
    val name: String,
    val defaultTextureId: LLUUID,
    val density: Float,         // kg/m³
    val friction: Float,        // physics friction coefficient
    val restitution: Float,     // physics bounciness coefficient
    val shatterSoundId: LLUUID  = LLUUID.NULL,
    val hpModifier: Float       = 1f,
    val damageModifier: Float   = 1f,
    val epModifier: Float       = 1f,
) {
    val mcode: UByte get() = type.code
}

// ---------------------------------------------------------------------------
// MaterialTable — singleton
// ---------------------------------------------------------------------------

/**
 * Global material property table.  Mirrors `LLMaterialTable` from the C++
 * viewer, with `LLMaterialTable::basic` collapsed into this singleton.
 *
 * Default texture UUIDs and sound UUIDs are taken from the original sources:
 *  - material_codes.cpp for texture UUIDs
 *  - llmaterialtable.cpp for shatter-sound UUIDs
 *  - Physics values from llmaterialtable.cpp `addFriction()` / `addRestitution()`
 *
 * Collision / sliding / rolling sound matrices are stubbed with TODO() because
 * they depend on sound_ids.h constants not yet ported.
 */
object MaterialTable {

    // ---- Friction constants (Havok4 values) ----------------------------

    const val FRICTION_MIN: Float     = 0.15f
    const val FRICTION_GLASS: Float   = 0.13f
    const val FRICTION_LIGHT: Float   = 0.14f
    const val FRICTION_METAL: Float   = 0.22f
    const val FRICTION_PLASTIC: Float = 0.30f
    const val FRICTION_WOOD: Float    = 0.44f
    const val FRICTION_FLESH: Float   = 0.46f
    const val FRICTION_LAND: Float    = 0.58f
    const val FRICTION_STONE: Float   = 0.60f
    const val FRICTION_RUBBER: Float  = 0.67f
    const val FRICTION_MAX: Float     = 0.71f

    // ---- Restitution constants -----------------------------------------

    const val RESTITUTION_MIN: Float     = 0.02f
    const val RESTITUTION_LAND: Float    = RESTITUTION_MIN
    const val RESTITUTION_FLESH: Float   = 0.20f
    const val RESTITUTION_STONE: Float   = 0.40f
    const val RESTITUTION_METAL: Float   = 0.40f
    const val RESTITUTION_WOOD: Float    = 0.50f
    const val RESTITUTION_GLASS: Float   = 0.70f
    const val RESTITUTION_PLASTIC: Float = 0.70f
    const val RESTITUTION_LIGHT: Float   = 0.70f
    const val RESTITUTION_RUBBER: Float  = 0.90f
    const val RESTITUTION_MAX: Float     = 0.95f

    const val DEFAULT_FRICTION: Float    = 0.50f
    const val DEFAULT_RESTITUTION: Float = 0.40f

    // ---- Density / avatar constants ------------------------------------

    /** Default prim density (kg/m³). */
    const val DEFAULT_OBJECT_DENSITY: Float        = 1000f
    /** Density used for legacy LSL mass calculations. */
    const val LEGACY_DEFAULT_OBJECT_DENSITY: Float = 10f
    /** Effective density for avatar physics. */
    const val DEFAULT_AVATAR_DENSITY: Float        = 445.3f

    // ---- Material entries ---------------------------------------------

    /**
     * Complete table of materials, populated from llmaterialtable.cpp
     * `initBasicTable()`.
     *
     * Density values are the viewer's internal "prim density" (not SI kg/m³);
     * friction and restitution match the Havok4-era constants above.
     */
    val entries: List<MaterialInfo> = listOf(
        MaterialInfo(
            type             = MaterialType.STONE,
            name             = "Stone",
            defaultTextureId = LLUUID("87c5765b-aa26-43eb-b8c6-c09a1ca6208e"),
            density          = 30f,
            friction         = 0.80f,
            restitution      = 0.40f,
            shatterSoundId   = LLUUID("ea296329-0f09-4993-af1b-e6784bab1dc9"),
        ),
        MaterialInfo(
            type             = MaterialType.METAL,
            name             = "Metal",
            defaultTextureId = LLUUID("6f3c53e9-ba60-4010-8f3e-30f51a762476"),
            density          = 50f,
            friction         = 0.30f,
            restitution      = 0.40f,
            shatterSoundId   = LLUUID("d1375446-1c4d-470b-9135-30132433b678"),
        ),
        MaterialInfo(
            type             = MaterialType.GLASS,
            name             = "Glass",
            defaultTextureId = LLUUID("b4ba225c-373f-446d-9f7e-6cb7b5cf9b3d"),
            density          = 20f,
            friction         = 0.20f,
            restitution      = 0.70f,
            shatterSoundId   = LLUUID("85cda060-b393-48e6-81c8-2cfdfb275351"),
        ),
        MaterialInfo(
            type             = MaterialType.WOOD,
            name             = "Wood",
            defaultTextureId = LLUUID("89556747-24cb-43ed-920b-47caed15465f"),
            density          = 10f,
            friction         = 0.60f,
            restitution      = 0.50f,
            shatterSoundId   = LLUUID("6f00669f-15e0-4793-a63e-c03f62fee43a"),
        ),
        MaterialInfo(
            type             = MaterialType.FLESH,
            name             = "Flesh",
            defaultTextureId = LLUUID("80736669-e4b9-450e-8890-d5169f988a50"),
            density          = 10f,
            friction         = 0.90f,
            restitution      = 0.30f,
            shatterSoundId   = LLUUID("2d8c6f51-149e-4e23-8413-93a379b42b67"),
        ),
        MaterialInfo(
            type             = MaterialType.PLASTIC,
            name             = "Plastic",
            defaultTextureId = LLUUID("304fcb4e-7d33-4339-ba80-76d3d22dc11a"),
            density          = 5f,
            friction         = 0.40f,
            restitution      = 0.70f,
            shatterSoundId   = LLUUID("d55c7f3c-e1c3-4ddc-9eff-9ef805d9190e"),
        ),
        MaterialInfo(
            type             = MaterialType.RUBBER,
            name             = "Rubber",
            defaultTextureId = LLUUID("9fae0bc5-666d-477e-9f70-84e8556ec867"),
            density          = 0.5f,
            friction         = 0.90f,
            restitution      = 0.90f,
            shatterSoundId   = LLUUID("212b6d1e-8d9c-4986-b3aa-f3c6df8d987d"),
        ),
        MaterialInfo(
            type             = MaterialType.LIGHT,
            name             = "Light",
            defaultTextureId = LLUUID("00000000-0000-0000-0000-000000000000"),
            density          = 20f,
            friction         = 0.20f,
            restitution      = 0.70f,
            shatterSoundId   = LLUUID("d55c7f3c-e1c3-4ddc-9eff-9ef805d9190e"),
        ),
    )

    // ---- Index maps ----------------------------------------------------

    private val byCode: Map<UByte, MaterialInfo>   = entries.associateBy { it.mcode }
    private val byName: Map<String, MaterialInfo>  = entries.associateBy { it.name }
    private val byType: Map<MaterialType, MaterialInfo> = entries.associateBy { it.type }

    // ---- Accessors -----------------------------------------------------

    fun get(code: UByte): MaterialInfo?       = byCode[code]
    fun get(name: String): MaterialInfo?      = byName[name]
    fun get(type: MaterialType): MaterialInfo = byType.getValue(type)

    fun getName(mcode: UByte): String          = byCode[mcode]?.name ?: ""
    fun getMCode(name: String): UByte?         = byName[name]?.mcode
    fun getMaterialType(name: String): MaterialType? = byName[name]?.type

    fun getDefaultTextureId(name: String): LLUUID = byName[name]?.defaultTextureId ?: LLUUID.NULL
    fun getDefaultTextureId(mcode: UByte): LLUUID = byCode[mcode]?.defaultTextureId ?: LLUUID.NULL

    fun getDensity(mcode: UByte): Float      = byCode[mcode]?.density    ?: 0f
    fun getFriction(mcode: UByte): Float     = byCode[mcode]?.friction   ?: DEFAULT_FRICTION
    fun getRestitution(mcode: UByte): Float  = byCode[mcode]?.restitution?: DEFAULT_RESTITUTION
    fun getHPMod(mcode: UByte): Float        = byCode[mcode]?.hpModifier ?: 1f
    fun getDamageMod(mcode: UByte): Float    = byCode[mcode]?.damageModifier ?: 1f
    fun getEPMod(mcode: UByte): Float        = byCode[mcode]?.epModifier ?: 1f

    fun getShatterSoundUUID(mcode: UByte): LLUUID =
        byCode[mcode]?.shatterSoundId ?: LLUUID.NULL

    // ---- Sound matrices (stubbed — depend on sound_ids.h) -------------

    /**
     * Return the UUID of the collision sound for the pair (mcode, mcode2).
     * Full implementation requires the collision-sound matrix populated from
     * sound_ids.h constants.
     */
    fun getCollisionSoundUUID(mcode: UByte, mcode2: UByte): LLUUID =
        LLUUID.NULL

    /**
     * Return the UUID of the sliding sound for the pair (mcode, mcode2).
     */
    fun getSlidingSoundUUID(mcode: UByte, mcode2: UByte): LLUUID =
        LLUUID.NULL

    /**
     * Return the UUID of the rolling sound for the pair (mcode, mcode2).
     */
    fun getRollingSoundUUID(mcode: UByte, mcode2: UByte): LLUUID =
        LLUUID.NULL

    fun getGroundCollisionSoundUUID(mcode: UByte): LLUUID =
        LLUUID.NULL

    fun getGroundSlidingSoundUUID(mcode: UByte): LLUUID =
        LLUUID.NULL

    fun getGroundRollingSoundUUID(mcode: UByte): LLUUID =
        LLUUID.NULL

    fun isCollisionSound(uuid: LLUUID): Boolean =
        false

    /**
     * Translate display names using a provided name map (e.g. for
     * localisation).  Mirrors `initTableTransNames()` from the C++ source.
     */
    fun translatedNames(nameMap: Map<String, String>): Map<UByte, String> =
        entries.associate { info ->
            info.mcode to (nameMap[info.name] ?: info.name)
        }
}
