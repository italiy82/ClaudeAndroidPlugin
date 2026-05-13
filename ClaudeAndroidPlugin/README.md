# Claude Android Generator — плагин для Android Studio

AI-генератор Android кода прямо в IDE. Пишешь ТЗ — получаешь готовые файлы.

## Что умеет

- 💬 **Чат с Claude** прямо в боковой панели Android Studio
- 📱 **Весь проект с нуля** — пишешь описание приложения, получаешь все файлы
- 🖥 **Новый экран** — Activity + ViewModel + XML Layout за один запрос
- 📋 **RecyclerView** — Adapter + ViewHolder + item layout
- 🗄 **Room Database** — Entity + DAO + Database класс
- 📂 **Автосоздание файлов** — Claude отвечает, ты жмёшь кнопку, файлы появляются в проекте

---

## Установка

### 1. Собери плагин

Нужно: **JDK 17+**, **Gradle 8.5+**

```bash
./gradlew buildPlugin
```

Плагин будет в: `build/distributions/ClaudeAndroidPlugin-1.0.0.zip`

### 2. Установи в Android Studio

1. Android Studio → **Settings** (Ctrl+Alt+S)
2. **Plugins** → шестерёнка ⚙️ → **Install Plugin from Disk...**
3. Выбери ZIP из `build/distributions/`
4. Перезапусти Studio

---

## Использование

### Боковая панель (рекомендуется)

1. Справа появится вкладка **"Claude Generator"**
2. Вставь API ключ Anthropic → нажми **Сохранить**
3. Выбери **режим** (весь проект / экран / RecyclerView / Room)
4. Напиши ТЗ в поле внизу → **Ctrl+Enter** или кнопка **▶ Отправить**
5. Claude генерирует код в чате
6. Нажми **📂 Создать файлы** → файлы появятся в проекте

### Через правую кнопку мыши

ПКМ на папке в дереве проекта → **New** → выбери нужный генератор

---

## Получить API ключ

1. Зайди на https://console.anthropic.com
2. **API Keys** → **Create Key**
3. Скопируй ключ (начинается с `sk-ant-...`)

---

## Примеры запросов

**Новый экран:**
```
Создай экран профиля пользователя с:
- Круглый аватар сверху
- Имя и email пользователя
- Список достижений в RecyclerView
- Кнопка "Редактировать профиль"
- Кнопка "Выйти" внизу
```

**RecyclerView:**
```
RecyclerView для списка товаров в магазине.
Каждый элемент: картинка товара, название, цена, кнопка "В корзину".
При клике на элемент открывать детальный экран.
```

**Room Database:**
```
База данных для заметок.
Заметка: id, заголовок, текст, дата создания, категория, флаг избранного.
CRUD операции + поиск по тексту + фильтр по категории.
```

**Весь проект:**
```
Приложение Todo List:
- Список задач с возможностью добавить/удалить/отметить выполненной
- Фильтр: все / активные / выполненные
- Локальное хранение через Room
- MVVM + Hilt + Coroutines
- Material Design 3
```

---

## Как Claude создаёт файлы

Плагин обучает Claude отвечать в специальном формате:

```
=== ФАЙЛ: app/src/main/java/com/example/ui/ProfileActivity.kt ===
(код файла)
=== КОНЕЦ ФАЙЛА ===
```

Плагин парсит этот формат и создаёт реальные файлы в нужных папках.

---

## Структура проекта плагина

```
ClaudeAndroidPlugin/
├── build.gradle.kts
├── settings.gradle.kts
└── src/main/
    ├── kotlin/com/claudeplugin/
    │   ├── api/
    │   │   └── ClaudeApiClient.kt      # Работа с Anthropic API (streaming)
    │   ├── generator/
    │   │   └── FileGenerator.kt        # Парсинг ответа + создание файлов
    │   ├── ui/
    │   │   ├── ClaudeToolWindowFactory.kt  # Боковая панель
    │   │   ├── ClaudeChatPanel.kt          # UI чата
    │   │   └── ClaudeGeneratorDialog.kt    # Диалог для ПКМ-генераторов
    │   └── actions/
    │       └── Actions.kt              # 4 действия в меню New
    └── resources/META-INF/
        └── plugin.xml                  # Описание плагина
```
