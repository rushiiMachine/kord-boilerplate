package bot.events

import bot.*
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.i18n.TranslationsProvider
import com.kotlindiscord.kord.extensions.utils.createdAt
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@OptIn(PrivilegedIntent::class)
class MemberLogExtension : Extension() {
    override val name = "memberLog"

    private val channelId = System.getProperty("MEMBER_LOG_CHANNEL_ID").toLongOrNull()
        ?: error("Failed to parse member log channel id in config. Please provide a valid id.")

    private suspend fun generateLog(
        user: User,
        targetChannel: TextChannel,
        joinedAt: Instant?,
        translations: TranslationsProvider,
    ) {
        targetChannel.createEmbed {
            color = if (joinedAt != null) embedColorFromUser(user) else Color.NO_ACTION
            configureAuthor(user)
            footer {
                text = translations.i18n("bot.memberlog.${if (joinedAt != null) "joined" else "left"}")
            }

            description = if (joinedAt == null)
                translations.i18n("bot.memberlog.leaveEmbed",
                    user.mention,
                    user.id.value,
                    user.createdAt.discord,
                    Clock.System.now().discord
                )
            else translations.i18n("bot.memberlog.joinEmbed",
                user.mention,
                user.id.value,
                user.createdAt.discord,
                joinedAt.discord
            )
        }
    }

    override suspend fun setup() {
        intents += Intent.GuildMembers

        val channel =
            kord.getChannelOf<TextChannel>(Snowflake(channelId), EntitySupplyStrategy.cacheWithCachingRestFallback)
                ?: error("Failed to fetch member log channel. Please provide a valid text channel id.")

        event<MemberJoinEvent> {
            action {
                generateLog(event.member.asUser(), channel, event.member.joinedAt, translationsProvider)
            }
        }

        event<MemberLeaveEvent> {
            action {
                generateLog(event.user, channel, null, translationsProvider)
            }
        }
    }
}

