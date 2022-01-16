package bot.commands

import bot.configureAuthor
import bot.i18n
import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock

class UnbanExtension : Extension() {
    override val name = "unban"

    override suspend fun setup() {
        publicSlashCommand(::EchoArguments) {
            name = "unban"
            description = "Unban a user by id"
            requireBotPermissions(Permission.BanMembers)

            check {
                anyGuild()
            }

            action {
                respond {
                    val author = user.asUser()
                    val id = arguments.id.toLongOrNull()
                        ?: throw DiscordRelayedException(i18n("bot.unban.errors.invalidId"))

                    val target = try {
                        channel.kord.getUser(Snowflake(id), EntitySupplyStrategy.rest)
                            ?: throw Error()
                    } catch (t: Throwable) {
                        throw DiscordRelayedException(i18n("bot.unban.errors.invalidId"))
                    }

                    val reason = i18n("bot.unban.reason",
                        author.tag,
                        author.id.value,
                        arguments.reason ?: i18n("bot.words.none")
                    )
                    guild!!.unban(Snowflake(id), reason)

                    embed {
                        configureAuthor(user.asUser())
                        timestamp = Clock.System.now()
                        description = i18n("bot.unban.embed",
                            target.tag,
                            target.id.value,
                            arguments.reason ?: i18n("bot.words.none")
                        )
                    }
                }
            }
        }
    }

    inner class EchoArguments : Arguments() {
        val id by string("id", "The target user's id")
        val reason by optionalString("reason", "Reason for unbanning this user")
    }
}
