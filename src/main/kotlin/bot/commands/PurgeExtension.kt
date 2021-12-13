package bot.commands

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
            name = this@PurgeExtension.name
            description = "Purge"
            requireBotPermissions(Permission.ManageMessages, Permission.ReadMessageHistory)

            check {
                anyGuild()
                hasPermission(Permission.ManageMessages)
                // TODO: prevent purging less than 2
            }

            action {
                val providedReason = arguments.reason ?: "None"
                val author = user.asUser()

                val limit = Clock.System.now() - 14.days
                val (messages, oldMessages) = channel.getMessagesBefore(Snowflake.max, arguments.count).toList()
                    .partition { it.timestamp > limit }

                channel.kord.rest.channel.bulkDelete(channel.id,
                    BulkDeleteRequest(messages.map { it.id }),
                    "Purge by ${author.tag} (${author.id}) with reason: $providedReason")

                val authors = messages // Message[]
                    .mapNotNull { it.author?.id?.value } // Snowflake[] message author ids
                    .groupBy { it } // Map<Snowflake, Snowflake[] all instances of that snowflake>
                    .entries.associateBy({ it.value.size }) { it.key } // Map<instances count, Snowflake>
                    .toSortedMap(Comparator.reverseOrder()) // Sort by instance count
                    .entries.take(10) // Take top 10

                respond {
                    content = "Purged ${messages.size} total messages."
                    if (arguments.count > 100)
                        content += " *Limited to 100 messages.*"
                    if (oldMessages.isNotEmpty())
                        content += "\n*This purge targeted messages older than 14 days which were ignored.*"
                    if (authors.isNotEmpty()) {
                        content += "\n\nTop message authors:\n"
                        content += authors.joinToString("\n") { "<@${it.value}>: ${it.key} messages" }
                    }
                }
            }
        }
    }

    inner class BanArgs : Arguments() {
        val count by defaultingInt("count", "Number of messages to delete (Range: 2-100)", 5)
        val reason by optionalString("reason", "Purge reason")
    }
}
