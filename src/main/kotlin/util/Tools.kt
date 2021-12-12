package org.laolittle.plugin.joinorquit.util

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

    fun getYinglishNode(chars: CharArray, part: String?): String {
        val randomOneTen = { (1..100).random() }
        var pon = ""
        if (randomOneTen() > yinLevel)
            return String(chars)
        if (chars[0] == '！' || chars[0] == '!')
            return "❤"
        if (chars[0] == '。' || chars[0] == '，')
            return "…"
        if (chars.size > 1 && randomOneTen() > 50)
            return "${chars[0]}…${String(chars)}"
        else if (part == "n" && randomOneTen() > 50) {
            repeat(chars.count()) { pon += "〇" }
            return pon
        }
        return "…${String(chars)}"
    }
}