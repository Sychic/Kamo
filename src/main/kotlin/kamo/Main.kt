package kamo

import java.util.*


suspend fun main(args: Array<String>) {
    val scanner = Scanner(System.`in`)
    Kamo.init(System.getProperty("kamo.token") ?:
        run {
            println("Please input token")
            scanner.nextLine()
        },
        System.getProperty("kamo.mongo") ?:
        run {
            println("Please input mongo link")
            scanner.nextLine()
        },
        System.getProperty("kamo.api") ?:
        run {
            println("Please input hypixel api key")
            scanner.nextLine()
        }
    )
}