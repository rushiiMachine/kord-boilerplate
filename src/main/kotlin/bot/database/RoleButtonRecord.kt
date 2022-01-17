package bot.database

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object RoleButtonRecord : Table() {
    val buttonId: Column<Long> = long("button_id")
    val targetRoleId: Column<Long> = long("role_id")
    val messageId: Column<Long> = long("message_id")
    val channelId: Column<Long> = long("channel_id")
    val guildId: Column<Long> = long("guild_id")

    override val primaryKey = PrimaryKey(buttonId)
}
