package com.claudeplugin.actions

import com.claudeplugin.ui.ClaudeGeneratorDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

// ─── Весь проект с нуля ───────────────────────────────────────────
class GenerateFullProjectAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ClaudeGeneratorDialog(project, "📱 Весь проект с нуля", 1).show()
    }
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}

// ─── Новый экран MVVM ─────────────────────────────────────────────
class GenerateScreenAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dir = e.getData(CommonDataKeys.VIRTUAL_FILE)
        ClaudeGeneratorDialog(project, "🖥 Новый экран (Activity + ViewModel + Layout)", 2, dir).show()
    }
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}

// ─── RecyclerView ─────────────────────────────────────────────────
class GenerateRecyclerViewAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dir = e.getData(CommonDataKeys.VIRTUAL_FILE)
        ClaudeGeneratorDialog(project, "📋 RecyclerView (Adapter + ViewHolder + Layout)", 3, dir).show()
    }
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}

// ─── Room Database ────────────────────────────────────────────────
class GenerateRoomAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dir = e.getData(CommonDataKeys.VIRTUAL_FILE)
        ClaudeGeneratorDialog(project, "🗄 Room Database (Entity + DAO + Database)", 4, dir).show()
    }
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
