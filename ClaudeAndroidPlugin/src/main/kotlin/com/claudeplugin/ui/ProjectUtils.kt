package com.claudeplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile

object ProjectUtils {
    fun getProjectDir(project: Project): VirtualFile? {
        return project.guessProjectDir()
    }
}
