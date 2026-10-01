package es.mixmat.listener.ui.sharesheet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The copy rules for the share sheet, in both languages, held as a test rather
 * than a review checklist. Reads the resource files themselves, so a new string
 * cannot slip past. Scoped to the share sheet: the app carries legitimate
 * website links elsewhere (legal pages, Open in MixMates).
 */
class GroupFlowCopyTest {

    /** Unit tests run with the module directory as the working directory. */
    private fun load(dir: String): Map<String, String> {
        val file = File("src/main/res/$dir/strings_sharesheet.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .associate { it.getAttribute("name") to unescape(it.textContent) }
    }

    /** Android's own escapes, undone so sentences compare as the user sees them. */
    private fun unescape(text: String) = text.replace("\\'", "'").replace("\\\"", "\"")

    private val english = load("values")
    private val spanish = load("values-es")

    private val banned = listOf(
        "mixmat.es", "upgrade", "paid", "plan", "subscription", "pricing", "free tier",
        "suscripción", "de pago", "premium", "precio", "gratis",
    )

    @Test
    fun `both files are found and complete`() {
        assertTrue(english.size >= 35)
        assertEquals(english.keys, spanish.keys)
    }

    @Test
    fun `no string names the website or talks about plans, in either language`() {
        (english + spanish.mapKeys { "es:${it.key}" }).forEach { (name, text) ->
            banned.forEach { word ->
                assertFalse("$name contains \"$word\"", text.contains(word, ignoreCase = true))
            }
        }
    }

    @Test
    fun `no string has an em or en dash, in either language`() {
        (english + spanish.mapKeys { "es:${it.key}" }).forEach { (name, text) ->
            assertFalse("$name has an em dash", '—' in text)
            assertFalse("$name has an en dash", '–' in text)
        }
    }

    @Test
    fun `placeholders survive translation`() {
        english.forEach { (name, text) ->
            assertEquals("$name placeholders", "%1\$s" in text, "%1\$s" in spanish.getValue(name))
        }
    }

    /** Agreed word for word with iOS. */
    @Test
    fun `the agreed English sentences are exact`() {
        val agreed = mapOf(
            "sharesheet_name_taken" to "That name's taken. Try adding something of your own to it.",
            "sharesheet_already_has_group" to "This account already has a group.",
            "sharesheet_choose_a_name" to "Choose a name your friends will see.",
            "sharesheet_private_relay" to
                "That's an Apple private address. Choose a name your friends will see.",
            "sharesheet_nickname_fine" to "A nickname is fine.",
            "sharesheet_your_name" to "Your name",
            "sharesheet_save_and_share" to "Save and share",
            "sharesheet_not_now" to "Not now",
            "sharesheet_name_save_failed" to "Couldn't save your name. Try again.",
            "sharesheet_too_many_tries" to "Too many tries. Try again later.",
            "sharesheet_invite_message_new" to "I started a group on MixMates. Join me:",
            "sharesheet_invite_message" to "Join %1\$s on MixMates:",
            "sharesheet_invite_to" to "Invite a friend to %1\$s",
        )
        agreed.forEach { (name, sentence) -> assertEquals(name, sentence, english[name]) }
    }
}
