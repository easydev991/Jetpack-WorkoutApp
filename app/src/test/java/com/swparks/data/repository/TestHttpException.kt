package com.swparks.data.repository

import io.mockk.every
import io.mockk.mockk
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response

/**
 * Собирает HttpException с настоящим okhttp-Response и телом [body].
 *
 * [retrofit2.Response.error] кладёт в raw() заглушку NoContentResponseBody,
 * на которой peekBody бросает IllegalStateException — в проде тело
 * реальное, поэтому для тестов классификации тела нужен настоящий Response
 */
internal fun testHttpException(
    code: Int,
    body: ResponseBody
): HttpException = HttpException(testErrorResponse(code, body))

/**
 * Собирает retrofit2.Response с настоящим okhttp-Response и телом [body]
 *
 * [retrofit2.Response.error] кладёт в raw() заглушку без тела, на которой
 * peekBody бросает IllegalStateException — в проде тело реальное, поэтому
 * для тестов классификации тела нужен настоящий Response
 */
internal fun testErrorResponse(
    code: Int,
    body: ResponseBody
): Response<Unit> {
    val raw =
        okhttp3.Response
            .Builder()
            .request(Request.Builder().url("http://localhost/").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("Error")
            .body(body)
            .build()
    val retrofitResponse =
        mockk<Response<Unit>>().apply {
            every { isSuccessful } returns false
            every { code() } returns code
            every { raw() } returns raw
            every { errorBody() } returns body
            every { message() } returns "Error"
        }
    return retrofitResponse
}
