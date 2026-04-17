package es.mixmat.listener.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MusicUrlExtractorTest {

    @Test
    fun `extracts Spotify track URL`() {
        assertEquals(
            "https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC",
            MusicUrlExtractor.extract("https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC"),
        )
    }

    @Test
    fun `extracts Spotify track URL with query params`() {
        assertEquals(
            "https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC?si=abc123",
            MusicUrlExtractor.extract("https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC?si=abc123"),
        )
    }

    @Test
    fun `extracts Spotify intl track URL`() {
        assertEquals(
            "https://open.spotify.com/intl-nz/track/4uLU6hMCjMI75M1A2tKUQC",
            MusicUrlExtractor.extract("https://open.spotify.com/intl-nz/track/4uLU6hMCjMI75M1A2tKUQC"),
        )
    }

    @Test
    fun `extracts Spotify short link`() {
        assertEquals(
            "https://spotify.link/abc123",
            MusicUrlExtractor.extract("Check this out https://spotify.link/abc123"),
        )
    }

    @Test
    fun `extracts Tidal browse track URL`() {
        assertEquals(
            "https://tidal.com/browse/track/12345678",
            MusicUrlExtractor.extract("https://tidal.com/browse/track/12345678"),
        )
    }

    @Test
    fun `extracts Tidal direct track URL`() {
        assertEquals(
            "https://tidal.com/track/12345678",
            MusicUrlExtractor.extract("https://tidal.com/track/12345678"),
        )
    }

    @Test
    fun `extracts listen tidal URL`() {
        assertEquals(
            "https://listen.tidal.com/track/12345678",
            MusicUrlExtractor.extract("https://listen.tidal.com/track/12345678"),
        )
    }

    @Test
    fun `extracts Apple Music URL`() {
        assertEquals(
            "https://music.apple.com/us/album/midnight-city/1440818584?i=1440818734",
            MusicUrlExtractor.extract("https://music.apple.com/us/album/midnight-city/1440818584?i=1440818734"),
        )
    }

    @Test
    fun `extracts URL from surrounding text`() {
        val text = "Check out this song! https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC Love it"
        assertEquals(
            "https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC",
            MusicUrlExtractor.extract(text),
        )
    }

    @Test
    fun `returns first music URL when multiple present`() {
        val text = "https://open.spotify.com/track/first https://tidal.com/track/second"
        assertEquals(
            "https://open.spotify.com/track/first",
            MusicUrlExtractor.extract(text),
        )
    }

    @Test
    fun `returns null for plain text`() {
        assertNull(MusicUrlExtractor.extract("just some random text"))
    }

    @Test
    fun `returns null for unsupported music service`() {
        assertNull(MusicUrlExtractor.extract("https://www.youtube.com/watch?v=abc123"))
    }

    @Test
    fun `returns null for Spotify playlist URL`() {
        assertNull(MusicUrlExtractor.extract("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M"))
    }

    @Test
    fun `returns null for empty string`() {
        assertNull(MusicUrlExtractor.extract(""))
    }
}
