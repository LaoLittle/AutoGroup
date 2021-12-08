package org.laolittle.plugin.joinorquit

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object AutoConfig : AutoSavePluginConfig("AutoConfig") {
    @ValueDescription("戳一戳的时间间隔(单位: 分)")
    val nudgeMin: Long by value(30L)

    @ValueDescription(
        """
        机器人被戳时的回复
        可任意按照格式添加
        当戳一戳未触发时便随机选取列表中的消息发送
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
    val counterNudge: Int by value(30)

    @ValueDescription(
        """
        被禁言后对禁言操作者私聊的消息
        消息会按顺序放出
    """
    )
    val mutedMessage: List<String> by value(listOf("就是你禁言的我吧", "咕姆姆，我记住你了"))
/*
    @ValueDescription("是否在禁言期间持续发送消息给操作人以及发送的消息")
    val keepSendMessageWhenMuted: Boolean by value(false)
    val initiativeMessage: Set<String> by value(setOf("快点给我解禁"))

 */
}