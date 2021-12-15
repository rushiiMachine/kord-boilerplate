package bot.commands

import bot.*
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalUser
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.types.respondEphemeral
import com.kotlindiscord.kord.extensions.utils.createdAt
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.RoleBehavior
import dev.kord.core.entity.User
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.flow.toList

class UserExtension : Extension() {
    override val name = "user"

    override suspend fun setup() {
        publicSlashCommand(::UserArgs) {
            name = this@UserExtension.name
            description = "Lookup details on a specific user. Defaults to you"

            action {
                val target: User?

                if (arguments.user != null)
                    target = arguments.user
                else if (arguments.id != null) {
                    val id = arguments.id!!.toULongOrNull()
                    if (id == null) {
                        respondEphemeral { content = i18n("bot.user.errors.id") }
                        return@action
                    }

                    target = channel.kord.getUser(Snowflake(id), EntitySupplyStrategy.cacheWithRestFallback)
                } else {
                    target = this.user.asUser()
                }

                if (target == null) {
                    respondEphemeral { content = i18n("bot.user.errors.fetch") }
                    return@action
                }

                val member = guild?.getMember(target.id)

                respond {
                    embed {
                        color = embedColorFromUser(target)
                        thumbnail {
                            url = target.displayAvatar()
                        }
                        field {
                            name = i18n("bot.user.embed.user.header")
                            value = i18n("bot.user.embed.user.content",
                                target.tag, target.id, target.createdAt.discord, target.createdAt.discordRelative)
                        }

                        if (member != null) field {
                            name = i18n("bot.user.embed.member.header")
                            // TODO: i18n for this too
                            value = """
                                • Nickname: ${code(member.nickname, "None")}
                                • Roles: ${joinList(member.roles.toList().map(RoleBehavior::mention), "None")}
                                • Joined: ${member.joinedAt.discord} ${member.joinedAt.discordRelative}
                                ${condStr(member.premiumSince) { "• Boosting: ${it.discord} ${it.discordRelative}" }}
                                ${condStr(member.accentColor) { "• Color: `#${it.rgb.toString(16)}`" }}
                                ${condStr(member.memberAvatar) { "• Server Avatar: [Link](${it.toUrl()})" }}
                            """.trimIndent()
                        }
                    }
                }
            }
        }
    }

    inner class UserArgs : Arguments() {
        val user by optionalUser("user", "A user mention")
        val id by optionalString("id", "A user's id")
    }
}
