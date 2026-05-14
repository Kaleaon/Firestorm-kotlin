package com.firestorm.llimage

import com.firestorm.llcommon.*
import kotlin.math.*

// ---------------------------------------------------------------------------
// Filter type enum
// ---------------------------------------------------------------------------

/**
 * The set of per-pixel filter operations supported by [ImageFilter].
 *
 * Mirrors the named filter operations dispatched by `LLImageFilter::executeFilter`
 * via its LLSD filter description list in the C++ source.
 */
enum class FilterType {
    /** Apply per-channel power-law (gamma) correction. */
    GAMMA,

    /** Shift all pixel values by a constant additive offset. */
    BRIGHTNESS,

    /** Scale pixel values around the mid-point to increase or decrease contrast. */
    CONTRAST,

    /** Adjust colour saturation (< 1.0 desaturates, > 1.0 saturates). */
    SATURATION,

    /** Unsharp-mask sharpening via a 3×3 Laplacian kernel. */
    SHARPEN,

    /** Gaussian / box blur via convolution. */
    BLUR,

    /** Convert to greyscale (luminosity-weighted). */
    GRAYSCALE,

    /** Apply a sepia-tone colour matrix. */
    SEPIA,

    /** Rotate hue by a given angle (degrees). */
    HUE_ROTATE,

    /** Linearise contrast using the brightness histogram. */
    LINEARIZE,

    /** Equalise contrast across a fixed number of histogram classes. */
    EQUALIZE,

    /** Colourise pixels by blending a solid colour at per-channel alpha weights. */
    COLORIZE,

    /** Overlay a procedural screen pattern (sine-wave or line grid). */
    SCREEN
}

// ---------------------------------------------------------------------------
// Stencil (mask) enums — direct ports of the C++ typedefs
// ---------------------------------------------------------------------------

/**
 * Blend mode used when compositing the filter effect with the original pixel.
 *
 * Mirrors `EStencilBlendMode` from llimagefilter.h.
 */
enum class StencilBlendMode {
    /** Linear alpha blend (default). */
    BLEND,
    /** Additive blend. */
    ADD,
    /** Blend applied in reverse (destination α used instead of source α). */
    ABACK,
    /** Fade-out blend (alpha decreases towards the stencil boundary). */
    FADE
}

/**
 * Geometric shape used to generate the stencil (mask) alpha values.
 *
 * Mirrors `EStencilShape` from llimagefilter.h.
 */
enum class StencilShape {
    /** Uniform alpha across the entire image (effectively no spatial mask). */
    UNIFORM,
    /** Linear gradient between two corners of the image. */
    GRADIENT,
    /** Radial vignette centred on the image. */
    VIGNETTE,
    /** Repeating horizontal scan-line pattern. */
    SCAN_LINES
}

/**
 * Procedural screen-pattern overlay mode.
 *
 * Mirrors `EScreenMode` from llimagefilter.h.
 */
enum class ScreenMode {
    /** Two-dimensional sine wave. */
    SINE_2D,
    /** Straight line grid. */
    LINE
}

// ---------------------------------------------------------------------------
// FilterParams — convenient value-type for batch filter application
// ---------------------------------------------------------------------------

/**
 * Aggregated parameters for the standard image-adjustment pipeline.
 *
 * Each field corresponds to one of the named filter operations in [FilterType].
 * Default values leave the image unchanged.
 *
 * @param gamma      Power-law exponent (1.0 = no change, < 1.0 brightens, > 1.0 darkens).
 * @param brightness Additive brightness offset in [−1, +1] range (maps to −255..+255 uint8).
 * @param contrast   Contrast slope (1.0 = no change, > 1.0 increases contrast).
 * @param saturation Saturation factor (1.0 = no change, 0.0 = greyscale, > 1.0 = over-saturate).
 * @param sharpness  Unsharp-mask strength (0.0 = no sharpening).
 */
data class FilterParams(
    val gamma: Float      = 1.0f,
    val brightness: Float = 0.0f,
    val contrast: Float   = 1.0f,
    val saturation: Float = 1.0f,
    val sharpness: Float  = 0.0f
)

// ---------------------------------------------------------------------------
// ImageFilter
// ---------------------------------------------------------------------------

/**
 * Convolution and colour-correction filter pipeline.
 *
 * Corresponds to `LLImageFilter` in the C++ viewer source.
 *
 * The C++ class loads an LLSD-formatted XML description file that lists
 * a sequence of named filter operations with their parameters, then applies
 * them in order via [executeFilter].  In Kotlin, the same pipeline is expressed
 * programmatically via [applyFilter] (batch parameter object) and the individual
 * filter methods.
 *
 * All pixel-manipulation methods are stubs marked with [TODO("CODEC: FILTER ...")]
 * to indicate where the actual arithmetic must be filled in.
 *
 * @param filterDescriptionPath  Optional path to an LLSD XML filter definition
 *                               file.  Pass an empty string to skip file loading.
 */
class ImageFilter(val filterDescriptionPath: String = "") {

    // ---- stencil / mask state (mirrors C++ private fields) -----------------

    private var stencilBlendMode: StencilBlendMode = StencilBlendMode.BLEND
    private var stencilShape: StencilShape         = StencilShape.UNIFORM
    private var stencilMin: Float  = 0.0f
    private var stencilMax: Float  = 1.0f
    private var stencilGamma: Float = 1.0f

    private var stencilCenterX: Int  = 0
    private var stencilCenterY: Int  = 0
    private var stencilWidth: Int    = 0

    private var stencilWavelength: Float = 0.0f
    private var stencilSine: Float       = 0.0f
    private var stencilCosine: Float     = 0.0f

    private var stencilStartX: Float = 0.0f
    private var stencilStartY: Float = 0.0f
    private var stencilGradX: Float  = 0.0f
    private var stencilGradY: Float  = 0.0f
    private var stencilGradN: Float  = 0.0f

    // ---- histogram state ----------------------------------------------------

    private var histoRed: IntArray?        = null
    private var histoGreen: IntArray?      = null
    private var histoBlue: IntArray?       = null
    private var histoBrightness: IntArray? = null

    // ---- public API ---------------------------------------------------------

    /**
     * Apply the full parameter set in [params] to [image] in one call.
     *
     * Operations are applied in the order: gamma → brightness → contrast →
     * saturation → sharpness, matching the typical photographic pipeline.
     * Operations whose parameter equals the identity value (1.0 for
     * multiplicative, 0.0 for additive) are skipped as an optimisation.
     *
     * @param image  Image to filter in-place.
     * @param params Filter parameter bundle.
     */
    fun applyFilter(image: ImageRaw, params: FilterParams) {
        System.err.println("ImageFilter: applyFilter not yet implemented")
    }

    /**
     * Execute the filter pipeline loaded from [filterDescriptionPath].
     *
     * In the C++ source, `LLImageFilter::executeFilter` iterates over the
     * LLSD array [mFilterData] and dispatches to private helpers based on
     * the filter-name string at index 0 of each entry.  Here the method is
     * a stub; callers should use [applyFilter] or the individual methods below
     * until the LLSD loading path is implemented.
     *
     * @param image  Image to filter in-place.
     */
    fun executeFilter(image: ImageRaw) {
        System.err.println("ImageFilter: executeFilter not yet implemented")
    }

    // ---- convolution --------------------------------------------------------

    /**
     * Apply a [kernelSize] × [kernelSize] convolution [kernel] to [image].
     *
     * The kernel is stored in row-major order with [kernelSize]² elements.
     * [normalize] causes the output to be divided by the kernel sum (useful
     * for blur kernels to avoid brightness shift).  [absValue] takes the
     * absolute value of the result before clamping (useful for edge detection).
     *
     * This is a direct port of `LLImageFilter::convolve(kernel, normalize, abs_value)`.
     *
     * @param image      Image to filter in-place.
     * @param kernel     Convolution coefficients, length = [kernelSize] × [kernelSize].
     * @param kernelSize Side length of the square kernel (must be odd).
     * @param normalize  Divide each output channel by the kernel sum.
     * @param absValue   Take the absolute value of the convolution result.
     */
    fun convolve(
        image: ImageRaw,
        kernel: FloatArray,
        kernelSize: Int,
        normalize: Boolean = true,
        absValue: Boolean  = false
    ) {
        require(kernelSize % 2 == 1) { "Kernel size must be odd, got $kernelSize" }
        require(kernel.size == kernelSize * kernelSize) {
            "Kernel array length ${kernel.size} != kernelSize² ${kernelSize * kernelSize}"
        }
        System.err.println("ImageFilter: convolve not yet implemented")
    }

    // ---- colour transforms --------------------------------------------------

    /**
     * Apply per-channel power-law (gamma) correction.
     *
     * Each channel C′ = clamp(C^[gamma], 0, 255).
     * [alpha] acts as a per-channel blend weight (RGB triple): a value of
     * (1, 0, 0) applies the correction only to the red channel.
     *
     * @param gamma  Exponent (1.0 = identity).
     * @param alphaR Blend weight for the red channel in [0, 1].
     * @param alphaG Blend weight for the green channel in [0, 1].
     * @param alphaB Blend weight for the blue channel in [0, 1].
     */
    fun filterGamma(
        image: ImageRaw,
        gamma: Float,
        alphaR: Float = 1.0f, alphaG: Float = 1.0f, alphaB: Float = 1.0f
    ) {
        System.err.println("ImageFilter: filterGamma not yet implemented")
    }

    /**
     * Convert [image] to greyscale (luminosity-weighted average).
     *
     * Uses the Rec. 601 coefficients 0.299 R + 0.587 G + 0.114 B,
     * matching the C++ `filterGrayScale` implementation.
     */
    fun filterGrayScale(image: ImageRaw) {
        System.err.println("ImageFilter: filterGrayScale not yet implemented")
    }

    /**
     * Apply a sepia-tone colour matrix to [image].
     *
     * The C++ implementation uses `LLMatrix3` multiplication per pixel.
     * A standard sepia matrix maps RGB → a warm brownish palette.
     */
    fun filterSepia(image: ImageRaw) {
        System.err.println("ImageFilter: filterSepia not yet implemented")
    }

    /**
     * Adjust colour saturation of [image].
     *
     * [saturation] < 1.0 desaturates (0.0 = greyscale), > 1.0 over-saturates.
     * Internally the C++ uses an HSV saturation matrix (LLMatrix3).
     */
    fun filterSaturate(image: ImageRaw, saturation: Float) {
        System.err.println("ImageFilter: filterSaturate not yet implemented")
    }

    /**
     * Rotate the hue of all pixels by [angleDeg] degrees.
     *
     * Implemented via an RGB rotation matrix in the C++ source.
     */
    fun filterRotateHue(image: ImageRaw, angleDeg: Float) {
        System.err.println("ImageFilter: filterRotateHue not yet implemented")
    }

    /**
     * Shift pixel brightness by an additive constant.
     *
     * [add] maps to an absolute offset in [−255, +255]; values from the
     * LLSD file are typically normalised to [−1, +1] and scaled here.
     *
     * @param add     Normalised brightness offset; −1.0 → −255, +1.0 → +255.
     * @param alphaR  Per-channel blend weight for red.
     * @param alphaG  Per-channel blend weight for green.
     * @param alphaB  Per-channel blend weight for blue.
     */
    fun filterBrightness(
        image: ImageRaw,
        add: Float,
        alphaR: Float = 1.0f, alphaG: Float = 1.0f, alphaB: Float = 1.0f
    ) {
        System.err.println("ImageFilter: filterBrightness not yet implemented")
    }

    /**
     * Adjust contrast by scaling pixel values around the mid-point (128).
     *
     * [slope] > 1.0 increases contrast; [slope] < 1.0 decreases it.
     *
     * @param slope  Contrast slope (1.0 = identity).
     */
    fun filterContrast(
        image: ImageRaw,
        slope: Float,
        alphaR: Float = 1.0f, alphaG: Float = 1.0f, alphaB: Float = 1.0f
    ) {
        System.err.println("ImageFilter: filterContrast not yet implemented")
    }

    /**
     * Linearise contrast using the per-channel histogram.
     *
     * Discards the brightest and darkest [tail] fraction of pixels and
     * stretches the remaining range to [0, 255].
     *
     * @param tail   Fraction of pixels to discard from each extreme (e.g. 0.01 = 1%).
     */
    fun filterLinearize(
        image: ImageRaw,
        tail: Float,
        alphaR: Float = 1.0f, alphaG: Float = 1.0f, alphaB: Float = 1.0f
    ) {
        System.err.println("ImageFilter: filterLinearize not yet implemented")
    }

    /**
     * Equalise contrast by distributing pixel values across [nbClasses] quantised levels.
     *
     * @param nbClasses Number of output intensity classes (e.g. 256 = full 8-bit range).
     */
    fun filterEqualize(
        image: ImageRaw,
        nbClasses: Int,
        alphaR: Float = 1.0f, alphaG: Float = 1.0f, alphaB: Float = 1.0f
    ) {
        System.err.println("ImageFilter: filterEqualize not yet implemented")
    }

    /**
     * Colourize each pixel by blending it with a solid [color] at per-channel [alpha] weights.
     *
     * @param colorR  Red component of the target colour in [0, 1].
     * @param colorG  Green component of the target colour in [0, 1].
     * @param colorB  Blue component of the target colour in [0, 1].
     */
    fun filterColorize(
        image: ImageRaw,
        colorR: Float, colorG: Float, colorB: Float,
        alphaR: Float = 1.0f, alphaG: Float = 1.0f, alphaB: Float = 1.0f
    ) {
        System.err.println("ImageFilter: filterColorize not yet implemented")
    }

    // ---- stencil / mask configuration --------------------------------------

    /**
     * Configure the procedural stencil (spatial mask) applied during blending.
     *
     * The stencil produces a per-pixel alpha value in [[stencilMin], [stencilMax]]
     * based on [shape] and optional [params] (used for gradient direction,
     * vignette size, scan-line pitch, etc.).
     *
     * @param shape  Geometric shape of the mask.
     * @param mode   How the filter effect is blended using the mask alpha.
     * @param min    Minimum mask alpha (at the centre or bright end).
     * @param max    Maximum mask alpha (at the edges or dark end).
     * @param params Shape-specific extra parameters (direction, wavelength, etc.).
     */
    fun setStencil(
        shape: StencilShape,
        mode: StencilBlendMode,
        min: Float,
        max: Float,
        params: FloatArray = FloatArray(0)
    ) {
        stencilShape     = shape
        stencilBlendMode = mode
        stencilMin       = min
        stencilMax       = max
        System.err.println("ImageFilter: setStencil initialise stencil geometry not yet implemented")
    }

    /**
     * Return the stencil alpha for pixel at column [col], row [row].
     *
     * Used internally during blending to weight the filter effect spatially.
     * The result is in [[stencilMin], [stencilMax]].
     */
    fun getStencilAlpha(col: Int, row: Int): Float {
        System.err.println("ImageFilter: getStencilAlpha not yet implemented")
        return 0f
    }

    // ---- screen / halftone overlay -----------------------------------------

    /**
     * Overlay a procedural screen pattern onto [image].
     *
     * @param mode        Pattern type (sine-wave or line grid).
     * @param waveLength  Spatial frequency of the pattern in pixels.
     * @param angle       Rotation angle of the pattern in degrees.
     */
    fun filterScreen(image: ImageRaw, mode: ScreenMode, waveLength: Float, angle: Float) {
        System.err.println("ImageFilter: filterScreen not yet implemented")
    }

    // ---- histogram helpers --------------------------------------------------

    /**
     * Compute per-channel and brightness histograms for [image].
     *
     * Populates [histoRed], [histoGreen], [histoBlue], and [histoBrightness]
     * (each a 256-entry frequency array).  Called lazily before operations
     * that require histogram data ([filterLinearize], [filterEqualize]).
     */
    private fun computeHistograms(image: ImageRaw) {
        System.err.println("ImageFilter: computeHistograms not yet implemented")
    }

    /**
     * Return the brightness histogram, computing it from [image] if needed.
     */
    fun getBrightnessHistogram(image: ImageRaw): IntArray {
        if (histoBrightness == null) computeHistograms(image)
        return histoBrightness ?: IntArray(256)
    }

    // ---- companion: built-in kernel presets ---------------------------------

    companion object {

        /** 3×3 Gaussian blur kernel (sum = 16). */
        val KERNEL_BLUR_3X3: FloatArray = floatArrayOf(
            1f, 2f, 1f,
            2f, 4f, 2f,
            1f, 2f, 1f
        )

        /** 3×3 unsharp-mask (Laplacian sharpening) kernel. */
        val KERNEL_SHARPEN_3X3: FloatArray = floatArrayOf(
             0f, -1f,  0f,
            -1f,  5f, -1f,
             0f, -1f,  0f
        )

        /** 3×3 edge-detection (Laplacian) kernel. */
        val KERNEL_EDGE_3X3: FloatArray = floatArrayOf(
            -1f, -1f, -1f,
            -1f,  8f, -1f,
            -1f, -1f, -1f
        )

        /** 5×5 Gaussian blur kernel (approximation, sum = 256). */
        val KERNEL_BLUR_5X5: FloatArray = floatArrayOf(
             1f,  4f,  6f,  4f, 1f,
             4f, 16f, 24f, 16f, 4f,
             6f, 24f, 36f, 24f, 6f,
             4f, 16f, 24f, 16f, 4f,
             1f,  4f,  6f,  4f, 1f
        )
    }
}
