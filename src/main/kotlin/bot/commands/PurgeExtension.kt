package bot.commands

import bot.configureAuthor
import bot.pluralize
import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingInt
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
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
                val providedReason = arguments.reason ?: "None"
                val author = user.asUser()

                val limit = Clock.System.now() - 14.days
                val (messages, oldMessages) = channel.getMessagesBefore(Snowflake.max, arguments.count).toList()
                    .partition { it.timestamp > limit }

                val reason = "Purge by ${author.tag} (${author.id}) with reason: $providedReason"
                if (messages.size == 1)
                    channel.deleteMessage(messages.first().id, reason)
                else if (messages.size > 1) {
                    channel.kord.rest.channel.bulkDelete(channel.id,
                        BulkDeleteRequest(messages.map(Message::id)), reason)
                }

                // TODO: count bot messages ??
                val authors = messages // Message[]
                    .map { it.author?.id?.value } // Snowflake[] message author ids
                    .groupBy { it } // Map<Snowflake, Snowflake[] all instances of that snowflake>
                    .entries.associateBy({ it.value.size }) { it.key } // Map<instances count, Snowflake>
                    .toSortedMap(Comparator.reverseOrder()) // Sort by instance count
                    .entries.take(10) // Take top 10

                respond {
                    embed {
                        configureAuthor(user.asUser())

                        description = "Purged ${messages.size} total messages."
                        if (arguments.count > 100)
                            description += " *(Limited to 100)*"
                        if (oldMessages.isNotEmpty())
                            description += "\n*This purge targeted messages older than 14 days which were ignored.*"

                        if (authors.isNotEmpty()) field {
                            name = "❯ Top message authors"
                            value = authors.joinToString("\n")
                            {
                                val id = if (it.value != null) "<@${it.value}>" else "Unknown"
                                "• $id: ${it.key} ${"message".pluralize(it.key)}"
                            }
                        }
                    }
                }
            }
        }
    }

    inner class BanArgs : Arguments() {
        val count by defaultingInt("count",
            "Number of messages to delete (Range: 1-100)",
            5,
            required = true // Require validator to pass
        ) { _, value ->
            if (value == 0)
                throw DiscordRelayedException("You cannot purge 0 messages!")
        }
        val reason by optionalString("reason", "Purge reason")
    }
}
