package com.github.mikasastacy.fishreading.reader

import com.github.mikasastacy.fishreading.state.FishReadingPersistentState
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile

class FishReaderServiceTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        resetPersistentState()
    }

    override fun tearDown() {
        try {
            resetPersistentState()
        } finally {
            super.tearDown()
        }
    }

    fun testNextLineLoadsPersistedActiveBookBeforeMoving() {
        val file = createTxtFile(
            """
            第一章 开始
            第一行。
            第二章 继续
            第二行。
            """.trimIndent()
        )
        val path = file.absolutePath
        val settings = service<FishReadingPersistentState>()
        settings.setLastActiveBookPath(path)
        settings.rememberBook(path, file.nameWithoutExtension)
        settings.updateChapterTitles(path, listOf("第一章 开始", "第二章 继续"))

        val readerService = FishReaderService()

        readerService.nextLine()

        assertEquals("// 第二行。", readerService.getCurrentLine())
        assertEquals(1, settings.bookProgress(path)?.chapterIdx)
        assertEquals(0, settings.bookProgress(path)?.lineIdx)
    }

    fun testRestoredProgressIsUsedAndSavedAfterMoving() {
        val file = createTxtFile(
            """
            第一章 开始
            第一行。
            第二章 继续
            第二行。
            第三章 收尾
            第三行。
            """.trimIndent()
        )
        val path = file.absolutePath
        val settings = service<FishReadingPersistentState>()
        settings.setLastActiveBookPath(path)
        settings.rememberBook(path, file.nameWithoutExtension)
        settings.saveProgress(path, chapterIdx = 1, lineIdx = 0)
        settings.updateChapterTitles(path, listOf("第一章 开始", "第二章 继续", "第三章 收尾"))

        val readerService = FishReaderService()

        assertEquals("// 第二行。", readerService.getCurrentLine())
        readerService.nextLine()

        assertEquals("// 第三行。", readerService.getCurrentLine())
        assertEquals(2, settings.bookProgress(path)?.chapterIdx)
        assertEquals(0, settings.bookProgress(path)?.lineIdx)
    }

    fun testChapterTitlesLoadPersistedActiveBook() {
        val file = createTxtFile(
            """
            第一章 开始
            第一行。
            第二章 继续
            第二行。
            """.trimIndent()
        )
        val path = file.absolutePath
        val settings = service<FishReadingPersistentState>()
        settings.setLastActiveBookPath(path)
        settings.rememberBook(path, file.nameWithoutExtension)
        settings.updateChapterTitles(path, listOf("第一章 开始", "第二章 继续"))

        val readerService = FishReaderService()

        assertEquals(
            listOf("[1] 第一章 开始", "[2] 第二章 继续"),
            readerService.getChapterTitles()
        )
    }

    fun testMissingActiveBookDoesNotOverwriteSavedProgress() {
        val missingPath = createTempFile(prefix = "missing-book", suffix = ".txt").toFile().apply {
            delete()
        }.absolutePath
        val settings = service<FishReadingPersistentState>()
        settings.setLastActiveBookPath(missingPath)
        settings.rememberBook(missingPath, "missing-book")
        settings.saveProgress(missingPath, chapterIdx = 3, lineIdx = 7)
        settings.updateChapterTitles(missingPath, listOf("旧目录"))

        val readerService = FishReaderService()

        readerService.nextLine()
        readerService.prevLine()

        assertEquals(3, settings.bookProgress(missingPath)?.chapterIdx)
        assertEquals(7, settings.bookProgress(missingPath)?.lineIdx)
        assertEquals("// [FishReading] Load or select a book from the Tools menu", readerService.getCurrentLine())
    }

    fun testCurrentPagePadsMissingActiveBookToReadingLineCount() {
        service<FishReadingPersistentState>().updateReadingLineCount(3)
        val readerService = FishReaderService()

        assertEquals(
            listOf("// [FishReading] Load or select a book from the Tools menu", "//", "//"),
            readerService.getCurrentPage()
        )
    }

    fun testNextPageSavesPageStartProgress() {
        val firstLine = "第一行内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容。"
        val secondLine = "第二行内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容。"
        val thirdLine = "第三行内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容。"
        val fourthLine = "第四行内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容。"
        val file = createTxtFile(
            """
            第一章 开始
            $firstLine
            $secondLine
            $thirdLine
            $fourthLine
            """.trimIndent()
        )
        val path = file.absolutePath
        val settings = service<FishReadingPersistentState>()
        settings.updateReadingLineCount(2)
        settings.setLastActiveBookPath(path)
        settings.rememberBook(path, file.nameWithoutExtension)
        settings.updateChapterTitles(path, listOf("第一章 开始"))

        val readerService = FishReaderService()

        assertEquals(listOf("// $firstLine", "// $secondLine"), readerService.getCurrentPage())
        readerService.nextPage()

        assertEquals(listOf("// $thirdLine", "// $fourthLine"), readerService.getCurrentPage())
        assertEquals(0, settings.bookProgress(path)?.chapterIdx)
        assertEquals(2, settings.bookProgress(path)?.lineIdx)
    }

    fun testReadingLineCountIsClampedToSupportedRange() {
        val settings = service<FishReadingPersistentState>()

        settings.loadState(FishReadingPersistentState.State(readingLineCount = 0))
        assertEquals(1, settings.normalizedReadingLineCount())

        settings.loadState(FishReadingPersistentState.State(readingLineCount = 21))
        assertEquals(20, settings.normalizedReadingLineCount())
    }

    fun testLoadBookAcceptsDirectoryEpubAndSavesAbsolutePathProgress() {
        val directory = createDirectoryEpub()
        val readerService = FishReaderService()

        val result = readerService.loadBook(directory)

        assertEquals("Loaded \"book\" successfully", result)
        assertEquals("// First第一章正文。", readerService.getCurrentLine())
        assertEquals(directory.absolutePath, service<FishReadingPersistentState>().lastActiveBookPath)
        assertEquals(listOf("[1] First", "[2] Second"), readerService.getChapterTitles())
    }

    private fun resetPersistentState() {
        service<FishReadingPersistentState>().loadState(FishReadingPersistentState.State())
    }

    private fun createTxtFile(text: String) = createTempFile(prefix = "book", suffix = ".txt").toFile().apply {
        Files.write(toPath(), text.toByteArray(StandardCharsets.UTF_8))
    }

    private fun createDirectoryEpub() = createTempDirectory().resolve("book.epub").toFile().apply {
        mkdirs()
        writeTextFile(
            "META-INF/container.xml",
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
              <rootfiles>
                <rootfile full-path="content.opf" media-type="application/oebps-package+xml"/>
              </rootfiles>
            </container>
            """.trimIndent()
        )
        writeTextFile(
            "content.opf",
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <package xmlns="http://www.idpf.org/2007/opf" version="3.0">
              <manifest>
                <item id="first" href="chapters/first.xhtml" media-type="application/xhtml+xml"/>
                <item id="second" href="chapters/second.xhtml" media-type="application/xhtml+xml"/>
              </manifest>
              <spine>
                <itemref idref="first"/>
                <itemref idref="second"/>
              </spine>
            </package>
            """.trimIndent()
        )
        writeTextFile("chapters/first.xhtml", chapterHtml("First", "第一章正文。"))
        writeTextFile("chapters/second.xhtml", chapterHtml("Second", "第二章正文。"))
    }

    private fun java.io.File.writeTextFile(relativePath: String, content: String) {
        val file = resolve(relativePath)
        file.parentFile.mkdirs()
        file.writeText(content, StandardCharsets.UTF_8)
    }

    private fun chapterHtml(title: String, body: String) =
        "<html><head><title>$title</title></head><body><h1>$title</h1><p>$body</p></body></html>"
}
