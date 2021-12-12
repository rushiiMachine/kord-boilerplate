package bot

import com.kotlindiscord.kord.extensions.utils.createdAt
import dev.kord.common.Color
import dev.kord.core.entity.User
import dev.kord.rest.Image
import dev.kord.rest.builder.message.EmbedBuilder
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

@OptIn(ExperimentalTime::class)
fun embedColorFromUser(user: User): Color {
    val duration = Clock.System.now() - user.createdAt
    return when {
        duration < 7.days -> Color.ERROR
        duration < 30.days -> Color.WARNING
        else -> Color.CONFIRM
    }
}

val Instant.discordTimestamp: String
    get() = "<t:${epochSeconds}>"

@Suppress("NOTHING_TO_INLINE")
inline fun condStr(condition: Boolean, value: String?): String = if (condition) value ?: "null" else ""

fun User.displayAvatar(size: Image.Size = Image.Size.Size64, format: Image.Format = Image.Format.PNG): String {
    return (avatar ?: defaultAvatar).cdnUrl.toUrl {
        this.format = format
        this.size = size
    }
}

fun EmbedBuilder.configureAuthor(user: User) {
    author {
        name = "${user.tag} (${user.id})"
        icon = user.displayAvatar()
    }
}
