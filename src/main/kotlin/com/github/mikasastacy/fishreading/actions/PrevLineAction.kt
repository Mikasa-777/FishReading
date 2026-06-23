package com.github.mikasastacy.fishreading.actions

import com.github.mikasastacy.fishreading.inlay.FishInlayService
import com.github.mikasastacy.fishreading.reader.FishReaderService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service

class PrevLineAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val readerService = service<FishReaderService>()

        readerService.prevPage()
        service<FishInlayService>().updateInlay(editor, readerService.getCurrentPage())
    }
}
