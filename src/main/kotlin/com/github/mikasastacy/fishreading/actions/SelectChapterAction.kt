package com.github.mikasastacy.fishreading.actions

import com.github.mikasastacy.fishreading.i18n.MyMessageBundle
import com.github.mikasastacy.fishreading.inlay.FishInlayService
import com.github.mikasastacy.fishreading.reader.FishReaderService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.popup.JBPopupFactory

class SelectChapterAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val readerService = service<FishReaderService>()
        val titles = readerService.getChapterTitles()
        if (titles.isEmpty()) return

        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(titles)
            .setTitle(MyMessageBundle.message("popup.chapter.title"))
            .setItemChosenCallback { selectedValue ->
                val indexString = selectedValue.substringAfter("[").substringBefore("]")
                val chapterIndex = indexString.toIntOrNull()?.minus(1) ?: return@setItemChosenCallback

                readerService.jumpToChapter(chapterIndex)
                service<FishInlayService>().updateInlay(editor, readerService.getCurrentPage())
            }
            .setMovable(true)
            .setResizable(true)
            .setNamerForFiltering { it }
            .createPopup()
            .showInBestPositionFor(e.dataContext)
    }
}
