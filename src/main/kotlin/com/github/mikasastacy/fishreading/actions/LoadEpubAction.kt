package com.github.mikasastacy.fishreading.actions

import com.github.mikasastacy.fishreading.reader.FishReaderService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.Messages
import java.io.File

class LoadEpubAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false).apply {
            title = "选择你的摸鱼秘籍"
            description = "Chose one epub book"
            withFileFilter { it.extension?.lowercase() == "epub" }
        }

        val virtualFile = FileChooser.chooseFile(descriptor, project, null) ?: return
        val resultNotify = service<FishReaderService>().loadEpub(File(virtualFile.path))
        Messages.showInfoMessage(project, resultNotify, "图书装载成功")
    }
}
