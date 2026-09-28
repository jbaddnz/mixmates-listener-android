package es.mixmat.listener.data.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnseenTracksTest {

    @Test
    fun `starts clear`() {
        assertFalse(UnseenTracks().hasUnseen.value)
    }

    @Test
    fun `marking then clearing round-trips`() {
        val unseen = UnseenTracks()

        unseen.markUnseen()
        assertTrue(unseen.hasUnseen.value)

        unseen.clear()
        assertFalse(unseen.hasUnseen.value)
    }

    @Test
    fun `marking twice stays marked`() {
        val unseen = UnseenTracks()

        unseen.markUnseen()
        unseen.markUnseen()

        assertTrue(unseen.hasUnseen.value)
    }

    @Test
    fun `clearing when already clear is harmless`() {
        val unseen = UnseenTracks()

        unseen.clear()

        assertFalse(unseen.hasUnseen.value)
    }
}
