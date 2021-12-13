package org.laolittle.plugin.joinorquit.model

import kotlinx.serialization.ExperimentalSerializationApi
import net.mamoe.mirai.console.command.descriptor.ExperimentalCommandDescriptors
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.utils.MiraiExperimentalApi
import net.mamoe.mirai.utils.info
import org.laolittle.plugin.joinorquit.AutoGroup
import java.io.File
import java.util.*

class CacheClear : TimerTask() {
    @OptIn(ExperimentalSerializationApi::class, ConsoleExperimentalApi::class, ExperimentalCommandDescriptors::class,
        MiraiExperimentalApi::class
    )
    override fun run() {
        val tmp = File("${AutoGroup.dataFolder}/tmp")
        when (if (tmp.exists()) tmp.deleteRecursively() else null) {
            true -> AutoGroup.logger.info { "缓存已自动清理" }
            false -> AutoGroup.logger.info { "缓存清理失败" }
        }
    }
}