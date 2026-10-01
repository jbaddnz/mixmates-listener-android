package es.mixmat.listener.data.api

import es.mixmat.listener.data.api.dto.ApiResponse
import es.mixmat.listener.data.api.dto.GroupListData
import es.mixmat.listener.di.NetworkModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decoded with the app's own Json, because `coerceInputValues` is what would
 * quietly turn a missing or null `can_create` into whatever the default says.
 * The default has to be false.
 */
class GroupListDecodingTest {

    private val json = NetworkModule.provideJson()

    private fun decode(body: String) =
        json.decodeFromString<ApiResponse<GroupListData>>(body).data.toDomain()

    @Test
    fun `can_create true is read as true`() {
        val list = decode("""{"data":{"can_create":true,"items":[]}}""")
        assertTrue(list.canCreate)
    }

    @Test
    fun `can_create false is read as false`() {
        val list = decode("""{"data":{"can_create":false,"items":[]}}""")
        assertFalse(list.canCreate)
    }

    @Test
    fun `a missing can_create means no`() {
        val list = decode("""{"data":{"items":[]}}""")
        assertFalse(list.canCreate)
    }

    @Test
    fun `a null can_create means no`() {
        val list = decode("""{"data":{"can_create":null,"items":[]}}""")
        assertFalse(list.canCreate)
    }

    /** An older server must not break today's picker just because the new fields are absent. */
    @Test
    fun `an older response with neither new field still decodes`() {
        val list = decode(
            """{"data":{"items":[{"id":"g1","name":"Friends","description":null}]}}""",
        )
        assertEquals("g1", list.groups.single().id)
        assertNull(list.groups.single().inviteUrl)
        assertFalse(list.canCreate)
    }

    @Test
    fun `invite_url is read when present and null on the demo group`() {
        val list = decode(
            """
            {"data":{"can_create":true,"items":[
              {"id":"g1","name":"Friends","description":null,
               "invite_url":"https://example.test/invite/AbCd"},
              {"id":"demo","name":"Demo","description":"x","invite_url":null}
            ]}}
            """.trimIndent(),
        )
        assertEquals("https://example.test/invite/AbCd", list.groups[0].inviteUrl)
        assertNull(list.groups[1].inviteUrl)
    }
}
