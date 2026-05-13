package com.claudeplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class ClaudeToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = ClaudeChatPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "AI Generator", false)
        toolWindow.contentManager.addContent(content)
    }
}
