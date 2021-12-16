@file:Suppress("DuplicatedCode")

package bot.commands

import bot.WARNING
import bot.configureAuthor
import bot.i18n
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

                guild!!.kick(arguments.target.id, i18n("bot.kick.reason", author.tag, author.id, reason))

                respond {
                    val target = arguments.target
                    embed {
                        timestamp = Clock.System.now()
                        configureAuthor(author)
                        color = Color.WARNING
                        description = i18n("bot.kick.embed",
                            target.tag, target.id, reason)
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
