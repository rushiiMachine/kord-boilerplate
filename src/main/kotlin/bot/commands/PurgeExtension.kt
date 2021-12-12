package bot.commands

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingInt
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.delete
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
        publicSlashCommand(::BanArgs) {
            name = this@PurgeExtension.name
            description = "Purge"
            requireBotPermissions(Permission.ManageMessages, Permission.ReadMessageHistory, Permission.ViewChannel)

            check {
                anyGuild()
                hasPermission(Permission.ManageMessages)
            }

            action {
                val providedReason = arguments.reason ?: "None"
                val author = user.asUser()

                val limit = Clock.System.now() - 14.days
                val (messages, oldMessages) = channel.getMessagesBefore(Snowflake.max, arguments.count).toList()
                    .map { it.id }
                    .partition { it.timestamp > limit }

                channel.kord.rest.channel.bulkDelete(channel.id,
                    BulkDeleteRequest(messages),
                    "Purge by ${author.tag} (${author.id}) with reason: $providedReason"
                )

                val response = respond {
                    content = "Purged ${messages.size} messages."
                    if (arguments.count > 100)
                        content += " *Limited to 100 messages.*"
                    if (oldMessages.isNotEmpty())
                        content += "\n*This purge targeted messages older than 14 days which were ignored.*"
                }
                response.delete(5000, true)
            }
        }
    }

    inner class BanArgs : Arguments() {
        val count by defaultingInt("count", "Number of messages to delete (Max 100, default 5)", 5)
        val reason by optionalString("reason", "Purge reason")
    }
}
