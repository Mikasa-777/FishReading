package com.github.mikasastacy.fishreading.actions

import com.github.mikasastacy.fishreading.i18n.MyMessageBundle
import com.github.mikasastacy.fishreading.inlay.FishInlayService
import com.github.mikasastacy.fishreading.reader.FishReaderService
import com.github.mikasastacy.fishreading.reader.TxtChapterRegexApplyResult
import com.github.mikasastacy.fishreading.reader.TxtParser
import com.github.mikasastacy.fishreading.state.FishReadingPersistentState
import com.intellij.openapi.project.Project
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import java.io.File
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel

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
                    val isTxtBook = File(path).extension.lowercase() == "txt"

                    if (progress.chapterTitles.isEmpty()) {
                        actions.add(object : AnAction(MyMessageBundle.message("menu.book.load.uncached")) {
                            override fun actionPerformed(event: AnActionEvent) {
                                val editor = event.getData(CommonDataKeys.EDITOR) ?: return
                                readerService.loadBook(File(path))
                                service<FishInlayService>().updateInlay(editor, readerService.getCurrentPage())
                            }
                        })
                        if (isTxtBook) {
                            actions.add(CustomTxtChapterRegexAction(path))
                        }
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

                    if (isTxtBook) {
                        actions.add(CustomTxtChapterRegexAction(path))
                    }

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

private class CustomTxtChapterRegexAction(private val path: String) :
    AnAction(MyMessageBundle.message("menu.book.customTxtChapterRegex")) {

    override fun actionPerformed(event: AnActionEvent) {
        val settings = service<FishReadingPersistentState>()
        val initialRegex = settings.bookProgress(path)?.chapterTitleRegex ?: TxtParser.DEFAULT_CHAPTER_TITLE_REGEX
        TxtChapterRegexDialog(event.project, File(path), initialRegex).show()
    }
}

private class TxtChapterRegexDialog(
    private val project: Project?,
    private val file: File,
    initialRegex: String
) : DialogWrapper(project, true) {
    private val regexTextArea = JBTextArea(initialRegex, 8, 56).apply {
        lineWrap = true
        wrapStyleWord = false
    }

    init {
        title = MyMessageBundle.message("dialog.txtChapterRegex.title")
        init()
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(BorderLayout()).apply {
            add(JBScrollPane(regexTextArea), BorderLayout.CENTER)
            add(
                JBLabel(MyMessageBundle.message("dialog.txtChapterRegex.warning")).apply {
                    foreground = JBColor.RED
                },
                BorderLayout.SOUTH
            )
        }
    }

    override fun createActions(): Array<Action> {
        return arrayOf(okAction, RestoreDefaultAction(), cancelAction)
    }

    override fun doOKAction() {
        val result = service<FishReaderService>().applyTxtChapterTitleRegex(file, regexTextArea.text)
        when (result) {
            is TxtChapterRegexApplyResult.Success -> {
                if (result.activeBookReloaded) {
                    val inlayService = service<FishInlayService>()
                    inlayService.activeEditor()?.let { editor ->
                        inlayService.updateInlay(editor, service<FishReaderService>().getCurrentPage())
                    }
                }
                Messages.showInfoMessage(
                    project,
                    MyMessageBundle.message("dialog.txtChapterRegex.success", result.chapterCount),
                    MyMessageBundle.message("dialog.txtChapterRegex.result.title")
                )
                super.doOKAction()
            }
            else -> {
                Messages.showErrorDialog(
                    project,
                    errorMessage(result),
                    MyMessageBundle.message("dialog.txtChapterRegex.result.title")
                )
            }
        }
    }

    private fun errorMessage(result: TxtChapterRegexApplyResult): String {
        return when (result) {
            TxtChapterRegexApplyResult.EmptyRegex -> MyMessageBundle.message("dialog.txtChapterRegex.error.empty")
            is TxtChapterRegexApplyResult.InvalidRegex -> MyMessageBundle.message(
                "dialog.txtChapterRegex.error.invalid",
                result.reason.orEmpty()
            )
            TxtChapterRegexApplyResult.NoMatchedTitle -> MyMessageBundle.message("dialog.txtChapterRegex.error.noMatchedTitle")
            TxtChapterRegexApplyResult.NoValidContent -> MyMessageBundle.message("dialog.txtChapterRegex.error.noValidContent")
            is TxtChapterRegexApplyResult.FileFailure -> MyMessageBundle.message(
                "dialog.txtChapterRegex.error.fileFailure",
                result.reason.orEmpty()
            )
            is TxtChapterRegexApplyResult.Success -> MyMessageBundle.message(
                "dialog.txtChapterRegex.success",
                result.chapterCount
            )
        }
    }

    private inner class RestoreDefaultAction : AbstractAction(
        MyMessageBundle.message("dialog.txtChapterRegex.restoreDefault")
    ) {
        override fun actionPerformed(event: java.awt.event.ActionEvent?) {
            regexTextArea.text = TxtParser.DEFAULT_CHAPTER_TITLE_REGEX
            regexTextArea.caretPosition = 0
        }
    }
}
