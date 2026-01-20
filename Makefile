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
	./gradlew connectedDebugAndroidTest --console=plain
	@echo ""
	@echo "Интеграционные тесты выполнены"
	@if [ -f app/build/reports/androidTests/connected/debug/index.html ]; then \
		echo "HTML отчет: app/build/reports/androidTests/connected/debug/index.html"; \
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

## apk: Создать подписанный APK для релизной конфигурации (без повышения версии). Файл: swparks{VERSION_CODE}.apk
apk:
	@printf "$(YELLOW)Проверка секретов для подписи...$(RESET)\n"
	@if [ ! -d ".secrets" ]; then \
		printf "$(YELLOW)Загрузка секретов из репозитория android-secrets...$(RESET)\n"; \
		if [ -d "../android-secrets/jetpack-workoutapp" ]; then \
			mkdir -p ".secrets"; \
			cp -r ../android-secrets/jetpack-workoutapp/* .secrets/; \
			sed -i.tmp 's|^KEYSTORE_FILE=.*|KEYSTORE_FILE=.secrets/keystore/workoutapp-release.keystore|' .secrets/secrets.properties && rm -f .secrets/secrets.properties.tmp; \
			printf "$(GREEN)Секреты загружены успешно$(RESET)\n"; \
		else \
			printf "$(RED)Ошибка: репозиторий android-secrets не найден в ../android-secrets/jetpack-workoutapp$(RESET)\n"; \
			printf "$(YELLOW)Проверьте, что репозиторий android-secrets склонирован в нужное место$(RESET)\n"; \
			exit 1; \
		fi \
	fi
	@printf "$(YELLOW)Создаю релизный APK...$(RESET)\n"
	@./gradlew assembleRelease
	@VERSION_CODE=$$(grep "^VERSION_CODE=" gradle.properties | cut -d'=' -f2); \
	VERSION_NAME=$$(grep "^VERSION_NAME=" gradle.properties | cut -d'=' -f2); \
	OUTPUT_FILE="workoutapp$$VERSION_CODE.apk"; \
	cp app/build/outputs/apk/release/app-release.apk "$$OUTPUT_FILE"; \
	printf "$(GREEN)APK создан: $$OUTPUT_FILE$(RESET)\n"; \
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
## screenshots: Генерировать скриншоты для всех локалей через fastlane
screenshots:
	@$(MAKE) _build_screenshots_apk
	@printf "$(YELLOW)Генерирую скриншоты через fastlane...$(RESET)\n"
	@$(MAKE) _ensure_fastlane
	@PATH="/Users/Oleg991/Library/Android/sdk/platform-tools:$$PATH" $(BUNDLE_EXEC) fastlane screenshots
	@$(MAKE) _cleanup_screenshots_apk

## screenshots-ru: Генерировать скриншоты только на русском
screenshots-ru:
	@$(MAKE) _build_screenshots_apk
	@printf "$(YELLOW)Генерирую скриншоты (русский)...$(RESET)\n"
	@$(MAKE) _ensure_fastlane
	@PATH="/Users/Oleg991/Library/Android/sdk/platform-tools:$$PATH" $(BUNDLE_EXEC) fastlane screenshots_ru
	@$(MAKE) _cleanup_screenshots_apk

## screenshots-en: Генерировать скриншоты только на английском
screenshots-en:
	@$(MAKE) _build_screenshots_apk
	@printf "$(YELLOW)Генерирую скриншоты (английский)...$(RESET)\n"
	@$(MAKE) _ensure_fastlane
	@PATH="/Users/Oleg991/Library/Android/sdk/platform-tools:$$PATH" $(BUNDLE_EXEC) fastlane screenshots_en
	@$(MAKE) _cleanup_screenshots_apk


## update_readme: Обновить таблицу со скриншотами в README.md
update_readme:
	@printf "$(YELLOW)Обновляю таблицу со скриншотами в README.md...$(RESET)\\n"
	@if [ -f "scripts/update_readme.py" ]; then \
		python3 scripts/update_readme.py; \
	else \
		printf "$(RED)Скрипт обновления не найден: scripts/update_readme.py$(RESET)\\n"; \
		exit 1; \
	fi

## _build_screenshots_apk: Подготовить APK для скриншотов (удаление старых и сборка новых)
_build_screenshots_apk:
	@printf "$(YELLOW)Удаляю старые APK артефакты...$(RESET)\n"
	@rm -rf app/build/outputs/apk
	@printf "$(YELLOW)Собираю APK для скриншотов...$(RESET)\n"
	@./gradlew assembleDebug assembleDebugAndroidTest --quiet

## _cleanup_screenshots_apk: Удалить APK артефакты после генерации скриншотов
_cleanup_screenshots_apk:
	@printf "$(YELLOW)Удаляю APK артефакты после генерации...$(RESET)\n"
	@rm -rf app/build/outputs/apk
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
		printf "$(YELLOW)Загрузка секретов из репозитория android-secrets...$(RESET)\n"; \
		if [ -d "../android-secrets/swparks" ]; then \
			mkdir -p ".secrets"; \
			cp -r ../android-secrets/swparks/* .secrets/; \
			sed -i.tmp 's|^KEYSTORE_FILE=.*|KEYSTORE_FILE=.secrets/keystore/swparks-release.keystore|' .secrets/secrets.properties && rm -f .secrets/secrets.properties.tmp; \
			printf "$(GREEN)Секреты загружены успешно$(RESET)\n"; \
		else \
			printf "$(RED)Ошибка: репозиторий android-secrets не найден в ../android-secrets/swparks$(RESET)\n"; \
			printf "$(YELLOW)Проверьте, что репозиторий android-secrets склонирован в нужное место$(RESET)\n"; \
			exit 1; \
		fi \
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

.PHONY: build clean test lint format check install all android-test test-all android-test-report screenshots screenshots-ru screenshots-en update_readme _build_screenshots_apk _cleanup_screenshots_apk _ensure_fastlane setup setup_fastlane update_fastlane fastlane help release apk _check_rbenv _check_ruby _check_ruby_version_file _check_bundler _check_gemfile _install_gemfile_deps _check_markdownlint
