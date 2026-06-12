package com.github.mikasastacy.fishreading.reader

import com.github.mikasastacy.fishreading.state.FishReadingPersistentState
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.charset.StandardCharsets
import java.nio.file.Files
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
        val state = service<FishReadingPersistentState>().state
        state.lastActiveBookPath = path
        val progress = state.getBookProgress(path, file.nameWithoutExtension)
        progress.chapterIdx = 0
        progress.lineIdx = 0
        state.updateChapterTitles(path, listOf("第一章 开始", "第二章 继续"))

        val readerService = FishReaderService()

        readerService.nextLine()

        assertEquals("// 第二行。", readerService.getCurrentLine())
        assertEquals(1, progress.chapterIdx)
        assertEquals(0, progress.lineIdx)
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
        val state = service<FishReadingPersistentState>().state
        state.lastActiveBookPath = path
        val progress = state.getBookProgress(path, file.nameWithoutExtension)
        progress.chapterIdx = 1
        progress.lineIdx = 0
        state.updateChapterTitles(path, listOf("第一章 开始", "第二章 继续", "第三章 收尾"))

        val readerService = FishReaderService()

        assertEquals("// 第二行。", readerService.getCurrentLine())
        readerService.nextLine()

        assertEquals("// 第三行。", readerService.getCurrentLine())
        assertEquals(2, progress.chapterIdx)
        assertEquals(0, progress.lineIdx)
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
        val state = service<FishReadingPersistentState>().state
        state.lastActiveBookPath = path
        val progress = state.getBookProgress(path, file.nameWithoutExtension)
        progress.chapterIdx = 0
        progress.lineIdx = 0
        state.updateChapterTitles(path, listOf("第一章 开始", "第二章 继续"))

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
        val state = service<FishReadingPersistentState>().state
        state.lastActiveBookPath = missingPath
        val progress = state.getBookProgress(missingPath, "missing-book")
        progress.chapterIdx = 3
        progress.lineIdx = 7
        state.updateChapterTitles(missingPath, listOf("旧目录"))

        val readerService = FishReaderService()

        readerService.nextLine()
        readerService.prevLine()

        assertEquals(3, progress.chapterIdx)
        assertEquals(7, progress.lineIdx)
        assertEquals("// [FishReading] 请去 Tools 菜单加载或选择书籍", readerService.getCurrentLine())
    }

    private fun resetPersistentState() {
        service<FishReadingPersistentState>().loadState(FishReadingPersistentState.State())
    }

    private fun createTxtFile(text: String) = createTempFile(prefix = "book", suffix = ".txt").toFile().apply {
        Files.write(toPath(), text.toByteArray(StandardCharsets.UTF_8))
    }
}
