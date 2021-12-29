package org.laolittle.plugin.joinorquit.utils

import com.huaban.analysis.jieba.JiebaSegmenter
import com.huaban.analysis.jieba.WordDictionary
import net.mamoe.mirai.contact.AudioSupported
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.OfflineAudio
import net.mamoe.mirai.message.data.toMessageChain
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

    fun getYinglishNode(wordChars: String, part: String?): String {
        val randomOneTen = { (1..100).random() }
        var pon = ""
        if (randomOneTen() > yinLevel)
            return wordChars
        if (wordChars[0] == '！' || wordChars[0] == '!')
            return "❤"
        if (wordChars[0] == '。' || wordChars[0] == '，')
            return "…"
        if (wordChars.length > 1 && randomOneTen() > 50)
            return "${wordChars[0]}…$wordChars"
        else if (part == "n" && randomOneTen() > 50) {
            repeat(wordChars.count()) { pon += "〇" }
            return pon
        }
        return "…$wordChars"
    }

    fun Message.reduplicate(): MessageChain {
        val miraiCode = toMessageChain().serializeToMiraiCode()
        val foo = JiebaSegmenter().process(miraiCode, JiebaSegmenter.SegMode.SEARCH)
        val reduplicate = StringBuffer()
        val random = { (1..10).random() }
        foo.forEach { keyWord ->
            val part = WordDictionary.getInstance().parts[keyWord.word]
            val word = keyWord.word
            val bar = if (random() > 7) {
                if ((part == "n") && word.length == 2)
                    if (random() > 3)
                        "$word${word[1]}"
                    else "${word[1]}${word[1]}"
                else word
            } else word
            reduplicate.append(bar)
        }
        return reduplicate.toString().deserializeMiraiCode()
    }
}