package bot.commands

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond

class EchoExtension : Extension() {
    override val name = "echo"

    override suspend fun setup() {
        publicSlashCommand(::EchoArguments) {
            name = "echo"
            description = "Make the bot say something"

            action {
                respond { content = arguments.message }
            }
        }
    }

    inner class EchoArguments : Arguments() {
        val message by string {
            name = "message"
            description = "The message the bot should say"
        }
    }
}
