package bot.commands

import bot.configureAuthor
import bot.i18n
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.kordLogger
import dev.kord.gateway.Intent
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
import java.util.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SnipeExtension : Extension() {
    override val name = "snipe"

    // Mapped by Channel ID -> list of the latest messages
    private val messageCache = hashMapOf<ULong, LinkedList<CachedMessage>>()

    override suspend fun setup() {
        intents += Intent.GuildMessages

        publicSlashCommand {
            name = "snipe"
            description = "Retrieve the last deleted message within the last minute"

            check {
                anyGuild()
            }

            action {
                respond {
                    // Find the most recent deleted message in cache
                    val snipedMsg = messageCache[channel.id.value]?.findLast(CachedMessage::deleted)
                    if (snipedMsg == null)
                        content = i18n("bot.snipe.nothingToSnipe")
                    else embed {
                        configureAuthor(snipedMsg.author)
                        timestamp = snipedMsg.id.timestamp
                        description = snipedMsg.content

                        // Clear entire list cache for channel
                        messageCache.remove(channel.id.value)
                    }
                }
            }
        }

        // Flush cache & add message to cache
        event<MessageCreateEvent> {
            action {
                flushCache()

                val msg = event.message
                if (msg.content.isEmpty() || msg.author == null || msg.author?.isBot == true) return@action

                // Get existing list or create new
                val list = messageCache[msg.channelId.value]
                    ?: LinkedList()
                // Add message to list
                list.add(CachedMessage(
                    msg.id,
                    msg.author!!,
                    msg.content
                ))
                // If list for channel didn't exist, add to the cache
                if (list.size == 1)
                    messageCache[msg.channelId.value] = list
            }
        }

        // Flush cache & mark cached message deleted
        event<MessageDeleteEvent> {
            action {
                flushCache()
                messageCache[event.channelId.value]
                    ?.find { it.id.value == event.messageId.value }
                    ?.deleted = true
            }
        }
    }

    private var lastFlush = Clock.System.now()
    private fun flushCache() {
        val now = Clock.System.now()

        if (lastFlush + 1.minutes > now) return
        lastFlush = now

        val iter = messageCache.iterator()
        while (iter.hasNext()) {
            val list = iter.next().value

            // Modify list in-place
            list.removeAll { it.id.timestamp + 1.minutes < now }
            if (list.isEmpty()) iter.remove()
        }
    }

    data class CachedMessage(
        val id: Snowflake,
        val author: User,
        val content: String,
        var deleted: Boolean = false,
    )
}
