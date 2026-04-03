package com.swparks.data.repository

import android.content.Context
import com.swparks.data.model.City
import com.swparks.data.model.Country
import com.swparks.domain.exception.NetworkException
import com.swparks.domain.exception.ServerException
import com.swparks.domain.repository.CountriesRepository
import com.swparks.network.SWApi
import com.swparks.util.Logger
import com.swparks.util.readJSONFromAssets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.File
import java.io.IOException

/**
 * Реализация репозитория для работы со справочником стран и городов
 *
 * Загружает данные из локального JSON-файла в app-private storage при первом обращении.
 * При отсутствии локального файла копирует seed из assets.
 * Поддерживает обновление справочника с сервера с атомарной записью в файл.
 *
 * @param context Контекст приложения для доступа к assets и файловой системе
 * @param swApi API клиент для запросов к серверу
 * @param logger Логгер для записи сообщений
 */
class CountriesRepositoryImpl(
    private val context: Context,
    private val swApi: SWApi,
    private val logger: Logger
) : CountriesRepository {
    companion object {
        private const val TAG = "CountriesRepository"
        private const val LOCAL_FILENAME = "countries.json"
    }

    /** JSON-сериализатор */
    private val json =
        Json {
            isLenient = true
            ignoreUnknownKeys = true
            prettyPrint = false
        }

    /** Кэшированные данные стран в памяти */
    private val cachedCountries: MutableStateFlow<List<Country>> = MutableStateFlow(emptyList())

    /** Флаг, указывающий, что данные загружены */
    @Volatile
    private var isLoaded = false

    /** Словарь для быстрого поиска городов по ID (оптимизация) */
    @Volatile
    private var citiesByIdMap: Map<String, City> = emptyMap()

    /** Словарь для быстрого поиска стран по ID (оптимизация) */
    @Volatile
    private var countriesByIdMap: Map<String, Country> = emptyMap()

    /** Словарь для быстрого поиска страны по ID города (оптимизация) */
    @Volatile
    private var countryByCityIdMap: Map<String, Country> = emptyMap()

    /**
     * Путь к локальному файлу с справочником стран
     */
    private val localFile: File
        get() = File(context.filesDir, LOCAL_FILENAME)

    /**
     * Проверяет, существует ли локальный файл с справочником
     */
    private fun localFileExists(): Boolean = localFile.exists()

    /**
     * Копирует seed файл из assets в app-private storage
     */
    private fun copySeedFromAssets() {
        if (localFileExists()) {
            logger.i(
                TAG,
                "Локальный файл countries.json уже существует, пропускаем копирование seed"
            )
            return
        }

        logger.i(TAG, "Копирование seed countries.json из assets в app-private storage")
        try {
            val seedJson = readJSONFromAssets(context, "countries.json")
            if (seedJson.isEmpty()) {
                logger.e(TAG, "Не удалось прочитать seed countries.json из assets")
                return
            }
            localFile.writeText(seedJson)
            logger.i(TAG, "Seed countries.json успешно скопирован")
        } catch (e: IOException) {
            logger.e(TAG, "Ошибка при копировании seed countries.json: ${e.message}", e)
        } catch (e: SecurityException) {
            logger.e(TAG, "Ошибка безопасности при копировании seed: ${e.message}", e)
        }
    }

    /**
     * Читает справочник стран из локального JSON файла
     */
    private fun loadCountriesFromLocalFile() {
        if (!isLoaded) {
            // Сначала копируем seed если локального файла нет
            if (!localFileExists()) {
                copySeedFromAssets()
            }

            val file = localFile
            if (!file.exists()) {
                logger.e(TAG, "Локальный файл countries.json не существует после копирования seed")
            } else {
                try {
                    val jsonString = file.readText()
                    if (jsonString.isEmpty()) {
                        logger.e(TAG, "Прочитан пустой файл countries.json")
                    } else {
                        val countries: List<Country> = json.decodeFromString(jsonString)
                        updateCache(countries)
                        logger.i(
                            TAG,
                            "Загружен справочник стран из локального файла: ${countries.size} стран"
                        )
                    }
                } catch (e: SerializationException) {
                    logger.e(TAG, "Ошибка десериализации countries.json: ${e.message}", e)
                } catch (e: IOException) {
                    logger.e(TAG, "Ошибка чтения countries.json: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Обновляет кэшированные данные и индексы
     */
    private fun updateCache(countries: List<Country>) {
        cachedCountries.value = countries
        isLoaded = true
        countriesByIdMap = countries.associateBy { it.id }
        citiesByIdMap = countries.flatMap { it.cities }.associateBy { it.id }
        countryByCityIdMap =
            countries
                .flatMap { country ->
                    country.cities.map { city -> city.id to country }
                }.toMap()
    }

    /**
     * Атомарно сохраняет справочник стран в локальный файл
     */
    private fun saveCountriesToLocalFile(countries: List<Country>) {
        try {
            val jsonString = json.encodeToString(countries)
            // Атомарная запись: сначала во временный файл, потом rename
            val tempFile = File(context.filesDir, "countries.json.tmp")
            tempFile.writeText(jsonString)
            tempFile.renameTo(localFile)
            logger.d(TAG, "Справочник стран сохранен в локальный файл")
        } catch (e: IOException) {
            logger.e(TAG, "Ошибка записи countries.json: ${e.message}", e)
            throw e
        }
    }

    /**
     * Гарантирует, что данные стран загружены из локального файла.
     *
     * Метод должен вызываться перед использованием getCountriesFlow(),
     * чтобы убедиться что данные загружены в кэш.
     */
    override fun ensureCountriesLoaded() {
        if (!isLoaded) {
            loadCountriesFromLocalFile()
        }
    }

    /**
     * Получить список всех стран в виде Flow
     *
     * @return Flow со списком всех стран
     */
    override fun getCountriesFlow(): Flow<List<Country>> = cachedCountries.map { it }

    /**
     * Получить страну по идентификатору
     *
     * @param countryId идентификатор страны
     * @return страна или null, если не найдена
     */
    override suspend fun getCountryById(countryId: String): Country? {
        if (!isLoaded) {
            loadCountriesFromLocalFile()
        }
        return countriesByIdMap[countryId]
    }

    /**
     * Получить город по идентификатору
     *
     * @param cityId идентификатор города
     * @return город или null, если не найден
     */
    override suspend fun getCityById(cityId: String): City? {
        if (!isLoaded) {
            loadCountriesFromLocalFile()
        }
        return citiesByIdMap[cityId]
    }

    /**
     * Получить список городов для конкретной страны
     *
     * @param countryId идентификатор страны
     * @return список городов страны
     */
    override suspend fun getCitiesByCountry(countryId: String): List<City> {
        if (!isLoaded) {
            loadCountriesFromLocalFile()
        }
        return getCountryById(countryId)?.cities ?: emptyList()
    }

    /**
     * Получить список всех городов из всех стран
     *
     * @return список всех городов
     */
    override suspend fun getAllCities(): List<City> {
        if (!isLoaded) {
            loadCountriesFromLocalFile()
        }
        return citiesByIdMap.values.toList()
    }

    /**
     * Получить страну, к которой принадлежит город
     *
     * @param cityId идентификатор города
     * @return страна или null, если город не найден
     */
    override suspend fun getCountryForCity(cityId: String): Country? {
        if (!isLoaded) {
            loadCountriesFromLocalFile()
        }
        return countryByCityIdMap[cityId]
    }

    /**
     * Обновить справочник стран и городов с сервера
     *
     * Загружает полный справочник с сервера и атомарно сохраняет в локальный файл.
     * После успешного сохранения обновляет кэш в памяти.
     *
     * @return Result<Unit> с результатом операции
     */
    override suspend fun updateCountriesFromServer(): Result<Unit> =
        try {
            logger.i(TAG, "Загрузка справочника стран с сервера")
            val countries = swApi.getCountries()

            // Атомарно сохраняем в локальный файл
            saveCountriesToLocalFile(countries)

            // Обновляем кэш в памяти после успешной записи
            updateCache(countries)

            logger.i(TAG, "Справочник стран успешно обновлен с сервера: ${countries.size} стран")
            Result.success(Unit)
        } catch (e: HttpException) {
            val statusCode = e.code()
            logger.e(TAG, "Ошибка сервера $statusCode при обновлении справочника стран", e)
            Result.failure(
                ServerException(
                    message = "Не удалось обновить справочник. Ошибка сервера: $statusCode",
                    cause = e
                )
            )
        } catch (e: IOException) {
            logger.e(TAG, "Ошибка сети при обновлении справочника стран: ${e.message}", e)
            Result.failure(
                NetworkException(
                    message = "Не удалось обновить справочник. Проверьте интернет-соединение",
                    cause = e
                )
            )
        }
}
