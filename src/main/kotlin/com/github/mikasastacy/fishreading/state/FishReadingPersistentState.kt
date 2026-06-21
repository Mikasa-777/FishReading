package com.github.mikasastacy.fishreading.state

import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

data class BookProgress(
    @JvmField val chapterIdx: Int = 0,
    @JvmField val lineIdx: Int = 0,
    @JvmField val bookName: String = "",
    @JvmField val chapterTitles: List<String> = emptyList()
)

@State(name = "FishReadingState", storages = [Storage("fish_reading_config.xml")])
@Service(Service.Level.APP)
class FishReadingPersistentState : SerializablePersistentStateComponent<FishReadingPersistentState.State>(State()) {

    val lastActiveBookPath: String?
        get() = state.lastActiveBookPath

    val readingLineCount: Int
        get() = state.readingLineCount

    val managedBooks: Map<String, BookProgress>
        get() = state.managedBooks

    fun setLastActiveBookPath(filePath: String?) {
        updateState { it.copy(lastActiveBookPath = filePath) }
    }

    fun rememberBook(filePath: String, bookName: String): BookProgress {
        lateinit var progress: BookProgress
        updateState {
            val current = it.managedBooks[filePath]
            progress = current?.copy(bookName = bookName) ?: BookProgress(bookName = bookName)
            it.copy(managedBooks = it.managedBooks + (filePath to progress))
        }
        return progress
    }

    fun bookProgress(filePath: String): BookProgress? = state.managedBooks[filePath]

    fun updateChapterTitles(filePath: String, titles: List<String>) {
        updateBookProgress(filePath) { it.copy(chapterTitles = titles) }
    }

    fun saveProgress(filePath: String, chapterIdx: Int, lineIdx: Int) {
        updateBookProgress(filePath) { it.copy(chapterIdx = chapterIdx, lineIdx = lineIdx) }
    }

    fun removeBook(filePath: String) {
        if (filePath !in state.managedBooks) return
        updateState {
            it.copy(
                lastActiveBookPath = if (it.lastActiveBookPath == filePath) null else it.lastActiveBookPath,
                managedBooks = it.managedBooks - filePath
            )
        }
    }

    fun normalizedReadingLineCount(): Int = readingLineCount.coerceIn(MIN_READING_LINE_COUNT, MAX_READING_LINE_COUNT)

    fun updateReadingLineCount(lineCount: Int) {
        updateState {
            it.copy(readingLineCount = lineCount.coerceIn(MIN_READING_LINE_COUNT, MAX_READING_LINE_COUNT))
        }
    }

    private fun updateBookProgress(filePath: String, transform: (BookProgress) -> BookProgress) {
        updateState {
            val current = it.managedBooks[filePath] ?: return@updateState it
            it.copy(managedBooks = it.managedBooks + (filePath to transform(current)))
        }
    }

    data class State(
        @JvmField val lastActiveBookPath: String? = null,
        @JvmField val readingLineCount: Int = DEFAULT_READING_LINE_COUNT,
        @JvmField val managedBooks: Map<String, BookProgress> = emptyMap()
    )

    companion object {
        const val MIN_READING_LINE_COUNT = 1
        const val MAX_READING_LINE_COUNT = 20
        const val DEFAULT_READING_LINE_COUNT = 1
    }
}
