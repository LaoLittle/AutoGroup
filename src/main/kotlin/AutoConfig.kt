package org.laolittle.plugin.joinorquit

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.utils.BotConfiguration

object AutoConfig : AutoSavePluginConfig("AutoConfig") {
    @ValueDescription("戳一戳的时间间隔(单位：分)")
    val nudgeMin: Long by value(30L)
}