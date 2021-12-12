@file:Suppress("DuplicatedCode")

package bot.commands

import bot.ERROR
import bot.configureAuthor
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.optionalNumberChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.Color
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.ban
import dev.kord.rest.Image
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock

class BanExtension : Extension() {
    override val name = "ban"

    override suspend fun setup() {
        publicSlashCommand(::BanArgs) {
            name = this@BanExtension.name
            description = "Ban a specific user"
            requireBotPermissions(Permission.BanMembers)

            check {
                anyGuild()
                hasPermission(Permission.BanMembers)
            }

            action {
                val providedReason = arguments.reason ?: "None"
                val author = user.asUser()

                guild!!.ban(arguments.target.id) {
                    reason = "Banned by ${author.tag} (${author.id}) with reason: $providedReason"
                    deleteMessagesDays = arguments.deleteMessages?.toInt()
                }

                respond {
                    val target = arguments.target
                    embed {
                        timestamp = Clock.System.now()
                        configureAuthor(author)
                        color = Color.ERROR
                        description = """
                            **Member:** `${target.tag}` (${target.id})
                            **Action:** Ban
                            **Reason:** $providedReason
                        """.trimIndent()
                    }
                }
            }
        }
    }

    inner class BanArgs : Arguments() {
        val target by user("target", "User to ban")
        val reason by optionalString("reason", "Ban reason")
        val deleteMessages by optionalNumberChoice(
            "delete_messages",
            "Days of messages to delete from this user",
            mapOf(
                "0 Days" to 0,
                "1 Day" to 1,
                "2 Days" to 2,
                "3 Days" to 3,
                "4 Days" to 4,
                "5 Days" to 5,
                "6 Days" to 6,
                "7 Days" to 7
            ))
    }
}
