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
// Collision / sliding / rolling sound UUID constants
// (from indra/llprimitive/sound_ids.cpp in the Second Life viewer)
// ---------------------------------------------------------------------------

private object SoundIds {
    // Collision sounds (material × material)
    val FLESH_FLESH          = LLUUID("dce5fdd4-afe4-4ea1-822f-dd52cac46b08")
    val FLESH_PLASTIC        = LLUUID("51011582-fbca-4580-ae9e-1a5593f094ec")
    val FLESH_RUBBER         = LLUUID("68d62208-e257-4d0c-bbe2-20c9ea9760bb")
    val GLASS_FLESH          = LLUUID("75872e8c-bc39-451b-9b0b-042d7ba36cba")
    val GLASS_GLASS          = LLUUID("6a45ba0b-5775-4ea8-8513-26008a17f873")
    val GLASS_PLASTIC        = LLUUID("992a6d1b-8c77-40e0-9495-4098ce539694")
    val GLASS_RUBBER         = LLUUID("2de4da5a-faf8-46be-bac6-c4d74f1e5767")
    val GLASS_WOOD           = LLUUID("6e3fb0f7-6d9c-42ca-b86b-1122ff562d7d")
    val METAL_FLESH          = LLUUID("14209133-4961-4acc-9649-53fc38ee1667")
    val METAL_GLASS          = LLUUID("bc4a4348-cfcc-4e5e-908e-8a52a8915fe6")
    val METAL_METAL          = LLUUID("9e5c1297-6eed-40c0-825a-d9bcd86e3193")
    val METAL_PLASTIC        = LLUUID("e534761c-1894-4b61-b20c-658a6fb68157")
    val METAL_RUBBER         = LLUUID("8761f73f-6cf9-4186-8aaa-0948ed002db1")
    val METAL_WOOD           = LLUUID("874a26fd-142f-4173-8c5b-890cd846c74d")
    val PLASTIC_PLASTIC      = LLUUID("0e24a717-b97e-4b77-9c94-b59a5a88b2da")
    val RUBBER_PLASTIC       = LLUUID("75cf3ade-9a5b-4c4d-bb35-f9799bda7fb2")
    val RUBBER_RUBBER        = LLUUID("153c8bf7-fb89-4d89-b263-47e58b1b4774")
    val STONE_FLESH          = LLUUID("55c3e0ce-275a-46fa-82ff-e0465f5e8703")
    val STONE_GLASS          = LLUUID("24babf58-7156-4841-9a3f-761bdbb8e237")
    val STONE_METAL          = LLUUID("aca261d8-e145-4610-9e20-9eff990f2c12")
    val STONE_PLASTIC        = LLUUID("0642fba6-5dcf-4d62-8e7b-94dbb529d117")
    val STONE_RUBBER         = LLUUID("25a863e8-dc42-4e8a-a357-e76422ace9b5")
    val STONE_STONE          = LLUUID("9538f37c-456e-4047-81be-6435045608d4")
    val STONE_WOOD           = LLUUID("8c0f84c3-9afd-4396-b5f5-9bca2c911c20")
    val WOOD_FLESH           = LLUUID("be582e5d-b123-41a2-a150-454c39e961c8")
    val WOOD_PLASTIC         = LLUUID("c70141d4-ba06-41ea-bcbc-35ea81cb8335")
    val WOOD_RUBBER          = LLUUID("7d1826f4-24c4-4aac-8c2e-eff45df37783")
    val WOOD_WOOD            = LLUUID("063c97d3-033a-4e9b-98d8-05c8074922cb")

    // Sliding sounds
    val SLIDE_STONE_STONE    = LLUUID("ade766dc-2e75-4699-9b41-7c8e53d2b3f2")
    val SLIDE_STONE_STONE_01 = LLUUID("2a7dcbd1-d3e6-4767-8432-8322648e7b9d")
    val SLIDE_STONE_WOOD     = LLUUID("174ef324-ed50-4f65-9479-b4da580aeb3c")
    val SLIDE_STONE_PLASTIC  = LLUUID("afd0bcc3-d41a-4572-9e7f-08a29eeb0b8a")
    val SLIDE_STONE_RUBBER   = LLUUID("0724b946-6a3f-4eeb-bb50-0a3b33120974")
    val SLIDE_METAL_METAL    = LLUUID("09461277-c691-45de-b2c5-89dfd3712f79")
    val SLIDE_METAL_GLASS    = LLUUID("4188be39-7b1f-4495-bf2b-83ddd82eea05")
    val SLIDE_METAL_WOOD     = LLUUID("4afb6926-a73f-4cb7-85d5-0f9a40107434")
    val SLIDE_METAL_FLESH    = LLUUID("dde65837-633c-4841-af2f-62ec471bf61e")
    val SLIDE_METAL_RUBBER   = LLUUID("12d97bc0-3c15-4744-b6bd-77d1316eb4f0")
    val SLIDE_WOOD_WOOD      = LLUUID("3079d569-b3e8-4df4-9e09-f0d4611213ef")
    val SLIDE_WOOD_FLESH     = LLUUID("84b026f3-a11c-4366-aa7c-07edcd89b2bb")
    val SLIDE_WOOD_PLASTIC   = LLUUID("505ca3c4-94a0-4e28-8fc1-ea72a428396b")
    val SLIDE_FLESH_FLESH    = LLUUID("614eec22-f73d-4fdc-8691-a37dc5c58333")
    val SLIDE_RUBBER_PLASTIC = LLUUID("a98ffa5a-e48e-4f9d-9242-b9a3210ad84a")

    // Rolling sounds
    val ROLL_STONE_STONE     = LLUUID("67d56e3f-6ed5-4658-9418-14f020c38b11")
    val ROLL_STONE_WOOD      = LLUUID("53e46fb7-6c21-4fe1-bffe-0567475d48fa")
    val ROLL_STONE_PLASTIC   = LLUUID("155f65a8-cae7-476e-a58b-fd362be7fd0e")
    val ROLL_METAL_GLASS     = LLUUID("63d530bb-a41f-402b-aa1f-be6b11959809")
    val ROLL_METAL_WOOD      = LLUUID("1d76af57-01b1-4c73-9a1d-69523bfa50ea")
    val ROLL_GLASS_WOOD      = LLUUID("d40b1f48-a061-4f6e-b18f-4326a3dd5c29")
    val ROLL_WOOD_WOOD       = LLUUID("2cc8eec4-bb4a-4ba8-b783-71526ec708e8")
    val ROLL_WOOD_FLESH      = LLUUID("26ee185d-6fc3-49f8-89ba-51cab04cfc42")
    val ROLL_WOOD_PLASTIC    = LLUUID("71c1000a-9f16-4cc3-8ede-ec4aa3bf5723")
    val ROLL_FLESH_PLASTIC   = LLUUID("89a0be4c-848d-4a6e-8886-298f56c2cff4")
    val ROLL_PLASTIC_PLASTIC = LLUUID("873f3d82-00b2-4082-9c69-7aef3461dba1")

    // Ground sounds
    val STONE_DIRT_02        = LLUUID("cbe75eb2-3375-41d8-9e3f-2ae46b4164ed")
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

    // ---- Sound matrices --------------------------------------------------

    /**
     * Flat 8×8 collision-sound matrix, indexed by [mcode1 * END + mcode2].
     * Symmetric: both [i][j] and [j][i] are set to the same UUID.
     * Populated from the Second Life viewer's `initBasicTable()`.
     */
    private val collisionSoundMatrix: Array<LLUUID> by lazy {
        val m = Array(MaterialCode.END.toInt() * MaterialCode.END.toInt()) { LLUUID.NULL }
        fun set(a: UByte, b: UByte, id: LLUUID) {
            val end = MaterialCode.END.toInt()
            m[a.toInt() * end + b.toInt()] = id
            if (a != b) m[b.toInt() * end + a.toInt()] = id
        }
        val S = MaterialCode.STONE; val Me = MaterialCode.METAL
        val G = MaterialCode.GLASS; val W  = MaterialCode.WOOD
        val F = MaterialCode.FLESH; val P  = MaterialCode.PLASTIC
        val R = MaterialCode.RUBBER; val L = MaterialCode.LIGHT
        set(S, S, SoundIds.STONE_STONE);  set(S, Me, SoundIds.STONE_METAL)
        set(S, G, SoundIds.STONE_GLASS);  set(S, W,  SoundIds.STONE_WOOD)
        set(S, F, SoundIds.STONE_FLESH);  set(S, P,  SoundIds.STONE_PLASTIC)
        set(S, R, SoundIds.STONE_RUBBER); set(S, L,  SoundIds.STONE_PLASTIC)
        set(Me, Me, SoundIds.METAL_METAL); set(Me, G, SoundIds.METAL_GLASS)
        set(Me, W,  SoundIds.METAL_WOOD);  set(Me, F, SoundIds.METAL_FLESH)
        set(Me, P,  SoundIds.METAL_PLASTIC); set(Me, L, SoundIds.METAL_PLASTIC)
        set(Me, R,  SoundIds.METAL_RUBBER)
        set(G, G, SoundIds.GLASS_GLASS); set(G, W, SoundIds.GLASS_WOOD)
        set(G, F, SoundIds.GLASS_FLESH); set(G, P, SoundIds.GLASS_PLASTIC)
        set(G, R, SoundIds.GLASS_RUBBER); set(G, L, SoundIds.GLASS_PLASTIC)
        set(W, W, SoundIds.WOOD_WOOD);  set(W, F, SoundIds.WOOD_FLESH)
        set(W, P, SoundIds.WOOD_PLASTIC); set(W, R, SoundIds.WOOD_RUBBER)
        set(W, L, SoundIds.WOOD_PLASTIC)
        set(F, F, SoundIds.FLESH_FLESH); set(F, P, SoundIds.FLESH_PLASTIC)
        set(F, R, SoundIds.FLESH_RUBBER); set(F, L, SoundIds.FLESH_PLASTIC)
        set(R, R, SoundIds.RUBBER_RUBBER); set(R, P, SoundIds.RUBBER_PLASTIC)
        set(R, L, SoundIds.RUBBER_PLASTIC)
        set(P, P, SoundIds.PLASTIC_PLASTIC); set(P, L, SoundIds.PLASTIC_PLASTIC)
        set(L, L, SoundIds.PLASTIC_PLASTIC)
        m
    }

    private val slidingSoundMatrix: Array<LLUUID> by lazy {
        val m = Array(MaterialCode.END.toInt() * MaterialCode.END.toInt()) { LLUUID.NULL }
        fun set(a: UByte, b: UByte, id: LLUUID) {
            val end = MaterialCode.END.toInt()
            m[a.toInt() * end + b.toInt()] = id
            if (a != b) m[b.toInt() * end + a.toInt()] = id
        }
        val S = MaterialCode.STONE; val Me = MaterialCode.METAL
        val G = MaterialCode.GLASS; val W  = MaterialCode.WOOD
        val F = MaterialCode.FLESH; val P  = MaterialCode.PLASTIC
        val R = MaterialCode.RUBBER; val L = MaterialCode.LIGHT
        val D = SoundIds.SLIDE_STONE_STONE_01  // common default
        set(S, S, SoundIds.SLIDE_STONE_STONE); set(S, Me, D)
        set(S, G, D); set(S, W, SoundIds.SLIDE_STONE_WOOD)
        set(S, F, D); set(S, P,  SoundIds.SLIDE_STONE_PLASTIC)
        set(S, R, SoundIds.SLIDE_STONE_RUBBER); set(S, L, SoundIds.SLIDE_STONE_PLASTIC)
        set(Me, Me, SoundIds.SLIDE_METAL_METAL); set(Me, G, SoundIds.SLIDE_METAL_GLASS)
        set(Me, W,  SoundIds.SLIDE_METAL_WOOD); set(Me, F, SoundIds.SLIDE_METAL_FLESH)
        set(Me, P,  D); set(Me, R, SoundIds.SLIDE_METAL_RUBBER); set(Me, L, D)
        set(G, G, D); set(G, W, D); set(G, F, D)
        set(G, P, D); set(G, R, D); set(G, L, D)
        set(W, W, SoundIds.SLIDE_WOOD_WOOD); set(W, F, SoundIds.SLIDE_WOOD_FLESH)
        set(W, P, SoundIds.SLIDE_WOOD_PLASTIC); set(W, R, D); set(W, L, SoundIds.SLIDE_WOOD_PLASTIC)
        set(F, F, SoundIds.SLIDE_FLESH_FLESH); set(F, P, D); set(F, R, D); set(F, L, D)
        set(R, R, D); set(R, P, SoundIds.SLIDE_RUBBER_PLASTIC); set(R, L, SoundIds.SLIDE_RUBBER_PLASTIC)
        set(P, P, D); set(P, L, D)
        set(L, L, D)
        m
    }

    private val rollingSoundMatrix: Array<LLUUID> by lazy {
        val m = Array(MaterialCode.END.toInt() * MaterialCode.END.toInt()) { LLUUID.NULL }
        fun set(a: UByte, b: UByte, id: LLUUID) {
            val end = MaterialCode.END.toInt()
            m[a.toInt() * end + b.toInt()] = id
            if (a != b) m[b.toInt() * end + a.toInt()] = id
        }
        val S = MaterialCode.STONE; val Me = MaterialCode.METAL
        val G = MaterialCode.GLASS; val W  = MaterialCode.WOOD
        val F = MaterialCode.FLESH; val P  = MaterialCode.PLASTIC
        val R = MaterialCode.RUBBER; val L = MaterialCode.LIGHT
        val D = SoundIds.SLIDE_STONE_STONE_01  // common default
        set(S, S, SoundIds.ROLL_STONE_STONE); set(S, Me, D)
        set(S, G, D); set(S, W, SoundIds.ROLL_STONE_WOOD)
        set(S, F, D); set(S, P, SoundIds.ROLL_STONE_PLASTIC)
        set(S, R, D); set(S, L, SoundIds.ROLL_STONE_PLASTIC)
        set(Me, Me, D); set(Me, G, SoundIds.ROLL_METAL_GLASS)
        set(Me, W, SoundIds.ROLL_METAL_WOOD); set(Me, F, D)
        set(Me, P, SoundIds.ROLL_METAL_WOOD); set(Me, R, D); set(Me, L, SoundIds.ROLL_METAL_WOOD)
        set(G, G, D); set(G, W, SoundIds.ROLL_GLASS_WOOD)
        set(G, F, D); set(G, P, D); set(G, R, D); set(G, L, D)
        set(W, W, SoundIds.ROLL_WOOD_WOOD); set(W, F, SoundIds.ROLL_WOOD_FLESH)
        set(W, P, SoundIds.ROLL_WOOD_PLASTIC); set(W, R, D); set(W, L, SoundIds.ROLL_WOOD_PLASTIC)
        set(F, F, D); set(F, P, SoundIds.ROLL_FLESH_PLASTIC); set(F, R, D); set(F, L, SoundIds.ROLL_FLESH_PLASTIC)
        set(R, R, D); set(R, P, D); set(R, L, D)
        set(P, P, SoundIds.ROLL_PLASTIC_PLASTIC); set(P, L, SoundIds.ROLL_PLASTIC_PLASTIC)
        set(L, L, SoundIds.ROLL_PLASTIC_PLASTIC)
        m
    }

    // ---- Sound matrix accessors -----------------------------------------

    fun getCollisionSoundUUID(mcode: UByte, mcode2: UByte): LLUUID {
        val a = (mcode and MaterialCode.MASK).toInt()
        val b = (mcode2 and MaterialCode.MASK).toInt()
        val end = MaterialCode.END.toInt()
        return if (a < end && b < end) collisionSoundMatrix[a * end + b] else LLUUID.NULL
    }

    fun getSlidingSoundUUID(mcode: UByte, mcode2: UByte): LLUUID {
        val a = (mcode and MaterialCode.MASK).toInt()
        val b = (mcode2 and MaterialCode.MASK).toInt()
        val end = MaterialCode.END.toInt()
        return if (a < end && b < end) slidingSoundMatrix[a * end + b] else LLUUID.NULL
    }

    fun getRollingSoundUUID(mcode: UByte, mcode2: UByte): LLUUID {
        val a = (mcode and MaterialCode.MASK).toInt()
        val b = (mcode2 and MaterialCode.MASK).toInt()
        val end = MaterialCode.END.toInt()
        return if (a < end && b < end) rollingSoundMatrix[a * end + b] else LLUUID.NULL
    }

    fun getGroundCollisionSoundUUID(mcode: UByte): LLUUID = SoundIds.STONE_DIRT_02

    fun getGroundSlidingSoundUUID(mcode: UByte): LLUUID = SoundIds.SLIDE_STONE_STONE_01

    fun getGroundRollingSoundUUID(mcode: UByte): LLUUID = SoundIds.SLIDE_STONE_STONE_01

    fun isCollisionSound(uuid: LLUUID): Boolean {
        val end = MaterialCode.END.toInt()
        for (i in 0 until end) {
            for (j in 0 until end) {
                if (collisionSoundMatrix[i * end + j] == uuid) return true
            }
        }
        return false
    }

    /**
     * Translate display names using a provided name map (e.g. for
     * localisation).  Mirrors `initTableTransNames()` from the C++ source.
     */
    fun translatedNames(nameMap: Map<String, String>): Map<UByte, String> =
        entries.associate { info ->
            info.mcode to (nameMap[info.name] ?: info.name)
        }
}
