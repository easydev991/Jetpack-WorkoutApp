package com.swparks.data.repository

import android.content.Context
import com.swparks.data.model.City
import com.swparks.data.model.Country
import com.swparks.domain.exception.NetworkException
import com.swparks.domain.exception.ServerException
import com.swparks.domain.repository.CountriesRepository
import com.swparks.network.SWApi
import com.swparks.util.Logger
import com.swparks.utils.ReadJSONFromAssets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/**
 * Реализация репозитория для работы со справочником стран и городов
 *
 * Загружает данные из локального JSON-файла при первом обращении и кэширует их в памяти.
 * Поддерживает обновление справочника с сервера для будущих версий.
 *
 * @param context Контекст приложения для доступа к assets
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
    }

    /** JSON-десериализатор */
    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
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

    /** Загружает справочник стран из локального JSON-файла в assets */
    private fun loadCountriesFromAssets() {
        if (isLoaded) {
            return
        }

        try {
            // Используем существующую функцию для чтения JSON из assets
            val jsonString = ReadJSONFromAssets(context, "countries.json")

            if (jsonString.isEmpty()) {
                logger.e(TAG, "Не удалось прочитать countries.json: пустая строка")
                return
            }

            // Десериализуем JSON в список стран
            val countries: List<Country> = json.decodeFromString(jsonString)

            // Обновляем кэшированные данные
            cachedCountries.value = countries
            isLoaded = true

            // Создаем индексы для быстрого поиска
            countriesByIdMap = countries.associateBy { it.id }
            citiesByIdMap = countries.flatMap { it.cities }.associateBy { it.id }

            logger.i(TAG, "Загружен справочник стран: ${countries.size} стран")
        } catch (e: SerializationException) {
            logger.e(TAG, "Ошибка десериализации JSON: ${e.message}", e)
        } catch (e: Exception) {
            logger.e(TAG, "Ошибка при загрузке справочника стран: ${e.message}", e)
        }
    }

    /**
     * Получить список всех стран в виде Flow
     *
     * @return Flow со списком всех стран
     */
    override fun getCountriesFlow(): Flow<List<Country>> {
        return cachedCountries.map { it }
    }

    /**
     * Получить страну по идентификатору
     *
     * @param countryId идентификатор страны
     * @return страна или null, если не найдена
     */
    override suspend fun getCountryById(countryId: String): Country? {
        // Загружаем данные, если они еще не загружены
        loadCountriesFromAssets()
        return countriesByIdMap[countryId]
    }

    /**
     * Получить город по идентификатору
     *
     * @param cityId идентификатор города
     * @return город или null, если не найден
     */
    override suspend fun getCityById(cityId: String): City? {
        // Загружаем данные, если они еще не загружены
        loadCountriesFromAssets()
        return citiesByIdMap[cityId]
    }

    /**
     * Получить список городов для конкретной страны
     *
     * @param countryId идентификатор страны
     * @return список городов страны
     */
    override suspend fun getCitiesByCountry(countryId: String): List<City> {
        // Загружаем данные, если они еще не загружены
        loadCountriesFromAssets()
        return getCountryById(countryId)?.cities ?: emptyList()
    }

    /**
     * Обновить справочник стран и городов с сервера
     *
     * Метод для будущего использования. Позволяет обновить справочник данных с сервера workout.su.
     *
     * @return Result<Unit> с результатом операции
     */
    override suspend fun updateCountriesFromServer(): Result<Unit> {
        return try {
            logger.i(TAG, "Загрузка справочника стран с сервера")
            val countries = swApi.getCountries()

            // Обновляем кэшированные данные
            cachedCountries.value = countries
            isLoaded = true

            // Обновляем индексы для быстрого поиска
            countriesByIdMap = countries.associateBy { it.id }
            citiesByIdMap = countries.flatMap { it.cities }.associateBy { it.id }

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
                    message =
                        "Не удалось обновить справочник. Проверьте интернет-соединение",
                    cause = e
                )
            )
        }
    }
}
