package org.laolittle.plugin.joinorquit

import com.huaban.analysis.jieba.JiebaSegmenter
import com.huaban.analysis.jieba.WordDictionary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import net.mamoe.mirai.console.command.descriptor.ExperimentalCommandDescriptors
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.console.plugin.jvm.AbstractJvmPlugin
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.contact.isAdministrator
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.contact.isOwner
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.event.selectMessages
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.PokeMessage
import net.mamoe.mirai.message.data.buildForwardMessage
import net.mamoe.mirai.utils.MiraiExperimentalApi
import net.mamoe.mirai.utils.info
import org.laolittle.plugin.joinorquit.AutoConfig.botMutedMessage
import org.laolittle.plugin.joinorquit.AutoConfig.botOperatedMuteMessage
import org.laolittle.plugin.joinorquit.AutoConfig.botOperatedUnmuteMessage
import org.laolittle.plugin.joinorquit.AutoConfig.botUnmuteMessage
import org.laolittle.plugin.joinorquit.AutoConfig.counterNudge
import org.laolittle.plugin.joinorquit.AutoConfig.counterNudgeMessage
import org.laolittle.plugin.joinorquit.AutoConfig.groupMuteAllRelease
import org.laolittle.plugin.joinorquit.AutoConfig.kickMessage
import org.laolittle.plugin.joinorquit.AutoConfig.memberMutedMessage
import org.laolittle.plugin.joinorquit.AutoConfig.memberUnmuteMessage
import org.laolittle.plugin.joinorquit.AutoConfig.nudgeMin
import org.laolittle.plugin.joinorquit.AutoConfig.nudgedReply
import org.laolittle.plugin.joinorquit.AutoConfig.quitMessage
import org.laolittle.plugin.joinorquit.AutoConfig.repeatSec
import org.laolittle.plugin.joinorquit.AutoConfig.superNudge
import org.laolittle.plugin.joinorquit.AutoConfig.superNudgeMessage
import org.laolittle.plugin.joinorquit.AutoConfig.superNudgeTimes
import org.laolittle.plugin.joinorquit.AutoConfig.yinglishCommand
import org.laolittle.plugin.joinorquit.model.CacheClear
import org.laolittle.plugin.joinorquit.model.getPat
import org.laolittle.plugin.joinorquit.util.Tools
import org.laolittle.plugin.joinorquit.util.Tools.encodeToMiraiCode
import java.io.File
import java.time.LocalDateTime
import java.util.*

@ExperimentalSerializationApi
@ConsoleExperimentalApi
@ExperimentalCommandDescriptors
@MiraiExperimentalApi
object AutoGroup : KotlinPlugin(
    JvmPluginDescription(
        id = "org.laolittle.plugin.AutoGroup",
        version = "1.2.1",
        name = "AutoGroup"
    ) {
        author("LaoLittle")
        info("折磨群友")
    }
) {
    override fun onEnable() {
        val osName = System.getProperties().getProperty("os.name")
        if (!osName.startsWith("Windows")) System.setProperty("java.awt.headless", "true")
        AutoConfig.reload()
        val lastMessage: MutableMap<Long, String> = mutableMapOf()
        val onEnable: MutableSet<Long> = mutableSetOf()
        val onYinable: MutableSet<Long> = mutableSetOf()
        val nudgePerm = this.registerPermission("timer.nudge", "每隔${nudgeMin}分钟戳一戳")

        logger.info { "开始折磨群友" }

        GlobalEventChannel.subscribeOnce<BotOnlineEvent> {
            val nudgeTimer = object : TimerTask() {
                override fun run() {
                    this@AutoGroup.launch {
                        val nowHour = LocalDateTime.now().hour
                        if (nowHour !in 0..8 && nowHour !in 22..23) {
                            logger.info { "开戳" }
                            bot.groups.filter { nudgePerm.testPermission(it.permitteeId) }.forEach {
                                val victim = it.members.random()
                                delay(3000)
                                victim.nudge().sendTo(it)
                            }
                        }
                    }
                }
            }

            Timer().schedule(nudgeTimer, Date(), nudgeMin * 60 * 1000)
        }

        GlobalEventChannel.subscribeAlways<GroupTalkativeChangeEvent> {
            if (previous.id == bot.id) {
                group.sendMessage("我的龙王被抢走了...")
                delay(2000)
                group.sendMessage(PlainText("呜呜呜...").plus(At(now)).plus(PlainText(" 你还我龙王！！！")))
                delay(3000)
                now.sendMessage("还给我还给我还给我还给我还给我")
            } else group.sendMessage(At(previous) + PlainText(" 的龙王被") + At(now) + PlainText(" 抢走了，好可怜"))
        }

        GlobalEventChannel.subscribeAlways<MemberJoinEvent> {
            group.sendMessage("欢淫")
            getPat(member, 80)
            group.sendImage(File("$dataFolder/tmp").resolve("${member.id}_pat.gif"))
        }

        GlobalEventChannel.filter { kickMessage.isNotEmpty() }.subscribeAlways<MemberLeaveEvent.Kick> {
            val msg = kickMessage.encodeToMiraiCode(operatorOrBot, member).deserializeMiraiCode()
            group.sendMessage(msg)
        }

        GlobalEventChannel.filter { quitMessage.isNotEmpty() }.subscribeAlways<MemberLeaveEvent.Quit> {
            val msg = quitMessage.encodeToMiraiCode(member, true).deserializeMiraiCode()
            group.sendMessage(msg)
        }

        GlobalEventChannel.filter { memberMutedMessage.isNotEmpty() && botOperatedMuteMessage.isNotEmpty() }
            .subscribeAlways<MemberMuteEvent> {
                val msg = if (operatorOrBot == group.botAsMember) botOperatedMuteMessage
                    .replace("%主动%", operatorOrBot.nameCardOrNick)
                    .encodeToMiraiCode(member, false)
                    .deserializeMiraiCode()
                else memberMutedMessage
                    .encodeToMiraiCode(operatorOrBot, member)
                    .deserializeMiraiCode()
                group.sendMessage(msg)
            }

        GlobalEventChannel.filter { memberUnmuteMessage.isNotEmpty() && botOperatedUnmuteMessage.isNotEmpty() }
            .subscribeAlways<MemberUnmuteEvent> {
                val msg = if (operatorOrBot == group.botAsMember) botOperatedUnmuteMessage
                    .replace("%主动%", operatorOrBot.nameCardOrNick)
                    .encodeToMiraiCode(member, false)
                    .deserializeMiraiCode()
                else memberUnmuteMessage
                    .encodeToMiraiCode(operatorOrBot, member)
                    .deserializeMiraiCode()
                group.sendMessage(msg)
            }

        GlobalEventChannel.filter { botMutedMessage.isNotEmpty() }.subscribeAlways<BotMuteEvent> {
            try {
                for (msg in botMutedMessage) {
                    operator.sendMessage(msg)
                    delay(1000)
                }
            } catch (e: Exception) {
                logger.error("$e 好像没法发送临时消息...")
            }
        }

        GlobalEventChannel.filter { botUnmuteMessage.isNotEmpty() }.subscribeAlways<BotUnmuteEvent> {
            delay(1000)
            val msg = botUnmuteMessage.encodeToMiraiCode(operator, true).deserializeMiraiCode()
            group.sendMessage(msg)
        }

        GlobalEventChannel.subscribeAlways<BotJoinGroupEvent.Invite> {
            group.sendMessage("我来啦！！！")
        }

        GlobalEventChannel.filter { groupMuteAllRelease.isNotEmpty() }.subscribeAlways<GroupMuteAllEvent> {
            if (!new) {
                val msg = groupMuteAllRelease.encodeToMiraiCode(operatorOrBot, true).deserializeMiraiCode()
                group.sendMessage(msg)
            }
        }

        GlobalEventChannel.subscribeAlways<MemberPermissionChangeEvent> {
            val msg = when {
                origin.isOwner() || new.isOwner() -> PlainText("群主变了？？？")
                origin.isAdministrator() && !new.isOperator() -> At(member).plus(PlainText(" 的管理没了，好可惜"))
                else -> At(member).plus(PlainText(" 升职啦！"))
            }
            group.sendMessage(msg)
        }

        GlobalEventChannel.filter { nudgedReply.isNotEmpty() }.subscribeAlways<NudgeEvent> {
            if (target == bot) {
                val msg = when ((1..100).random()) {
                    in 1..counterNudge -> {
                        when ((1..100).random()){
                            in 1..superNudge -> {
                                repeat(superNudgeTimes) {
                                    try {
                                        if (!from.nudge().sendTo(subject))
                                            subject.sendMessage(PokeMessage.ChuoYiChuo)
                                    } catch (e: UnsupportedOperationException){
                                        subject.sendMessage(PokeMessage.ChuoYiChuo)
                                    }
                                }
                                delay(1000)
                                superNudgeMessage
                            }
                            else -> {
                                if (counterNudgeMessage != "")
                                    subject.sendMessage(counterNudgeMessage)
                                delay(1000)
                                try {
                                    if (!from.nudge().sendTo(subject)) {
                                        subject.sendMessage("戳不了...那我")
                                        delay(1000)
                                        subject.sendMessage(PokeMessage.ChuoYiChuo)
                                    }
                                } catch (e: UnsupportedOperationException) {
                                    logger.info { "当前使用协议为不支持的协议，改用Poke戳一戳" }
                                    subject.sendMessage(PokeMessage.ChuoYiChuo)
                                }
                                "哼"
                            }
                        }
                    }
                    else -> nudgedReply.random()
                }
                delay(1000)
                subject.sendMessage(msg)
            }
        }

        GlobalEventChannel.filter { repeatSec > 0 }.subscribeAlways<GroupMessageEvent> {
            if (onEnable.contains(group.id)) return@subscribeAlways
            if (lastMessage[group.id] == message.serializeToMiraiCode()) {
                onEnable.add(group.id)
                subject.sendMessage(message)
                logger.info { "复读了一次" }
                this@AutoGroup.launch {
                    delay(repeatSec * 1000)
                    onEnable.remove(group.id)
                }
            }
            lastMessage[group.id] = message.serializeToMiraiCode()
        }

        /**
         * 一些小功能
         * 目前加入:
         * allinall 总之就是假消息
         * yinglish 淫语翻译姬！✩
         * */
        GlobalEventChannel.subscribeGroupMessages {
            /*
            "justtest" {
                for (i in nudgedReply)
                    subject.sendMessage(i)
            }
            */
            startsWith("allinall") {
                if (it == "") return@startsWith
                val msg = buildForwardMessage {
                    val randomMember = subject.members.random()
                    add(randomMember, PlainText(it))
                }
                    subject.sendMessage(msg)
            }

            yinglishCommand {
                if (onYinable.contains(sender.id)) return@yinglishCommand
                onYinable.add(sender.id)
                subject.sendMessage("请输入要翻译的内容哦 ✩")
                this@AutoGroup.launch {
                    selectMessages {
                        default {
                            // val a = TFIDFAnalyzer().analyze(it, 100)
                            val foo = JiebaSegmenter().process(it, JiebaSegmenter.SegMode.SEARCH)
                            var yinglish = ""

                            foo.forEach { keyWord ->
                                val part = WordDictionary.getInstance().parts[keyWord.word]
                                val chars = keyWord.word.toCharArray()
                                yinglish = yinglish.plus(Tools.getYinglishNode(chars, part))
                            }
                            subject.sendMessage(yinglish)
                            Unit
                        }
                        timeout(25_000) {
                            subject.sendMessage("太久了哦 ✩")
                            Unit
                        }
                    }
                    onYinable.remove(sender.id)
                }
            }

            "party" {
                repeat(10) {
                    sender.nudge().sendTo(subject)
                }
            }
        }

        val cacheClear = CacheClear()
        Timer().schedule(cacheClear, Date(), 60 * 30 * 1000)
    }

    override fun onDisable() {
        logger.info { "让他们休息会" }
    }

    private fun AbstractJvmPlugin.registerPermission(name: String, description: String): Permission {
        return PermissionService.INSTANCE.register(permissionId(name), description, parentPermission)
    }
}