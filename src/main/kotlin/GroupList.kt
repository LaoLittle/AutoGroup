package org.laolittle.plugin.joinorquit

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.contact.Group

object GroupList : AutoSavePluginConfig("GroupList") {
    enum class ListType {
        BlackList,
        WhiteList
    }

    fun Group.enable(): Boolean {
        return when (type) {
            ListType.BlackList -> this.id !in groupList
            ListType.WhiteList -> this.id in groupList
        }
    }

    @ValueDescription(
        """
        启用白名单/黑名单
        请输入BlackList/WhiteList
        """
    )
    val type by value(ListType.BlackList)

    @ValueDescription("群聊名单")
    val groupList by value(mutableSetOf<Long>(123456))
}