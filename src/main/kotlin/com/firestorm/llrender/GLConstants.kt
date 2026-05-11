package com.firestorm.llrender

/**
 * OpenGL ES 3.2 numeric constants used by code that talks to `GpuBackend`.
 * Values mirror the canonical GL enumerants so the Android implementation can
 * pass them straight to `android.opengl.GLES3x.*`.
 */
object GL {

    // -------- capabilities (glEnable / glDisable) --------------------------
    const val DEPTH_TEST = 0x0B71
    const val STENCIL_TEST = 0x0B90
    const val SCISSOR_TEST = 0x0C11
    const val BLEND = 0x0BE2
    const val CULL_FACE = 0x0B44
    const val POLYGON_OFFSET_FILL = 0x8037
    const val PRIMITIVE_RESTART_FIXED_INDEX = 0x8D69
    const val SAMPLE_COVERAGE = 0x80A0
    const val SAMPLE_ALPHA_TO_COVERAGE = 0x809E
    const val DEBUG_OUTPUT = 0x92E0
    const val DEBUG_OUTPUT_SYNCHRONOUS = 0x8242

    // -------- depth / face --------------------------------------------------
    const val NEVER = 0x0200
    const val LESS = 0x0201
    const val EQUAL = 0x0202
    const val LEQUAL = 0x0203
    const val GREATER = 0x0204
    const val NOTEQUAL = 0x0205
    const val GEQUAL = 0x0206
    const val ALWAYS = 0x0207

    const val FRONT = 0x0404
    const val BACK = 0x0405
    const val FRONT_AND_BACK = 0x0408
    const val CCW = 0x0901
    const val CW = 0x0900

    // -------- blending ------------------------------------------------------
    const val ZERO = 0
    const val ONE = 1
    const val SRC_COLOR = 0x0300
    const val ONE_MINUS_SRC_COLOR = 0x0301
    const val SRC_ALPHA = 0x0302
    const val ONE_MINUS_SRC_ALPHA = 0x0303
    const val DST_ALPHA = 0x0304
    const val ONE_MINUS_DST_ALPHA = 0x0305
    const val DST_COLOR = 0x0306
    const val ONE_MINUS_DST_COLOR = 0x0307

    // -------- clear masks ---------------------------------------------------
    const val COLOR_BUFFER_BIT = 0x4000
    const val DEPTH_BUFFER_BIT = 0x0100
    const val STENCIL_BUFFER_BIT = 0x0400

    // -------- pixel store ---------------------------------------------------
    const val UNPACK_ALIGNMENT = 0x0CF5
    const val PACK_ALIGNMENT = 0x0D05

    // -------- buffer / draw types ------------------------------------------
    const val UNSIGNED_BYTE = 0x1401
    const val UNSIGNED_SHORT = 0x1403
    const val UNSIGNED_INT = 0x1405
    const val FLOAT = 0x1406

    // -------- texture targets ----------------------------------------------
    const val TEXTURE_2D = 0x0DE1
    const val TEXTURE_3D = 0x806F
    const val TEXTURE_CUBE_MAP = 0x8513
    const val TEXTURE_CUBE_MAP_POSITIVE_X = 0x8515
    const val TEXTURE_CUBE_MAP_NEGATIVE_X = 0x8516
    const val TEXTURE_CUBE_MAP_POSITIVE_Y = 0x8517
    const val TEXTURE_CUBE_MAP_NEGATIVE_Y = 0x8518
    const val TEXTURE_CUBE_MAP_POSITIVE_Z = 0x8519
    const val TEXTURE_CUBE_MAP_NEGATIVE_Z = 0x851A
    const val TEXTURE_CUBE_MAP_SEAMLESS = 0x884F
    const val TEXTURE_2D_ARRAY = 0x8C1A
    const val TEXTURE_2D_MULTISAMPLE = 0x9100
    const val TEXTURE0 = 0x84C0

    // -------- texture parameters -------------------------------------------
    const val TEXTURE_MIN_FILTER = 0x2800
    const val TEXTURE_MAG_FILTER = 0x2801
    const val TEXTURE_WRAP_S = 0x2802
    const val TEXTURE_WRAP_T = 0x2803
    const val TEXTURE_WRAP_R = 0x8072
    const val TEXTURE_MAX_ANISOTROPY = 0x84FE

    const val NEAREST = 0x2600
    const val LINEAR = 0x2601
    const val NEAREST_MIPMAP_NEAREST = 0x2700
    const val LINEAR_MIPMAP_NEAREST = 0x2701
    const val NEAREST_MIPMAP_LINEAR = 0x2702
    const val LINEAR_MIPMAP_LINEAR = 0x2703

    const val CLAMP_TO_EDGE = 0x812F
    const val MIRRORED_REPEAT = 0x8370
    const val REPEAT = 0x2901

    // -------- texture formats ----------------------------------------------
    const val RGB = 0x1907
    const val RGBA = 0x1908
    const val RGBA8 = 0x8058
    const val RGB8 = 0x8051
    const val RGBA16F = 0x881A
    const val RGBA32F = 0x8814
    const val DEPTH_COMPONENT = 0x1902
    const val DEPTH_COMPONENT16 = 0x81A5
    const val DEPTH_COMPONENT24 = 0x81A6
    const val DEPTH_COMPONENT32F = 0x8CAC
    const val DEPTH24_STENCIL8 = 0x88F0
    const val LUMINANCE = 0x1909
    const val LUMINANCE_ALPHA = 0x190A
    const val ALPHA = 0x1906
    const val SRGB8_ALPHA8 = 0x8C43

    // -------- framebuffer --------------------------------------------------
    const val FRAMEBUFFER = 0x8D40
    const val DRAW_FRAMEBUFFER = 0x8CA9
    const val READ_FRAMEBUFFER = 0x8CA8
    const val COLOR_ATTACHMENT0 = 0x8CE0
    const val COLOR_ATTACHMENT1 = 0x8CE1
    const val COLOR_ATTACHMENT2 = 0x8CE2
    const val COLOR_ATTACHMENT3 = 0x8CE3
    const val DEPTH_ATTACHMENT = 0x8D00
    const val STENCIL_ATTACHMENT = 0x8D20
    const val FRAMEBUFFER_COMPLETE = 0x8CD5
    const val NONE = 0

    // -------- shader types -------------------------------------------------
    const val VERTEX_SHADER = 0x8B31
    const val FRAGMENT_SHADER = 0x8B30
    const val GEOMETRY_SHADER = 0x8DD9

    // -------- program parameters -------------------------------------------
    const val COMPILE_STATUS = 0x8B81
    const val LINK_STATUS = 0x8B82
    const val INFO_LOG_LENGTH = 0x8B84
    const val ACTIVE_UNIFORMS = 0x8B86
    const val ACTIVE_ATTRIBUTES = 0x8B89

    // -------- queries ------------------------------------------------------
    const val TIME_ELAPSED = 0x88BF
    const val SAMPLES_PASSED = 0x8914
    const val PRIMITIVES_GENERATED = 0x8C87

    // -------- errors -------------------------------------------------------
    const val NO_ERROR = 0
    const val INVALID_ENUM = 0x0500
    const val INVALID_VALUE = 0x0501
    const val INVALID_OPERATION = 0x0502
    const val OUT_OF_MEMORY = 0x0505

    // -------- get parameters -----------------------------------------------
    const val MAJOR_VERSION = 0x821B
    const val MINOR_VERSION = 0x821C
    const val MAX_TEXTURE_SIZE = 0x0D33
    const val MAX_VERTEX_ATTRIBS = 0x8869

    // -------- debug --------------------------------------------------------
    const val DEBUG_SOURCE_APPLICATION = 0x824A
    const val DEBUG_TYPE_OTHER = 0x8251
    const val DEBUG_SEVERITY_LOW = 0x9148
    const val DEBUG_SEVERITY_MEDIUM = 0x9147
    const val DEBUG_SEVERITY_HIGH = 0x9146

    // -------- uniform sampler types (for reflection) -----------------------
    const val SAMPLER_2D = 0x8B5E
    const val SAMPLER_3D = 0x8B5F
    const val SAMPLER_CUBE = 0x8B60
    const val SAMPLER_2D_ARRAY = 0x8DC1
    const val SAMPLER_CUBE_MAP_ARRAY = 0x900C
    const val SAMPLER_2D_SHADOW = 0x8B62
}
