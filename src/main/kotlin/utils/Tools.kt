package org.laolittle.plugin.joinorquit.utils

import net.mamoe.mirai.contact.AudioSupported
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.OfflineAudio
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import org.laolittle.plugin.joinorquit.AutoConfig.yinLevel
import org.laolittle.plugin.joinorquit.AutoGroup.dataFolder

object Tools {

    suspend fun String.encodeImageToMiraiCode(contact: Contact): String {
        var miraiCode = this
        while (miraiCode.contains("%图")) {
            val startIndex = miraiCode.indexOf("%图")
            val filePath = miraiCode.substring(startIndex + 2, miraiCode.indexOf("%", startIndex + 3))
            val imageCode = dataFolder.resolve(filePath).uploadAsImage(contact).serializeToMiraiCode()
            miraiCode = miraiCode.replace("%图$filePath%", imageCode)
        }
        return miraiCode
    }

    suspend fun String.encodeToAudio(contact: AudioSupported): OfflineAudio {
        val audioCode = this
        val startIndex = audioCode.indexOf("%声")
        val filePath = audioCode.substring(startIndex + 2, audioCode.indexOf("%", startIndex + 3))
        return dataFolder.resolve(filePath).toExternalResource().use { contact.uploadAudio(it) }
    }

    fun String.encodeToMiraiCode(initiative: User, passive: User): String {
        return replace("%主动%", "[mirai:at:${initiative.id}]")
            .replace("%被动%", "[mirai:at:${passive.id}]")
    }

    fun String.encodeToMiraiCode(user: User, initiative: Boolean): String {
        return if (initiative) replace("%主动%", "[mirai:at:${user.id}]")
        else replace("%被动%", "[mirai:at:${user.id}]")
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