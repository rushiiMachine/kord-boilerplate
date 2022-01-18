package bot

import bot.commands.*
import bot.database.BlockedAttachmentsTable
import bot.database.RoleButtonRecord
import bot.events.MemberLogExtension
import bot.events.ReadyExtension
import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.cache.map.MapLikeCollection
import dev.kord.cache.map.internal.MapEntryCache
import dev.kord.cache.map.lruLinkedHashMap
import dev.kord.core.kordLogger
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.github.cdimascio.dotenv.dotenv
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import kotlin.system.exitProcess

@OptIn(PrivilegedIntent::class)
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun main() {
    // Load env variables
    val env = File(".env")
    if (!env.exists()) {
        val defaultEnvUri = BanExtension::class.java.classLoader.getResource("config/.env.default")
            ?: error("Failed to load default config from jar")
        env.writeText(defaultEnvUri.readText())

        println(".env Config file missing, generating config and exiting. Please fill out the configuration and restart the application.")
        exitProcess(1)
    }

    dotenv {
        systemProperties = true
    }

    val database = Database.connect("jdbc:sqlite:./data.db")
    transaction {
        SchemaUtils.createMissingTablesAndColumns(
            RoleButtonRecord,
            BlockedAttachmentsTable
        )
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
            add(::StealExtension)
            add(::UnbanExtension)
            add(::RoleButtonsExtension)
            add(::AntiAttachmentsExtension)

            add(::MemberLogExtension)
            add(::ReadyExtension)
        }

        kord {
            cache {
                users { cache, description ->
                    MapEntryCache(cache, description, MapLikeCollection.lruLinkedHashMap(maxSize = 50))
                }
                members { cache, description ->
                    MapEntryCache(cache, description, MapLikeCollection.lruLinkedHashMap(maxSize = 50))
                }
                emojis { cache, description ->
                    MapEntryCache(cache, description, MapLikeCollection.none())
                }
                channels { cache, description ->
                    MapEntryCache(cache, description, MapLikeCollection.lruLinkedHashMap(maxSize = 200))
                }
                roles { cache, description ->
                    MapEntryCache(cache, description, MapLikeCollection.lruLinkedHashMap(maxSize = 50))
                }
            }
        }
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        kordLogger.info("Shutting down...")
        database.connector().close()
    })

    bot.start()
}
