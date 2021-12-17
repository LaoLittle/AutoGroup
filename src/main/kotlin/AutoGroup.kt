package org.laolittle.plugin.joinorquit

import com.huaban.analysis.jieba.JiebaSegmenter
import com.huaban.analysis.jieba.WordDictionary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.console.plugin.jvm.AbstractJvmPlugin
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.MiraiExperimentalApi
import net.mamoe.mirai.utils.error
import net.mamoe.mirai.utils.info
import org.laolittle.plugin.joinorquit.AutoConfig.allowRejoinRoulette
import org.laolittle.plugin.joinorquit.AutoConfig.botMutedMessage
import org.laolittle.plugin.joinorquit.AutoConfig.botOperatedMuteMessage
import org.laolittle.plugin.joinorquit.AutoConfig.botOperatedUnmuteMessage
import org.laolittle.plugin.joinorquit.AutoConfig.botUnmuteMessage
import org.laolittle.plugin.joinorquit.AutoConfig.counterNudge
import org.laolittle.plugin.joinorquit.AutoConfig.counterNudgeCompleteMessage
import org.laolittle.plugin.joinorquit.AutoConfig.counterNudgeMessage
import org.laolittle.plugin.joinorquit.AutoConfig.groupMuteAllRelease
import org.laolittle.plugin.joinorquit.AutoConfig.kickMessage
import org.laolittle.plugin.joinorquit.AutoConfig.maxPlayer
import org.laolittle.plugin.joinorquit.AutoConfig.memberMutedMessage
import org.laolittle.plugin.joinorquit.AutoConfig.memberUnmuteMessage
import org.laolittle.plugin.joinorquit.AutoConfig.newMemberJoinMessage
import org.laolittle.plugin.joinorquit.AutoConfig.newMemberJoinPat
import org.laolittle.plugin.joinorquit.AutoConfig.nudgeMin
import org.laolittle.plugin.joinorquit.AutoConfig.nudgedReply
import org.laolittle.plugin.joinorquit.AutoConfig.quitMessage
import org.laolittle.plugin.joinorquit.AutoConfig.repeatSec
import org.laolittle.plugin.joinorquit.AutoConfig.roulette
import org.laolittle.plugin.joinorquit.AutoConfig.rouletteOutMessage
import org.laolittle.plugin.joinorquit.AutoConfig.rouletteOutMuteRange
import org.laolittle.plugin.joinorquit.AutoConfig.superNudge
import org.laolittle.plugin.joinorquit.AutoConfig.superNudgeMessage
import org.laolittle.plugin.joinorquit.AutoConfig.superNudgeTimes
import org.laolittle.plugin.joinorquit.AutoConfig.tenkiNiNokoSaReTaKo
import org.laolittle.plugin.joinorquit.AutoConfig.yinglishCommand
import org.laolittle.plugin.joinorquit.command.Zuan
import org.laolittle.plugin.joinorquit.model.CacheClear
import org.laolittle.plugin.joinorquit.model.PatPatTool.getPat
import org.laolittle.plugin.joinorquit.utils.NumberUtil.intConvertToChs
import org.laolittle.plugin.joinorquit.utils.Tools.encodeImageToMiraiCode
import org.laolittle.plugin.joinorquit.utils.Tools.encodeToAudio
import org.laolittle.plugin.joinorquit.utils.Tools.encodeToMiraiCode
import org.laolittle.plugin.joinorquit.utils.Tools.getYinglishNode
import java.io.File
import java.time.LocalDateTime
import java.util.*

object AutoGroup : KotlinPlugin(
    JvmPluginDescription(
        id = "org.laolittle.plugin.AutoGroup",
        version = "1.9.5",
        name = "AutoGroup"
    ) {
        author("LaoLittle")
        info("折磨群友")
    }
) {
    @OptIn(MiraiExperimentalApi::class)
    override fun onEnable() {
        val osName = System.getProperties().getProperty("os.name")
        if (!osName.startsWith("Windows")) System.setProperty("java.awt.headless", "true")
        AutoConfig.reload()
        Zuan.register()
        val cacheClear = CacheClear()
        Timer().schedule(cacheClear, Date(), 60 * 30 * 1000)
        val lastMessage: MutableMap<Long, String> = mutableMapOf()
        val onEnable: MutableSet<Long> = mutableSetOf()
        val onYinable: MutableSet<Long> = mutableSetOf()
        val rouletteData: MutableMap<Group, MutableSet<User>> = mutableMapOf()
        val nudgePerm = this.registerPermission(
            "timer.nudge",
            if (nudgeMin > 0) "每隔${nudgeMin}分钟戳一戳" else "配置文件错误，请前往配置文件设置nudgeMin大于0"
        )

        logger.info { "开始折磨群友" }

        GlobalEventChannel.filter { nudgeMin > 0 }.subscribeOnce<BotOnlineEvent> {
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

        GlobalEventChannel.filter { newMemberJoinMessage.isNotEmpty() }.subscribeAlways<MemberJoinEvent> {
            group.sendMessage(newMemberJoinMessage.random())
            if (newMemberJoinPat) {
                getPat(member, 80)
                group.sendImage(File("$dataFolder/tmp").resolve("${member.id}_pat.gif"))
            }
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
            .subscribeAlways<MemberMuteEvent>(
                priority = EventPriority.LOWEST
            ) {
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
            .subscribeAlways<MemberUnmuteEvent>(
                priority = EventPriority.LOWEST
            ) {
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
                when ((1..100).random()) {
                    in 1..counterNudge -> {
                        when ((1..100).random()) {
                            in 1..superNudge -> {
                                repeat(superNudgeTimes) {
                                    try {
                                        if (!from.nudge().sendTo(subject)) {
                                            subject.sendMessage(PokeMessage.ChuoYiChuo)
                                            return@repeat
                                        }
                                    } catch (e: UnsupportedOperationException) {
                                        subject.sendMessage(PokeMessage.ChuoYiChuo)
                                        return@repeat
                                    }
                                }
                                delay(1000)
                                subject.sendMessage(superNudgeMessage)
                            }
                            else -> {
                                if (counterNudgeMessage.isNotEmpty())
                                    subject.sendMessage(counterNudgeMessage.random())
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
                                delay(1000)
                                if (counterNudgeCompleteMessage.isNotEmpty()) subject.sendMessage(
                                    counterNudgeCompleteMessage.random()
                                )
                            }
                        }
                    }
                    else -> {
                        val nudgedPerReply = nudgedReply.random()
                        if (nudgedPerReply.contains("%声")) {
                            nudgedPerReply.encodeToAudio(subject as AudioSupported).sendTo(subject)
                        } else
                            subject.sendMessage(
                                nudgedReply.random().encodeImageToMiraiCode(subject).deserializeMiraiCode()
                            )
                    }
                }
            }
        }

        GlobalEventChannel.filter { repeatSec >= 0 }.subscribeAlways<GroupMessageEvent> {
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
         * inall 总之就是假消息
         * yinglish 淫语翻译姬！✩
         * 天弃之子 随机禁言
         * party ( 暂未做完 ) 派对模式！
         * Roulette 轮盘赌注
         * */
        GlobalEventChannel.subscribeGroupMessages(
            priority = EventPriority.LOW
        ) {
            startsWith("allinall") { msg ->
                if (msg == "") return@startsWith
                val memberFake = buildForwardMessage {
                    when (subject.members.size) {
                        in 1..100 -> {
                            subject.members.forEach {
                                add(it, PlainText(msg))
                            }
                        }
                        else -> {
                            val members = mutableSetOf<Member>()
                            while (members.size < 100) {
                                members.add(subject.members.random())
                            }
                            members.forEach {
                                add(it, PlainText(msg))
                            }
                        }
                    }
                }
                subject.sendMessage(memberFake)
            }

            startsWith("randominall") {
                if (it == "") return@startsWith
                val msg = buildForwardMessage {
                    val randomMember = subject.members.random()
                    add(randomMember, PlainText(it))
                }
                subject.sendMessage(msg)
            }

            // [mirai:at:123]
            startsWith("oneinall") {
                if (it == "") return@startsWith
                val miraiCode = message.serializeToMiraiCode()
                val fakeMessage = miraiCode.substring(miraiCode.indexOf("]") + 1)
                if (fakeMessage == "") return@startsWith
                val miraiCodeBegin = "[mirai:at:"
                val targetId = miraiCode.substring(
                    miraiCode.indexOf(miraiCodeBegin) + miraiCodeBegin.length,
                    miraiCode.indexOf("]")
                )

                subject.sendMessage(buildForwardMessage {
                    subject[targetId.toLong()]?.let { target -> add(target, PlainText(fakeMessage)) }
                })
            }

            yinglishCommand {
                if (onYinable.contains(sender.id)) return@yinglishCommand
                onYinable.add(sender.id)
                val promMsg = subject.sendMessage("请输入要翻译的内容哦 ✩")
                this@AutoGroup.launch {
                    selectMessages {
                        default {
                            // val a = TFIDFAnalyzer().analyze(it, 100)
                            val foo = JiebaSegmenter().process(it, JiebaSegmenter.SegMode.SEARCH)
                            val yinglish = StringBuffer()

                            foo.forEach { keyWord ->
                                val part = WordDictionary.getInstance().parts[keyWord.word]
                                val chars = keyWord.word.toCharArray()
                                yinglish.append(getYinglishNode(chars, part))
                            }

                            subject.sendMessage(yinglish.toString())
                            Unit
                        }
                        timeout(25_000) {
                            subject.sendMessage("太久了哦 ✩")
                            Unit
                        }
                    }
                    promMsg.recall()
                    onYinable.remove(sender.id)
                }
            }

            tenkiNiNokoSaReTaKo {
                if (!sender.isOperator()) {
                    subject.sendMessage("不是管理员不能选出天弃之子呢")
                    return@tenkiNiNokoSaReTaKo
                }
                if (!group.botPermission.isOperator()) {
                    subject.sendMessage("呜呜，我不是管理员，没法选出天弃之子")
                    logger.error(PermissionDeniedException("呜呜，没权限"))
                    return@tenkiNiNokoSaReTaKo
                }
                subject.sendMessage("看看天弃之子！")
                delay(3000)
                subject.members.random().mute((1..100).random())
            }

            roulette {
                if (rouletteData.keys.contains(subject)) {
                    subject.sendMessage("本群已经开启了一个赌局！")
                    return@roulette
                }
                rouletteData[subject] = mutableSetOf()
                val rouGroup = subject
                subject.sendMessage(buildMessageChain {
                    add("请")
                    add(At(sender))
                    add(" 输入要装填的弹药量")
                })
                var bulletNum = 1
                whileSelectMessages {
                    default { msg ->
                        if (Regex("""\D""").containsMatchIn(msg))
                            subject.sendMessage("请输入数字 !")
                        else if ((msg.toInt() > maxPlayer) or (msg.toInt() <= 0))
                            subject.sendMessage("请输入正确的数字 !")
                        else {
                            bulletNum = msg.toInt()
                            return@default false
                        }
                        true
                    }
                    timeout(10_000) {
                        subject.sendMessage("太久没装弹了，似乎只装入了一颗呢")
                        false
                    }
                }

                subject.sendMessage(
                    """
                        |现在有一把 "封口枪"
                        |里面${intConvertToChs(maxPlayer)}个弹槽装填了${intConvertToChs(bulletNum)}发子弹
                        |群员可以发送 "s" 对自己开枪
                        |被 "禁言子弹" 击中的群员将获得随机禁言套餐！
                    """.trimMargin()
                )
                var i = 0
                var delayTimes = 0

                class Roulette : TimerTask() {
                    override fun run() {
                        this@AutoGroup.launch {
                            delayTimes++
                            if (delayTimes >= 2) {
                                subject.sendMessage(
                                    """
                           许久没有人动那把枪了
                           枪的色泽逐渐暗淡
                        """.trimIndent()
                                )
                                rouletteData.remove(subject)
                                when ((1..4).random()) {
                                    2 -> {
                                        val luckyDog = subject.members.random()
                                        subject.sendMessage(rouletteOutMessage.random())
                                        subject.sendMessage("枪走火了！ ${luckyDog.nameCardOrNick} 中枪了！")
                                        try {
                                            luckyDog.mute(30)
                                            intercept()
                                        } catch (e: PermissionDeniedException) {
                                            logger.error { "禁言失败！权限不足" }
                                        }
                                    }
                                }
                                cancel()
                            }
                        }
                    }
                }

                var calc = Roulette()

                Timer().schedule(calc, Date(), 120_000)

                val bullets = mutableSetOf<Int>()
                while (bullets.size < bulletNum)
                    bullets.add((1..maxPlayer).random())
                val lastBullet = Collections.max(bullets)
                GlobalEventChannel.subscribe<GroupMessageEvent> {
                    if (this.subject == rouGroup) {
                        if (message.content == "s" && rouletteData[subject]?.contains(sender) == false) {
                            i++
                            delay(3000)
                            when {
                                bullets.contains(i) -> {
                                    subject.sendMessage(rouletteOutMessage.random())
                                    try {
                                        sender.mute((1..rouletteOutMuteRange).random())
                                        intercept()
                                    } catch (e: PermissionDeniedException) {
                                        subject.sendMessage("可惜我没法禁言呢")
                                    } catch (e: IllegalStateException) {
                                        sender.mute(30)
                                        logger.error { "禁言时间异常！$e" }
                                    }
                                    if (i >= lastBullet) {
                                        rouletteData.remove(subject)
                                        if (bulletNum >= 1)
                                            subject.sendMessage("枪里的子弹全部射完了...本次赌局自动结束")
                                        calc.cancel()
                                        return@subscribe ListeningStatus.STOPPED
                                    }
                                }
                                else -> {
                                    // 囸
                                    if (!allowRejoinRoulette)
                                        rouletteData[subject]?.add(sender)
                                    delayTimes = 0
                                    calc.cancel()
                                    calc = Roulette()
                                    Timer().schedule(calc, Date(), 120_000)
                                    subject.sendMessage(AutoConfig.roulettePassedMessage.random())
                                }
                            }
                        }
                    }
                    ListeningStatus.LISTENING
                }
            }

            "party" {
                repeat(10) {
                    sender.nudge().sendTo(subject)
                }
            }
        }

        GlobalEventChannel.subscribeFriendMessages {
            /*   "心灵控制" {
                     subject.sendMessage("请发送你需要转换的聊天记录")
                     whileSelectMessages {
                         default {
                             if (message.toForwardMessage() is ForwardMessage)
                                 true
                             true
                         }
                     }
                 }

             */

        }
    }

    override fun onDisable() {
        logger.info { "让他们休息会" }
    }

    private fun AbstractJvmPlugin.registerPermission(name: String, description: String): Permission {
        return PermissionService.INSTANCE.register(permissionId(name), description, parentPermission)
    }
}