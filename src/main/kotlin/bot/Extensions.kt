package bot

import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.types.PublicInteractionContext
import com.kotlindiscord.kord.extensions.utils.createdAt
import dev.kord.common.Color
import dev.kord.core.behavior.interaction.followUp
import dev.kord.core.entity.Icon
import dev.kord.core.entity.User
import dev.kord.rest.Image
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.FollowupMessageCreateBuilder
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

val Color.Companion.WARNING
    get() = Color(247, 149, 84)

val Color.Companion.ERROR
    get() = Color(255, 92, 92)

val Color.Companion.CONFIRM
    get() = Color(76, 255, 76)

val Color.Companion.NO_ACTION
    get() = Color(47, 49, 54)

/**
 * Get color from a user account's creation date,
 * More red meaning higher chance it's a bot.
 */
@OptIn(ExperimentalTime::class)
fun embedColorFromUser(user: User): Color {
    val duration = Clock.System.now() - user.createdAt
    return when {
        duration < 7.days -> Color.ERROR
        duration < 30.days -> Color.WARNING
        else -> Color.CONFIRM
    }
}

val Instant.discord: String
    get() = "<t:${epochSeconds}>"

val Instant.discordRelative: String
    get() = "<t:${epochSeconds}:R>"

@Suppress("NOTHING_TO_INLINE")
inline fun condStr(condition: Boolean, value: String?): String = if (condition) value ?: "null" else ""

/** Invokes callback to make lazy string if value not null, else returns empty string */
fun <T> condStr(value: T?, lazy: (T) -> String): String = if (value != null) lazy.invoke(value) else ""

/**
 * Display avatar; their avatar or the default avatar
 */
fun User.displayAvatar(size: Image.Size = Image.Size.Size64, format: Image.Format = Image.Format.PNG): String {
    return (avatar ?: defaultAvatar).cdnUrl.toUrl {
        this.format = format
        this.size = size
    }
}

/**
 * Set the author text to "User#000 (userid)"
 * and author icon to their avatar
 */
fun EmbedBuilder.configureAuthor(user: User) {
    author {
        name = "${user.tag} (${user.id})"
        icon = user.displayAvatar()
    }
}

/** Follow up with a message returning void */
suspend inline fun PublicInteractionContext.respondV(
    builder: FollowupMessageCreateBuilder.() -> Unit,
) {
    interactionResponse.followUp(builder)
}

/** Follow up with an ephemeral message returning void */
suspend inline fun PublicInteractionContext.respondEV(
    builder: FollowupMessageCreateBuilder.() -> Unit,
) {
    interactionResponse.followUp(builder)
}

/** Wrap a string in a single-line codeblock */
@Suppress("NOTHING_TO_INLINE")
inline fun code(str: String?) = "`${str}`"

/** Wrap a string in a single-line codeblock, if null then default */
@Suppress("NOTHING_TO_INLINE")
inline fun code(str: String?, default: String) = if (str != null) code(str) else default

/** Wrap each string in a single-line codeblock or default if list empty/null */
fun code(strings: List<String>?, default: String) =
    if (strings == null || strings.isEmpty()) default else strings.joinToString { code(it) }

/** Join a list together or default if list is empty */
fun joinList(list: List<String>, default: String) = if (list.isEmpty()) default else list.joinToString()

/** Shortcut for Icon.cdnUrl.toUrl { options } */
fun Icon.toUrl(size: Image.Size = Image.Size.Size64, format: Image.Format = Image.Format.PNG): String = cdnUrl.toUrl {
    this.size = size
    this.format = format
}

/** Pluralize the string if count is not 1 */
//fun String.pluralize(count: Int): String {
//    return if (count != 1)
//        this.plus("s")
//    else this
//}

/* Shortcut for translations */
suspend fun CommandContext.i18n(key: String, vararg replacements: Any?) = translate(key, arrayOf(*replacements))

/* Pluralize a string from translations. Targets key or key + ".pluralized" if plural */
suspend fun CommandContext.i18nPluralize(key: String, count: Int) =
    translate(if (count == 1) key else "$key.pluralized")
