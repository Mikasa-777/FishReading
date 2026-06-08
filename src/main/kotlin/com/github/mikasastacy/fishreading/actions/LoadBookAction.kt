package com.github.mikasastacy.fishreading.actions

import com.github.mikasastacy.fishreading.reader.FishReaderService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtilCore

class LoadBookAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false).apply {
            title = "选择你的摸鱼秘籍"
            description = "支持格式：.epub, .txt"
            withFileFilter { it.extension?.lowercase() in supportedExtensions }
        }

        val virtualFile = FileChooser.chooseFile(descriptor, project, null) ?: return
        val file = VfsUtilCore.virtualToIoFile(virtualFile)
        val resultNotify = service<FishReaderService>().loadBook(file)
        Messages.showInfoMessage(project, resultNotify, "图书装载结果")
    }

    private companion object {
        val supportedExtensions = setOf("epub", "txt")
    }
}
