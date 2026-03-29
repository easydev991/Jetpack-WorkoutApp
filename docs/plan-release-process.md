# План: Релизный процесс для Jetpack-WorkoutApp

## Цель

Внедрить в Jetpack-WorkoutApp полный релизный процесс, аналогичный JetpackDays:
- Управление версиями через `gradle.properties`
- Автоматическое увеличение VERSION_CODE при релизе
- Настройка Fastlane для бета-тестирования через Crashlytics Beta
- Конфигурация подписи release-сборок
- **Интеграция Firebase Crashlytics для автоматической загрузки mapping files**

---

## Этап 1: Подготовка — Аудит текущего состояния

### Задачи

- [x] Проверить текущую структуру `gradle.properties` (VERSION_NAME, VERSION_CODE)
- [x] Проверить текущую конфигурацию `app/build.gradle.kts` (signing, buildTypes)
- [x] Проверить существующий `Makefile` (команды `make release`, `make apk`)
- [x] Проверить конфигурацию secrets (загружаются из `../android-secrets/swparks`)
- [x] Определить текущие значения:
  - `VERSION_NAME=1.0`, `VERSION_CODE=1`
  - `applicationId=com.swparks`
  - Пакет: `com.swparks`

### Выявленные проблемы

1. **В `android-secrets` отсутствует папка `swparks/`** — репозиторий содержит только `jetpackdays/`. Папку `swparks/` необходимо создать.
2. **В `app/build.gradle.kts` отсутствует блок `signingConfigs`** — подпись настроена в Makefile, но Gradle не знает о ней.
3. **Нет Fastlane** — папка `fastlane/` не существует.

### Что уже настроено

- ✅ ProGuard/R8 обфускация (`isMinifyEnabled = true`, `isShrinkResources = true`)
- ✅ Firebase Crashlytics (`firebaseCrashlytics { mappingFileUploadEnabled = true }`)
- ✅ Crashlytics Collection в manifestPlaceholders

### Как устроен `android-secrets`

Это **отдельный приватный GitHub-репозиторий** (`git@github.com:easydev991/android-secrets.git`), управляемый через Makefile.

Структура репозитория:

```
android-secrets/
├── Makefile                        # Команды: init, gen-keystore, export-zip, export-pem
├── tools/pepk.jar                  # Google Play App Signing tool
├── jetpackdays/                    # Пример: только один проект
│   ├── keystore/
│   │   └── dayscounter-release.keystore
│   ├── certificates/
│   │   ├── pepk_out.zip
│   │   └── uploadcert.pem
│   └── secrets.properties
└── swparks/                        # ⬅️ НЕ СУЩЕСТВУЕТ — нужно создать
    ├── keystore/
    ├── certificates/
    └── secrets.properties
```

Интеграция в JetpackDays (аналогично нужно сделать в Jetpack-WorkoutApp):
- `Makefile` клонирует `android-secrets` по SSH в `.secrets/`
- `sed` корректирует `KEYSTORE_FILE` → `.secrets/keystore/...`
- `app/build.gradle.kts` читает `.secrets/secrets.properties` и применяет к `signingConfigs`

---

## Этап 2: Конфигурация версий

### Задачи

- [x] `gradle.properties` уже содержит: `VERSION_NAME=1.0`, `VERSION_CODE=1`
- [x] `app/build.gradle.kts` уже читает версии из `gradle.properties`
- [x] Makefile уже имеет `make release` с инкрементом VERSION_CODE и сборкой `swparks{VERSION_CODE}.aab`

---

## Этап 3: Настройка подписи release-сборок

### 3.0 Добавление `setup_ssh` в Makefile

Для клонирования `android-secrets` по SSH требуется настроить SSH-доступ к GitHub.

**Добавить переменные в начало Makefile:**

```makefile
# Репозиторий с секретами для подписи (SSH)
SECRETS_REPO = git@github.com:easydev991/android-secrets.git
SECRETS_DIR = swparks
```

**Добавить target `setup_ssh`** (код из JetpackDays, строки 230-300):

```makefile
## setup_ssh: Настраивает SSH-доступ к GitHub (проверка, создание ключа при необходимости)
setup_ssh:
 @printf "$(YELLOW)Проверка SSH-доступа к GitHub...$(RESET)\n"
 @if ssh -T git@github.com 2>&1 | grep -q "successfully authenticated"; then \
  printf "$(GREEN)SSH-доступ к GitHub уже настроен$(RESET)\n"; \
  exit 0; \
 fi
 @# Проверка наличия jq
 @if ! command -v jq >/dev/null 2>&1; then \
  printf "$(YELLOW)Утилита jq не найдена. Устанавливаю через Homebrew...$(RESET)\n"; \
  if command -v brew >/dev/null 2>&1; then brew install jq; else printf "$(RED)Homebrew не найден. Установите jq вручную и повторите.$(RESET)\n"; exit 1; fi; \
 fi
 @# Создание каталога ~/.ssh при необходимости
 @if [ ! -d $$HOME/.ssh ]; then \
  mkdir -p $$HOME/.ssh; \
  printf "$(GREEN)Создана папка ~/.ssh$(RESET)\n"; \
 fi
 @# Создание ключа, если отсутствует
 @if [ ! -f $$HOME/.ssh/id_ed25519 ]; then \
  read -p "Введите email для комментария ключа: " KEY_EMAIL; \
  while [ -z "$$KEY_EMAIL" ]; do read -p "Email не может быть пустым. Введите email: " KEY_EMAIL; done; \
  printf "$(YELLOW)Создаю новый SSH-ключ id_ed25519...$(RESET)\n"; \
  ssh-keygen -t ed25519 -N "" -C "$$KEY_EMAIL" -f $$HOME/.ssh/id_ed25519; \
 else \
  printf "$(GREEN)SSH-ключ $$HOME/.ssh/id_ed25519 уже существует$(RESET)\n"; \
 fi
 @# Запуск ssh-agent и добавление ключа
 @eval "$$((ssh-agent -s) 2>/dev/null)" >/dev/null || true
 @ssh-add -K $$HOME/.ssh/id_ed25519 >/dev/null 2>&1 || ssh-add $$HOME/.ssh/id_ed25519 >/dev/null 2>&1 || true
 @# Настройка ~/.ssh/config для github.com
 @CONFIG_FILE="$$HOME/.ssh/config"; \
 HOST_ENTRY="Host github.com\n  HostName github.com\n  User git\n  AddKeysToAgent yes\n  UseKeychain yes\n  IdentityFile $$HOME/.ssh/id_ed25519\n"; \
 if [ -f "$$CONFIG_FILE" ]; then \
  if ! grep -q "Host github.com" "$$CONFIG_FILE"; then \
   echo "$$HOST_ENTRY" >> "$$CONFIG_FILE"; \
   printf "$(GREEN)Добавлена секция для github.com в ~/.ssh/config$(RESET)\n"; \
  else \
   printf "$(GREEN)Секция для github.com уже есть в ~/.ssh/config$(RESET)\n"; \
  fi; \
 else \
  echo "$$HOST_ENTRY" > "$$CONFIG_FILE"; \
  chmod 600 "$$CONFIG_FILE"; \
  printf "$(GREEN)Создан ~/.ssh/config с секцией для github.com$(RESET)\n"; \
 fi
 @# Предложение добавить публичный ключ в аккаунт GitHub через API
 @printf "$(YELLOW)Добавление публичного ключа в ваш аккаунт GitHub через API...$(RESET)\n"; \
 printf "Требуется персональный токен GitHub с правом 'admin:public_key'.\n"; \
 read -p "Добавить ключ в GitHub через API? [y/N]: " ADD_GH; \
 if [[ "$$ADD_GH" =~ ^[Yy]$$ ]]; then \
  read -p "Введите ваш GitHub Personal Access Token: " TOKEN; \
  read -p "Введите название для SSH-ключа (например, 'work-macbook'): " TITLE; \
  if [ -z "$$TITLE" ]; then TITLE="JetpackDays key"; fi; \
  PUB_KEY=$$(cat $$HOME/.ssh/id_ed25519.pub); \
  DATA=$$(jq -n --arg title "$$TITLE" --arg key "$$PUB_KEY" '{title:$$title, key:$$key}'); \
  RESPONSE=$$(curl -s -w "\n%{http_code}" -X POST "https://api.github.com/user/keys" -H "Accept: application/vnd.github+json" -H "Authorization: token $$TOKEN" -d "$$DATA"); \
  BODY=$$(echo "$$RESPONSE" | sed '$$d'); \
  STATUS=$$(echo "$$RESPONSE" | tail -n 1); \
  if [ "$$STATUS" = "201" ]; then \
   printf "$(GREEN)SSH-ключ успешно добавлен в GitHub$(RESET)\n"; \
  elif [ "$$STATUS" = "422" ]; then \
   printf "$(YELLOW)Ключ уже добавлен или недопустим. Сообщение GitHub:$(RESET)\n"; \
   echo "$$BODY"; \
  else \
   printf "$(RED)Ошибка при добавлении ключа в GitHub (HTTP $$STATUS)$(RESET)\n"; \
   echo "$$BODY"; \
  fi; \
 else \
  printf "$(YELLOW)Пропускаю авто-добавление ключа. Добавьте его вручную: $(RESET)https://github.com/settings/keys\n"; \
 fi
 @printf "$(YELLOW)Проверка соединения с github.com...$(RESET)\n"; \
 ssh -T git@github.com || true
```

**Добавить вызов в `setup` target:**

```makefile
setup:
 @$(MAKE) _check_rbenv
 @$(MAKE) _check_ruby
 @$(MAKE) _check_ruby_version_file
 @$(MAKE) _check_bundler
 @$(MAKE) _check_gemfile
 @$(MAKE) _install_gemfile_deps
 @$(MAKE) setup_fastlane
 @$(MAKE) _check_markdownlint
 @$(MAKE) setup_ssh   # ← заменить _setup_git_hooks на setup_ssh
```

### 3.1 Создание папки `swparks/` в `android-secrets`

В репозитории `android-secrets` **отсутствует** папка `swparks/`. Создание полностью автоматизировано через `make init`.

**Выполнить:**

```bash
cd /Users/Oleg991/Documents/GitHub/android-secrets
make init APP=swparks
```

**Что делает `make init APP=swparks`:**
1. Проверяет — если папка `swparks/` уже существует с keystore, выдаёт ошибку
2. Создаёт структуру:

   ```
   swparks/
   ├── keystore/
   │   └── swparks-release.keystore
   ├── certificates/
   │   ├── pepk_out.zip
   │   └── uploadcert.pem
   └── secrets.properties
   ```

3. Генерирует keystore (RSA-ключ с паролем)
4. Экспортирует ZIP для RuStore через pepk.jar
5. Экспортирует PEM-сертификат для RuStore

**Отдельные команды (если нужно по шагам):**

```bash
make gen-keystore APP=swparks   # Только keystore + secrets.properties
make export-zip APP=swparks      # Только ZIP для RuStore
make export-pem APP=swparks     # Только PEM для RuStore
make reset-secrets APP=swparks  # Удалить все секреты (если нужно пересоздать)
```

### 3.2 Добавление targets для загрузки секретов в Makefile

В JetpackDays используется SSH-клон `android-secrets` в `.secrets/`. В Jetpack-WorkoutApp — аналогично, но с `SECRETS_DIR=swparks`.

**Добавить в Makefile (код из JetpackDays, строки 93-122, адаптировано для swparks):**

```makefile
## _load_secrets: Загрузить секреты из SSH-репозитория во временную директорию
_load_secrets:
 @printf "$(YELLOW)Загрузка секретов из репозитория по SSH...$(RESET)\\n"
 @TEMP_DIR=$$(mktemp -d); \
 trap "rm -rf $$TEMP_DIR" EXIT; \
 printf "$(YELLOW)Клонирую репозиторий $(SECRETS_REPO)...$(RESET)\\n"; \
 if ! git clone --depth 1 $(SECRETS_REPO) "$$TEMP_DIR" 2>/dev/null; then \
  printf "$(RED)Ошибка: не удалось клонировать репозиторий$(RESET)\\n"; \
  printf "$(YELLOW)Проверьте SSH-доступ к GitHub: ssh -T git@github.com$(RESET)\\n"; \
  exit 1; \
 fi; \
 mkdir -p .secrets; \
 cp -r "$$TEMP_DIR/$(SECRETS_DIR)/*" .secrets/ 2>/dev/null || cp -r "$$TEMP_DIR/$(SECRETS_DIR)"/* .secrets/; \
 sed -i.tmp 's|^KEYSTORE_FILE=.*|KEYSTORE_FILE=.secrets/keystore/swparks-release.keystore|' .secrets/secrets.properties && rm -f .secrets/secrets.properties.tmp; \
 printf "$(GREEN)Секреты загружены успешно$(RESET)\\n"

## apk: Создать подписанный APK для GitHub Releases (arm64-v8a + armeabi-v7a, без повышения версии)
apk:
 @printf "$(YELLOW)Проверка секретов для подписи...$(RESET)\n"
 @if [ ! -d ".secrets" ]; then \
  $(MAKE) _load_secrets; \
 fi
 @printf "$(YELLOW)Создаю релизный APK (arm64-v8a + armeabi-v7a)...$(RESET)\n"
 @./gradlew assembleRelease
 @VERSION_NAME=$$(grep "^VERSION_NAME=" gradle.properties | cut -d'=' -f2); \
 VERSION_CODE=$$(grep "^VERSION_CODE=" gradle.properties | cut -d'=' -f2); \
 cp app/build/outputs/apk/release/app-release.apk "swparks$$VERSION_CODE.apk"; \
 printf "$(GREEN)APK создан: swparks$$VERSION_CODE.apk (arm64-v8a + armeabi-v7a)$(RESET)\n"; \
 printf "$(YELLOW)Версия: $$VERSION_NAME (build $$VERSION_CODE)$(RESET)\n"
```

**Ключевые моменты:**
- APK содержит только `arm64-v8a` и `armeabi-v7a` ABI (благодаря `ndk.abiFilters`)
- Размер APK ~30MB вместо ~60MB (без x86/x86_64 нативных библиотек)
- `TEMP_DIR=$$(mktemp -d)` с `trap "rm -rf $$TEMP_DIR" EXIT` — безопасная очистка
- `git clone --depth 1` — shallow clone для скорости
- `sed` заменяет **полный путь** keystore: `KEYSTORE_FILE=.*` → `.secrets/keystore/swparks-release.keystore`
- `.secrets/` должен быть в `.gitignore`

### 3.3 Добавление `signingConfigs` в `app/build.gradle.kts`

> **Примечание:** ProGuard/R8 обфускация (`isMinifyEnabled = true`, `isShrinkResources = true`, `proguardFiles(...)`) **уже настроена** в WorkoutApp. Также `firebaseCrashlytics { mappingFileUploadEnabled = true }` уже добавлен. Требуется только добавить блок `signingConfigs`.

В `app/build.gradle.kts` **отсутствует** блок `signingConfigs`. Необходимо добавить:

1. **Чтение секретов** (после `android {`):

```kotlin
val secretsProperties = Properties()
val secretsPropertiesFile = rootProject.file(".secrets/secrets.properties")
if (secretsPropertiesFile.exists()) {
    secretsPropertiesFile.inputStream().use { secretsProperties.load(it) }
}
```

2. **Блок `signingConfigs`**:

```kotlin
signingConfigs {
    create("release") {
        val keystoreFile = secretsProperties["KEYSTORE_FILE"] as? String ?: ".secrets/keystore/swparks-release.keystore"
        val keystorePassword = secretsProperties["KEYSTORE_PASSWORD"] as? String ?: ""
        val keyAlias = secretsProperties["KEY_ALIAS"] as? String ?: "upload"
        val keyPassword = secretsProperties["KEY_PASSWORD"] as? String ?: ""

        storeFile = rootProject.file(keystoreFile)
        storePassword = keystorePassword
        this.keyAlias = keyAlias
        this.keyPassword = keyPassword
    }
}
```

3. **Применение к release buildType**:

```kotlin
release {
    signingConfig = signingConfigs.getByName("release")
    firebaseCrashlytics {
        mappingFileUploadEnabled = true
    }
    // ...
}
```

### 3.4 Проверка

- [x] Запустить `make release` — убедиться что AAB подписывается корректно ✅
- [x] Запустить `make apk` — убедиться что APK подписывается корректно (arm64-v8a + armeabi-v7a) ✅
- [x] Добавить ABI-фильтры для release: `arm64-v8a` + `armeabi-v7a` (isShrinkResources=true, isMinifyEnabled=true) ✅

---

## Этап 4: Конфигурация Fastlane

Fastlane используется только для Crashlytics Beta (аналогично JetpackDays).
Публикация в RuStore и GitHub Releases — **ручная**.

### Текущее состояние

Fastlane **не настроен** — папка `fastlane/` отсутствует. Makefile имеет `setup_fastlane`, но конфигурация не завершена.

### Задачи

- [x] Создать `fastlane/Appfile`:

  ```ruby
  package_name("com.swparks")
  json_key_file(".secrets/google-services.json") # если используется Firebase
  ```

- [x] Создать `fastlane/Fastfile` с lane-ами:
  - `test` — запуск unit-тестов (`gradle task: "test"`)
  - `beta` — сборка release и загрузка в Crashlytics Beta
  - `screenshots` — генерация скриншотов
  - `screenshots_ru` — скриншоты только для ru-RU
  - `screenshots_en` — скриншоты только для en-US
- [x] Создать `fastlane/Screengrabfile` для локализованных скриншотов (ru-RU, en-US)

### Lane `beta` (аналогично JetpackDays)

```ruby
desc "Submit a new Beta Build to Crashlytics Beta"
lane :beta do
  gradle(task: "clean assembleRelease")
  crashlytics
end
```

**Код `release` target** (аналог JetpackDays, строки 405-423, адаптировано для swparks):

```makefile
## release: Создать подписанную AAB-сборку для публикации (аналог testflight в iOS). Файл: swparks{VERSION_CODE}.aab
release:
 @printf "$(YELLOW)Проверка секретов для подписи...$(RESET)\n"
 @if [ ! -d ".secrets" ]; then \
  $(MAKE) _load_secrets; \
 fi
 @printf "$(YELLOW)Увеличиваю VERSION_CODE...$(RESET)\n"
 @CURRENT_VERSION_CODE=$$(grep "^VERSION_CODE=" gradle.properties | cut -d'=' -f2); \
 NEW_VERSION_CODE=$$((CURRENT_VERSION_CODE + 1)); \
 sed -i.tmp "s/^VERSION_CODE=.*/VERSION_CODE=$$NEW_VERSION_CODE/" gradle.properties && rm -f gradle.properties.tmp; \
 printf "$(GREEN)VERSION_CODE обновлен с $$CURRENT_VERSION_CODE на $$NEW_VERSION_CODE$(RESET)\n"
 @printf "$(YELLOW)Создаю релиз-сборку (AAB)...$(RESET)\n"
 @./gradlew bundleRelease uploadCrashlyticsMappingFileRelease
 @VERSION_CODE=$$(grep "^VERSION_CODE=" gradle.properties | cut -d'=' -f2); \
 OUTPUT_FILE="swparks$$VERSION_CODE.aab"; \
 cp app/build/outputs/bundle/release/app-release.aab "$$OUTPUT_FILE"; \
 printf "$(GREEN)AAB создан и mapping files загружены в Firebase: $$OUTPUT_FILE$(RESET)\n"
 OUTPUT_FILE="swparks$$VERSION_CODE.aab"; \
 cp app/build/outputs/bundle/release/app-release.aab "$$OUTPUT_FILE"; \
 printf "$(GREEN)AAB создан: $$OUTPUT_FILE$(RESET)\n"
 @printf "$(YELLOW)Версия для публикации: $$(grep "^VERSION_NAME=" gradle.properties | cut -d'=' -f2) (build $$NEW_VERSION_CODE)$(RESET)\n"
 @printf "$(YELLOW)Для публикации используйте этот файл в RuStore или Google Play Store$(RESET)\\n"
```

**Примечание:** Деплой в RuStore — `make release` создаёт AAB. Деплой в GitHub Releases — `make apk` создаёт два APK (arm64-v8a + armeabi-v7a). Оба процесса — ручные.

---

## Этап 5: Проверка и тестирование

### Задачи

- [ ] Проверить `make test` — все тесты проходят
- [ ] Проверить `make lint` — lint проходит
- [x] Запустить `make release` — убедиться что VERSION_CODE увеличивается и AAB подписывается ✅
- [x] Проверить `make apk` — создаёт два подписанных APK (arm64-v8a + armeabi-v7a) ✅
- [ ] Проверить Fastlane `fastlane beta` — загружает в Crashlytics Beta
- [ ] Проверить `fastlane screenshots` — генерирует скриншоты

---

## Этап 6: Документация

### Задачи

- [ ] Обновить README.md с инструкциями по релизу
- [ ] Добавить секцию в CONTRIBUTING.md
- [ ] Документировать процесс получения keystore и настройки secrets

---

## Ожидаемый результат

После выполнения плана Jetpack-WorkoutApp будет иметь:

1. Централизованное управление версиями в `gradle.properties`
2. Автоматический инкремент VERSION_CODE при `make release`
3. Настроенную подпись release-сборок
4. ABI-фильтры для release: `arm64-v8a` + `armeabi-v7a` (сокращение размера APK с 60MB до ~30MB)
5. Один универсальный APK для GitHub Releases (с filtered ABIs, не split)
6. Fastlane с lane-ами `test`, `beta`, `screenshots`, `screenshots_ru`, `screenshots_en`
7. Ручной процесс публикации: `make release` → AAB файл → RuStore / GitHub Releases
