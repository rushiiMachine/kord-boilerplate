package bot.commands

import bot.i18n
import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.createEmoji
import dev.kord.rest.Image
import dev.kord.rest.json.JsonErrorCode
import dev.kord.rest.request.KtorRequestException
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class StealExtension : Extension() {
    override val name = "steal"

    private val emojiIdRegex = Regex("(\\d{16,20})")

    override suspend fun setup() {
        publicSlashCommand(::StealArguments) {
            name = "steal"
            description = "Clone an emoji or create from url."
            requireBotPermissions(Permission.ManageEmojis)

            check {
                anyGuild()
                hasPermission(Permission.ManageEmojis)
            }

            action {
                respond {
                    val guild = guild!!.asGuild()
                    val author = user.asUser()
                    val ext = if (arguments.animated) "gif" else "png"

                    // Create emoji from an existing
                    if (arguments.emoji != null) {
                        // Parse emoji argument to a long id
                        val emojiId = emojiIdRegex.find(arguments.emoji!!)?.groups?.first()?.value?.toLongOrNull()
                            ?: throw DiscordRelayedException(i18n("bot.steal.errors.invalidEmoji"))

                        // Fetch image
                        val image = try {
                            Image.fromUrl(HttpClient(), "https://cdn.discordapp.com/emojis/$emojiId.$ext")
                        } catch (e: ClientRequestException) {
                            if (e.response.status == HttpStatusCode.NotFound)
                                throw DiscordRelayedException(i18n("bot.steal.errors.invalidEmoji"))
                            else throw e
                        }

                        // Create emoji
                        val emoji = try {
                            guild.createEmoji(arguments.name, image) {
                                reason = i18n("bot.steal.reason", author.tag, author.id.value)
                            }
                        } catch (e: KtorRequestException) {
                            if (e.error?.code?.code == 30008)
                                throw DiscordRelayedException(i18n("bot.steal.errors.maxEmojis"))
                            throw e
                        }

                        content = emoji.mention
                    }

                    // Create emoji from url
                    else if (arguments.url != null) {
                        // Fetch image
                        val imageRequest: HttpStatement = HttpClient().get(arguments.url!!)
                        val imageBytes: ByteArray = try {
                            imageRequest.receive()
                        } catch (t: Throwable) {
                            throw DiscordRelayedException(i18n("bot.steal.errors.invalidUrl"))
                        }

                        // Resize image if too big
                        // TODO: preserve gif animation
                        val newBytes = if (imageBytes.size <= 256000) imageBytes
                        else {
                            val image = ImageIO.read(ByteArrayInputStream(imageBytes))
                            val newImage = BufferedImage(128, 128, image.type)
                            val graphics = newImage.createGraphics()
                            val transform = AffineTransform.getScaleInstance(128.0 / image.width, 128.0 / image.height)
                            graphics.drawRenderedImage(image, transform)

                            val outStream = ByteArrayOutputStream()
                            ImageIO.write(newImage, ext, outStream)
                            outStream.toByteArray()
                        }

                        // Create emoji
                        val emoji = try {
                            val format = if (arguments.animated) Image.Format.GIF else Image.Format.PNG

                            guild.createEmoji(arguments.name, Image.raw(newBytes, format)) {
                                reason = i18n("bot.steal.reason", author.tag, author.id.value)
                            }
                        } catch (e: KtorRequestException) {
                            if (e.error?.code == JsonErrorCode.MaxEmojis || e.error?.code == JsonErrorCode.MaxAnimatedEmojis)
                                throw DiscordRelayedException(i18n("bot.steal.errors.maxEmojis"))
                            throw e
                        }

                        content = emoji.mention
                    }

                    // No arguments were supplied
                    else throw DiscordRelayedException(i18n("bot.steal.errors.noArguments"))
                }
            }
        }
    }

    inner class StealArguments : Arguments() {
        val name by string {
            name = "name"
            description = "The name for the cloned emoji"
            validate {
                if (value.length < 2 || value.length > 32)
                    throw DiscordRelayedException(i18n("bot.steal.errors.nameLength"))
            }
        }
        val animated by defaultingBoolean {
            name = "animated"
            description = "Whether to create this emoji as animated (tldr; gif support)"
            defaultValue = false
        }

        val emoji by optionalString {
            name = "emoji"
            description = "Option 1: existing emoji / emoji id"
        }
        val url by optionalString {
            name = "url"
            description = "Option 2: Target emoji url"
        }
    }
}
