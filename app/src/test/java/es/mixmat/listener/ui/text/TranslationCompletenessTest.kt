package es.mixmat.listener.ui.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Every language has every English string, taking the same arguments.
 *
 * A missing translation does not fail the build on Android: the phone quietly
 * falls back to English mid-screen. A mismatched placeholder is worse, because
 * it crashes when the string is formatted. Both are cheap to catch here.
 *
 * Every values-* folder holding strings files is checked, so a new language is
 * covered the moment its folder exists. Each one is a full copy, never a partial
 * override of another language, so each file can be reviewed on its own.
 *
 * Reads the resource files from disk. JVM unit tests run with the module
 * directory as the working directory.
 */
class TranslationCompletenessTest {

    private val english = File("src/main/res/values")

    /** Folder name -> its strings, for every translated language. */
    private val languages: Map<String, Map<String, String>> =
        File("src/main/res").listFiles { f -> f.isDirectory && f.name.startsWith("values-") }
            .orEmpty()
            .associate { it.name to read(it) }
            .filterValues { it.isNotEmpty() }

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
        // The languages shipped today. A folder silently dropping out would
        // otherwise pass every other test here by having nothing to check.
        listOf("values-es", "values-b+es+419", "values-pt").forEach {
            assertTrue("no strings in $it", it in languages)
        }
    }

    @Test
    fun `every language has every English string`() {
        val englishKeys = read(english).keys
        languages.forEach { (folder, strings) ->
            val missing = englishKeys - strings.keys
            assertTrue("Missing in $folder: ${missing.sorted()}", missing.isEmpty())
        }
    }

    @Test
    fun `no language has strings English lacks`() {
        val englishKeys = read(english).keys
        languages.forEach { (folder, strings) ->
            val extra = strings.keys - englishKeys
            assertTrue("Only in $folder: ${extra.sorted()}", extra.isEmpty())
        }
    }

    @Test
    fun `every language takes the same arguments as English`() {
        val source = read(english)
        languages.forEach { (folder, strings) ->
            source.forEach { (name, text) ->
                val translated = strings[name] ?: return@forEach
                assertEquals(
                    "Placeholders differ for $name in $folder",
                    placeholders(text),
                    placeholders(translated),
                )
            }
        }
    }

    /** Anything with more than one argument must number them, or word order cannot change. */
    @Test
    fun `strings with several arguments number them`() {
        val all = read(english) + languages.flatMap { (folder, strings) ->
            strings.map { (name, text) -> "$folder:$name" to text }
        }
        all.forEach { (name, text) ->
            val found = placeholder.findAll(text).toList()
            if (found.size > 1) {
                assertTrue("$name has unnumbered placeholders", found.all { it.groupValues[1].isNotEmpty() })
            }
        }
    }
}
