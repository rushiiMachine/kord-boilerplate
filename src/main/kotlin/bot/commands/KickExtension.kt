@file:Suppress("DuplicatedCode")

package bot.commands

import bot.WARNING
import bot.configureAuthor
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.Color
import dev.kord.common.entity.Permission
import dev.kord.rest.Image
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock

class KickExtension : Extension() {
    override val name = "kick"

    override suspend fun setup() {
        publicSlashCommand(::KickArgs) {
            name = this@KickExtension.name
            description = "Kick a specific user"
            requireBotPermissions(Permission.KickMembers)

            check {
                anyGuild()
                hasPermission(Permission.KickMembers)
            }

            action {
                val reason = arguments.reason ?: "None"
                val author = user.asUser()

                guild!!.kick(arguments.target.id, "Kicked by ${author.tag} (${author.id}) with reason: $reason")

                respond {
                    val target = arguments.target
                    embed {
                        timestamp = Clock.System.now()
                        configureAuthor(author)
                        color = Color.WARNING
                        description = """
                            **Member:** `${target.tag}` (${target.id})
                            **Action:** Kick
                            **Reason:** $reason
                        """.trimIndent()
                    }
                }
            }
        }
    }

    inner class KickArgs : Arguments() {
        val target by user("target", "User to kick")
        val reason by optionalString("reason", "Kick reason")
    }
}
