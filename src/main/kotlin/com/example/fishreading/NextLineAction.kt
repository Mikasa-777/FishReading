package com.example.fishreading

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager

class NextLineAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        val readerService = ApplicationManager.getApplication().getService(FishReaderService::class.java)
        readerService.nextLine()

        FishInlayManager.updateInlay(editor, readerService.getCurrentLine())
    }
}