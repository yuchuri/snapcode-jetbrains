package com.github.yuchuri.snapcodejetbrains.render

import com.github.yuchuri.snapcodejetbrains.settings.SnapcodeSettings
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import java.awt.*
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

object SnapcodeRenderer {

    private const val CORNER_RADIUS = 12f
    private const val SHADOW_STEPS = 24
    private const val MAX_SHADOW_ALPHA = 90
    private const val CHROME_HEIGHT = 40
    private const val TAB_SPACES = "    "

    /**
     * Renders [code] to a [BufferedImage] using syntax highlighting provided by the IDE.
     *
     * @param project   The current project (used to create an [EditorHighlighterFactory]).
     * @param code      The raw source code string to render.
     * @param fileType  The language / file type for syntax-highlighting.
     * @param settings  Visual options (theme, colors, padding, …).
     */
    fun render(
        project: Project,
        code: String,
        fileType: FileType,
        settings: SnapcodeSettings
    ): BufferedImage {
        val normalizedCode = code.replace("\r\n", "\n").replace("\r", "\n")
        val lines = normalizedCode.split("\n")

        val colorsScheme = EditorColorsManager.getInstance().globalScheme

        val highlighter = EditorHighlighterFactory.getInstance()
            .createEditorHighlighter(project, fileType)
        highlighter.setColorScheme(colorsScheme)
        highlighter.setText(normalizedCode)

        val fontSize = settings.fontSize.toInt()
        val baseFont = Font(Font.MONOSPACED, Font.PLAIN, fontSize)

        val fm = fontMetrics(baseFont)
        val lineHeight = fm.height + 2
        val pad = settings.padding
        val chromeH = if (settings.showWindowChrome) CHROME_HEIGHT else 0

        val maxLineWidth = lines.maxOfOrNull { fm.stringWidth(it.replace("\t", TAB_SPACES)) } ?: 0
        val contentW = maxOf(maxLineWidth + pad * 2, 400)
        val contentH = lines.size * lineHeight + pad * 2 + chromeH

        val totalW = contentW + pad * 2
        val totalH = contentH + pad * 2

        val image = BufferedImage(totalW, totalH, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.applyRenderingHints()

        // Background gradient
        g.paint = GradientPaint(
            0f, 0f, settings.backgroundStartColor(),
            totalW.toFloat(), totalH.toFloat(), settings.backgroundEndColor()
        )
        g.fillRect(0, 0, totalW, totalH)

        val winX = pad
        val winY = pad
        val windowShape = RoundRectangle2D.Float(
            winX.toFloat(), winY.toFloat(),
            contentW.toFloat(), contentH.toFloat(),
            CORNER_RADIUS, CORNER_RADIUS
        )

        // Shadow
        if (settings.shadowEnabled) {
            drawShadow(g, winX, winY, contentW, contentH)
        }

        // Code window background
        g.color = if (settings.theme == "DARK") Color(30, 30, 30) else Color(250, 250, 250)
        g.fill(windowShape)

        // Window chrome
        if (settings.showWindowChrome) {
            drawChrome(g, winX, winY, contentW, chromeH, settings.theme)
        }

        // Clip to window shape before drawing text
        g.clip = windowShape

        // Syntax-highlighted code
        val defaultFg = colorsScheme.defaultForeground
            ?: if (settings.theme == "DARK") Color.WHITE else Color.BLACK
        val codeX = winX + pad
        val codeY = winY + chromeH + pad + fm.ascent

        drawCode(g, normalizedCode, highlighter, baseFont, fm, codeX, codeY, lineHeight, defaultFg)

        g.dispose()
        return image
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun drawCode(
        g: Graphics2D,
        code: String,
        highlighter: EditorHighlighter,
        baseFont: Font,
        fm: FontMetrics,
        startX: Int,
        startY: Int,
        lineHeight: Int,
        defaultFg: Color
    ) {
        g.font = baseFont
        var lineIdx = 0
        var colX = startX

        val iter = highlighter.createIterator(0)
        while (!iter.atEnd()) {
            val attrs = iter.textAttributes
            val fgColor = attrs?.foregroundColor ?: defaultFg
            val fontStyle = attrs?.fontType ?: Font.PLAIN
            val tokenFont = if (fontStyle == Font.PLAIN) baseFont else baseFont.deriveFont(fontStyle)

            g.font = tokenFont
            g.color = fgColor

            val tokenText = code.substring(iter.start, iter.end)
            val newlineSegments = tokenText.split("\n")

            newlineSegments.forEachIndexed { segIdx, segment ->
                // Draw each tab-separated piece
                val tabParts = segment.split("\t")
                tabParts.forEachIndexed { tabIdx, part ->
                    if (part.isNotEmpty()) {
                        g.drawString(part, colX, startY + lineIdx * lineHeight)
                        colX += fm.stringWidth(part)
                    }
                    if (tabIdx < tabParts.lastIndex) {
                        colX += fm.stringWidth(TAB_SPACES)
                    }
                }
                // Move to next line after every "\n" except the last segment
                if (segIdx < newlineSegments.lastIndex) {
                    lineIdx++
                    colX = startX
                }
            }

            iter.advance()
        }
    }

    private fun drawShadow(g: Graphics2D, x: Int, y: Int, width: Int, height: Int) {
        for (i in SHADOW_STEPS downTo 0) {
            val alpha = (MAX_SHADOW_ALPHA * (1.0 - i.toDouble() / SHADOW_STEPS)).toInt()
            g.color = Color(0, 0, 0, alpha)
            g.fill(
                RoundRectangle2D.Float(
                    (x + i / 2f), (y + i / 2f),
                    width.toFloat(), height.toFloat(),
                    CORNER_RADIUS, CORNER_RADIUS
                )
            )
        }
    }

    private fun drawChrome(g: Graphics2D, x: Int, y: Int, width: Int, height: Int, theme: String) {
        val chromeColor = if (theme == "DARK") Color(42, 42, 42) else Color(236, 236, 236)

        // Rounded top + flat bottom
        g.color = chromeColor
        g.fill(
            RoundRectangle2D.Float(
                x.toFloat(), y.toFloat(),
                width.toFloat(), (height + CORNER_RADIUS.toInt()).toFloat(),
                CORNER_RADIUS, CORNER_RADIUS
            )
        )

        // Traffic-light buttons
        val btnY = y + height / 2
        g.color = Color(255, 96, 92)
        g.fillOval(x + 16, btnY - 6, 12, 12)
        g.color = Color(255, 189, 68)
        g.fillOval(x + 36, btnY - 6, 12, 12)
        g.color = Color(39, 201, 63)
        g.fillOval(x + 56, btnY - 6, 12, 12)
    }

    private fun fontMetrics(font: Font): FontMetrics {
        val tmp = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val g = tmp.createGraphics()
        g.font = font
        val fm = g.fontMetrics
        g.dispose()
        return fm
    }

    private fun Graphics2D.applyRenderingHints() {
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)
        setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
    }
}
