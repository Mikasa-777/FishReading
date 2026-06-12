package com.github.mikasastacy.fishreading.reader

import com.github.mikasastacy.fishreading.state.BookProgress
import com.github.mikasastacy.fishreading.state.FishReadingPersistentState
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.io.File

@Service(Service.Level.APP)
class FishReaderService : Disposable {
    private val session = ReaderSession(emptyList())
    private var loadedBookPath: String? = null

    fun getCurrentLine(): String {
        ensureActiveBookLoaded()
        return session.currentLine()
    }

    fun nextLine() {
        if (!ensureActiveBookLoaded()) return
        session.nextLine()
        saveProgress()
    }

    fun prevLine() {
        if (!ensureActiveBookLoaded()) return
        session.prevLine()
        saveProgress()
    }

    fun getChapterTitles(): List<String> {
        ensureActiveBookLoaded()
        return session.chapterTitles()
    }

    fun getCurrentChapterIdx(): Int = session.chapterIndex

    fun jumpToChapter(index: Int) {
        if (!ensureActiveBookLoaded()) return
        session.jumpToChapter(index)
        saveProgress()
    }

    fun loadBook(file: File): String {
        return when (val extension = file.extension.lowercase()) {
            "epub" -> loadEpub(file)
            "txt" -> loadTxt(file)
            else -> "暂不支持扩展名为 .$extension 的书籍"
        }
    }

    fun loadEpub(file: File): String {
        return try {
            val newChapters = EpubParser.parse(file)
            if (newChapters.isEmpty()) {
                "未发现有效文本"
            } else {
                applyLoadedBook(file, newChapters)
                "成功装载《${file.nameWithoutExtension}》"
            }
        } catch (e: Exception) {
            "加载失败: ${e.message}"
        }
    }

    fun loadTxt(file: File): String {
        return try {
            val newChapters = TxtParser.parse(file)
            if (newChapters.isEmpty()) {
                "未发现有效文本"
            } else {
                applyLoadedBook(file, newChapters)
                "成功装载《${file.nameWithoutExtension}》"
            }
        } catch (e: Exception) {
            "加载失败: ${e.message}"
        }
    }

    private fun applyLoadedBook(file: File, newChapters: List<Chapter>) {
        val state = service<FishReadingPersistentState>().state
        state.lastActiveBookPath = file.absolutePath

        val progress = state.getBookProgress(file.absolutePath, file.nameWithoutExtension)
        state.updateChapterTitles(file.absolutePath, newChapters.map { it.title })
        session.replaceChapters(newChapters, progress.chapterIdx, progress.lineIdx)
        loadedBookPath = file.absolutePath
    }

    private fun ensureActiveBookLoaded(): Boolean {
        val path = service<FishReadingPersistentState>().state.lastActiveBookPath ?: return false
        if (loadedBookPath == path) return true

        val file = File(path)
        if (!file.exists() || file.extension.lowercase() !in SUPPORTED_EXTENSIONS) {
            return false
        }

        loadBook(file)
        return loadedBookPath == path
    }

    private fun saveProgress() {
        val state = service<FishReadingPersistentState>().state
        val path = state.lastActiveBookPath ?: return
        state.saveProgress(path, session.chapterIndex, session.lineIndex)
    }

    override fun dispose() {}

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("epub", "txt")
    }
}
