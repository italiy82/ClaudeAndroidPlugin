package com.claudeplugin.generator

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import java.io.File

object FileGenerator {

    data class GeneratedFile(val relativePath: String, val content: String)

    /**
     * Парсит ответ Claude и извлекает файлы в формате:
     * === ФАЙЛ: path/to/file.kt ===
     * (content)
     * === КОНЕЦ ФАЙЛА ===
     */
    fun parseFiles(claudeResponse: String): List<GeneratedFile> {
        val files = mutableListOf<GeneratedFile>()
        val regex = Regex(
            """=== ФАЙЛ: (.+?) ===\s*\n(.*?)\n=== КОНЕЦ ФАЙЛА ===""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
        )
        regex.findAll(claudeResponse).forEach { match ->
            val path = match.groupValues[1].trim()
            val content = match.groupValues[2].trim()
            files.add(GeneratedFile(path, content))
        }
        return files
    }

    /**
     * Создаёт файлы в проекте
     */
    fun createFiles(
        project: Project,
        targetDir: VirtualFile,
        files: List<GeneratedFile>,
        onProgress: (String) -> Unit,
        onDone: (Int) -> Unit
    ) {
        WriteCommandAction.runWriteCommandAction(project) {
            var created = 0
            files.forEach { file ->
                try {
                    createFile(targetDir, file.relativePath, file.content)
                    onProgress("✅ Создан: ${file.relativePath}")
                    created++
                } catch (e: Exception) {
                    onProgress("❌ Ошибка: ${file.relativePath} — ${e.message}")
                }
            }
            onDone(created)
        }
    }

    private fun createFile(baseDir: VirtualFile, relativePath: String, content: String) {
        val parts = relativePath.replace("\\", "/").split("/")
        var currentDir = baseDir

        // Создаём директории
        for (i in 0 until parts.size - 1) {
            val dirName = parts[i]
            currentDir = currentDir.findChild(dirName)
                ?: currentDir.createChildDirectory(this, dirName)
        }

        // Создаём файл
        val fileName = parts.last()
        val existingFile = currentDir.findChild(fileName)
        val vFile = existingFile ?: currentDir.createChildData(this, fileName)
        vFile.setBinaryContent(content.toByteArray(Charsets.UTF_8))
    }

    /**
     * Создаёт структуру нового Android проекта с нуля
     */
    fun createProjectStructure(basePath: String, packageName: String, appName: String): String {
        return """
Создай полный Android проект со следующими параметрами:
- Название приложения: $appName
- Package name: $packageName
- Min SDK: 26
- Target SDK: 34
- Язык: Kotlin
- Архитектура: MVVM + Clean Architecture
- DI: Hilt
- БД: Room (если нужна)
- Навигация: Navigation Component
- UI: Material Design 3 + ViewBinding

Создай все необходимые файлы включая:
1. build.gradle.kts (app и project уровень)
2. AndroidManifest.xml
3. MainActivity.kt
4. MainViewModel.kt  
5. activity_main.xml
6. Hilt Application класс
7. strings.xml, colors.xml, themes.xml
8. Базовые зависимости в gradle
        """.trimIndent()
    }
}
