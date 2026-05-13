package com.claudeplugin.ui

import com.claudeplugin.api.ApiKeyStorage
import com.claudeplugin.api.ClaudeApiClient
import com.claudeplugin.api.Message
import com.claudeplugin.generator.FileGenerator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.text.DefaultCaret

class ClaudeGeneratorDialog(
    private val project: Project,
    private val title: String,
    private val modeIndex: Int,
    private val targetDir: VirtualFile? = null
) : DialogWrapper(project) {

    private val apiKeyField = JPasswordField(30).apply {
        text = ApiKeyStorage.loadKey()
        background = Color(0x2B2D30)
        foreground = Color(0xBCBEC4)
        caretColor = Color(0xBCBEC4)
        font = Font("JetBrains Mono", Font.PLAIN, 12)
    }

    private val descArea = JTextArea(5, 50).apply {
        lineWrap = true
        wrapStyleWord = true
        font = Font("JetBrains Mono", Font.PLAIN, 13)
        background = Color(0x2B2D30)
        foreground = Color(0xBCBEC4)
        caretColor = Color(0xBCBEC4)
        border = EmptyBorder(8, 8, 8, 8)
    }

    private val resultArea = JTextArea(12, 50).apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        font = Font("JetBrains Mono", Font.PLAIN, 12)
        background = Color(0x1E1F22)
        foreground = Color(0xBCBEC4)
        border = EmptyBorder(8, 8, 8, 8)
        (caret as DefaultCaret).updatePolicy = DefaultCaret.ALWAYS_UPDATE
    }

    private val statusLabel = JLabel("Введи описание и нажми «Генерировать»").apply {
        foreground = Color(0x6A8759)
        font = Font("JetBrains Mono", Font.ITALIC, 11)
    }

    private val generateBtn = JButton("⚡ Генерировать").apply {
        background = Color(0x4C9BE8)
        foreground = Color.WHITE
        font = Font("JetBrains Mono", Font.BOLD, 13)
        isBorderPainted = false
    }

    private val createFilesBtn = JButton("📂 Создать файлы").apply {
        background = Color(0x499C54)
        foreground = Color.WHITE
        font = Font("JetBrains Mono", Font.BOLD, 13)
        isBorderPainted = false
        isEnabled = false
    }

    private var lastResponse = ""

    init {
        this.title = title
        init()
        setSize(700, 650)
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(8, 8)).apply {
            background = Color(0x1E1F22)
            border = EmptyBorder(12, 12, 12, 12)
        }

        // API Key
        val keyPanel = JPanel(BorderLayout(8, 0)).apply {
            background = Color(0x2B2D30)
            border = EmptyBorder(8, 8, 8, 8)
        }
        keyPanel.add(JLabel("API Key Anthropic:").apply {
            foreground = Color(0x6A8759)
            font = Font("JetBrains Mono", Font.PLAIN, 12)
        }, BorderLayout.WEST)
        keyPanel.add(apiKeyField, BorderLayout.CENTER)

        // Описание
        val descPanel = JPanel(BorderLayout()).apply {
            background = Color(0x1E1F22)
        }
        descPanel.add(JLabel("Описание (ТЗ):").apply {
            foreground = Color(0xBCBEC4)
            font = Font("JetBrains Mono", Font.BOLD, 12)
            border = EmptyBorder(0, 0, 4, 0)
        }, BorderLayout.NORTH)
        descPanel.add(JScrollPane(descArea).apply {
            border = BorderFactory.createLineBorder(Color(0x4C9BE8))
        }, BorderLayout.CENTER)

        // Кнопки
        val btnPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 4)).apply {
            background = Color(0x1E1F22)
        }
        btnPanel.add(generateBtn)
        btnPanel.add(createFilesBtn)
        btnPanel.add(statusLabel)

        // Результат
        val resultPanel = JPanel(BorderLayout()).apply {
            background = Color(0x1E1F22)
        }
        resultPanel.add(JLabel("Результат Claude:").apply {
            foreground = Color(0xBCBEC4)
            font = Font("JetBrains Mono", Font.BOLD, 12)
            border = EmptyBorder(4, 0, 4, 0)
        }, BorderLayout.NORTH)
        resultPanel.add(JScrollPane(resultArea).apply {
            border = BorderFactory.createLineBorder(Color(0x3C3F41))
        }, BorderLayout.CENTER)

        val topPanel = JPanel(BorderLayout(0, 8)).apply {
            background = Color(0x1E1F22)
        }
        topPanel.add(keyPanel, BorderLayout.NORTH)
        topPanel.add(descPanel, BorderLayout.CENTER)
        topPanel.add(btnPanel, BorderLayout.SOUTH)

        panel.add(topPanel, BorderLayout.NORTH)
        panel.add(resultPanel, BorderLayout.CENTER)

        // Listeners
        generateBtn.addActionListener { generate() }
        createFilesBtn.addActionListener { createFiles() }

        return panel
    }

    private fun generate() {
        val apiKey = String(apiKeyField.password)
        if (apiKey.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Вставь API ключ!", "Нет ключа", JOptionPane.ERROR_MESSAGE)
            return
        }
        ApiKeyStorage.saveKey(apiKey)
        val desc = descArea.text.trim()
        if (desc.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Напиши описание!", "Пусто", JOptionPane.WARNING_MESSAGE)
            return
        }

        val modePrefix = when (modeIndex) {
            1 -> "Создай полный Android проект с нуля. "
            2 -> "Создай новый экран (Activity + ViewModel + XML layout). "
            3 -> "Создай RecyclerView (Adapter + ViewHolder + item layout). "
            4 -> "Создай Room Database (Entity + DAO + Database класс). "
            else -> ""
        }

        generateBtn.isEnabled = false
        generateBtn.text = "⏳ Генерация..."
        resultArea.text = ""
        statusLabel.text = "Генерирую..."
        createFilesBtn.isEnabled = false

        val client = ClaudeApiClient(apiKey)
        client.sendMessage(
            messages = listOf(Message("user", modePrefix + desc)),
            systemPrompt = ClaudeApiClient.SYSTEM_ANDROID_GENERATOR,
            onChunk = { chunk ->
                SwingUtilities.invokeLater { resultArea.append(chunk) }
            },
            onComplete = { full ->
                lastResponse = full
                SwingUtilities.invokeLater {
                    generateBtn.isEnabled = true
                    generateBtn.text = "⚡ Генерировать"
                    val files = FileGenerator.parseFiles(full)
                    if (files.isNotEmpty()) {
                        createFilesBtn.isEnabled = true
                        statusLabel.text = "✅ Найдено файлов: ${files.size}"
                        statusLabel.foreground = Color(0x6A8759)
                    } else {
                        statusLabel.text = "✅ Готово (файлов не обнаружено)"
                    }
                }
            },
            onError = { err ->
                SwingUtilities.invokeLater {
                    resultArea.append("\n❌ Ошибка: $err")
                    generateBtn.isEnabled = true
                    generateBtn.text = "⚡ Генерировать"
                    statusLabel.text = "❌ Ошибка"
                    statusLabel.foreground = Color(0xCC666E)
                }
            }
        )
    }

    private fun createFiles() {
        val files = FileGenerator.parseFiles(lastResponse)
        if (files.isEmpty()) return

        val dir = targetDir ?: ProjectUtils.getProjectDir(project) ?: return
        resultArea.append("\n\n--- Создаю файлы ---\n")

        FileGenerator.createFiles(
            project = project,
            targetDir = dir,
            files = files,
            onProgress = { msg -> SwingUtilities.invokeLater { resultArea.append("$msg\n") } },
            onDone = { count ->
                SwingUtilities.invokeLater {
                    resultArea.append("\n🎉 Готово! Создано файлов: $count")
                    statusLabel.text = "🎉 Создано: $count файлов"
                    createFilesBtn.isEnabled = false
                    dir.refresh(true, true)
                }
            }
        )
    }

    override fun createActions(): Array<Action> = arrayOf(cancelAction)
}
