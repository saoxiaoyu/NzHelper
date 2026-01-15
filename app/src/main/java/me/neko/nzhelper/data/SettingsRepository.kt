package me.neko.nzhelper.data

import android.content.Context
import androidx.core.content.edit

/**
 * 设置存储仓库
 * 使用 SharedPreferences 持久化用户偏好设置
 */
object SettingsRepository {

    private const val PREFS_NAME = "settings_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color"
    private const val KEY_APP_LOCK = "app_lock"

    /**
     * 主题模式枚举
     */
    enum class ThemeMode {
        SYSTEM,  // 跟随系统
        DARK,    // 深色模式
        LIGHT    // 浅色模式
    }

    // ==================== 主题模式 ====================

    fun getThemeMode(context: Context): ThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(value ?: ThemeMode.SYSTEM.name)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
    }

    // ==================== 动态颜色 ====================

    fun isDynamicColorEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DYNAMIC_COLOR, true) // 默认开启
    }

    fun setDynamicColorEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_DYNAMIC_COLOR, enabled) }
    }

    // ==================== 应用锁 ====================

    fun isAppLockEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_APP_LOCK, false) // 默认关闭
    }

    fun setAppLockEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_APP_LOCK, enabled) }
    }
}
