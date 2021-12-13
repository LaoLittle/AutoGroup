package org.laolittle.plugin.joinorquit.utils

import net.mamoe.mirai.contact.User
import org.laolittle.plugin.joinorquit.AutoConfig.yinLevel

object Tools {

    fun String.encodeToMiraiCode(initiative: User, passive: User): String {
        return this
            .replace("%主动%", "[mirai:at:${initiative.id}]")
            .replace("%被动%", "[mirai:at:${passive.id}]")
    }

    fun String.encodeToMiraiCode(user: User, initiative: Boolean): String {
        return if (initiative) this.replace("%主动%", "[mirai:at:${user.id}]")
        else this.replace("%被动%", "[mirai:at:${user.id}]")
    }

    fun getYinglishNode(wordChars: CharArray, part: String?): String {
        val randomOneTen = { (1..100).random() }
        var pon = ""
        if (randomOneTen() > yinLevel)
            return String(wordChars)
        if (wordChars[0] == '！' || wordChars[0] == '!')
            return "❤"
        if (wordChars[0] == '。' || wordChars[0] == '，')
            return "…"
        if (wordChars.size > 1 && randomOneTen() > 50)
            return "${wordChars[0]}…${String(wordChars)}"
        else if (part == "n" && randomOneTen() > 50) {
            repeat(wordChars.count()) { pon += "〇" }
            return pon
        }
        return "…${String(wordChars)}"
    }
}