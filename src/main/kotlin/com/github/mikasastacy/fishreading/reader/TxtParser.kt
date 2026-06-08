package com.github.mikasastacy.fishreading.reader

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

object TxtParser {
    private val gbkCharset: Charset = Charset.forName("GBK")
    private val chapterTitleRegex = Regex(
        pattern = """^\s*(?:第[0-9零〇一二两三四五六七八九十百千万]+[章节回卷部篇]|卷[0-9零〇一二两三四五六七八九十百千万]+|chapter\s+[0-9ivxlcdm]+)(?:\s+.*)?\s*$""",
        option = RegexOption.IGNORE_CASE
    )

    fun parse(file: File, maxLineLength: Int = 50): List<Chapter> {
        val text = decodeText(file.readBytes()).replace("\r\n", "\n").replace("\r", "\n")
        if (text.isBlank()) return emptyList()

        val chapters = splitChapters(text, file.nameWithoutExtension)
        return chapters.mapNotNull { (title, content) ->
            val lines = TextSegmenter.segment(content, maxLineLength)
            if (lines.isEmpty()) null else Chapter(title, lines)
        }
    }

    private fun splitChapters(text: String, fallbackTitle: String): List<Pair<String, String>> {
        val chapters = mutableListOf<Pair<String, String>>()
        var currentTitle: String? = null
        var currentContent = StringBuilder()
        var foundChapterTitle = false

        fun flushCurrent() {
            val title = currentTitle ?: return
            val content = currentContent.toString()
            if (content.isNotBlank()) {
                chapters.add(title to content)
            }
        }

        for (line in text.lines()) {
            val trimmedLine = line.trim()
            if (isChapterTitle(trimmedLine)) {
                if (!foundChapterTitle && currentContent.isNotBlank()) {
                    chapters.add("序章" to currentContent.toString())
                } else {
                    flushCurrent()
                }
                foundChapterTitle = true
                currentTitle = trimmedLine
                currentContent = StringBuilder()
            } else {
                currentContent.appendLine(line)
            }
        }

        if (!foundChapterTitle) {
            return listOf(fallbackTitle to text)
        }

        flushCurrent()
        return chapters
    }

    private fun isChapterTitle(line: String): Boolean {
        return line.isNotEmpty() && chapterTitleRegex.matches(line)
    }

    private fun decodeText(bytes: ByteArray): String {
        return try {
            decodeStrict(bytes, StandardCharsets.UTF_8)
        } catch (_: CharacterCodingException) {
            decodeStrict(bytes, gbkCharset)
        }
    }

    private fun decodeStrict(bytes: ByteArray, charset: Charset): String {
        return charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    }
}
