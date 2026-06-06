package com.example.fishreading

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class PrevLineAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        val readerService = project.getService(FishReaderService::class.java)
        readerService.prevLine()

        FishInlayManager.updateInlay(editor, readerService.getCurrentLine())
    }
}