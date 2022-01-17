package bot.commands

import bot.NO_ACTION
import bot.configureAuthor
import bot.i18n
import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingInt
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.Color
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.json.request.BulkDeleteRequest
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class PurgeExtension : Extension() {
    override val name = "purge"

    override suspend fun setup() {
        ephemeralSlashCommand(::BanArgs) {
            name = "purge"
            description = "Purge latest messages from this channel"
            requireBotPermissions(Permission.ManageMessages, Permission.ReadMessageHistory)

            check {
                anyGuild()
                hasPermission(Permission.ManageMessages)
            }

            action {
                val providedReason = arguments.reason ?: i18n("bot.words.none")
                val author = user.asUser()

                val limit = Clock.System.now() - 14.days
                val (messages, oldMessages) = channel.getMessagesBefore(Snowflake.max, arguments.count).toList()
                    .partition { it.timestamp > limit }

                val reason = i18n("bot.purge.reason", author.tag, author.id.value, providedReason)
                if (messages.size == 1)
                    channel.deleteMessage(messages.first().id, reason)
                else if (messages.size > 1) {
                    channel.kord.rest.channel.bulkDelete(channel.id,
                        BulkDeleteRequest(messages.map(Message::id)), reason)
                }

                val authors = messages // Message[]
                    .map { it.data.author.id } // Snowflake[] message author ids
                    .groupBy { it } // Map<Snowflake, Snowflake[] all instances of that snowflake>
                    .entries.associateBy({ it.value.size }) { it.key } // Map<instances count, Snowflake>
                    .toSortedMap(Comparator.reverseOrder()) // Sort by instance count
                    .entries.take(10) // Take top 10

                respond {
                    embed {
                        color = Color.NO_ACTION
                        configureAuthor(user.asUser())

                        description = i18n("bot.purge.embed.count", messages.size)
                        if (arguments.count > 100)
                            description += i18n("bot.purge.embed.limited")
                        if (oldMessages.isNotEmpty())
                            description += i18n("bot.purge.embed.old")

                        if (authors.isNotEmpty()) field {
                            val strMessage = i18n("bot.words.message")
                            val strMessages = i18n("bot.words.message.pluralized")
                            name = i18n("bot.purge.embed.topAuthors")
                            value = authors.joinToString("\n")
                            { "• <@${it.value.value}>: ${it.key} ${if (it.key == 1) strMessage else strMessages}" }
                        }
                    }
                }
            }
        }
    }

    inner class BanArgs : Arguments() {
        val count by defaultingInt {
            name = "count"
            description = "Number of messages to delete (Range: 1-100)"
            defaultValue = 5
            ignoreErrors = false
            validate {
                if (value == 0)
                    throw DiscordRelayedException(i18n("bot.purge.errors.zeroPurged"))
            }
        }
        val reason by optionalString {
            name = "reason"
            description = "Purge reason"
        }
    }
}
