package es.mixmat.listener.data.api

import es.mixmat.listener.data.api.dto.ApiErrorResponse
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * A server error carrying its own error code, not just an HTTP status.
 *
 * Needed because one status maps to several codes. A 403 from
 * `POST /history/{id}/share` is `group_locked`, `not_found` (not a member of
 * the group) or `auth_listen_disabled`, and each wants different copy —
 * keying on the status alone would tell someone who has left a group that it
 * has stopped accepting tracks.
 */
class ApiException(
    val code: String,
    val serverMessage: String,
    val httpStatus: Int,
) : Exception("HTTP $httpStatus ($code): $serverMessage")

/** Lenient on purpose: an error body we can't fully parse must not mask the error. */
private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Reads the `{ error: { code, message } }` envelope out of a failed response.
 *
 * Returns null when there is no usable code, so callers can fall back to the
 * original [HttpException] rather than inventing one. Converted at the point of
 * use rather than in an interceptor: `ShareViewModel` keys its copy on
 * `HttpException.code() == 400`, and throwing a different type for every failed
 * request would break that silently.
 */
fun HttpException.asApiException(): ApiException? {
    val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
    if (body.isNullOrBlank()) return null

    val error = runCatching {
        errorJson.decodeFromString<ApiErrorResponse>(body).error
    }.getOrNull() ?: return null

    if (error.code.isBlank()) return null

    return ApiException(
        code = error.code,
        serverMessage = error.message,
        httpStatus = code(),
    )
}
