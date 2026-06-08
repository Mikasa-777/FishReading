package com.github.mikasastacy.fishreading.reader

class ReaderSession(
    chapters: List<Chapter>,
    chapterIndex: Int = 0,
    lineIndex: Int = 0
) {
    var chapters: List<Chapter> = chapters.ifEmpty {
        listOf(Chapter("欢迎使用", listOf("// [FishReading] 请去 Tools 菜单加载或选择书籍")))
    }
        private set

    var chapterIndex: Int = 0
        private set

    var lineIndex: Int = 0
        private set

    init {
        restore(chapterIndex, lineIndex)
    }

    fun currentLine(): String {
        val chapter = chapters.getOrNull(chapterIndex) ?: return "// 暂无内容"
        return chapter.lines.getOrNull(lineIndex) ?: return "// 本章结束"
    }

    fun chapterTitles(): List<String> = chapters.mapIndexed { i, chapter -> "[${i + 1}] ${chapter.title}" }

    fun nextLine() {
        val currentChapter = chapters.getOrNull(chapterIndex) ?: return
        if (lineIndex < currentChapter.lines.size - 1) {
            lineIndex++
        } else if (chapterIndex < chapters.size - 1) {
            chapterIndex++
            lineIndex = 0
        }
    }

    fun prevLine() {
        if (lineIndex > 0) {
            lineIndex--
        } else if (chapterIndex > 0) {
            chapterIndex--
            lineIndex = (chapters[chapterIndex].lines.size - 1).coerceAtLeast(0)
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

    private fun restore(chapterIndex: Int, lineIndex: Int) {
        this.chapterIndex = chapterIndex.coerceIn(this.chapters.indices)
        val lines = this.chapters[this.chapterIndex].lines
        this.lineIndex = if (lines.isEmpty()) 0 else lineIndex.coerceIn(lines.indices)
    }
}
