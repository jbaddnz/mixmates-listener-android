package es.mixmat.listener.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * This label has to render identically on web, iOS and Android, so the format is
 * a contract rather than a preference. These assert the exact strings on purpose:
 * a copy change should fail here loudly rather than drift between clients.
 */
class TrackMetaTest {

    @Test
    fun `renders bpm and key with the separator`() {
        assertEquals("105 BPM · F♯m", trackMetaLabel(105.0, "FSharp", "MINOR"))
    }

    @Test
    fun `major keys carry no suffix`() {
        assertEquals("128 BPM · C", trackMetaLabel(128.0, "C", "MAJOR"))
    }

    @Test
    fun `bpm rounds to the nearest whole number`() {
        assertEquals("120 BPM", trackMetaLabel(119.6, null, null))
        assertEquals("119 BPM", trackMetaLabel(119.4, null, null))
    }

    @Test
    fun `an unmapped key falls back to the raw server string`() {
        assertEquals("H", trackMetaLabel(null, "H", "MAJOR"))
    }

    @Test
    fun `flats and sharps use the musical glyphs`() {
        assertEquals("E♭m", trackMetaLabel(null, "Eb", "MINOR"))
        assertEquals("C♯", trackMetaLabel(null, "CSharp", "MAJOR"))
    }

    @Test
    fun `either field alone still renders`() {
        assertEquals("90 BPM", trackMetaLabel(90.0, null, null))
        assertEquals("Am", trackMetaLabel(null, "A", "MINOR"))
    }

    /** "0 BPM" reads worse than no tag, and a zero would be a fault not a measurement. */
    @Test
    fun `zero bpm is treated as absent`() {
        assertNull(trackMetaLabel(0.0, null, null))
        assertEquals("Am", trackMetaLabel(0.0, "A", "MINOR"))
    }

    @Test
    fun `nothing to show returns null rather than an empty string`() {
        assertNull(trackMetaLabel(null, null, null))
    }

    /** A missing scale is not minor — only an explicit MINOR earns the suffix. */
    @Test
    fun `a null scale renders the key bare`() {
        assertEquals("G", trackMetaLabel(null, "G", null))
    }
}
