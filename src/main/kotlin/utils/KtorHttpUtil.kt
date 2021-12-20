package org.laolittle.plugin.joinorquit.utils

import io.ktor.client.*
import io.ktor.client.request.*
import org.laolittle.plugin.joinorquit.AutoConfig.maxZuanLevel

object KtorHttpUtil {
    private val client = HttpClient()
    private val zuanUrl = StringBuffer()
    private const val ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36 Edg/96.0.1054.57"

    suspend fun getZuan(): String {
        return client.get {
            url(zuanUrl.toString())
            header("Accept", "*/*")
            header("Referer", "https://zuanbot.com/")
            header("User-Agent", ua)
        }
    }

    init {
        zuanUrl.append("https://zuanbot.com/api.php?lang=zh_cn")
        if (!maxZuanLevel) zuanUrl.append("&level=min")
    }
}