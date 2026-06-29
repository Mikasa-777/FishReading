package com.github.mikasastacy.fishreading.i18n

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.util.Locale
import java.util.ResourceBundle

class MyMessageBundleTest : BasePlatformTestCase() {

    fun testDefaultBundleUsesEnglishUserFacingText() {
        val bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ROOT)

        assertEquals("Reading lines", bundle.getString("group.com.github.mikasastacy.fishreading.ReadingLineCountMenu.text"))
        assertEquals("Resume reading (current book)", bundle.getString("menu.book.resume.current"))
        assertEquals("No cached table of contents. Click to load it.", bundle.getString("menu.book.load.uncached"))
        assertEquals("Custom chapter title regex", bundle.getString("menu.book.customTxtChapterRegex"))
        assertEquals("Select a reading chapter (type to search)", bundle.getString("popup.chapter.title"))
        assertEquals("Custom TXT chapter regex", bundle.getString("dialog.txtChapterRegex.title"))
        assertEquals("Book import result", bundle.getString("dialog.loadBook.result.title"))
        assertEquals("// [FishReading] Load or select a book from the Tools menu", bundle.getString("reader.welcome.line"))
    }

    fun testSimplifiedChineseBundleUsesChineseUserFacingText() {
        val bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.SIMPLIFIED_CHINESE)

        assertEquals("阅读行数", bundle.getString("group.com.github.mikasastacy.fishreading.ReadingLineCountMenu.text"))
        assertEquals("继续阅读 (当前书籍)", bundle.getString("menu.book.resume.current"))
        assertEquals("尚未缓存目录，点击激活载入", bundle.getString("menu.book.load.uncached"))
        assertEquals("自定义识别章节正则", bundle.getString("menu.book.customTxtChapterRegex"))
        assertEquals("选择摸鱼章节 (支持动态搜索)", bundle.getString("popup.chapter.title"))
        assertEquals("自定义 TXT 章节正则", bundle.getString("dialog.txtChapterRegex.title"))
        assertEquals("图书装载结果", bundle.getString("dialog.loadBook.result.title"))
        assertEquals("// [FishReading] 请去 Tools 菜单加载或选择书籍", bundle.getString("reader.welcome.line"))
    }

    private companion object {
        const val BUNDLE_NAME = "messages.MyMessageBundle"
    }
}
