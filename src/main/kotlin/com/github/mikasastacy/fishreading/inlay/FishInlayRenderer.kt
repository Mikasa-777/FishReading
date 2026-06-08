package com.github.mikasastacy.fishreading.inlay

import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Font
import java.awt.Graphics2D
import java.awt.geom.Rectangle2D

class FishInlayRenderer(
    private val text: String,
    private val attributes: TextAttributes
) : EditorCustomElementRenderer {

    private fun getSafeFont(inlay: Inlay<*>): Font {
        val editorFont = inlay.editor.colorsScheme.getFont(EditorFontType.PLAIN)

        // 阶梯 1：优先信任并采用用户当前 IDE 正在用的代码字体（例如 JetBrains Mono，测试证明它最健康）
        val primaryFont = Font(editorFont.name, Font.ITALIC, editorFont.size)
        if (primaryFont.canDisplayUpTo(text) == -1) {
            return primaryFont
        }

        // 阶梯 2：如果用户用的是 Consolas 等残疾英文纯物理字体，针对 Windows 强行祭出“微软雅黑”实施无缝跨界救场
        val windowsFont = Font("Microsoft YaHei", Font.ITALIC, editorFont.size)
        if (windowsFont.canDisplayUpTo(text) == -1) {
            return windowsFont
        }

        // 阶梯 3：终极多语言大一统系统 UI 兜底（Java 官方 Dialog 容器，绝无绝育可能）
        return Font(Font.DIALOG, Font.ITALIC, editorFont.size)
    }

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val safeFont = getSafeFont(inlay) // 动态抓取活字
        val metrics = inlay.editor.contentComponent.getFontMetrics(safeFont)
        return metrics.stringWidth(text)
    }

    override fun calcHeightInPixels(inlay: Inlay<*>): Int = inlay.editor.lineHeight

    override fun paint(inlay: Inlay<*>, g: Graphics2D, targetRegion: Rectangle2D, textAttributes: TextAttributes) {
        val editor = inlay.editor
        val safeFont = getSafeFont(inlay) // 动态抓取活字

        g.font = safeFont
        g.color = attributes.foregroundColor

        val y = targetRegion.y + editor.ascent
        val x = targetRegion.x

        g.drawString(text, x.toFloat(), y.toFloat())
    }
}