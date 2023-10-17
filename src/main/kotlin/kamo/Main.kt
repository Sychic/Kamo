package kamo

import java.io.FileInputStream
import java.util.*


val properties = Properties().also {
    it.load(FileInputStream("/opt/kamo/config"))
}

suspend fun main(args: Array<String>) {
    val scanner = Scanner(System.`in`)
    Kamo.init(
        properties.getProperty("token"),
        properties.getProperty("mongo"),
        properties.getProperty("hypixel")
    )
}