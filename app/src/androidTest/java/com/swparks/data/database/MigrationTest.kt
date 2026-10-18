package com.swparks.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.testing.TimeoutTest
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Смоук-тест связки schema export → assets androidTest: база, созданная по
 * `2.json`, открывается настоящим [SWDatabase] без ошибок валидации.
 *
 * НЕ ловит «изменил entity, забыв поднять `@Database(version)`»: тот же билд
 * молча перезаписывает `app/schemas/2.json`, и тест сравнивал бы код с его же
 * выводом. Эта защита — `git diff app/schemas` в `.githooks/pre-commit`.
 *
 * Схемы берутся из assets androidTest (`app/schemas` подключён
 * sourceSet'ом в `build.gradle.kts`). Будущие миграции N-1 → N — по
 * рецепту `docs/plan-room-migrations.md` (Правило на будущее).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest : TimeoutTest(180) {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            SWDatabase::class.java
        )

    @Test
    fun createDatabase_version2_whenOpenedByRealDatabase_thenSchemaIsValid() {
        // Given — файловая БД по эталонной схеме из 2.json
        helper.createDatabase(TEST_DB, VERSION).close()

        // When — настоящий SWDatabase открывает её: createDatabase пишет
        // только CREATE TABLE без room_master_table, поэтому Room сверяет
        // схему через TableInfo (fallback), а не по identityHash
        val db =
            Room
                .databaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    SWDatabase::class.java,
                    TEST_DB
                ).build()
        try {
            // Первое открытие БД — Room валидирует схему; расхождение = IllegalStateException
            db.openHelper.writableDatabase
        } finally {
            db.close()
        }
    }

    @Test
    fun openDatabase_newerVersionFile_whenOpenedByRealDatabase_thenDowngradeRejected() {
        // Given — файловая БД v2 по эталону 2.json; user_version поднят до 3,
        // имитируя БД пользователя с более новой схемой (откат сборки назад).
        // Прямой вариант «открыть как v1» требует несуществующий 1.json (v1 в
        // релизы не выходила) — MigrationTestHelper валидирует таргет по JSON
        helper.createDatabase(TEST_DOWNGRADE_DB, VERSION).close()
        val dbFile =
            ApplicationProvider
                .getApplicationContext<Context>()
                .getDatabasePath(TEST_DOWNGRADE_DB)
        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { raw ->
            raw.version = FUTURE_VERSION
        }

        // When/Then — настоящий SWDatabase (version = 2) открывает файл v3:
        // downgrade без миграции и без деструктивного fallback →
        // IllegalStateException (данные не стираются молча)
        val db =
            Room
                .databaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    SWDatabase::class.java,
                    TEST_DOWNGRADE_DB
                ).build()
        try {
            db.openHelper.writableDatabase
            fail(
                "Открытие БД версии $FUTURE_VERSION кодом версии $VERSION " +
                    "должно быть отклонено IllegalStateException"
            )
        } catch (expected: IllegalStateException) {
            // ожидаемое поведение
        } finally {
            db.close()
        }
    }

    private companion object {
        const val TEST_DB: String = "migration-test.db"
        const val TEST_DOWNGRADE_DB: String = "migration-test-downgrade.db"
        const val VERSION: Int = 2
        const val FUTURE_VERSION: Int = 3
    }
}
