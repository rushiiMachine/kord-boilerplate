package bot

import bot.commands.*
import bot.events.MemberLogExtension
import bot.events.ReadyExtension
import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.github.cdimascio.dotenv.dotenv
import java.io.File
import kotlin.system.exitProcess

@OptIn(PrivilegedIntent::class)
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun main() {
    val env = File(".env")
    if (!env.exists()) {
        val defaultConfigUri = BanExtension::class.java.classLoader.getResource("config/.env.default")
            ?: error("Failed to load default config from jar")
        env.writeText(defaultConfigUri.readText())

        println("Config file missing, generating config and exiting...")
        exitProcess(1)
    }

    dotenv {
        systemProperties = true
    }

    val bot = ExtensibleBot(System.getProperty("TOKEN")) {
        intents(false) {
            +Intent.Guilds
        }

        extensions {
            add(::BanExtension)
            add(::EchoExtension)
            add(::KickExtension)
            add(::PurgeExtension)
            add(::UserExtension)
            add(::SnipeExtension)

            add(::MemberLogExtension)
            add(::ReadyExtension)
        }

        // Make all slash commands guild commands
//        applicationCommandsBuilder.defaultGuild(Snowflake(676284863967526928L))
    }

    bot.start()
}
