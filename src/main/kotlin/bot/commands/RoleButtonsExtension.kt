package bot.commands

import bot.database.RoleButtonRecord
import bot.i18n
import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.i18n.TranslationsProvider
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.botHasPermissions
import com.kotlindiscord.kord.extensions.utils.getTopRole
import com.kotlindiscord.kord.extensions.utils.selfMember
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.channel.ChannelDeleteEvent
import dev.kord.core.event.guild.GuildDeleteEvent
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.json.JsonErrorCode
import dev.kord.rest.request.RestRequestException
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class RoleButtonsExtension : Extension() {
    override val name = "roleButtons"

    // All the edit sessions mapped by user id -> session
    private val sessions = mutableMapOf<Snowflake, EditSession>()

    override suspend fun setup() {
        ephemeralSlashCommand {
            name = "rolebuttons"
            description = "Create auto role buttons"
            requireBotPermissions(Permission.ManageRoles)

            check {
                anyGuild()
            }

            // Start a new session for the author
            ephemeralSubCommand(::StartArguments) {
                name = "start"
                description = "Start a multi-step edit session for creating a role buttons message"

                action {
                    if (sessions.containsKey(user.id))
                        throw DiscordRelayedException(i18n("bot.rolebuttons.errors.sessionExists"))

                    val channel = guild!!.getChannelOfOrNull<TextChannel>(arguments.channel.id)
                        ?: throw DiscordRelayedException(i18n("bot.rolebuttons.errors.invalidChannel"))
                    if (!channel.botHasPermissions(Permission.SendMessages))
                        throw DiscordRelayedException(i18n("bot.rolebuttons.errors.sendPermissions"))

                    sessions[user.id] = EditSession(channel.id)
                    respond { content = i18n("bot.rolebuttons.create.nextStep") }
                }
            }

            // Add a record to the author's session
            ephemeralSubCommand(::AddRoleArguments) {
                name = "add"
                description = "Add a button-role record onto the current session"

                action {
                    val session = sessions[user.id]
                        ?: throw DiscordRelayedException(i18n("bot.rolebuttons.errors.noSession"))
                    if (session.roles.size == 25)
                        throw DiscordRelayedException(i18n("bot.rolebuttons.errors.maxRoles"))
                    if (arguments.role.managed)
                        throw DiscordRelayedException(i18n("bot.rolebuttons.errors.managedRole"))
                    if (arguments.role.rawPosition > (guild!!.selfMember().getTopRole()?.rawPosition ?: 0))
                        throw DiscordRelayedException(i18n("bot.rolebuttons.errors.roleHierarchy"))

                    val name = arguments.name ?: arguments.role.name
                    val id = arguments.role.id
                    session.roles[name] = EditSessionRole(name, id.value.toLong())

                    respond {
                        embed {
                            embedUpdate(session, translationsProvider)
                            description = i18n("bot.rolebuttons.addRole.description", name, id.value)
                        }
                    }
                }
            }

            // Remove a record from the author's session
            ephemeralSubCommand(::RemoveRoleArguments) {
                name = "remove"
                description = "Remove a role-button pair by name in the current edit session"

                action {
                    val session = sessions[user.id]
                        ?: throw DiscordRelayedException(i18n("bot.rolebuttons.errors.noSession"))

                    respond {
                        embed {
                            description =
                                if (session.roles.remove(arguments.name) == null)
                                    i18n("bot.rolebuttons.removeRole.invalidName")
                                else
                                    i18n("bot.rolebuttons.removeRole.success", arguments.name)
                            embedUpdate(session, translationsProvider)
                        }
                    }
                }
            }

            // Save record to db & create message
            ephemeralSubCommand {
                name = "finish"
                description = "Finalize an edit session and post the role buttons message"

                action {
                    val session = sessions[user.id]
                        ?: throw DiscordRelayedException(i18n("bot.rolebuttons.errors.noSession"))

                    if (session.roles.isEmpty())
                        throw DiscordRelayedException(i18n("bot.rolebuttons.errors.noEntries"))

                    val targetChannel = channel.kord.getChannelOf<TextChannel>(session.channelId)
                        ?: throw DiscordRelayedException(i18n("bot.rolebuttons.errors.invalidChannel"))

                    // Create the new message with all the buttons on it
                    val message = try {
                        targetChannel.createMessage {
                            content = i18n("bot.rolebuttons.message")
                            session.roles.values.chunked(5).forEach { row ->
                                actionRow {
                                    row.forEach { record ->
                                        interactionButton(ButtonStyle.Primary, record.buttonId) {
                                            label = record.name
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: RestRequestException) {
                        if (e.error?.code == JsonErrorCode.PermissionLack)
                            throw DiscordRelayedException(i18n("bot.rolebuttons.errors.sendPermissions"))
                        throw e
                    }

                    // Save the record to db & delete session
                    transaction {
                        session.roles.values.forEach { (_, roleId, id) ->
                            RoleButtonRecord.insert {
                                it[buttonId] = id
                                it[targetRoleId] = roleId
                                it[messageId] = message.id.value.toLong()
                                it[channelId] = session.channelId.value.toLong()
                                it[guildId] = guild!!.id.value.toLong()
                            }
                        }
                    }
                    sessions.remove(user.id)

                    respond { content = i18n("bot.rolebuttons.finish.nextStep", channel.mention) }
                }
            }

            // Delete session for author
            ephemeralSubCommand {
                name = "cancel"
                description = "Cancel the current edit session"

                action {
                    sessions.remove(user.id)
                        ?: throw DiscordRelayedException(i18n("bot.rolebuttons.errors.noSession"))
                    respond { content = i18n("bot.rolebuttons.cancel") }
                }
            }
        }

        // Remove all records that are tied to this guild
        event<GuildDeleteEvent> {
            action {
                val id = event.guildId.value.toLong()
                transaction {
                    RoleButtonRecord.deleteWhere { RoleButtonRecord.guildId eq id }
                }
            }
        }

        // Remove all records that are tied to this channel
        event<ChannelDeleteEvent> {
            action {
                val id = event.channel.id.value.toLong()
                transaction {
                    RoleButtonRecord.deleteWhere { RoleButtonRecord.channelId eq id }
                }
            }
        }

        // Remove all records that are tied to this message
        event<MessageDeleteEvent> {
            action {
                val id = event.messageId.value.toLong()
                transaction {
                    RoleButtonRecord.deleteWhere { RoleButtonRecord.messageId eq id }
                }
            }
        }

        event<GuildButtonInteractionCreateEvent> {
            action {
                val interaction = event.interaction
                val id = interaction.componentId
                val member = interaction.member

                // Ignore non-role buttons
                if (!id.startsWith("rb_")) return@action

                // Fetch the button role if exists
                val record = transaction {
                    val databaseId = id.substring(3).toLongOrNull()
                        ?: return@transaction null
                    RoleButtonRecord.select { RoleButtonRecord.buttonId eq databaseId }.singleOrNull()
                }

                // If no record, then its unknown/lost
                if (record == null) {
                    interaction.respondEphemeral {
                        content = i18n("bot.rolebuttons.errors.unknownButton")
                    }
                    return@action
                }

                val snowflake = Snowflake(record[RoleButtonRecord.targetRoleId])

                // Add/Remove role
                val hasRole = member.roles.toList().any { it.id == snowflake }
                try {
                    val reason = i18n("bot.rolebuttons.reason")

                    interaction.respondEphemeral {
                        if (hasRole) {
                            content = i18n("bot.rolebuttons.removedRole", snowflake.value)
                            member.removeRole(snowflake, reason)
                        } else {
                            content = i18n("bot.rolebuttons.addedRole", snowflake.value)
                            member.addRole(snowflake, reason)
                        }
                    }
                } catch (t: RestRequestException) {
                    if (t.error?.code == JsonErrorCode.PermissionLack) interaction.respondEphemeral {
                        content = i18n("bot.rolebuttons.errors.rolePermissions")
                    }
                    else throw t
                }
            }
        }
    }

    private suspend fun EmbedBuilder.embedUpdate(session: EditSession, translations: TranslationsProvider) {
        field {
            this.name = translations.i18n("bot.rolebuttons.addRole.fieldHeader")
            value =
                if (session.roles.isEmpty()) translations.i18n("bot.words.none")
                else session.roles.values.joinToString("\n") { (name, roleId) -> "• $name: <@&$roleId>" }
        }
    }

    private data class EditSession(
        var channelId: Snowflake,
        // All roles added to the current session. Mapped by name
        var roles: MutableMap<String, EditSessionRole> = mutableMapOf(),
    )

    private data class EditSessionRole(
        var name: String,
        var roleId: Long,
        val databaseId: Long = (0..Long.MAX_VALUE).random(),
        val buttonId: String = "rb_$databaseId",
    )

    inner class StartArguments : Arguments() {
        val channel by channel(
            "target_channel",
            "The channel you want the message to be posted in",
            requireSameGuild = true
        )
        // TODO: restrict to text channel type
    }

    inner class AddRoleArguments : Arguments() {
        val role by role("role", "The role you want to add as an entry")
        val name by optionalString("name", "The display name of the button")
    }

    inner class RemoveRoleArguments : Arguments() {
        val name by string("name", "The name of the role-button pair to delete")
    }
}
