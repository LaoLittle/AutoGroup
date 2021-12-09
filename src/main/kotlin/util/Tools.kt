package org.laolittle.plugin.joinorquit.util

import net.mamoe.mirai.contact.User

object Tools {

    fun String.encodeToMiraiCode(initiative: User, passive: User): String {
        return this
            .replace("%主动%", "[mirai:at:${initiative.id}]")
            .replace("%被动%", "[mirai:at:${passive.id}]")
    }

    fun String.encodeToMiraiCode(user: User, initiative: Boolean): String {
        return if (initiative) this
            .replace("%主动%", "[mirai:at:${user.id}]")
        else this.replace("%被动%", "[mirai:at:${user.id}]")
    }

    fun getYinglish(chars: CharArray, yingLev: Int, part: String?): String {
        val randomOneTen = { (1..10).random() }
        var O = ""
        if (randomOneTen() > yingLev)
            return String(chars)
        if (chars[0] == '！' || chars[0] == '!')
            return "❤"
        if (chars[0] == '。' || chars[0] == '，')
            return "......"
        if (chars.size > 1 && randomOneTen() > 5)
            return "${chars[0]}......${String(chars)}"
        else if (part == "n" && randomOneTen() > 5 ) {
            repeat(chars.count()) { O += "〇" }
            return O
        }
        return "......${String(chars)}"
    }
}