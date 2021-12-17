package org.laolittle.plugin.joinorquit

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object AutoConfig : AutoSavePluginConfig("AutoConfig") {
    @ValueDescription("戳一戳的时间间隔(单位: 分)")
    val nudgeMin: Long by value(60L)

    @ValueDescription(
        """
        新人入群欢迎提示语
        为空则不欢迎
        """
    )
    val newMemberJoinMessage: Set<String> by value(setOf("欢淫", "欢迎"))

    @ValueDescription("新人入群摸头")
    val newMemberJoinPat: Boolean by value(true)

    @ValueDescription(
        """
        Bot 被戳时的回复
        可任意按照格式添加
        当戳一戳未触发反击时便随机选取列表中的消息发送
        为空时不开启
        """
    )
    val nudgedReply: Set<String> by value(
        setOf(
            "ヾ(≧へ≦)〃",
            "请不要戳我~>_<~",
            "别戳啦",
            "再戳我你就是笨批<( ￣^￣)",
            "吾身乃高性能机器人...呜呜"
        )
    )

    @ValueDescription("戳一戳触发反击的概率百分比(%)")
    val counterNudge: Int by value(20)

    @ValueDescription("戳一戳触发反击的回复消息")
    val counterNudgeMessage: Set<String> by value(setOf("戳回去(￣ ‘i ￣;)"))

    @ValueDescription("戳一戳反击结束语")
    val counterNudgeCompleteMessage: Set<String> by value(setOf("哼", "切"))

    @ValueDescription("触发戳一戳超级加倍的概率 (仅触发反击时)")
    val superNudge: Int by value(15)
    val superNudgeMessage: String by value("超级加倍！")

    @ValueDescription("超级加倍戳一戳次数")
    val superNudgeTimes: Int by value(10)

    @ValueDescription(
        """
        Bot 被禁言后对禁言操作者私聊的消息
        消息会按顺序放出
    """
    )
    val botMutedMessage: List<String> by value(
        listOf(
            "就是你禁言的我吧",
            "咕姆姆，我记住你了"
        )
    )

    @ValueDescription(
        """
        Bot 被管理员解禁时的回复
        变量: %主动% (解禁操作人)
        """
    )
    val botUnmuteMessage: String by value("我自由啦！感谢%主动% 大人 🥵🥵🥵🥵🥵🥵🥵🥵")

    @ValueDescription(
        """
        群员被禁言时的回复
        变量: %主动% (解禁操作人), %被动% (被解禁的成员)
        特殊: botOperatedMuteMessage 为 Bot 主动发起禁言时的回复
    """
    )
    val memberMutedMessage: String by value("%被动% 被%主动% 禁言了，好可惜")
    val botOperatedMuteMessage: String by value("%被动% 被本大人禁言了，好好反省吧！")

    @ValueDescription(
        """
        群员被解禁时的回复
        变量: %主动% (解禁操作人), %被动% (被解禁的成员)
        特殊: botOperatedUnmuteMessage 为 Bot 主动解除禁言时的回复
    """
    )
    val memberUnmuteMessage: String by value("%被动% 你自由啦！还不快感谢%主动% 大人")
    val botOperatedUnmuteMessage: String by value("%被动% 你自由啦！还不快感谢本大人")

    @ValueDescription(
        """
        全体解禁时的回复
        变量: %主动% (解禁操作人)
    """
    )
    val groupMuteAllRelease: String by value("嗯？好像能说话了耶")

    @ValueDescription(
        """
        有人被踢出群时的回复
        变量: %主动% (踢人操作人), %被动% (被踢出的前群员)
    """
    )
    val kickMessage: String by value("有个人被%主动% 踢了！好可怕")

    @ValueDescription(
        """
        有人主动退群时的回复
        变量: %主动% (退出的前群员)
    """
    )
    val quitMessage: String by value("有个人悄悄退群了...")

    @ValueDescription(
        """
        加入复读的冷却时长 (单位: 秒)
        为-1时关闭此功能
        """
    )
    val repeatSec: Long by value(25L)

    @ValueDescription("淫语翻译触发关键词")
    val yinglishCommand: String by value("翻译")

    @ValueDescription("淫乱度 (%)")
    var yinLevel: Int by value(70)

    @ValueDescription("随机禁言的命令")
    val tenkiNiNokoSaReTaKo: String by value("天弃之子")

    @ValueDescription("轮盘赌注命令")
    val roulette: String by value("赌")

    @ValueDescription("轮盘赌注弹槽量")
    val maxPlayer: Int by value(6)

    @ValueDescription("轮盘赌注消息")
    val rouletteOutMessage: Set<String> by value(setOf("Bang!"))
    val roulettePassedMessage: Set<String> by value(setOf("你扣动了扳机，但什么也没有发生...", "Bang! 远处传来了一声巨响，但你什么事也没有"))

    @ValueDescription("轮盘赌注最大禁言时间 (单位: 秒)")
    val rouletteOutMuteRange: Int by value(100)

    @ValueDescription("轮盘赌注是否允许重复加入")
    val allowRejoinRoulette: Boolean by value(false)

    @ValueDescription("祖安问候家人")
    val maxZuanLevel: Boolean by value(false)

/*
    @ValueDescription("是否在禁言期间持续发送消息给操作人以及发送的消息")
    val keepSendMessageWhenMuted: Boolean by value(false)
    val initiativeMessage: Set<String> by value(setOf("快点给我解禁"))

 */
}