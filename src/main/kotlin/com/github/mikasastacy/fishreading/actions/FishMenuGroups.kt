package com.github.mikasastacy.fishreading.actions

import com.github.mikasastacy.fishreading.i18n.MyMessageBundle
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
        val settings = service<FishReadingPersistentState>()
        val readerService = service<FishReaderService>()

        if (settings.managedBooks.isEmpty()) {
            return EMPTY_ARRAY
        }

        return settings.managedBooks.map { (path, progress) ->
            val isCurrentBook = settings.lastActiveBookPath == path
            val displayName = if (isCurrentBook) "✓ ${progress.bookName}" else progress.bookName

            object : ActionGroup(displayName, true) {
                override fun getChildren(ae: AnActionEvent?): Array<AnAction> {
                    val actions = mutableListOf<AnAction>()

                    if (progress.chapterTitles.isEmpty()) {
                        actions.add(object : AnAction(MyMessageBundle.message("menu.book.load.uncached")) {
                            override fun actionPerformed(event: AnActionEvent) {
                                val editor = event.getData(CommonDataKeys.EDITOR) ?: return
                                readerService.loadBook(File(path))
                                service<FishInlayService>().updateInlay(editor, readerService.getCurrentPage())
                            }
                        })
                        return actions.toTypedArray()
                    }

                    val resumeText = if (isCurrentBook) {
                        MyMessageBundle.message("menu.book.resume.current")
                    } else {
                        MyMessageBundle.message("menu.book.resume.saved")
                    }
                    actions.add(object : AnAction(resumeText) {
                        override fun actionPerformed(event: AnActionEvent) {
                            val editor = event.getData(CommonDataKeys.EDITOR) ?: return
                            if (settings.lastActiveBookPath != path) {
                                readerService.loadBook(File(path))
                            }
                            service<FishInlayService>().updateInlay(editor, readerService.getCurrentPage())
                        }
                    })

                    actions.add(object : AnAction(MyMessageBundle.message("menu.book.forget")) {
                        override fun actionPerformed(event: AnActionEvent) {
                            if (readerService.forgetBook(path)) {
                                service<FishInlayService>().clearInlay()
                            }
                        }
                    })

                    actions.add(Separator.getInstance())

                    val chapterActions = progress.chapterTitles.mapIndexed { chapIndex, title ->
                        val isCurrentChapter = isCurrentBook && progress.chapterIdx == chapIndex
                        val chapterDisplayName = if (isCurrentChapter) "-> $title" else title

                        object : AnAction(chapterDisplayName) {
                            override fun actionPerformed(event: AnActionEvent) {
                                val editor = event.getData(CommonDataKeys.EDITOR) ?: return
                                if (settings.lastActiveBookPath != path) {
                                    readerService.loadBook(File(path))
                                }
                                readerService.jumpToChapter(chapIndex)
                                service<FishInlayService>().updateInlay(editor, readerService.getCurrentPage())
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
