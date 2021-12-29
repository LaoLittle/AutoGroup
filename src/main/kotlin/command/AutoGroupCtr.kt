package org.laolittle.plugin.joinorquit.command

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import org.laolittle.plugin.joinorquit.AutoGroup
import org.laolittle.plugin.joinorquit.GroupList

object AutoGroupCtr : CompositeCommand(
    AutoGroup, "autogroup", "ag",
    description = "AutoGroup命令"
) {

    @SubCommand("add")
    suspend fun CommandSender.add(target: Long? = null) {
        if (target == null) {
            sendMessage("未输入群号")
            return
        }
        if (GroupList.groupList.add(target))
            sendMessage("已添加 $target")
        else sendMessage("添加失败，原因: 该号码已在名单中")
    }

    @SubCommand("rm")
    suspend fun CommandSender.remove(target: Long? = null) {
        if (target == null) {
            sendMessage("未输入群号")
            return
        }
        if (GroupList.groupList.remove(target))
            sendMessage("已移除 $target")
        else sendMessage("移除失败，原因: 该号码不在名单中")
    }
}