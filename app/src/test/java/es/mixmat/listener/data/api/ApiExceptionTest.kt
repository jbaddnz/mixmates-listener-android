package es.mixmat.listener.data.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ApiExceptionTest {

    private fun httpException(status: Int, body: String) = HttpException(
        Response.error<Any>(status, body.toResponseBody("application/json".toMediaType())),
    )

    @Test
    fun `parses the error code out of a 403`() {
        val e = httpException(
            403,
            """{"error":{"code":"group_locked","message":"Group is mastering"}}""",
        ).asApiException()

        assertNotNull(e)
        assertEquals("group_locked", e!!.code)
        assertEquals("Group is mastering", e.serverMessage)
        assertEquals(403, e.httpStatus)
    }

    /**
     * The three codes 403 carries on the share endpoint must stay distinguishable.
     * Keying copy on the status would tell someone who left a group that it has
     * stopped accepting tracks.
     */
    @Test
    fun `distinguishes the three 403 codes`() {
        val codes = listOf("group_locked", "not_found", "auth_listen_disabled")

        val parsed = codes.map { code ->
            httpException(403, """{"error":{"code":"$code","message":"x"}}""")
                .asApiException()?.code
        }

        assertEquals(codes, parsed)
    }

    @Test
    fun `ignores unknown fields in the envelope`() {
        val e = httpException(
            403,
            """{"error":{"code":"not_found","message":"Not a member of group g1","hint":"x"},
                "meta":{"request_id":"r1","unexpected":1}}""",
        ).asApiException()

        assertEquals("not_found", e?.code)
    }

    @Test
    fun `returns null for a body that is not the error envelope`() {
        assertNull(httpException(500, "<html>gateway</html>").asApiException())
    }

    @Test
    fun `returns null when there is no error object`() {
        assertNull(httpException(400, """{"data":null}""").asApiException())
    }

    @Test
    fun `returns null when the code is blank, so the caller keeps the original`() {
        assertNull(httpException(400, """{"error":{"code":"","message":"x"}}""").asApiException())
    }

    @Test
    fun `returns null for an empty body`() {
        assertNull(httpException(503, "").asApiException())
    }
}
