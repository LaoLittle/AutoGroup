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
import net.mamoe.mirai.message.nextMessage
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
import org.laolittle.plugin.joinorquit.AutoConfig.reduplicate
import org.laolittle.plugin.joinorquit.AutoConfig.repeatSec
import org.laolittle.plugin.joinorquit.AutoConfig.roulette
import org.laolittle.plugin.joinorquit.AutoConfig.rouletteOutMessage
import org.laolittle.plugin.joinorquit.AutoConfig.rouletteOutMuteRange
import org.laolittle.plugin.joinorquit.AutoConfig.superNudge
import org.laolittle.plugin.joinorquit.AutoConfig.superNudgeMessage
import org.laolittle.plugin.joinorquit.AutoConfig.superNudgeTimes
import org.laolittle.plugin.joinorquit.AutoConfig.tenkiNiNokoSaReTaKo
import org.laolittle.plugin.joinorquit.AutoConfig.yinglishCommand
import org.laolittle.plugin.joinorquit.GroupList.enable
import org.laolittle.plugin.joinorquit.command.AutoGroupCtr
import org.laolittle.plugin.joinorquit.command.Zuan
import org.laolittle.plugin.joinorquit.utils.NumberUtil.intConvertToChs
import org.laolittle.plugin.joinorquit.utils.Tools.encodeImageToMiraiCode
import org.laolittle.plugin.joinorquit.utils.Tools.encodeToAudio
import org.laolittle.plugin.joinorquit.utils.Tools.encodeToMiraiCode
import org.laolittle.plugin.joinorquit.utils.Tools.getYinglishNode
import org.laolittle.plugin.joinorquit.utils.Tools.reduplicate
import org.laolittle.plugin.model.PatPatTool.getPat
import java.io.File
import java.time.LocalDateTime
import java.util.*

object AutoGroup : KotlinPlugin(
    JvmPluginDescription(
        id = "org.laolittle.plugin.AutoGroup",
        version = "2.0.2",
        name = "AutoGroup"
    ) {
        author("LaoLittle")
        info("????????????")
    }
) {
    @OptIn(MiraiExperimentalApi::class)
    override fun onEnable() {
        init()
        val lastMessage: MutableMap<Long, String> = mutableMapOf()
        val onEnable: MutableSet<Long> = mutableSetOf()
        val onYinable: MutableSet<Long> = mutableSetOf()
        val rouletteData: MutableMap<Group, MutableSet<User>> = mutableMapOf()
        val nudgePerm = this.registerPermission(
            "timer.nudge",
            if (nudgeMin > 0) "??????${nudgeMin}???????????????" else "????????????????????????????????????????????????nudgeMin??????0"
        )

        logger.info { "??????????????????" }

        GlobalEventChannel.filter { nudgeMin > 0 }.subscribeOnce<BotOnlineEvent> {
            val nudgeTimer = object : TimerTask() {
                override fun run() {
                    this@AutoGroup.launch {
                        val nowHour = LocalDateTime.now().hour
                        if (nowHour !in 0..8 && nowHour !in 22..23) {
                            logger.info { "??????" }
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
            if (!group.enable()) return@subscribeAlways
            if (previous.id == bot.id) {
                group.sendMessage("????????????????????????...")
                delay(2000)
                group.sendMessage(PlainText("?????????...").plus(At(now)).plus(PlainText(" ????????????????????????")))
                delay(3000)
                now.sendMessage("?????????????????????????????????????????????")
            } else group.sendMessage(At(previous) + PlainText(" ????????????") + At(now) + PlainText(" ?????????????????????"))
        }

        GlobalEventChannel.filter { newMemberJoinMessage.isNotEmpty() }.subscribeAlways<MemberJoinEvent> {
            if (!group.enable()) return@subscribeAlways
            group.sendMessage(newMemberJoinMessage.random())
            if (newMemberJoinPat) {
                runCatching {
                    getPat(member, 60)
                    group.sendImage(File("$dataFolder").resolve("tmp").resolve("${member.id}_pat.gif"))
                }.onFailure {
                    if (it is ClassNotFoundException) logger.error { "?????????????????????PatPat, ???????????????https://mirai.mamoe.net/topic/740" }
                }
            }
        }

        GlobalEventChannel.filter { kickMessage.isNotEmpty() }.subscribeAlways<MemberLeaveEvent.Kick> {
            if (!group.enable()) return@subscribeAlways
            val msg = kickMessage.encodeToMiraiCode(operatorOrBot, member).deserializeMiraiCode()
            group.sendMessage(msg)
        }

        GlobalEventChannel.filter { quitMessage.isNotEmpty() }.subscribeAlways<MemberLeaveEvent.Quit> {
            if (!group.enable()) return@subscribeAlways
            val msg = quitMessage.encodeToMiraiCode(member, true).deserializeMiraiCode()
            group.sendMessage(msg)
        }

        GlobalEventChannel.filter { memberMutedMessage.isNotEmpty() && botOperatedMuteMessage.isNotEmpty() }
            .subscribeAlways<MemberMuteEvent>(
                priority = EventPriority.LOWEST
            ) {
                if (!group.enable()) return@subscribeAlways
                val msg = if (operatorOrBot == group.botAsMember) botOperatedMuteMessage
                    .replace("%??????%", operatorOrBot.nameCardOrNick)
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
                if (!group.enable()) return@subscribeAlways
                val msg = if (operatorOrBot == group.botAsMember) botOperatedUnmuteMessage
                    .replace("%??????%", operatorOrBot.nameCardOrNick)
                    .encodeToMiraiCode(member, false)
                    .deserializeMiraiCode()
                else memberUnmuteMessage
                    .encodeToMiraiCode(operatorOrBot, member)
                    .deserializeMiraiCode()
                group.sendMessage(msg)
            }

        GlobalEventChannel.filter { botMutedMessage.isNotEmpty() }.subscribeAlways<BotMuteEvent> {
            if (!group.enable()) return@subscribeAlways
            try {
                for (msg in botMutedMessage) {
                    operator.sendMessage(msg)
                    delay(1000)
                }
            } catch (e: Exception) {
                logger.error("$e ??????????????????????????????...")
            }
        }

        GlobalEventChannel.filter { botUnmuteMessage.isNotEmpty() }.subscribeAlways<BotUnmuteEvent> {
            if (!group.enable()) return@subscribeAlways
            delay(1000)
            val msg = botUnmuteMessage.encodeToMiraiCode(operator, true).deserializeMiraiCode()
            group.sendMessage(msg)
        }

        GlobalEventChannel.subscribeAlways<BotJoinGroupEvent.Invite> {
            if (!group.enable()) return@subscribeAlways
            group.sendMessage("??????????????????")
        }

        GlobalEventChannel.filter { groupMuteAllRelease.isNotEmpty() }.subscribeAlways<GroupMuteAllEvent> {
            if (!group.enable()) return@subscribeAlways
            if (!new) {
                val msg = groupMuteAllRelease.encodeToMiraiCode(operatorOrBot, true).deserializeMiraiCode()
                group.sendMessage(msg)
            }
        }

        GlobalEventChannel.subscribeAlways<MemberPermissionChangeEvent> {
            if (!group.enable()) return@subscribeAlways
            val msg = when {
                origin.isOwner() || new.isOwner() -> PlainText("?????????????????????")
                origin.isAdministrator() && !new.isOperator() -> At(member).plus(PlainText(" ???????????????????????????"))
                else -> At(member).plus(PlainText(" ????????????"))
            }
            group.sendMessage(msg)
        }

        GlobalEventChannel.filter { nudgedReply.isNotEmpty() }.subscribeAlways<NudgeEvent> {
            if (subject is Group && !(subject as Group).enable()) return@subscribeAlways
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
                                        subject.sendMessage("?????????...??????")
                                        delay(1000)
                                        subject.sendMessage(PokeMessage.ChuoYiChuo)
                                    }
                                } catch (e: UnsupportedOperationException) {
                                    logger.info { "????????????????????????????????????????????????Poke?????????" }
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
                        if (nudgedPerReply.contains("%???")) {
                            nudgedPerReply.encodeToAudio(subject as AudioSupported).sendTo(subject)
                        } else
                            subject.sendMessage(
                                nudgedPerReply.encodeImageToMiraiCode(subject).deserializeMiraiCode()
                            )
                    }
                }
            }
        }

        GlobalEventChannel.filter { repeatSec >= 0 }.subscribeAlways<GroupMessageEvent> {
            if (!group.enable())
                if (onEnable.contains(group.id)) return@subscribeAlways
            if (lastMessage[group.id] == message.serializeToMiraiCode()) {
                onEnable.add(group.id)
                subject.sendMessage(message)
                logger.info { "???????????????" }
                this@AutoGroup.launch {
                    delay(repeatSec * 1000)
                    onEnable.remove(group.id)
                }
            }
            lastMessage[group.id] = message.serializeToMiraiCode()
        }

        /**
         * ???????????????
         * ????????????:
         * inall ?????????????????????
         * yinglish ?????????????????????
         * ???????????? ????????????
         * party ( ???????????? ) ???????????????
         * Roulette ????????????
         * */
        GlobalEventChannel.subscribeGroupMessages {
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

            "?????????" Here@{
                val promMsg = subject.sendMessage("?????????")
                val next = runCatching { nextMessage(30_000) }.getOrNull() ?: return@Here
                subject.sendMessage(next.reduplicate())
                promMsg.recall()
            }

            yinglishCommand {
                if (onYinable.contains(sender.id)) return@yinglishCommand
                onYinable.add(sender.id)
                val promMsg = subject.sendMessage("?????????????????????????????? ???")
                this@AutoGroup.launch {
                    selectMessages {
                        default {
                            // val a = TFIDFAnalyzer().analyze(it, 100)
                            val foo = JiebaSegmenter().process(it, JiebaSegmenter.SegMode.SEARCH)
                            val yinglish = StringBuffer()

                            foo.forEach { keyWord ->
                                val part = WordDictionary.getInstance().parts[keyWord.word]
                                val word = keyWord.word
                                yinglish.append(getYinglishNode(word, part))
                            }

                            subject.sendMessage(yinglish.toString())
                            Unit
                        }
                        timeout(25_000) {
                            subject.sendMessage("???????????? ???")
                            Unit
                        }
                    }
                    promMsg.recall()
                    onYinable.remove(sender.id)
                }
            }

            tenkiNiNokoSaReTaKo {
                if (!sender.isOperator()) {
                    subject.sendMessage("??????????????????????????????????????????")
                    return@tenkiNiNokoSaReTaKo
                }
                if (!group.botPermission.isOperator()) {
                    subject.sendMessage("??????????????????????????????????????????????????????")
                    logger.error(PermissionDeniedException("??????????????????"))
                    return@tenkiNiNokoSaReTaKo
                }
                subject.sendMessage("?????????????????????")
                delay(3000)
                (subject.members - sender).filter { subject.botPermission > it.permission }.random()
                    .mute((1..100).random())
            }

            roulette {
                if (rouletteData.keys.contains(subject)) {
                    subject.sendMessage("????????????????????????????????????")
                    return@roulette
                }
                rouletteData[subject] = mutableSetOf()
                val rouGroup = subject
                subject.sendMessage(buildMessageChain {
                    add("???")
                    add(At(sender))
                    add(" ???????????????????????????")
                })
                var bulletNum = 1
                whileSelectMessages {
                    default { msg ->
                        if (Regex("""\D""").containsMatchIn(msg))
                            subject.sendMessage("??????????????? !")
                        else if ((msg.toInt() > maxPlayer) or (msg.toInt() <= 0))
                            subject.sendMessage("???????????????????????? !")
                        else {
                            bulletNum = msg.toInt()
                            return@default false
                        }
                        true
                    }
                    timeout(10_000) {
                        subject.sendMessage("????????????????????????????????????????????????")
                        false
                    }
                }

                subject.sendMessage(
                    """
                        |??????????????? "?????????"
                        |??????${intConvertToChs(maxPlayer)}??????????????????${intConvertToChs(bulletNum)}?????????
                        |?????????????????? "s" ???????????????
                        |??? "????????????" ?????????????????????????????????????????????
                    """.trimMargin()
                )
                var delayTimes = 0

                class Roulette : TimerTask() {
                    override fun run() {
                        this@AutoGroup.launch {
                            delayTimes++
                            if (delayTimes >= 2) {
                                subject.sendMessage(
                                    """
                           ??????????????????????????????
                           ????????????????????????
                        """.trimIndent()
                                )
                                rouletteData.remove(subject)
                                when ((1..4).random()) {
                                    2 -> {
                                        val luckyDog = subject.members.random()
                                        subject.sendMessage(rouletteOutMessage.random())
                                        subject.sendMessage("??????????????? ${luckyDog.nameCardOrNick} ????????????")
                                        try {
                                            luckyDog.mute(30)
                                            intercept()
                                        } catch (e: PermissionDeniedException) {
                                            logger.error { "???????????????????????????" }
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
                var i = 0
                GlobalEventChannel.subscribe<GroupMessageEvent> {
                    if (this.subject == rouGroup) {
                        if (message.content == "s" && rouletteData[subject]?.contains(sender) == false) {
                            i++
                            when {
                                bullets.contains(i) -> {
                                    subject.sendMessage(rouletteOutMessage.random())
                                    try {
                                        sender.mute((1..rouletteOutMuteRange).random())
                                    } catch (e: PermissionDeniedException) {
                                        subject.sendMessage("????????????????????????")
                                    } catch (e: IllegalStateException) {
                                        sender.mute(30)
                                        logger.error { "?????????????????????$e" }
                                    }
                                    if (i >= lastBullet) {
                                        rouletteData.remove(subject)
                                        if (bulletNum > 1)
                                            subject.sendMessage("??????????????????????????????...????????????????????????")
                                        calc.cancel()
                                        return@subscribe ListeningStatus.STOPPED
                                    }
                                }
                                else -> {
                                    // ???
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

        GlobalEventChannel.filter { reduplicate > 0 }.subscribeAlways<MessagePreSendEvent> {
            val random = (1..100).random()
            if (random < reduplicate) {
                message = message.reduplicate()
            }
        }

        GlobalEventChannel.subscribeFriendMessages {
            /*   "????????????" {
                     subject.sendMessage("???????????????????????????????????????")
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
        logger.info { "??????????????????" }
    }

    private fun AbstractJvmPlugin.registerPermission(name: String, description: String): Permission {
        return PermissionService.INSTANCE.register(permissionId(name), description, parentPermission)
    }

    private fun init() {
        val osName = System.getProperties().getProperty("os.name")
        if (!osName.startsWith("Windows")) System.setProperty("java.awt.headless", "true")
        AutoConfig.reload()
        Zuan.register()
        GroupList.reload()
        AutoGroupCtr.register()
        val cacheClear = CacheClear()
        Timer().schedule(cacheClear, Date(), 60 * 30 * 1000)
    }
}
