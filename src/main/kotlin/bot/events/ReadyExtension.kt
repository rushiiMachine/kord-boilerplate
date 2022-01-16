package bot.events

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.kordLogger
import kotlinx.coroutines.flow.toList

class ReadyExtension : Extension() {
    override val name = "ready"

    override suspend fun setup() {
        event<ReadyEvent> {
            action {
                val self = kord.getSelf()
                val count = kord.guilds.toList().size

                kordLogger.info("Logged in as ${self.tag} (${self.id.value}). Watching $count servers.")
                kord.editPresence {
                    watching("$count servers")
                }
            }
        }
    }
}
