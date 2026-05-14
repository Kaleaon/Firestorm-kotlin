package com.firestorm.llui

import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max

var gConsole: Console? = null

private const val FADE_DURATION = 2f
private const val PADDING_HORIZONTAL = 15f
private const val PADDING_VERTICAL = 8f

enum class FontStyleFlag { NORMAL, BOLD, ITALIC, UNDERLINE }

data class ParagraphColorSegment(val numChars: Int, val color: Color4)

data class LineColorSegment(val text: String, val color: Color4, val xPosition: Float)

data class ConsoleLine(
    val colorSegments: MutableList<LineColorSegment> = mutableListOf(),
    var styleFlag: FontStyleFlag = FontStyleFlag.NORMAL,
)

class Paragraph(
    text: String,
    color: Color4,
    val addTime: Float,
    screenWidth: Float,
    val styleFlag: FontStyleFlag,
    val sessionId: UUID,
    parseUrls: Boolean,
) {
    var paragraphText: String = text
    val colorSegments: MutableList<ParagraphColorSegment> = mutableListOf()
    var maxWidth: Float = -1f
    val lines: MutableList<ConsoleLine> = mutableListOf()

    val id: UUID = UUID.randomUUID()
    val sourceText: String = text
    val urlLabels: MutableMap<String, String> = mutableMapOf()

    init {
        if (parseUrls) {
            System.err.println("Paragraph: init not yet implemented")
        }
        makeParagraphColorSegments(color)
        updateLines(screenWidth, styleFlag)
    }

    fun makeParagraphColorSegments(color: Color4) {
        colorSegments.clear()
        colorSegments.add(ParagraphColorSegment(paragraphText.length, color))
    }

    fun updateLines(screenWidth: Float, styleFlag: FontStyleFlag, forceResize: Boolean = false) {
        if (!forceResize && maxWidth >= 0f && maxWidth < screenWidth) return
        if (paragraphText.isEmpty() || colorSegments.isEmpty()) return

        lines.clear()
        maxWidth = 0f

        System.err.println("Paragraph: updateLines not yet implemented")
    }
}

class Console(
    maxLines: Int = 128,
    persistTime: Float = 0f,
    fontSizeIndex: Int? = null,
    val parseUrls: Boolean = false,
    val backgroundImage: String = "Console_Background",
    val sessionSupport: Boolean = false,
) {
    companion object {
        private val instances: MutableList<Console> = mutableListOf()

        fun updateClass() {
            instances.forEach { it.update() }
        }
    }

    init {
        instances.add(this)
    }

    var maxLines: Int = maxLines
    var linePersistTime: Float = persistTime
        set(value) {
            field = value
            fadeTime = value - FADE_DURATION
        }

    private var fadeTime: Float = persistTime - FADE_DURATION

    private val mutex = ReentrantLock()

    val paragraphs: ArrayDeque<Paragraph> = ArrayDeque()

    private val pendingLines: ArrayDeque<String> = ArrayDeque()
    private val pendingColors: ArrayDeque<Color4> = ArrayDeque()
    private val pendingStyles: ArrayDeque<FontStyleFlag> = ArrayDeque()
    private val pendingSessionIds: ArrayDeque<UUID> = ArrayDeque()
    private val pendingAddTimes: ArrayDeque<Float> = ArrayDeque()

    private val currentSessions: MutableSet<UUID> = mutableSetOf()

    private var consoleWidth: Int = 0
    private var consoleHeight: Int = 0
    private var elapsedTime: Float = 0f

    init {
        fontSizeIndex?.let { setFontSize(it) }
    }

    fun setFontSize(sizeIndex: Int) {
        System.err.println("Console: setFontSize not yet implemented")
    }

    fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        val newWidth = max(50, width)
        val newHeight = max(15, height)
        if (consoleWidth == newWidth && consoleHeight == newHeight) return
        consoleWidth = newWidth
        consoleHeight = newHeight
        for (paragraph in paragraphs) {
            paragraph.updateLines(newWidth.toFloat(), paragraph.styleFlag, forceResize = true)
        }
    }

    fun addConsoleLine(
        line: String,
        color: Color4,
        sessionId: UUID = UUID(0, 0),
        styleFlag: FontStyleFlag = FontStyleFlag.NORMAL,
    ) {
        if (line.isEmpty()) return
        if (!sessionSupport) removeExtraLines()
        mutex.withLock {
            pendingLines.addLast(line)
            pendingAddTimes.addLast(elapsedTime)
            pendingColors.addLast(color)
            pendingStyles.addLast(styleFlag)
            pendingSessionIds.addLast(sessionId)
        }
    }

    fun clear() {
        mutex.withLock {
            pendingLines.clear()
            pendingAddTimes.clear()
            pendingColors.clear()
            pendingStyles.clear()
            pendingSessionIds.clear()
        }
        elapsedTime = 0f
    }

    protected fun removeExtraLines() {
        mutex.withLock {
            while (pendingLines.size > max(0, maxLines - 1)) {
                pendingLines.removeFirst()
                pendingAddTimes.removeFirst()
                pendingColors.removeFirstOrNull()
                pendingStyles.removeFirstOrNull()
                pendingSessionIds.removeFirstOrNull()
            }
        }
    }

    fun draw(currentTime: Float) {
        elapsedTime = currentTime
        if (paragraphs.isEmpty()) return
        // no-op
    }

    fun onUrlLabelCallback(paragraphId: UUID, url: String, label: String) {
        val paragraph = paragraphs.lastOrNull { it.id == paragraphId } ?: return
        paragraph.urlLabels[url] = label

        var newText = paragraph.sourceText
        for ((u, l) in paragraph.urlLabels) {
            newText = newText.replace(u, l)
        }
        paragraph.paragraphText = newText

        val firstColor = paragraph.lines.firstOrNull()?.colorSegments?.firstOrNull()?.color
            ?: Color4(1f, 1f, 1f, 1f)
        paragraph.makeParagraphColorSegments(firstColor)
        val firstStyle = paragraph.lines.firstOrNull()?.styleFlag ?: FontStyleFlag.NORMAL
        paragraph.updateLines(consoleWidth.toFloat(), firstStyle, forceResize = true)
    }

    fun addSession(sessionId: UUID) { currentSessions.add(sessionId) }
    fun removeSession(sessionId: UUID) { currentSessions.remove(sessionId) }

    private fun update() {
        mutex.withLock {
            while (pendingLines.isNotEmpty()) {
                val line = pendingLines.removeFirst()
                val color = pendingColors.removeFirstOrNull() ?: Color4(1f, 1f, 1f, 1f)
                val time = pendingAddTimes.removeFirstOrNull() ?: elapsedTime
                val style = pendingStyles.removeFirstOrNull() ?: FontStyleFlag.NORMAL
                val session = pendingSessionIds.removeFirstOrNull() ?: UUID(0, 0)

                paragraphs.addLast(
                    Paragraph(
                        text = line,
                        color = color,
                        addTime = time,
                        screenWidth = consoleWidth.toFloat(),
                        styleFlag = style,
                        sessionId = session,
                        parseUrls = parseUrls,
                    )
                )
            }
        }

        if (!sessionSupport) {
            while (paragraphs.size > max(0, maxLines)) paragraphs.removeFirst()
        } else {
            val skipTime = elapsedTime - linePersistTime
            val kept = ArrayDeque<Paragraph>()
            val sessionLineCounts = mutableMapOf<UUID, Int>()
            for (para in paragraphs.reversed()) {
                val count = sessionLineCounts.getOrDefault(para.sessionId, 0) + para.lines.size
                sessionLineCounts[para.sessionId] = count
                val expired = linePersistTime > 0f &&
                    (para.addTime - skipTime) / (linePersistTime - fadeTime) <= 0f
                if (count <= maxLines && !expired) kept.addFirst(para)
            }
            paragraphs.clear()
            paragraphs.addAll(kept)
        }
    }
}
