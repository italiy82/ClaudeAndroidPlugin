package com.claudeplugin.ui

import com.claudeplugin.api.ApiKeyStorage
import com.claudeplugin.api.ClaudeApiClient
import com.claudeplugin.api.Message
import com.claudeplugin.generator.FileGenerator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.text.DefaultCaret

class ClaudeChatPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val messageHistory = mutableListOf<Message>()
    private var apiKey: String = ApiKeyStorage.loadKey()
    private var selectedDir: VirtualFile? = null
    private var isLoading = false

    // UI компоненты
    private val chatArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        font = Font("JetBrains Mono", Font.PLAIN, 13)
        background = Color(0x1E1F22)
        foreground = Color(0xBCBEC4)
        border = EmptyBorder(12, 12, 12, 12)
        (caret as DefaultCaret).updatePolicy = DefaultCaret.ALWAYS_UPDATE
    }

    private val inputArea = JTextArea(3, 40).apply {
        lineWrap = true
        wrapStyleWord = true
        font = Font("JetBrains Mono", Font.PLAIN, 13)
        background = Color(0x2B2D30)
        foreground = Color(0xBCBEC4)
        caretColor = Color(0xBCBEC4)
        border = EmptyBorder(8, 8, 8, 8)
    }

    private val sendBtn = JButton("▶ Отправить").apply {
        background = Color(0x4C9BE8)
        foreground = Color.WHITE
        font = Font("JetBrains Mono", Font.BOLD, 13)
        isBorderPainted = false
        isFocusPainted = false
        cursor = Cursor(Cursor.HAND_CURSOR)
        preferredSize = Dimension(150, 36)
    }

    private val clearBtn = JButton("🗑 Очистить").apply {
        background = Color(0x3C3F41)
        foreground = Color(0xBCBEC4)
        font = Font("JetBrains Mono", Font.PLAIN, 12)
        isBorderPainted = false
        isFocusPainted = false
        cursor = Cursor(Cursor.HAND_CURSOR)
        preferredSize = Dimension(120, 36)
    }

    private val dirBtn = JButton("📁 Папка").apply {
        background = Color(0x3C3F41)
        foreground = Color(0xBCBEC4)
        font = Font("JetBrains Mono", Font.PLAIN, 12)
        isBorderPainted = false
        isFocusPainted = false
        cursor = Cursor(Cursor.HAND_CURSOR)
        preferredSize = Dimension(120, 36)
    }

    private val apiKeyField = JPasswordField(30).apply {
        text = ApiKeyStorage.loadKey()
        background = Color(0x2B2D30)
        foreground = Color(0xBCBEC4)
        caretColor = Color(0xBCBEC4)
        font = Font("JetBrains Mono", Font.PLAIN, 12)
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color(0x4C9BE8), 1),
            EmptyBorder(4, 8, 4, 8)
        )
    }

    private val modeSelector = JComboBox(arrayOf(
        "💬 Свободный чат",
        "📱 Весь проект с нуля",
        "🖥 Новый экран (MVVM)",
        "📋 RecyclerView",
        "🗄 Room Database"
    )).apply {
        background = Color(0x2B2D30)
        foreground = Color(0xBCBEC4)
        font = Font("JetBrains Mono", Font.PLAIN, 12)
        (renderer as? JLabel)?.border = EmptyBorder(4, 8, 4, 8)
    }

    private val statusLabel = JLabel("Готов к работе").apply {
        foreground = Color(0x6A8759)
        font = Font("JetBrains Mono", Font.ITALIC, 11)
        border = EmptyBorder(4, 8, 4, 8)
    }

    private val createFilesBtn = JButton("📂 Создать файлы в проекте").apply {
        background = Color(0x499C54)
        foreground = Color.WHITE
        font = Font("JetBrains Mono", Font.BOLD, 12)
        isBorderPainted = false
        isFocusPainted = false
        cursor = Cursor(Cursor.HAND_CURSOR)
        isEnabled = false
        preferredSize = Dimension(220, 32)
    }

    private var lastClaudeResponse = ""

    init {
        background = Color(0x1E1F22)
        setupUI()
        setupListeners()
        appendMessage("Claude", "👋 Привет! Я Claude — AI-генератор Android кода.\n\n" +
                "Как использовать:\n" +
                "1️⃣ Вставь API ключ Anthropic сверху\n" +
                "2️⃣ Выбери режим генерации\n" +
                "3️⃣ Опиши что нужно создать\n" +
                "4️⃣ Нажми «Создать файлы» чтобы добавить код в проект\n\n" +
                "Пример: «Создай экран профиля пользователя с аватаром, именем и кнопкой выхода»")
    }

    private fun setupUI() {
        // Верхняя панель — API ключ
        val topPanel = JPanel(BorderLayout(8, 0)).apply {
            background = Color(0x2B2D30)
            border = EmptyBorder(8, 8, 8, 8)
        }
        topPanel.add(JLabel("API Key:").apply {
            foreground = Color(0x6A8759)
            font = Font("JetBrains Mono", Font.PLAIN, 12)
        }, BorderLayout.WEST)
        topPanel.add(apiKeyField, BorderLayout.CENTER)

        val saveKeyBtn = JButton("Сохранить").apply {
            background = Color(0x4C9BE8)
            foreground = Color.WHITE
            font = Font("JetBrains Mono", Font.PLAIN, 11)
            isBorderPainted = false
            isFocusPainted = false
        }
        saveKeyBtn.addActionListener {
            apiKey = String(apiKeyField.password)
            if (apiKey.isNotEmpty()) ApiKeyStorage.saveKey(apiKey) else ApiKeyStorage.clearKey()
            statusLabel.text = if (apiKey.isNotEmpty()) "✅ API ключ сохранён" else "❌ Ключ пустой"
            statusLabel.foreground = if (apiKey.isNotEmpty()) Color(0x6A8759) else Color(0xCC666E)
        }
        topPanel.add(saveKeyBtn, BorderLayout.EAST)

        // Панель режима
        val modePanel = JPanel(BorderLayout(8, 0)).apply {
            background = Color(0x2B2D30)
            border = EmptyBorder(0, 8, 8, 8)
        }
        modePanel.add(JLabel("Режим:").apply {
            foreground = Color(0x6A8759)
            font = Font("JetBrains Mono", Font.PLAIN, 12)
            preferredSize = Dimension(55, 28)
        }, BorderLayout.WEST)
        modePanel.add(modeSelector, BorderLayout.CENTER)

        val northPanel = JPanel(BorderLayout()).apply {
            background = Color(0x2B2D30)
        }
        northPanel.add(topPanel, BorderLayout.NORTH)
        northPanel.add(modePanel, BorderLayout.SOUTH)

        // Чат
        val chatScroll = JBScrollPane(chatArea).apply {
            border = BorderFactory.createLineBorder(Color(0x3C3F41))
            verticalScrollBar.unitIncrement = 16
        }

        // Панель ввода
        val inputScroll = JBScrollPane(inputArea).apply {
            border = BorderFactory.createLineBorder(Color(0x4C9BE8))
            minimumSize = Dimension(0, 80)
            preferredSize = Dimension(0, 80)
        }

        // Кнопки действий
        val actionsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 4)).apply {
            background = Color(0x1E1F22)
        }
        actionsPanel.add(sendBtn)
        actionsPanel.add(clearBtn)
        actionsPanel.add(dirBtn)
        actionsPanel.add(createFilesBtn)
        actionsPanel.add(statusLabel)

        val bottomPanel = JPanel(BorderLayout()).apply {
            background = Color(0x1E1F22)
            border = EmptyBorder(4, 4, 4, 4)
        }
        bottomPanel.add(inputScroll, BorderLayout.CENTER)
        bottomPanel.add(actionsPanel, BorderLayout.SOUTH)

        add(northPanel, BorderLayout.NORTH)
        add(chatScroll, BorderLayout.CENTER)
        add(bottomPanel, BorderLayout.SOUTH)
    }

    private fun setupListeners() {
        sendBtn.addActionListener { sendMessage() }

        inputArea.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER && e.isControlDown) {
                    sendMessage()
                    e.consume()
                }
            }
        })

        clearBtn.addActionListener {
            chatArea.text = ""
            messageHistory.clear()
            lastClaudeResponse = ""
            createFilesBtn.isEnabled = false
            statusLabel.text = "Чат очищен"
        }

        dirBtn.addActionListener {
            val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            descriptor.title = "Выбери папку для создания файлов"
            val file = FileChooser.chooseFile(descriptor, project, null)
            selectedDir = file
            if (file != null) {
                statusLabel.text = "📁 ${file.name}"
                statusLabel.foreground = Color(0x6A8759)
            }
        }

        createFilesBtn.addActionListener { createFilesInProject() }
    }

    private fun sendMessage() {
        if (isLoading) return
        val text = inputArea.text.trim()
        if (text.isEmpty()) return

        if (apiKey.isEmpty()) {
            Messages.showErrorDialog(project, "Вставь API ключ Anthropic!", "Нет API ключа")
            return
        }

        // Добавляем контекст режима к запросу
        val modePrefix = when (modeSelector.selectedIndex) {
            1 -> "Создай полный Android проект с нуля. "
            2 -> "Создай новый экран (Activity + ViewModel + XML layout). "
            3 -> "Создай RecyclerView (Adapter + ViewHolder + item layout). "
            4 -> "Создай Room Database (Entity + DAO + Database класс). "
            else -> ""
        }
        val fullText = modePrefix + text

        appendMessage("Ты", text)
        inputArea.text = ""
        messageHistory.add(Message("user", fullText))

        isLoading = true
        sendBtn.isEnabled = false
        sendBtn.text = "⏳ Генерация..."
        statusLabel.text = "Генерирую код..."
        statusLabel.foreground = Color(0xBCBEC4)
        createFilesBtn.isEnabled = false

        chatArea.append("\nClaude: ")
        val currentLength = chatArea.text.length

        val client = ClaudeApiClient(apiKey)
        client.sendMessage(
            messages = messageHistory,
            systemPrompt = ClaudeApiClient.SYSTEM_ANDROID_GENERATOR,
            onChunk = { chunk ->
                SwingUtilities.invokeLater {
                    chatArea.append(chunk)
                }
            },
            onComplete = { fullResponse ->
                lastClaudeResponse = fullResponse
                messageHistory.add(Message("assistant", fullResponse))
                SwingUtilities.invokeLater {
                    chatArea.append("\n\n")
                    isLoading = false
                    sendBtn.isEnabled = true
                    sendBtn.text = "▶ Отправить"
                    statusLabel.text = "✅ Готово"
                    statusLabel.foreground = Color(0x6A8759)
                    val files = FileGenerator.parseFiles(fullResponse)
                    if (files.isNotEmpty()) {
                        createFilesBtn.isEnabled = true
                        statusLabel.text = "✅ Найдено файлов: ${files.size} — нажми «Создать файлы»"
                    }
                }
            },
            onError = { error ->
                SwingUtilities.invokeLater {
                    chatArea.append("\n❌ Ошибка: $error\n\n")
                    isLoading = false
                    sendBtn.isEnabled = true
                    sendBtn.text = "▶ Отправить"
                    statusLabel.text = "❌ Ошибка"
                    statusLabel.foreground = Color(0xCC666E)
                }
            }
        )
    }

    private fun createFilesInProject() {
        val files = FileGenerator.parseFiles(lastClaudeResponse)
        if (files.isEmpty()) {
            Messages.showInfoMessage(project, "В последнем ответе нет файлов для создания.", "Файлы не найдены")
            return
        }

        val targetDir = selectedDir ?: ProjectUtils.getProjectDir(project)
        if (targetDir == null) {
            Messages.showErrorDialog(project, "Не удалось определить папку проекта.", "Ошибка")
            return
        }

        appendMessage("Система", "Создаю ${files.size} файлов в: ${targetDir.path}")

        FileGenerator.createFiles(
            project = project,
            targetDir = targetDir,
            files = files,
            onProgress = { msg ->
                SwingUtilities.invokeLater { chatArea.append("\n$msg") }
            },
            onDone = { count ->
                SwingUtilities.invokeLater {
                    chatArea.append("\n\n🎉 Создано файлов: $count\n\n")
                    statusLabel.text = "🎉 Создано: $count файлов"
                    statusLabel.foreground = Color(0x6A8759)
                    createFilesBtn.isEnabled = false
                    // Обновляем дерево проекта
                    targetDir.refresh(true, true)
                }
            }
        )
    }

    private fun appendMessage(sender: String, text: String) {
        SwingUtilities.invokeLater {
            chatArea.append("\n$sender: $text\n")
        }
    }
}
