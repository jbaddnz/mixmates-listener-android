package es.mixmat.listener.ui.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Every English string has a Spanish one, and both take the same arguments.
 *
 * A missing translation does not fail the build on Android: the phone quietly
 * falls back to English mid-screen. A mismatched placeholder is worse, because
 * it crashes when the string is formatted. Both are cheap to catch here.
 *
 * Reads the resource files from disk. JVM unit tests run with the module
 * directory as the working directory.
 */
class TranslationCompletenessTest {

    private val english = File("src/main/res/values")
    private val spanish = File("src/main/res/values-es")

    /** name -> text, for <string> and each <plurals> item, skipping translatable="false". */
    private fun read(dir: File): Map<String, String> {
        val files = dir.listFiles { f -> f.name.startsWith("strings") && f.name.endsWith(".xml") }
            ?: return emptyMap()
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val out = mutableMapOf<String, String>()
        for (file in files) {
            val root = builder.parse(file).documentElement
            val nodes = root.childNodes
            for (i in 0 until nodes.length) {
                val node = nodes.item(i) as? Element ?: continue
                if (node.getAttribute("translatable") == "false") continue
                val name = node.getAttribute("name")
                when (node.tagName) {
                    "string" -> out[name] = node.textContent
                    "plurals" -> {
                        val items = node.getElementsByTagName("item")
                        // Quantities differ between languages, so only the
                        // "other" form is compared for placeholders.
                        for (j in 0 until items.length) {
                            val item = items.item(j) as Element
                            if (item.getAttribute("quantity") == "other") {
                                out["$name#other"] = item.textContent
                            }
                        }
                    }
                }
            }
        }
        return out
    }

    private val placeholder = Regex("""%(\d+\$)?[sd]""")

    private fun placeholders(text: String) =
        placeholder.findAll(text).map { it.value }.sorted().toList()

    @Test
    fun `the resource files are found`() {
        assertTrue("no English strings found", read(english).size > 100)
    }

    @Test
    fun `every English string has a Spanish translation`() {
        val missing = read(english).keys - read(spanish).keys
        assertTrue("Missing in values-es: ${missing.sorted()}", missing.isEmpty())
    }

    @Test
    fun `Spanish has no strings English lacks`() {
        val extra = read(spanish).keys - read(english).keys
        assertTrue("Only in values-es: ${extra.sorted()}", extra.isEmpty())
    }

    @Test
    fun `both languages take the same arguments`() {
        val es = read(spanish)
        read(english).forEach { (name, text) ->
            val translated = es[name] ?: return@forEach
            assertEquals("Placeholders differ for $name", placeholders(text), placeholders(translated))
        }
    }

    /** Anything with more than one argument must number them, or word order cannot change. */
    @Test
    fun `strings with several arguments number them`() {
        (read(english) + read(spanish).mapKeys { "es:${it.key}" }).forEach { (name, text) ->
            val found = placeholder.findAll(text).toList()
            if (found.size > 1) {
                assertTrue("$name has unnumbered placeholders", found.all { it.groupValues[1].isNotEmpty() })
            }
        }
    }
}
