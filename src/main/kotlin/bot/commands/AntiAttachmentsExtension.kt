package bot.commands

import bot.database.BlockedAttachmentsTable
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.notHasPermission
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.common.entity.Permission
import dev.kord.core.event.guild.GuildDeleteEvent
import dev.kord.core.event.message.MessageCreateEvent
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class AntiAttachmentsExtension : Extension() {
    override val name = "AntiAttachments"

    // TODO: add settings for this

    override suspend fun setup() {
        event<MessageCreateEvent> {
            check {
                notHasPermission(Permission.Administrator)
                anyGuild()
            }

            action {
                if (event.message.attachments.isEmpty() || event.guildId == null) return@action

                val record = transaction {
                    val guildId = event.guildId!!.value.toLong()
                    BlockedAttachmentsTable
                        .select { BlockedAttachmentsTable.guildId eq guildId }
                        .singleOrNull()
                } ?: return@action

                val blockedExts = record[BlockedAttachmentsTable.blockedExtensions]
                    ?.split(',')
                    ?: return@action

                val isBlocked = event.message.attachments.any { attachment ->
                    blockedExts.any { attachment.filename.endsWith(".$it") }
                }

                // need the extension that triggered

                if (isBlocked) try {
                    event.message.delete()
                } catch (_: Throwable) {
                    // Don't do anything
                }
            }
        }

        // Delete records when leaving server
        event<GuildDeleteEvent> {
            action {
                val guildId = event.guildId.value.toLong()
                transaction {
                    BlockedAttachmentsTable.deleteWhere { BlockedAttachmentsTable.guildId eq guildId }
                }
            }
        }
    }
}
