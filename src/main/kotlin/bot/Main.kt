package bot

import bot.commands.BanExtension
import bot.commands.KickExtension
import bot.events.ReadyExtension
import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.gateway.Intent
import io.github.cdimascio.dotenv.dotenv
import java.io.File
import kotlin.system.exitProcess

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun main() {
    val env = File(".env")
    if (!env.exists()) {
        val defaultConfigUri = BanExtension::class.java.classLoader.getResource("config/.env.default")
            ?: throw Error("Failed to load default config from jar")
        env.writeText(defaultConfigUri.readText())

        println("Config file missing, generating config and exiting...")
        exitProcess(1)
    }

    dotenv {
        systemProperties = true
    }

    val bot = ExtensibleBot(System.getProperty("TOKEN")) {
        intents {
            +Intent.Guilds
        }

        extensions {
            add(::KickExtension)
            add(::BanExtension)

            add(::ReadyExtension)
        }

//        applicationCommandsBuilder.defaultGuild(Snowflake(676284863967526928L))
    }


    try {
        bot.start()
    } catch (e: Throwable) {
        println("Failed to start bot:")
        error(e)
    }
}
