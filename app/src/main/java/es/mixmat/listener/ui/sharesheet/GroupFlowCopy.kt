package es.mixmat.listener.ui.sharesheet

/**
 * Every string in the start-a-group, invite and name flows, in one place so a
 * test can hold them to the rules: no website, no upgrade or plan talk, and no
 * em or en dashes. Wording matches iOS exactly; change both together.
 *
 * Constants only. Anything built from a group name is assembled at the call site
 * from these pieces, so the test still sees every word.
 */
object GroupFlowCopy {
    // -- Start a group --
    const val START_A_GROUP = "Start a group"
    const val START_HINT = "Make a group, then send your friends the invite link."
    const val NAME_YOUR_GROUP = "Name your group"
    const val CREATE = "Create"
    const val CANCEL = "Cancel"

    // -- Created --
    const val GROUP_READY = "Your group is ready"
    const val INVITE_A_FRIEND = "Invite a friend"
    const val SHARE_TO_IT = "Share this track to it"

    /** Share sheet text after a create, followed by the invite link. */
    const val INVITE_MESSAGE_NEW = "I started a group on MixMates. Join me:"

    /** Share sheet text from a picker row, which may be a friend's group. */
    const val INVITE_MESSAGE = "Join my group on MixMates:"

    // -- Start errors --
    const val NAME_TAKEN = "That name's taken. Try adding something of your own to it."
    const val ALREADY_HAS_GROUP = "This account already has a group."
    const val CREATE_FAILED = "Couldn't start the group. Try again."
    const val TOO_MANY_TRIES = "Too many tries. Try again later."

    // -- Names --
    const val CHOOSE_A_NAME = "Choose a name your friends will see."
    const val NICKNAME_FINE = "A nickname is fine."
    const val YOUR_NAME = "Your name"

    /** After a refused share. Agreed with iOS. */
    const val SAVE_AND_SHARE = "Save and share"

    /** After a refused create. iOS has no create yet, so not agreed. */
    const val SAVE = "Save"

    /** Back to where the refused action left off, selection kept. Agreed with iOS. */
    const val NOT_NOW = "Not now"
    const val PRIVATE_RELAY = "That's an Apple private address. Choose a name your friends will see."
    const val NAME_SAVE_FAILED = "Couldn't save your name. Try again."
}
