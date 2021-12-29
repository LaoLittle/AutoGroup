package org.laolittle.plugin.joinorquit.command

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.command.descriptor.ExperimentalCommandDescriptors
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.info
import org.laolittle.plugin.joinorquit.AutoConfig
import org.laolittle.plugin.joinorquit.AutoGroup
import org.laolittle.plugin.joinorquit.utils.KtorHttpUtil

object Zuan : SimpleCommand(
    AutoGroup, "骂我",
    description = "祖安机器人"
) {

    @OptIn(ExperimentalCommandDescriptors::class, ConsoleExperimentalApi::class)
    override val prefixOptional: Boolean = true

    @Handler
    suspend fun CommandSender.handle(times: Int = 1) {
        val inTimes: Int = if (times <= 1) 1 else times
        if (inTimes >= 5) {
            subject?.sendMessage(AutoConfig.outOfLimitation)
                ?: AutoGroup.logger.info { AutoConfig.outOfLimitation }
            return
        }
        repeat(inTimes) {
           val zuAn = KtorHttpUtil.getZuan()
            subject?.sendMessage(At(user!!).plus(PlainText(zuAn)))
                ?: AutoGroup.logger.info { zuAn }
        }
    }
}