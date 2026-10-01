package es.mixmat.listener.ui.sharesheet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * The copy rules for the start-a-group, invite and name flows, held as a test
 * rather than a review checklist. Scoped to [GroupFlowCopy]: the app carries
 * legitimate website links elsewhere (legal pages, Open in MixMates).
 */
class GroupFlowCopyTest {

    /** Every constant, found by reflection so a new one cannot slip past. */
    private val strings: Map<String, String> =
        GroupFlowCopy::class.java.declaredFields
            .filter { it.type == String::class.java && Modifier.isStatic(it.modifiers) }
            .associate { it.name to it.get(null) as String }

    private val banned = listOf("mixmat.es", "upgrade", "paid", "plan", "subscription", "pricing", "free tier")

    @Test
    fun `the reflection actually finds the strings`() {
        assertTrue(strings.size >= 20)
        assertTrue("NAME_TAKEN" in strings)
    }

    @Test
    fun `no string names the website or talks about plans`() {
        strings.forEach { (name, text) ->
            banned.forEach { word ->
                assertFalse("$name contains \"$word\"", text.contains(word, ignoreCase = true))
            }
        }
    }

    @Test
    fun `no string has an em or en dash`() {
        strings.forEach { (name, text) ->
            assertFalse("$name has an em dash", '—' in text)
            assertFalse("$name has an en dash", '–' in text)
        }
    }

    @Test
    fun `the agreed sentences are exact`() {
        assertEquals("That name's taken. Try adding something of your own to it.", GroupFlowCopy.NAME_TAKEN)
        assertEquals("This account already has a group.", GroupFlowCopy.ALREADY_HAS_GROUP)
        assertEquals("Choose a name your friends will see.", GroupFlowCopy.CHOOSE_A_NAME)
        assertEquals("A nickname is fine.", GroupFlowCopy.NICKNAME_FINE)
        assertEquals("Your name", GroupFlowCopy.YOUR_NAME)
        assertEquals("Save and share", GroupFlowCopy.SAVE_AND_SHARE)
        assertEquals("Not now", GroupFlowCopy.NOT_NOW)
        assertEquals("Too many tries. Try again later.", GroupFlowCopy.TOO_MANY_TRIES)
        assertEquals("I started a group on MixMates. Join me:", GroupFlowCopy.INVITE_MESSAGE_NEW)
        assertEquals("Join Night Shift on MixMates:", GroupFlowCopy.inviteMessage("Night Shift"))
        assertEquals("Invite a friend to Night Shift", GroupFlowCopy.inviteTo("Night Shift"))
        assertEquals("Couldn't save your name. Try again.", GroupFlowCopy.NAME_SAVE_FAILED)
        assertEquals(
            "That's an Apple private address. Choose a name your friends will see.",
            GroupFlowCopy.PRIVATE_RELAY,
        )
    }
}
