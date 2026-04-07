package es.mixmat.listener.data.api

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class RateLimitException(
    val retryAfterSeconds: Int,
    val limitRemaining: Int?,
) : Exception("Rate limited. Retry after $retryAfterSeconds seconds.")

class RateLimitInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (response.code == 429) {
            val retryAfter = response.header("Retry-After")?.toIntOrNull() ?: 60
            val remaining = response.header("X-RateLimit-Remaining")?.toIntOrNull()
            throw RateLimitException(retryAfter, remaining)
        }

        return response
    }
}
