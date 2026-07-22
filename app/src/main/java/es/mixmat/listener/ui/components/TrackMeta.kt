package es.mixmat.listener.ui.components

import kotlin.math.roundToInt

/**
 * Builds the BPM/key meta label shown under a track, e.g. "105 BPM · F♯m".
 *
 * This must render identically to the web and iOS clients — keep the format,
 * key map, and separator in sync across all three. Returns null when there is
 * nothing to show (e.g. a freshly recognised track before async enrichment).
 */
fun trackMetaLabel(bpm: Double?, musicalKey: String?, keyScale: String?): String? {
    val parts = mutableListOf<String>()
    if (bpm != null) parts += "${bpm.roundToInt()} BPM"
    if (musicalKey != null) {
        val key = keyDisplay[musicalKey] ?: musicalKey
        val suffix = if (keyScale == "MINOR") "m" else ""
        parts += "$key$suffix"
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

/** Raw server key form -> display glyph. Fallback to the raw string if unmapped. */
private val keyDisplay = mapOf(
    "C" to "C",
    "CSharp" to "C♯",
    "D" to "D",
    "Eb" to "E♭",
    "E" to "E",
    "F" to "F",
    "FSharp" to "F♯",
    "G" to "G",
    "Ab" to "A♭",
    "A" to "A",
    "Bb" to "B♭",
    "B" to "B",
)
