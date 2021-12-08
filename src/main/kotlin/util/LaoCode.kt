package org.laolittle.plugin.joinorquit.util

import net.mamoe.mirai.contact.User


fun String.encodeToMiraiCode(initiative: User, passive: User): String {
    return this
        .replace("%主动%", "[mirai:at:${initiative.id}]")
        .replace("%被动%", "[mirai:at:${passive.id}]")
}

fun String.encodeToMiraiCode(user: User, initiative: Boolean): String {
    return if (initiative) this
        .replace("%主动%", "[mirai:at:${user.id}]")
    else this.replace("%被动%", "[mirai:at:${user.id}]")
}