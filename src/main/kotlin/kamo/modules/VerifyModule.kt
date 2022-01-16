package kamo.modules

import com.kotlindiscord.kord.extensions.checks.isNotBot
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.respond
import dev.kord.common.Color
import dev.kord.common.entity.ChannelType
import dev.kord.core.Kord
import dev.kord.core.any
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.Role
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.request.KtorRequestException
import kamo.Kamo
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.litote.kmongo.eq
import org.litote.kmongo.setTo
import skytils.hylin.extension.nonDashedString

object VerifyModule : Extension() {
    override val name: String = "verification"
    private val usernameRegex = Regex("^\\w{3,16}\$")
    private val userCollection = Kamo.mongo.getDatabase("Users").getCollection<Obj>("Users")

    override suspend fun setup() {
        event<MessageCreateEvent> {
            check {
                isNotBot()
                failIfNot {
                    event.getGuild()?.let { guild ->
                        guild.channels.filter {
                            it.type == ChannelType.GuildText &&
                                    it.name.contains("verify", ignoreCase = true)
                        }.any { channel ->
                            channel == event.message.channel
                        }
                    } ?: false
                }
                failIf {
                    event.member?.roles?.any { it.name == "Unverified" } == false
                }
            }

            action {
                event.message.author?.let { verify(event.message, event.message.content, it) }
            }

        }
    }

    private suspend fun verify(message: Message, username: String, user: User) {
        if (!usernameRegex.matches(username)) {
            message.respond {
                embed {
                    description = "<:error:741896426052648981> Invalid username!"
                    color = Color(0xff0033)
                }
            }
            return
        }
        Kamo.client.getKoin().get<Kord>().launch {
            try {
                Kamo.hylin.getPlayerSync(username).let { player ->
                    val discord = player.socials.discord
                    if (discord != user.tag) {
                        message.respond {
                            embed {
                                description = "<:error:741896426052648981> Your discord doesn't match the linked one (${discord ?: "None Linked"})!\n" +
                                        "Please follow the gif below to link your discord!"
                                color = Color(0xff0033)
                                if (discord == null) {
                                    image = "https://i.giphy.com/H98mSLt6J9F2hgqHEa.gif"
                                }
                            }
                        }
                    } else {
                        // update mongo
                        userCollection
                            .findOne(Obj::discord eq user.id.toString())?.let {
                                userCollection.updateOne(Obj::discord eq user.id.toString(), Obj(user.id.toString(), player.uuid.nonDashedString()))
                            } ?: userCollection.insertOne(Obj(user.id.toString(), player.uuid.nonDashedString()))
                        var success = true
                        // change nickname
                        try {
                            message.getAuthorAsMember()?.edit {
                                nickname = player.name
                                reason = "Verification"
                            } ?: message.respond {
                                embed {
                                    description = "<:error:741896426052648981> Something went wrong!"
                                    color = Color(0xff0033)
                                }
                            }
                        } catch (e: KtorRequestException) {
                            message.respond {
                                embed {
                                    description = "<:error:741896426052648981> I don't have sufficient nickname permissions!"
                                    color = Color(0xff0033)
                                }
                            }
                            success = false
                        }
                        // role stuff
                        try {
                            // find verified role
                            message.getGuild().roles.firstOrNull { it.name == "Hypixel Verified" }?.run {
                                // add role
                                addVerifiedRole(this, message, success)
                            } ?: message.getGuild().createRole { // create role if it doesn't exist
                                reason = "Create role for verified users"
                                name = "Hypixel Verified"
                                color = Color(0x77b6b6)
                            }.run {
                                // add role
                                addVerifiedRole(this, message, success)
                            }
                        } catch (e: KtorRequestException) {
                            message.respond {
                                embed {
                                    description = "<:error:741896426052648981> I don't have sufficient role permissions!"
                                    color = Color(0xff0033)
                                }
                            }
                            return@launch
                        }
                    }
                }
            } catch (e: Exception) {
                launch {
                    e.printStackTrace()
                    message.respond {
                        embed {
                            description = "<:error:741896426052648981> Something went wrong!"
                            color = Color(0xff0033)
                        }
                    }
                }
            }
        }

    }

    private suspend fun addVerifiedRole(role: Role, message: Message, success: Boolean) {
        try {
            message.getAuthorAsMember()?.addRole(role.id, "Verification") ?: message.respond {
                embed {
                    description = "<:error:741896426052648981> Something went wrong!"
                    color = Color(0xff0033)
                }
            }
            message.getGuild().roles.firstOrNull { it.name == "Unverified" }?.let { unv ->
                message.getAuthorAsMember()?.removeRole(unv.id, "Verification") ?: message.respond {
                    embed {
                        description = "<:error:741896426052648981> Something went wrong!"
                        color = Color(0xff0033)
                    }
                }
            }
            if (success) {
                message.respond {
                    embed {
                        description = "<a:verified:741883408728457257> You're all good to go!"
                        color = Color(0x1da1f2)
                    }
                }
            }
        } catch (e: KtorRequestException) {
            message.respond {
                embed {
                    description = "<:error:741896426052648981> I couldn't add the role to you! Perhaps I don't have sufficient permissions?"
                    color = Color(0xff0033)
                }
            }
        }

    }

    @Serializable
    class Obj(
        val discord: String,
        val uuid: String,
        val alias: String? = null
    )

}