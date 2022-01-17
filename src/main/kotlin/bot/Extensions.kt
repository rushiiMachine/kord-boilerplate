package bot

import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.builders.ValidationContext
import com.kotlindiscord.kord.extensions.events.EventContext
import com.kotlindiscord.kord.extensions.i18n.TranslationsProvider
import com.kotlindiscord.kord.extensions.utils.createdAt
import com.kotlindiscord.kord.extensions.utils.getTopRole
import dev.kord.common.Color
import dev.kord.core.entity.Icon
import dev.kord.core.entity.Member
import dev.kord.core.entity.User
import dev.kord.core.event.Event
import dev.kord.rest.Image
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.flow.toList
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

/**
 * Convert to the default long discord timestamp
 */
val Instant.discord: String
    get() = "<t:${epochSeconds}>"

/**
 * Convert to the relative discord timestamp
 */
val Instant.discordRelative: String
    get() = "<t:${epochSeconds}:R>"

/** Invokes callback to make lazy string if value not null, else returns empty string */
suspend fun <T> conditionalLazy(value: T?, lazy: suspend (T) -> String): String =
    if (value != null) lazy.invoke(value) else ""

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
        name = "${user.tag} (${user.id.value})"
        icon = user.displayAvatar()
    }
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

/**
 * Join a string together or use default value if list empty
 * @param default Default value
 */
fun <T> List<T>.joinToStringDefault(default: String) =
    if (isEmpty()) default else joinToString()

/** Shortcut for Icon.cdnUrl.toUrl { options } */
fun Icon.toUrl(size: Image.Size = Image.Size.Size64, format: Image.Format = Image.Format.PNG): String = cdnUrl.toUrl {
    this.size = size
    this.format = format
}

/* Shortcut for translations */
suspend fun CommandContext.i18n(key: String, vararg replacements: Any?) =
    translate(key, "bot", arrayOf(*replacements))

/* Shortcut for translations */
suspend fun <T : Event> EventContext<T>.i18n(key: String, vararg replacements: Any?) =
    translate(key, "bot", arrayOf(*replacements))

/* Shortcut for translations */
suspend fun TranslationsProvider.i18n(key: String, vararg replacements: Any?) =
    translate(key, "bot", arrayOf(*replacements))

/* Shortcut for translations */
suspend fun <T> ValidationContext<T>.i18n(key: String, vararg replacements: Any?) =
    translate(key, "bot", arrayOf(*replacements))

/* Pluralize a string from translations. Targets key or key + ".pluralized" if plural */
suspend fun CommandContext.i18nPluralize(key: String, count: Int) =
    translate(if (count == 1) key else "$key.pluralized")

/** Get the visible color (if any) for this member */
suspend fun Member.getColor(): Color? = roles.toList()
    .toSortedSet(Comparator.comparing { -it.rawPosition })
    .firstOrNull { it.color.rgb != 0 }?.color

/** Check if this member is above another member in the role hierarchy. Does not check for permissions. */
suspend fun Member.canManage(otherMember: Member): Boolean {
    val ownerId = guild.asGuild().ownerId
    if (id == ownerId) return true
    return (getTopRole()?.rawPosition ?: 0) > (otherMember.getTopRole()?.rawPosition ?: 0)
            && guild.asGuild().ownerId != otherMember.id
}
