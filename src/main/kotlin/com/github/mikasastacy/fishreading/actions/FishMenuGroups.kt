package com.github.mikasastacy.fishreading.actions

import com.github.mikasastacy.fishreading.inlay.FishInlayService
import com.github.mikasastacy.fishreading.reader.FishReaderService
import com.github.mikasastacy.fishreading.state.FishReadingPersistentState
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.components.service
import java.io.File

class BookAndChapterMenuGroup : ActionGroup() {

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val state = service<FishReadingPersistentState>().state
        val readerService = service<FishReaderService>()

        if (state.managedBooks.isEmpty()) {
            return EMPTY_ARRAY
        }

        return state.managedBooks.map { (path, progress) ->
            val isCurrentBook = state.lastActiveBookPath == path
            val displayName = if (isCurrentBook) "✓ ${progress.bookName}" else progress.bookName

            object : ActionGroup(displayName, true) {
                override fun getChildren(ae: AnActionEvent?): Array<AnAction> {
                    val actions = mutableListOf<AnAction>()

                    if (progress.chapterTitles.isEmpty()) {
                        actions.add(object : AnAction("尚未缓存目录，点击激活载入") {
                            override fun actionPerformed(event: AnActionEvent) {
                                val editor = event.getData(CommonDataKeys.EDITOR) ?: return
                                readerService.loadBook(File(path))
                                service<FishInlayService>().updateInlay(editor, readerService.getCurrentLine())
                            }
                        })
                        return actions.toTypedArray()
                    }

                    val resumeText = if (isCurrentBook) "继续阅读 (当前书籍)" else "继续阅读 (从上次进度恢复)"
                    actions.add(object : AnAction(resumeText) {
                        override fun actionPerformed(event: AnActionEvent) {
                            val editor = event.getData(CommonDataKeys.EDITOR) ?: return
                            if (state.lastActiveBookPath != path) {
                                readerService.loadBook(File(path))
                            }
                            service<FishInlayService>().updateInlay(editor, readerService.getCurrentLine())
                        }
                    })

                    actions.add(Separator.getInstance())

                    actions.add(object : AnAction("忘记本书") {
                        override fun actionPerformed(event: AnActionEvent) {
                            state.removeBook(path)
                        }
                    })

                    val chapterActions = progress.chapterTitles.mapIndexed { chapIndex, title ->
                        val isCurrentChapter = isCurrentBook && progress.chapterIdx == chapIndex
                        val chapterDisplayName = if (isCurrentChapter) "-> $title" else title

                        object : AnAction(chapterDisplayName) {
                            override fun actionPerformed(event: AnActionEvent) {
                                val editor = event.getData(CommonDataKeys.EDITOR) ?: return
                                if (state.lastActiveBookPath != path) {
                                    readerService.loadBook(File(path))
                                }
                                readerService.jumpToChapter(chapIndex)
                                service<FishInlayService>().updateInlay(editor, readerService.getCurrentLine())
                            }
                        }
                    }
                    actions.addAll(chapterActions)

                    return actions.toTypedArray()
                }
            }
        }.toTypedArray()
    }
}
