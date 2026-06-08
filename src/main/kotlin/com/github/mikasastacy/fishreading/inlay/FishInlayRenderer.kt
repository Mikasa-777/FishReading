package com.github.mikasastacy.fishreading.inlay

import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Graphics2D
import java.awt.geom.Rectangle2D

class FishInlayRenderer(
    private val text: String,
    private val attributes: TextAttributes
) : EditorCustomElementRenderer {

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val font = inlay.editor.colorsScheme.getFont(EditorFontType.PLAIN)
        val metrics = inlay.editor.contentComponent.getFontMetrics(font)
        return metrics.stringWidth(text)
    }

    override fun calcHeightInPixels(inlay: Inlay<*>): Int = inlay.editor.lineHeight

    override fun paint(inlay: Inlay<*>, g: Graphics2D, targetRegion: Rectangle2D, textAttributes: TextAttributes) {
        val editor = inlay.editor
        g.font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
        g.color = attributes.foregroundColor

        g.drawString(text, targetRegion.x.toFloat(), (targetRegion.y + editor.ascent).toFloat())
    }
}
