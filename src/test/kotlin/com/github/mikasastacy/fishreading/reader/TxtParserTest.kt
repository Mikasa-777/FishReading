package com.github.mikasastacy.fishreading.reader

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.io.path.createTempFile
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TxtParserTest {
    @Test
    fun `parses utf8 chinese chapter titles`() {
        val file = createTxtFile(
            """
            第一章 风起
            鱼来了。快藏好！
            第二章 继续
            继续写代码。
            """.trimIndent()
        )

        val chapters = TxtParser.parse(file, maxLineLength = 20)

        assertEquals(listOf("第一章 风起", "第二章 继续"), chapters.map { it.title })
        assertEquals(listOf("// 鱼来了。快藏好！"), chapters[0].lines)
        assertEquals(listOf("// 继续写代码。"), chapters[1].lines)
    }

    @Test
    fun `parses gbk encoded text`() {
        val file = createTxtFile(
            text = """
            第1章 开始
            中文内容。
            """.trimIndent(),
            charset = Charset.forName("GBK")
        )

        val chapters = TxtParser.parse(file, maxLineLength = 20)

        assertEquals("第1章 开始", chapters.single().title)
        assertEquals(listOf("// 中文内容。"), chapters.single().lines)
    }

    @Test
    fun `parses english chapter titles case insensitively`() {
        val file = createTxtFile(
            """
            Chapter 1 Start
            hello world.
            CHAPTER 2 Next
            keep reading.
            """.trimIndent()
        )

        val chapters = TxtParser.parse(file, maxLineLength = 20)

        assertEquals(listOf("Chapter 1 Start", "CHAPTER 2 Next"), chapters.map { it.title })
    }

    @Test
    fun `uses file name as single chapter title when no chapter title exists`() {
        val file = createNamedTxtFile("plain-book", "没有目录。只有正文。")

        val chapters = TxtParser.parse(file, maxLineLength = 20)

        assertEquals("plain-book", chapters.single().title)
        assertEquals(listOf("// 没有目录。只有正文。"), chapters.single().lines)
    }

    @Test
    fun `keeps detected chapter titles out of content lines`() {
        val file = createTxtFile(
            """
            第十二回 重逢
            正文第一句。
            """.trimIndent()
        )

        val chapters = TxtParser.parse(file, maxLineLength = 20)

        assertTrue(chapters.single().lines.none { it.contains("第十二回 重逢") })
    }

    @Test
    fun `returns no chapters for blank text`() {
        val file = createTxtFile(" \n\t\n")

        assertTrue(TxtParser.parse(file).isEmpty())
    }

    private fun createTxtFile(
        text: String,
        charset: Charset = StandardCharsets.UTF_8,
        fileNamePrefix: String = "book"
    ) = createTempFile(prefix = fileNamePrefix, suffix = ".txt").toFile().apply {
        Files.write(toPath(), text.toByteArray(charset))
    }

    private fun createNamedTxtFile(
        fileNameWithoutExtension: String,
        text: String,
        charset: Charset = StandardCharsets.UTF_8
    ) = createTempDirectory().resolve("$fileNameWithoutExtension.txt").toFile().apply {
        Files.write(toPath(), text.toByteArray(charset))
    }
}
