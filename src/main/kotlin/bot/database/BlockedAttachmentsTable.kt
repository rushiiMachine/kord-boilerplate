package bot.database

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object BlockedAttachmentsTable : Table() {
    val guildId: Column<Long> = long("guild_id")

    /**
     * Encoded as a comma-seperated list of blocked file extensions
     */
    val blockedExtensions: Column<String?> = text("blocked_exts").nullable()

    override val primaryKey = PrimaryKey(guildId)
}
