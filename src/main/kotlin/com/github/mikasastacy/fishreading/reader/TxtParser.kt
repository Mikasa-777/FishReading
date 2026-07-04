package com.github.mikasastacy.fishreading.reader

import com.github.mikasastacy.fishreading.i18n.MyMessageBundle
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

data class TxtParseResult(val chapters: List<Chapter>, val matchedTitleCount: Int)

object TxtParser {
    const val DEFAULT_CHAPTER_TITLE_REGEX: String =
        """^\s*(?:第[0-9零〇一二两三四五六七八九十百千万]+[章节回卷部篇]|卷[0-9零〇一二两三四五六七八九十百千万]+|chapter\s+[0-9ivxlcdm]+)(?:\s+.*)?\s*$"""

    private val gbkCharset: Charset = Charset.forName("GBK")

    fun parse(file: File, maxLineLength: Int = 50): List<Chapter> {
        return parseWithResult(file, maxLineLength).chapters
    }

    fun parseWithResult(
        file: File,
        maxLineLength: Int = 50,
        chapterTitleRegex: String = DEFAULT_CHAPTER_TITLE_REGEX
    ): TxtParseResult {
        val titleRegex = Regex(chapterTitleRegex, RegexOption.IGNORE_CASE)
        val text = decodeText(file.readBytes()).replace("\r\n", "\n").replace("\r", "\n")
        if (text.isBlank()) return TxtParseResult(emptyList(), matchedTitleCount = 0)

        val splitResult = splitChapters(text, file.nameWithoutExtension, titleRegex)
        val chapters = splitResult.chapters.mapNotNull { (title, content) ->
            val lines = TextSegmenter.segment(content, maxLineLength)
            if (lines.isEmpty()) null else Chapter(title, lines)
        }
        return TxtParseResult(chapters, splitResult.matchedTitleCount)
    }

    private fun splitChapters(text: String, fallbackTitle: String, titleRegex: Regex): SplitChaptersResult {
        val chapters = mutableListOf<Pair<String, String>>()
        var currentTitle: String? = null
        var currentContent = StringBuilder()
        var foundChapterTitle = false
        var matchedTitleCount = 0

        fun flushCurrent() {
            val title = currentTitle ?: return
            val content = currentContent.toString()
            if (content.isNotBlank()) {
                chapters.add(title to content)
            }
        }

        for (line in text.lines()) {
            val trimmedLine = line.trim()
            if (isChapterTitle(trimmedLine, titleRegex)) {
                matchedTitleCount++
                if (!foundChapterTitle && currentContent.isNotBlank()) {
                    chapters.add(MyMessageBundle.message("reader.prologue") to currentContent.toString())
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
            return SplitChaptersResult(listOf(fallbackTitle to text), matchedTitleCount)
        }

        flushCurrent()
        return SplitChaptersResult(chapters, matchedTitleCount)
    }

    private fun isChapterTitle(line: String, titleRegex: Regex): Boolean {
        return line.isNotEmpty() && titleRegex.matches(line)
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

    private data class SplitChaptersResult(
        val chapters: List<Pair<String, String>>,
        val matchedTitleCount: Int
    )
}
