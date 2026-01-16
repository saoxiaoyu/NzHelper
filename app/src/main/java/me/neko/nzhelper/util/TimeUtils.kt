package me.neko.nzhelper.util

import android.annotation.SuppressLint

/**
 * 时间格式化工具类
 */
object TimeUtils {
    
    /**
     * 将秒数格式化为时:分:秒格式
     * @param totalSeconds 总秒数
     * @return 格式化后的字符串，如 "05:30" 或 "01:05:30"
     */
    @SuppressLint("DefaultLocale")
    fun formatTime(totalSeconds: Int): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return buildString {
            if (h > 0) append(String.format("%02d:", h))
            append(String.format("%02d:%02d", m, s))
        }
    }
}
