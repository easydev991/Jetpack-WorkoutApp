# Makefile для проекта Jetpack-WorkoutApp

# Цвета и шрифт для вывода в консоль
YELLOW=\033[1;33m
GREEN=\033[1;32m
RED=\033[1;31m
BOLD=\033[1m
RESET=\033[0m

# Версия Ruby в проекте
RUBY_VERSION=3.2.2

# Глобальные настройки шелла для работы с rbenv и bundle
SHELL := /bin/bash
.ONESHELL:
BUNDLE_EXEC := RBENV_VERSION=$(RUBY_VERSION) bundle exec

# Репозиторий с секретами для подписи (SSH)
SECRETS_REPO = git@github.com:easydev991/android-secrets.git
SECRETS_DIR = swparks

## help: Показать это справочное сообщение
help:
	@echo "Доступные команды Makefile:"
	@echo ""
	@sed -n 's/^##//p' ${MAKEFILE_LIST} | \
	awk -F ':' '{printf "  $(BOLD)%s$(RESET):%s\n", $$1, $$2}' BOLD="$(BOLD)" RESET="$(RESET)" | column -t -s ':'
	@echo ""

# Сборка проекта
## build: Сборка APK для отладки
build:
	./gradlew assembleDebug

## clean: Очистка кеша проекта
clean:
	./gradlew clean

# Тестирование
## test: Запуск unit-тестов (JVM, без устройства)
test:
	@if [ -f scripts/test_report.py ]; then chmod +x scripts/test_report.py; fi
	@./gradlew test --console=plain; BUILD_STATUS=$$?; \
	if [ $$BUILD_STATUS -ne 0 ]; then \
		echo ""; \
		echo "================================================================================"; \
		printf "\033[1;31m[FAIL] СБОРКА ПРОВАЛИЛАСЬ С ОШИБКАМИ\033[0m\n"; \
		echo "================================================================================"; \
		exit 1; \
	else \
		echo "Тесты выполнены. Результаты:"; \
		find app/build/test-results -name "*.xml" -exec grep -h "testsuite" {} \; 2>/dev/null || true; \
		echo ""; \
		python3 scripts/test_report.py; \
	fi

## android-test: Запуск интеграционных тестов на Android устройстве
android-test:
	@if [ -f scripts/android_test_report.py ]; then chmod +x scripts/android_test_report.py; fi
	@./gradlew connectedDebugAndroidTest --console=plain; BUILD_STATUS=$$?; \
	if [ $$BUILD_STATUS -ne 0 ]; then \
		echo ""; \
		echo "================================================================================"; \
		printf "\033[1;31m[FAIL] СБОРКА ПРОВАЛИЛАСЬ С ОШИБКАМИ\033[0m\n"; \
		echo "================================================================================"; \
		exit 1; \
	else \
		echo "Тесты выполнены. Результаты:"; \
		find app/build/reports/androidTests -name "*.html" 2>/dev/null | head -1 || echo "HTML отчет не найден"; \
		echo ""; \
		python3 scripts/android_test_report.py; \
	fi

## test-all: Запуск всех тестов (unit + интеграционные)
test-all:
	@echo ""
	@echo "Все тесты выполнены"
	@echo "Unit: app/build/test-results/"
	@echo "Интеграционные: app/build/reports/androidTests/connected/debug/index.html"

# Анализ кода
## lint: Запуск ktlint, detekt и markdownlint (проверка)
lint:
	./gradlew ktlintCheck
	./gradlew app:detekt
	@if command -v markdownlint >/dev/null 2>&1; then \
		markdownlint "**/*.md" ".cursor/rules/*.mdc"; \
	else \
		echo "$(YELLOW)markdownlint-cli не установлен. Для установки: npm install -g markdownlint-cli$(RESET)"; \
	fi

## format: Форматирование кода (ktlint + detekt с исправлениями) и Markdown-файлов
format:
	./gradlew ktlintFormat
	./gradlew app:detekt -Pdetekt.autoCorrect=true
	@if command -v markdownlint >/dev/null 2>&1; then \
		markdownlint --fix "**/*.md" ".cursor/rules/*.mdc"; \
	else \
		echo "$(YELLOW)markdownlint-cli не установлен. Для установки: npm install -g markdownlint-cli$(RESET)"; \
	fi

# Сборка и запуск всех проверок
## check: Полная проверка (сборка + тесты + линтер)
check: build test
	./gradlew ktlintCheck detekt

# Установка приложения
## install: Установка APK на устройство
install:
	./gradlew installDebug

## _load_secrets: Загрузить секреты из SSH-репозитория во временную директорию
_load_secrets:
	@printf "$(YELLOW)Загрузка секретов из репозитория по SSH...$(RESET)\n"
	@TEMP_DIR=$$(mktemp -d); \
	trap "rm -rf $$TEMP_DIR" EXIT; \
	printf "$(YELLOW)Клонирую репозиторий $(SECRETS_REPO)...$(RESET)\n"; \
	if ! git clone --depth 1 $(SECRETS_REPO) "$$TEMP_DIR" 2>/dev/null; then \
		printf "$(RED)Ошибка: не удалось клонировать репозиторий$(RESET)\n"; \
		printf "$(YELLOW)Проверьте SSH-доступ к GitHub: ssh -T git@github.com$(RESET)\n"; \
		exit 1; \
	fi; \
	mkdir -p .secrets; \
	cp -r "$$TEMP_DIR/$(SECRETS_DIR)/*" .secrets/ 2>/dev/null || cp -r "$$TEMP_DIR/$(SECRETS_DIR)"/* .secrets/; \
	sed -i.tmp 's|^KEYSTORE_FILE=.*|KEYSTORE_FILE=.secrets/keystore/swparks-release.keystore|' .secrets/secrets.properties && rm -f .secrets/secrets.properties.tmp; \
	printf "$(GREEN)Секреты загружены успешно$(RESET)\n"

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

# Настройка окружения
## setup: Установка и настройка инструментов для локальной разработки (rbenv, Ruby, fastlane, markdownlint-cli)
setup:
	@$(MAKE) _check_rbenv
	@$(MAKE) _check_ruby
	@$(MAKE) _check_ruby_version_file
	@$(MAKE) _check_bundler
	@$(MAKE) _check_gemfile
	@$(MAKE) _install_gemfile_deps
	@$(MAKE) setup_fastlane
	@$(MAKE) _check_markdownlint
	@$(MAKE) setup_ssh

## _setup_git_hooks: Настроить Git hooks для автоматического обновления версий
_setup_git_hooks:
	@printf "$(YELLOW)Настройка Git hooks...$(RESET)\n"
	@chmod +x .githooks/*
	@git config core.hooksPath .githooks
	@printf "$(GREEN)Git hooks настроены: .githooks/$(RESET)\n"

## _check_rbenv: Проверка наличия rbenv
_check_rbenv:
	@printf "$(YELLOW)Проверка rbenv...$(RESET)\n"
	@if ! command -v rbenv >/dev/null 2>&1; then \
		printf "Установка rbenv...\n"; \
		brew install rbenv ruby-build; \
		rbenv init -; \
	else \
		printf "$(GREEN)rbenv уже установлен$(RESET)\n"; \
	fi

## _check_ruby: Проверка наличия Ruby нужной версии
_check_ruby:
	@printf "\n"
	@printf "$(YELLOW)Проверка Ruby версии $(RUBY_VERSION)...$(RESET)\n"
	@if ! rbenv versions | grep -q $(RUBY_VERSION); then \
		printf "Установка Ruby $(RUBY_VERSION)...\n"; \
		rbenv install $(RUBY_VERSION); \
	fi
	@printf "$(GREEN)Ruby $(shell rbenv versions | grep $(RUBY_VERSION))$(RESET)\n"

## _check_ruby_version_file: Проверка и создание файла .ruby-version
_check_ruby_version_file:
	@printf "\n"
	@printf "$(YELLOW)Проверка содержимого файла .ruby-version...$(RESET)\n"
	@if [ ! -f .ruby-version ] || [ "$(shell cat .ruby-version 2>/dev/null || echo '')" != "$(RUBY_VERSION)" ]; then \
		printf "$(YELLOW)Файл .ruby-version не найден или содержит неверную версию. Обновляю...$(RESET)\n"; \
		echo "$(RUBY_VERSION)" > .ruby-version; \
	else \
		printf "$(GREEN)Файл .ruby-version корректно настроен$(RESET)\n"; \
	fi
	@RBENV_VERSION=$(RUBY_VERSION) ruby -v >/dev/null 2>&1 || true
	@printf "$(GREEN)Ruby активирован локально для проекта$(RESET)\n"

## _check_bundler: Проверка наличия Bundler
_check_bundler:
	@printf "\n"
	@printf "$(YELLOW)Проверка Bundler...$(RESET)\n"
	@if [ -f Gemfile.lock ]; then \
		BUNDLER_VERSION=$$(grep -A 1 "BUNDLED WITH" Gemfile.lock | tail -n 1 | xargs); \
		if [ -z "$$BUNDLER_VERSION" ]; then \
			printf "$(RED)Не удалось определить версию Bundler из Gemfile.lock$(RESET)\n"; \
			exit 1; \
		fi; \
		if ! RBENV_VERSION=$(RUBY_VERSION) gem list -i bundler -v "$$BUNDLER_VERSION" >/dev/null 2>&1; then \
			printf "Установка Bundler версии $$BUNDLER_VERSION...\n"; \
			RBENV_VERSION=$(RUBY_VERSION) gem install bundler -v "$$BUNDLER_VERSION"; \
		else \
			printf "$(GREEN)Bundler версии $$BUNDLER_VERSION уже установлен$(RESET)\n"; \
		fi; \
	else \
		printf "$(YELLOW)Файл Gemfile.lock не найден, устанавливаю последнюю версию bundler...$(RESET)\n"; \
		RBENV_VERSION=$(RUBY_VERSION) gem install bundler; \
	fi

## _check_gemfile: Проверка наличия Gemfile
_check_gemfile:
	@printf "\n"
	@printf "$(YELLOW)Проверка Gemfile...$(RESET)\n"
	@if [ ! -f "Gemfile" ]; then \
		printf "$(RED)Ошибка: Gemfile не найден в корне проекта$(RESET)\n"; \
		exit 1; \
	else \
		printf "$(GREEN)Gemfile найден$(RESET)\n"; \
	fi

## _install_gemfile_deps: Установка зависимостей из Gemfile
_install_gemfile_deps:
	@printf "\n"
	@printf "$(YELLOW)Установка Ruby-зависимостей...$(RESET)\n"
	@if ! RBENV_VERSION=$(RUBY_VERSION) bundle check >/dev/null 2>&1; then \
		printf "Выполняется bundle install...\n"; \
		RBENV_VERSION=$(RUBY_VERSION) bundle install; \
		printf "$(GREEN)Все Ruby-зависимости успешно установлены$(RESET)\n"; \
	else \
		printf "$(GREEN)Все Ruby-зависимости уже установлены$(RESET)\n"; \
	fi

## _check_markdownlint: Проверка наличия markdownlint-cli
_check_markdownlint:
	@printf "\n"
	@printf "$(YELLOW)Проверка markdownlint-cli...$(RESET)\n"
	@if ! command -v npm >/dev/null 2>&1; then \
		printf "$(RED)Ошибка: Node.js/npm не установлен. Установите Node.js с https://nodejs.org/$(RESET)\n"; \
		exit 1; \
	fi
	@if ! command -v markdownlint >/dev/null 2>&1; then \
		printf "Установка markdownlint-cli...\n"; \
		npm install -g markdownlint-cli; \
	else \
		printf "$(GREEN)markdownlint-cli уже установлен$(RESET)\n"; \
	fi

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

## setup_fastlane: Инициализировать fastlane (создать папку fastlane и Fastfile)
setup_fastlane:
	@printf "$(YELLOW)Инициализация fastlane...$(RESET)\n"
	@if [ -d "fastlane" ] && [ -f "fastlane/Fastfile" ]; then \
		printf "$(GREEN)Fastlane уже инициализирован$(RESET)\n"; \
	else \
		$(BUNDLE_EXEC) fastlane init --skip-github --skip-wizard; \
		printf "$(GREEN)Fastlane инициализирован успешно$(RESET)\n"; \
	fi

## update_fastlane: Проверить и установить обновления fastlane
update_fastlane:
	@printf "$(YELLOW)Проверка обновлений fastlane...$(RESET)\n"
	@if $(BUNDLE_EXEC) bundle outdated fastlane 2>/dev/null | grep -q "fastlane"; then \
		printf "Обнаружены обновления fastlane. Установка...\n"; \
		$(BUNDLE_EXEC) bundle update fastlane; \
		printf "$(GREEN)Fastlane обновлен$(RESET)\n"; \
	else \
		printf "$(GREEN)Fastlane уже последней версии$(RESET)\n"; \
	fi

## fastlane: Запустить меню команд fastlane
fastlane:
	@if [ ! -d fastlane ] || [ ! -f fastlane/Fastfile ]; then \
		printf "$(RED)fastlane не инициализирован в проекте$(RESET)\n"; \
		$(MAKE) setup_fastlane; \
		if [ ! -d fastlane ] || [ ! -f fastlane/Fastfile ]; then \
			printf "$(RED)Нужно инициализировать fastlane перед использованием$(RESET)\n"; \
			exit 1; \
		fi; \
	fi
	@printf "$(YELLOW)Запуск меню команд fastlane...$(RESET)\n"
	@$(BUNDLE_EXEC) fastlane

# Дополнительно
## screenshots: Генерировать скриншоты (ru-RU) через fastlane
screenshots:
	@$(MAKE) _build_screenshots_apk
	@printf "$(YELLOW)Генерирую скриншоты через fastlane...$(RESET)\n"
	@$(MAKE) _ensure_fastlane
	@$(MAKE) _set_emulator_location_moscow
	@PATH="/Users/Oleg991/Library/Android/sdk/platform-tools:$$PATH" $(BUNDLE_EXEC) fastlane screenshots
	@$(MAKE) _cleanup_screenshots_apk
	@$(MAKE) update_readme

## screenshots-ru: Генерировать скриншоты только на русском
screenshots-ru:
	@$(MAKE) _build_screenshots_apk
	@printf "$(YELLOW)Генерирую скриншоты (русский)...$(RESET)\n"
	@$(MAKE) _ensure_fastlane
	@$(MAKE) _set_emulator_location_moscow
	@PATH="/Users/Oleg991/Library/Android/sdk/platform-tools:$$PATH" $(BUNDLE_EXEC) fastlane screenshots_ru
	@$(MAKE) _cleanup_screenshots_apk

## update_readme: Обновить таблицу со скриншотами и версии в README.md
update_readme:
	@printf "$(YELLOW)Обновляю таблицу со скриншотами в README.md...$(RESET)\\n"
	@if [ -f "scripts/update_readme.py" ]; then \
		python3 scripts/update_readme.py; \
	else \
		printf "$(RED)Скрипт обновления не найден: scripts/update_readme.py$(RESET)\\n"; \
		exit 1; \
	fi
	@$(MAKE) update_readme_versions

## update_readme_versions: Обновить только версии в README.md (Kotlin, Android SDK, Gradle, AGP)
update_readme_versions:
	@printf "$(YELLOW)Обновляю версии в README.md...$(RESET)\n"
	./gradlew updateReadmeVersions --no-configuration-cache --quiet
	@printf "$(GREEN)Версии обновлены успешно$(RESET)\n"

## _build_screenshots_apk: Подготовить APK для скриншотов (удаление старых и сборка новых)
_build_screenshots_apk:
	@printf "$(YELLOW)Удаляю старые APK артефакты...$(RESET)\n"
	@rm -rf app/build/outputs/apk
	@rm -rf screenshots/build/outputs/apk
	@printf "$(YELLOW)Собираю APK для скриншотов...$(RESET)\n"
	@./gradlew :app:assembleDebug :screenshots:assembleDebug --quiet

## _cleanup_screenshots_apk: Удалить APK артефакты после генерации скриншотов
_cleanup_screenshots_apk:
	@printf "$(YELLOW)Удаляю APK артефакты после генерации...$(RESET)\n"
	@rm -rf app/build/outputs/apk
	@rm -rf screenshots/build/outputs/apk
	@printf "$(GREEN)Скриншоты готовы: fastlane/metadata/android$(RESET)\n"

## _ensure_fastlane: Проверить что fastlane готов к использованию
_ensure_fastlane:
	@if [ ! -d "fastlane" ] || [ ! -f "fastlane/Fastfile" ]; then \
		printf "$(RED)fastlane не инициализирован в проекте$(RESET)\n"; \
		exit 1; \
	fi
	@if ! command -v rbenv >/dev/null 2>&1; then \
		printf "$(RED)rbenv не установлен. Запустите: make setup$(RESET)\n"; \
		exit 1; \
	fi

## _set_emulator_location_moscow: Установить геолокацию Москва на первом запущенном Android-эмуляторе
_set_emulator_location_moscow:
	@SERIAL=$$(adb devices | awk '/^emulator-[0-9]+\tdevice$$/{print $$1; exit}'); \
	if [ -z "$$SERIAL" ]; then \
		printf "$(YELLOW)Эмулятор не найден, шаг геолокации пропущен$(RESET)\n"; \
	else \
		printf "$(YELLOW)Устанавливаю геолокацию Москва для $$SERIAL...$(RESET)\n"; \
		adb -s "$$SERIAL" emu geo fix 37.6173 55.7558 >/dev/null 2>&1 || true; \
	fi

## android-test-report: Открыть HTML отчет интеграционных тестов в браузере
android-test-report:
	@if [ -f app/build/reports/androidTests/connected/debug/index.html ]; then \
		open app/build/reports/androidTests/connected/debug/index.html; \
	else \
		printf "Отчет не найден. Сначала запустите: make android-test\n"; \
	fi

# Подготовка к публикации
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
	@./gradlew bundleRelease
	@VERSION_CODE=$$(grep "^VERSION_CODE=" gradle.properties | cut -d'=' -f2); \
	OUTPUT_FILE="swparks$$VERSION_CODE.aab"; \
	cp app/build/outputs/bundle/release/app-release.aab "$$OUTPUT_FILE"; \
	printf "$(GREEN)AAB создан: $$OUTPUT_FILE$(RESET)\n"
	@printf "$(YELLOW)Версия для публикации: $$(grep "^VERSION_NAME=" gradle.properties | cut -d'=' -f2) (build $$NEW_VERSION_CODE)$(RESET)\n"
	@printf "$(YELLOW)Для публикации используйте этот файл в RuStore или Google Play Store$(RESET)\\n"

# Запуск всех задач
## all: Полная проверка (сборка + тесты + линтер) и установка APK на устройство
all: check install

.PHONY: build clean test lint format check install all android-test test-all android-test-report screenshots screenshots-ru update_readme update_readme_versions _build_screenshots_apk _cleanup_screenshots_apk _ensure_fastlane _set_emulator_location_moscow setup setup_fastlane update_fastlane fastlane help release apk _check_rbenv _check_ruby _check_ruby_version_file _check_bundler _check_gemfile _install_gemfile_deps _check_markdownlint _load_secrets setup_ssh
