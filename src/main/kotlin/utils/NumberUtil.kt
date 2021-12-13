package org.laolittle.plugin.joinorquit.utils

object NumberUtil {
    private val CN_NUM = arrayOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")

    private val CN_UNIT = arrayOf("", "十", "百", "千", "万", "十", "百", "千", "亿", "十", "百", "千")

    private const val CN_NEGATIVE = "负"

    /**
     * int 转 中文数字
     * 支持到int最大值
     *
     * @param intNum 要转换的整型数
     * @return 中文数字
     */
    fun intConvertToChs(intNum: Int): String {
        var intNumBuff = intNum
        val sb = StringBuffer()
        var isNegative = false
        if (intNumBuff < 0) {
            isNegative = true
            intNumBuff *= -1
        }
        var count = 0
        while (intNumBuff > 0) {
            sb.insert(0, CN_NUM[intNumBuff % 10] + CN_UNIT[count])
            intNumBuff /= 10
            count++
        }
        if (isNegative) sb.insert(0, CN_NEGATIVE)
      val chs = sb.toString().replace("零[千百十]".toRegex(), "零").replace("零+万".toRegex(), "万")
            .replace("零+亿".toRegex(), "亿").replace("亿万".toRegex(), "亿零")
            .replace("零+".toRegex(), "零").replace("零$".toRegex(), "")

        return if (intNum / 10 == 1) chs.replace("一", "") else chs
    }

}