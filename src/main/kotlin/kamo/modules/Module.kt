package kamo.modules

import dev.kord.core.Kord
import kamo.Kamo

abstract class Module {
    abstract val name: String
    val kord = Kamo.client

    abstract suspend fun setup()
}