package com.example.fishreading

import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType // 正确的导包
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Graphics2D
import java.awt.geom.Rectangle2D

class FishInlayRenderer(
    private val text: String,
    private val attributes: TextAttributes
) : EditorCustomElementRenderer {

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        // 直接使用引入的 EditorFontType.PLAIN
        val font = inlay.editor.colorsScheme.getFont(EditorFontType.PLAIN)
        val metrics = inlay.editor.contentComponent.getFontMetrics(font)
        return metrics.stringWidth(text)
    }

    override fun calcHeightInPixels(inlay: Inlay<*>): Int {
        return inlay.editor.lineHeight
    }

    override fun paint(inlay: Inlay<*>, g: Graphics2D, targetRegion: Rectangle2D, textAttributes: TextAttributes) {
        val editor = inlay.editor
        val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
        g.font = font
        g.color = attributes.foregroundColor

        val y = targetRegion.y + editor.ascent
        val x = targetRegion.x

        g.drawString(text, x.toFloat(), y.toFloat())
    }
}