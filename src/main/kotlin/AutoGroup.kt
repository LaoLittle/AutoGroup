package org.laolittle.plugin.joinorquit

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
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.contact.isAdministrator
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.contact.isOwner
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.MiraiExperimentalApi
import net.mamoe.mirai.utils.info
import org.laolittle.plugin.joinorquit.AutoConfig.nudgeMin
import org.laolittle.plugin.joinorquit.model.getPat
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
        version = "1.0",
        name = "AutoGroup"
    ) {
        author("LaoLittle")
        info("折磨群友")
    }
) {
    override fun onEnable() {
        AutoConfig.reload()
        var lastMessage: Map<Long, String> = mutableMapOf()
        var onEnable: Map<Long, Boolean> = mutableMapOf()
        val nudgePerm = AutoGroup.registerPermission("timer.nudge", "每隔${nudgeMin}分钟戳一戳")

        logger.info { "开始折磨群友" }
        GlobalEventChannel.subscribeOnce<BotOnlineEvent> {
            class NudgeTimer : TimerTask() {
                override fun run() {
                    logger.info { "开戳" }
                    this@AutoGroup.launch {
                        bot.groups.filter {
                            val nowHour = LocalDateTime.now().hour
                            nowHour !in 0..8 && nowHour !in 22..23
                        }.filter { nudgePerm.testPermission(it.permitteeId) }.forEach {
                            val sender = it.members.random()
                            delay(3000)
                            sender.nudge().sendTo(it)
                        }
                    }
                }
            }

            val nudgeTimer = NudgeTimer()
            Timer().schedule(nudgeTimer, Date(), nudgeMin * 60 * 1000)
        }

        GlobalEventChannel.subscribeAlways<GroupTalkativeChangeEvent> {
            group.sendMessage(At(previous) + PlainText(" 的龙王被") + At(now) + PlainText(" 抢走了，好可怜"))
        }

        GlobalEventChannel.subscribeAlways<MemberJoinEvent> {
            group.sendMessage("欢淫")
            getPat(member, 80)
            group.sendImage(File("$dataFolder/tmp").resolve("${member.id}_pat.gif"))
        }

        GlobalEventChannel.subscribeAlways<MemberLeaveEvent.Kick> {
            group.sendMessage("有个人被$operator 踢了！！好可怕")
        }

        GlobalEventChannel.subscribeAlways<MemberLeaveEvent.Quit> {
            group.sendMessage("有个人悄悄退群了...")
        }

        GlobalEventChannel.subscribeAlways<MemberMuteEvent> {
            group.sendMessage(buildMessageChain {
                add(At(member))
                add(PlainText(" 被"))
                add(At(operator as User))
                add(PlainText(" 禁言了，好可惜"))
            })
        }

        GlobalEventChannel.subscribeAlways<MemberUnmuteEvent> {
            group.sendMessage(buildMessageChain {
                add(At(member))
                add(" 你自由啦！还不快感谢")
                if (operatorOrBot == group.botAsMember)
                    add("我")
                else {
                    add(At(operatorOrBot))
                    add(" 大人")
                }
            })
        }

        GlobalEventChannel.subscribeAlways<BotMuteEvent> {
            try {
                operator.sendMessage("就是你禁言的我吧")
                delay(1000)
                operator.sendMessage("咕姆姆，我记住你了")
            } catch (e: Exception) {
                logger.error("$e 好像没法发送临时消息...")
            }
        }

        GlobalEventChannel.subscribeAlways<BotUnmuteEvent> {
            delay(1000)
            group.sendMessage(buildMessageChain {
                add("我自由啦！感谢")
                add(At(operator))
                add(" 大人 🥵🥵🥵🥵🥵🥵🥵🥵")
            })
        }

        GlobalEventChannel.subscribeGroupMessages {
            startsWith("allinall") {
                val replaced = it.replace("allinall", "")
                if (replaced == "") return@startsWith
                val msg = buildForwardMessage {
                    val randomMember = subject.members.random()
                    add(randomMember, PlainText(replaced))
                }
                subject.sendMessage(msg)
            }
        }

        GlobalEventChannel.subscribeAlways<BotJoinGroupEvent> {
            group.sendMessage("我来啦！！！")
        }

        GlobalEventChannel.subscribeAlways<GroupMuteAllEvent> {
            if (!new) {
                group.sendMessage("嗯？好像能说话了耶")
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

        GlobalEventChannel.subscribeAlways<NudgeEvent> {
            if (target == bot) {
                val msg = when ((0..5).random()) {
                    0 -> "请不要戳亚托莉~>_<~"
                    1 -> "别戳啦"
                    2 -> "再戳我你就是笨批<( ￣^￣)"
                    3 -> "ヾ(≧へ≦)〃"
                    4 -> "亚托莉是高性能机器人...呜呜"
                    else -> {
                        subject.sendMessage("戳回去(￣ ‘i ￣;)")
                        delay(1000)
                        try {
                            if (!from.nudge().sendTo(subject)) {
                                subject.sendMessage("戳不了...那我")
                                delay(1000)
                                subject.sendMessage(PokeMessage.ChuoYiChuo)
                            }
                        } catch (e: UnsupportedOperationException) {
                            logger.info { "协议为不支持的协议，改用Poke戳一戳" }
                            subject.sendMessage(PokeMessage.ChuoYiChuo)
                        }
                        "哼"
                    }
                }
                delay(1000)
                subject.sendMessage(msg)
            }
        }

        GlobalEventChannel.subscribeAlways<GroupMessageEvent> {
            if (onEnable[group.id] == true) return@subscribeAlways
            if (lastMessage[group.id] == message.serializeToMiraiCode()) {
                onEnable = onEnable.plus(group.id to true)
                subject.sendMessage(message)
                logger.info { "复读了一次" }
                this@AutoGroup.launch {
                    delay(8_000)
                    onEnable = onEnable.minus(group.id)
                }
            }
            lastMessage = lastMessage.plus(group.id to message.serializeToMiraiCode())
        }

        Timer().schedule(CacheClear(), Date(), 60 * 30 * 1000)
    }

    override fun onDisable() {
        logger.info { "让他们休息会" }
    }

    class CacheClear : TimerTask() {
        override fun run() {
            val tmp = File("${AutoGroup.dataFolder}/tmp")
            when (if (tmp.exists()) tmp.deleteRecursively() else null) {
                true -> AutoGroup.logger.info { "缓存清理完成" }
                false -> AutoGroup.logger.info { "缓存清理失败" }
            }
        }
    }

    private fun AbstractJvmPlugin.registerPermission(name: String, description: String): Permission {
        return PermissionService.INSTANCE.register(permissionId(name), description, parentPermission)
    }
}