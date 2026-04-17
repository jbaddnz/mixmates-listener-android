package es.mixmat.listener.util

object MusicUrlExtractor {

    private val MUSIC_URL_PATTERN = Regex(
        """https?://(?:""" +
            """open\.spotify\.com/(?:intl-[a-z]+/)?track/\S+""" +
            """|spotify\.link/\S+""" +
            """|(?:listen\.)?tidal\.com/(?:browse/)?track/\S+""" +
            """|music\.apple\.com/\S+/album/\S+""" +
            """)""",
        RegexOption.IGNORE_CASE,
    )

    fun extract(text: String): String? =
        MUSIC_URL_PATTERN.find(text)?.value
}
