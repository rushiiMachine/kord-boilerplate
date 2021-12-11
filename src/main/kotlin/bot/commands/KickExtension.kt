@file:Suppress("DuplicatedCode")

package bot.commands

import bot.WARNING
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.Color
import dev.kord.rest.Image
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock

class KickExtension : Extension() {
    override val name = "kick"

    override suspend fun setup() {
        publicSlashCommand(::KickArgs) {
            name = this@KickExtension.name
            description = "Kick a specific user"
            check { anyGuild() }

            action {
                val reason = arguments.reason ?: "None"
                val author = user.asUser()

                guild!!.kick(arguments.target.id, "Kicked by ${author.tag} (${author.id}) with reason: $reason")

                respond {
                    val target = arguments.target
                    embed {
                        color = Color.WARNING
                        timestamp = Clock.System.now()
                        author {
                            name = "${author.tag} (${author.id})"
                            icon = (author.avatar ?: author.defaultAvatar).cdnUrl.toUrl {
                                format = Image.Format.PNG
                                Image.Size.Size64
                            }
                        }
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
