package com.swparks.data.repository

import com.swparks.data.APIError
import com.swparks.data.ErrorResponse
import com.swparks.domain.exception.NetworkException
import com.swparks.domain.exception.ServerException
import com.swparks.util.CrashReporter
import com.swparks.util.Logger
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@Suppress("TooGenericExceptionCaught")
open class BaseRepository(
    protected val logger: Logger,
    protected val crashReporter: CrashReporter
) {
    protected val json: Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
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
        logger.e(tag, "Ошибка сервера $statusCode при $operation")
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
        } catch (se: kotlinx.serialization.SerializationException) {
            logger.e(tag, "Не удалось десериализовать ответ об ошибке: ${se.message}")
            crashReporter.logException(se, "Ошибка десериализации ответа сервера")
            ServerException(message = "Ошибка обработки ответа сервера", cause = se)
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
