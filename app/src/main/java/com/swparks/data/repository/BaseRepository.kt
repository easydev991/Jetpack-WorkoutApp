package com.swparks.data.repository

import com.swparks.data.APIError
import com.swparks.data.ErrorResponse
import com.swparks.domain.exception.NetworkException
import com.swparks.domain.exception.ServerException
import com.swparks.util.CrashReporter
import com.swparks.util.HTTP_NOT_FOUND
import com.swparks.util.HTTP_SERVICE_UNAVAILABLE
import com.swparks.util.Logger
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** Размер peek-копии тела ошибки для классификации 404 (запас на длинные JSON-ошибки) */
internal const val ERROR_BODY_PEEK_BYTES = 65536L

/** Единая конфигурация разбора тел ошибок API — для BaseRepository и репозиториев вне его иерархии */
internal val json: Json =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

/**
 * Определяет, является ли 404-ответ реальным API-404, по содержимому тела.
 *
 * Классификация выполняется по содержимому тела, Content-Type не проверяется.
 * Реальный API (Yii2 ErrorAction / дефолтный error handler) всегда кладёт в 404
 * одно из полей `errors`/`name` — JSON без них (в том числе с чужим `message`,
 * как у прокси Spring Boot/API Gateway) считается инфраструктурным ответом,
 * чтобы не удалять локальный кэш.
 *
 * @param body Peek-копия тела ответа (okhttp3.Response.peekBody) — функцией
 *   потребляется, исходное errorBody не затрагивается
 * @param json Конфигурация Json для парсинга ErrorResponse
 * @return `null` — не API-404 (инфраструктурный 404: HTML/XML/пустое или битое
 *   тело, валидный JSON без полей API); `ErrorResponse` — API-404, сообщение
 *   сервера в `realMessage` (может быть `null`, если заполнено только `name`)
 */
internal fun isApiNotFoundBody(
    body: ResponseBody?,
    json: Json
): ErrorResponse? {
    val content = body?.string().orEmpty()
    return runCatching { json.decodeFromString<ErrorResponse>(content) }
        .getOrNull()
        ?.takeIf { it.errors.isNotEmpty() || it.name != null }
}

/**
 * Инфраструктурный 404 (HTML/чужой JSON от прокси): единый ответ всех репозиториев —
 * «Сервер недоступен» с кодом 503, локальный кэш не трогается.
 */
internal fun infraNotFound(cause: Throwable? = null): ServerException =
    ServerException(
        message = "Сервер недоступен",
        code = HTTP_SERVICE_UNAVAILABLE,
        cause = cause
    )

@Suppress("TooGenericExceptionCaught")
open class BaseRepository(
    protected val logger: Logger,
    protected val crashReporter: CrashReporter
) {
    /**
     * Пробует классифицировать HttpException как реальный API-404.
     *
     * Для кода 404 берёт peek-копию тела (исходное errorBody не потребляется)
     * и классифицирует её через [isApiNotFoundBody]; для остальных кодов
     * возвращает `null`.
     */
    protected fun HttpException.tryApiNotFoundBody(): ErrorResponse? =
        if (code() == HTTP_NOT_FOUND) {
            isApiNotFoundBody(response()?.raw()?.peekBody(ERROR_BODY_PEEK_BYTES), json)
        } else {
            null
        }

    protected fun handleIOException(
        e: IOException,
        tag: String,
        operation: String
    ): NetworkException {
        logger.e(tag, "Ошибка сети при $operation: ${e.message}")
        crashReporter.logException(e, "Ошибка сети при $operation")
        return NetworkException(
            message = "Не удалось выполнить операцию. Проверьте интернет-соединение",
            cause = e
        )
    }

    protected fun handleHttpException(
        e: HttpException,
        tag: String,
        operation: String
    ): Exception {
        val statusCode = e.code()
        // Классификация до чтения body: peek-копия валидна только до потребления errorBody
        val errResp = e.tryApiNotFoundBody()
        logger.e(tag, "Ошибка сервера $statusCode при $operation")

        return when {
            // Настоящий API-404: серверное сообщение или fallback, без crash-отчёта;
            // code = 404 — чтобы VM могли отличить его от инфра-404 (503) и прочих ошибок
            errResp != null ->
                ServerException(
                    message = errResp.realMessage ?: "Ресурс не найден",
                    cause = e,
                    code = HTTP_NOT_FOUND
                )
            // Инфра-404 (HTML/чужой JSON от прокси): мапим в 503; в crash-отчёты
            // не идёт как исключение — только breadcrumb-лог
            statusCode == HTTP_NOT_FOUND -> {
                logger.e(tag, "[infra_html_404] Инфраструктурный 404 при $operation")
                crashReporter.log("[infra_html_404] Инфраструктурный 404 при $operation")
                infraNotFound(cause = e)
            }
            else -> parseHttpExceptionBody(e, statusCode, tag, operation)
        }
    }

    /** Легаси-путь для не-404 ответов: crash-отчёт + парсинг тела ошибки */
    private fun parseHttpExceptionBody(
        e: HttpException,
        statusCode: Int,
        tag: String,
        operation: String
    ): Exception {
        crashReporter.logException(e, "Ошибка сервера $statusCode при $operation")

        return try {
            val responseBody = e.response()?.errorBody()?.string()
            if (responseBody != null) {
                val errorResponse = json.decodeFromString<ErrorResponse>(responseBody)
                val errorMessage = errorResponse.realMessage ?: "Ошибка сервера: $statusCode"
                ServerException(message = errorMessage, cause = e)
            } else {
                val errorMessage = APIError.fromStatusCode(statusCode).errorMessage
                ServerException(message = errorMessage, cause = e)
            }
        } catch (ex: Exception) {
            logger.e(tag, "Не удалось десериализовать ответ об ошибке: ${ex.message}")
            crashReporter.logException(ex, "Ошибка десериализации ответа сервера")
            ServerException(message = "Ошибка обработки ответа сервера", cause = ex)
        }
    }

    protected fun handleResponseError(
        response: Response<*>,
        tag: String,
        operation: String
    ): ServerException {
        val statusCode = response.code()
        logger.e(tag, "Ошибка сервера $statusCode при $operation")

        return try {
            val errorBody = response.errorBody()?.string()
            if (errorBody != null) {
                val errorResponse = json.decodeFromString<ErrorResponse>(errorBody)
                val errorMessage = errorResponse.realMessage ?: "Ошибка сервера: $statusCode"
                ServerException(message = errorMessage)
            } else {
                val errorMessage = APIError.fromStatusCode(statusCode).errorMessage
                ServerException(message = errorMessage)
            }
        } catch (e: Exception) {
            logger.e(tag, "Не удалось десериализовать ответ об ошибке: ${e.message}")
            crashReporter.logException(e, "Ошибка десериализации ответа об ошибке сервера")
            ServerException(message = "Ошибка сервера: $statusCode")
        }
    }

    protected fun parseErrorResponse(
        response: Response<*>,
        tag: String,
        context: String
    ): String {
        val errorBody = response.errorBody()?.string()
        return if (errorBody != null) {
            try {
                val errorResponse = json.decodeFromString<ErrorResponse>(errorBody)
                errorResponse.realMessage ?: "Ошибка сервера: ${response.code()}"
            } catch (e: Exception) {
                logger.w(tag, "Не удалось распарсить ошибку $context: ${e.message}")
                "Ошибка сервера: ${response.code()}"
            }
        } else {
            "Ошибка сервера: ${response.code()}"
        }
    }
}
