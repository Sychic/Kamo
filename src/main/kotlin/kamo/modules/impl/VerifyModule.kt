package kamo.modules.impl

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates
import dev.kord.common.Color
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.entity.Role
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.request.KtorRequestException
import kamo.Kamo
import kamo.modules.Module
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import skytils.hylin.extension.nonDashedString

object VerifyModule : Module() {
    override val name: String = "verification"
    private val usernameRegex = Regex("^[\\w]{3,16}\$")
    private val userCollection = Kamo.mongo.getDatabase("Users").getCollection<Obj>("Users")

    override suspend fun setup() {
        kord.on<MessageCreateEvent> {
            if (this.message.author?.isBot == true) return@on
            if (message.getChannel().data.name.value?.contains("verify", ignoreCase = true) == false) return@on
            if (message.getAuthorAsMemberOrNull()?.roles?.firstOrNull { it.name == "Unverified" } == null) return@on
            message.author?.let { verify(message, message.content, it) }
        }
    }

    private suspend fun verify(message: Message, username: String, user: User) {
        if (!usernameRegex.matches(username)) {
            message.reply {
                println(username)
                embed {
                    description = "<:error:741896426052648981> Invalid username!"
                    color = Color(0xff0033)
                }
            }
            return
        }
        try {
            Kamo.hylin.getPlayerSync(username).let { player ->
                val discord = player.socials.discord
                if (discord != user.tag) {
                    message.reply {
                        embed {
                            description = "<:error:741896426052648981> Your discord doesn't match the linked one (${discord ?: "None Linked"})!"
                            color = Color(0xff0033)
                            if (discord == null) {
                                description+= "\nPlease follow the gif below to link your discord!"
                                image = "https://i.giphy.com/H98mSLt6J9F2hgqHEa.gif"
                            }
                        }
                    }
                } else {
                    // update mongo
                    userCollection
                        .findOneAndUpdate(
                            filter = eq("discord", user.id.toString()),
                            Updates.set("uuid", player.uuid.nonDashedString())
                        ) ?: userCollection.insertOne(Obj(user.id.toString(), player.uuid.nonDashedString()))
                    var success = true
                    // change nickname
                    try {
                        message.getAuthorAsMemberOrNull()?.edit {
                            nickname = player.name
                            reason = "Verification"
                        } ?: message.reply {
                            embed {
                                description = "<:error:741896426052648981> Something went wrong!"
                                color = Color(0xff0033)
                            }
                        }
                    } catch (e: KtorRequestException) {
                        message.reply {
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
                        message.reply {
                            embed {
                                description = "<:error:741896426052648981> I don't have sufficient role permissions!"
                                color = Color(0xff0033)
                            }
                        }
                        return
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            message.reply {
                embed {
                    description = "<:error:741896426052648981> Something went wrong!"
                    color = Color(0xff0033)
                }
            }
        }

    }

    private suspend fun addVerifiedRole(role: Role, message: Message, success: Boolean) {
        try {
            message.getAuthorAsMemberOrNull()?.addRole(role.id, "Verification") ?: message.reply {
                embed {
                    description = "<:error:741896426052648981> Something went wrong!"
                    color = Color(0xff0033)
                }
            }
            message.getGuild().roles.firstOrNull { it.name == "Unverified" }?.let { unv ->
                message.getAuthorAsMemberOrNull()?.removeRole(unv.id, "Verification") ?: message.reply {
                    embed {
                        description = "<:error:741896426052648981> Something went wrong!"
                        color = Color(0xff0033)
                    }
                }
            }
            if (success) {
                message.reply {
                    embed {
                        description = "<a:verified:741883408728457257> You're all good to go!"
                        color = Color(0x1da1f2)
                    }
                }
            }
        } catch (e: KtorRequestException) {
            message.reply {
                embed {
                    description = "<:error:741896426052648981> I couldn't add the role to you! Perhaps I don't have sufficient permissions?"
                    color = Color(0xff0033)
                }
            }
        }

    }

    @Serializable
    data class Obj(
        val discord: String,
        val uuid: String,
        val alias: String? = null
    )

}