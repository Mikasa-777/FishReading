package com.github.mikasastacy.fishreading.reader

import com.github.mikasastacy.fishreading.i18n.MyMessageBundle

class ReaderSession(
    chapters: List<Chapter>,
    chapterIndex: Int = 0,
    lineIndex: Int = 0
) {
    var chapters: List<Chapter> = chapters.ifEmpty { welcomeChapters() }
        private set

    var chapterIndex: Int = 0
        private set

    var lineIndex: Int = 0
        private set

    init {
        restore(chapterIndex, lineIndex)
    }

    fun currentLine(): String {
        val chapter = chapters.getOrNull(chapterIndex) ?: return MyMessageBundle.message("reader.emptyContent")
        return chapter.lines.getOrNull(lineIndex) ?: return MyMessageBundle.message("reader.endOfChapter")
    }

    fun currentPage(pageSize: Int): List<String> {
        val normalizedPageSize = pageSize.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE)
        val chapter = chapters.getOrNull(chapterIndex)
        val lines = chapter?.lines
            ?.drop(lineIndex)
            ?.take(normalizedPageSize)
            .orEmpty()
        return lines + List(normalizedPageSize - lines.size) { EMPTY_SLOT }
    }

    fun chapterTitles(): List<String> = chapters.mapIndexed { i, chapter -> "[${i + 1}] ${chapter.title}" }

    fun nextLine() = nextPage(1)

    fun nextPage(pageSize: Int) {
        val normalizedPageSize = pageSize.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE)
        val currentChapter = chapters.getOrNull(chapterIndex) ?: return
        if (lineIndex + normalizedPageSize < currentChapter.lines.size) {
            lineIndex += normalizedPageSize
        } else if (chapterIndex < chapters.size - 1) {
            chapterIndex++
            lineIndex = 0
        }
    }

    fun prevLine() = prevPage(1)

    fun prevPage(pageSize: Int) {
        val normalizedPageSize = pageSize.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE)
        if (lineIndex > 0) {
            lineIndex = (lineIndex - normalizedPageSize).coerceAtLeast(0)
        } else if (chapterIndex > 0) {
            chapterIndex--
            val previousChapterLineCount = chapters[chapterIndex].lines.size
            lineIndex = (previousChapterLineCount - normalizedPageSize).coerceAtLeast(0)
        }
    }

    fun jumpToChapter(index: Int) {
        if (index in chapters.indices) {
            chapterIndex = index
            lineIndex = 0
        }
    }

    fun replaceChapters(chapters: List<Chapter>, chapterIndex: Int, lineIndex: Int) {
        this.chapters = chapters.ifEmpty { this.chapters }
        restore(chapterIndex, lineIndex)
    }

    fun resetToWelcome() {
        chapters = welcomeChapters()
        restore(0, 0)
    }

    private fun restore(chapterIndex: Int, lineIndex: Int) {
        this.chapterIndex = chapterIndex.coerceIn(this.chapters.indices)
        val lines = this.chapters[this.chapterIndex].lines
        this.lineIndex = if (lines.isEmpty()) 0 else lineIndex.coerceIn(lines.indices)
    }

    companion object {
        private const val MIN_PAGE_SIZE = 1
        private const val MAX_PAGE_SIZE = 20
        private const val EMPTY_SLOT = "//"

        private fun welcomeChapters(): List<Chapter> =
            listOf(
                Chapter(
                    MyMessageBundle.message("reader.welcome.chapter"),
                    listOf(MyMessageBundle.message("reader.welcome.line"))
                )
            )
    }
}
