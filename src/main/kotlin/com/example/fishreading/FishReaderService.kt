package com.example.fishreading

import com.example.fishreading.loader.BookParser
import com.example.fishreading.loader.EpubParser
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import java.io.File

@Service(Service.Level.APP) // ✨ 修正：确保这里是 APP 级别
class FishReaderService : Disposable {

    data class Chapter(val title: String, val lines: List<String>)

    private var chapters: List<Chapter> = listOf(
        Chapter("欢迎使用", listOf("// [FishReading] 请去 Tools 菜单加载或选择书籍"))
    )
    private var currentChapterIdx = 0
    private var currentLineIdx = 0

    init {
        // 注册全局光标监听器。只要光标移动（点击或上下换行），立刻擦除虚拟文字
        EditorFactory.getInstance().eventMulticaster.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                FishInlayManager.clearInlay()
            }
        }, this)

        // 自动加载上次看过的书
        val state = com.intellij.openapi.application.ApplicationManager.getApplication()
            .getService(FishReadingPersistentState::class.java).state
        state.lastActiveBookPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                loadBook(file)
            }
        }
    }

    fun getCurrentLine(): String {
        val chapter = chapters.getOrNull(currentChapterIdx) ?: return "// 暂无内容"
        return chapter.lines.getOrNull(currentLineIdx) ?: "// 本章结束"
    }

    fun nextLine() {
        val currentChapter = chapters.getOrNull(currentChapterIdx) ?: return
        if (currentLineIdx < currentChapter.lines.size - 1) {
            currentLineIdx++
        } else if (currentChapterIdx < chapters.size - 1) {
            currentChapterIdx++
            currentLineIdx = 0
        }
        saveProgress()
    }

    fun prevLine() {
        if (currentLineIdx > 0) {
            currentLineIdx--
        } else if (currentChapterIdx > 0) {
            currentChapterIdx--
            currentLineIdx = (chapters[currentChapterIdx].lines.size - 1).coerceAtLeast(0)
        }
        saveProgress()
    }

    fun getChapterTitles(): List<String> = chapters.mapIndexed { i, c -> "[${i + 1}] ${c.title}" }
//    fun getChapterTitlesRaw(): List<String> = chapters.map { it.title }
    fun getCurrentChapterIdx(): Int = currentChapterIdx

    fun jumpToChapter(index: Int) {
        if (index in chapters.indices) {
            currentChapterIdx = index
            currentLineIdx = 0
            saveProgress()
        }
    }

    private fun saveProgress() {
        val state = com.intellij.openapi.application.ApplicationManager.getApplication()
            .getService(FishReadingPersistentState::class.java).state
        val path = state.lastActiveBookPath ?: return
        state.managedBooks[path]?.apply {
            chapterIdx = currentChapterIdx
            lineIdx = currentLineIdx
        }
    }

    fun loadBook(file: File): String {
        try {
            // 1. 动态策略分发器
            val parser: BookParser = when (val ext = file.extension.lowercase()) {
                "epub" -> EpubParser()
                // "txt"  -> TxtParser()  <-- 未来扩充 TXT 只需要写在这里，它会自动享受智能切分服务
                else -> return "暂不支持扩展名为 .${ext} 的书籍"
            }

            // 2. 多态解析，完美适配 Windows 编码
            val newChapters = parser.parse(file)

            if (newChapters.isNotEmpty()) {
                this.chapters = newChapters

                val state = com.intellij.openapi.application.ApplicationManager.getApplication()
                    .getService(FishReadingPersistentState::class.java).state
                state.lastActiveBookPath = file.absolutePath

                val progress = state.managedBooks.getOrPut(file.absolutePath) {
                    BookProgress().apply { bookName = file.nameWithoutExtension }
                }

                progress.chapterTitles = newChapters.map { it.title }
                this.currentChapterIdx = progress.chapterIdx
                this.currentLineIdx = progress.lineIdx

                return "成功装载《${file.nameWithoutExtension}》"
            }
        } catch (e: Exception) {
            return "加载失败: ${e.message}"
        }
        return "未发现有效文本"
    }

    override fun dispose() {}
}