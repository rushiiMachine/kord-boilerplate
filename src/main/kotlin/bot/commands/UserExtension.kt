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
import dev.kord.rest.Image
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
                                target.id.value, target.tag, target.createdAt.discord, target.createdAt.discordRelative)
                        }

                        if (member != null) field {
                            name = i18n("bot.user.embed.member.header")

                            val noneI18n = i18n("bot.words.none")
                            value = i18n("bot.user.embed.member.content",
                                code(member.nickname, value),
                                member.roles
                                    .toList()
                                    .map(RoleBehavior::mention)
                                    .joinToStringDefault(noneI18n),
                                member.joinedAt.discord,
                                member.joinedAt.discordRelative
                            )

                            value += conditionalLazy(member.premiumSince) {
                                i18n("bot.user.embed.member.content.boosting",
                                    it.discord,
                                    it.discordRelative)
                            }

                            value += conditionalLazy(member.getColor()) {
                                i18n("bot.user.embed.member.content.color",
                                    it.rgb.toString(16))
                            }

                            value += conditionalLazy(member.memberAvatar) {
                                i18n("bot.user.embed.member.content.avatar",
                                    it.toUrl(Image.Size.Size128))
                            }
                        }
                    }
                }
            }
        }
    }

    inner class UserArgs : Arguments() {
        val user by optionalUser {
            name = "user"
            description = "A user mention"
        }
        val id by optionalString {
            name = "id"
            description = "A user's id"
        }
    }
}
