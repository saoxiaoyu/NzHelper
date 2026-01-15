package me.neko.nzhelper

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import me.neko.nzhelper.data.SettingsRepository
import me.neko.nzhelper.data.SettingsRepository.ThemeMode
import me.neko.nzhelper.ui.screens.AppLockScreen
import me.neko.nzhelper.ui.theme.NzHelperTheme
import me.neko.nzhelper.ui.screens.MainScreen

class MainActivity : FragmentActivity() {
    
    // 主题状态
    private var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    private var dynamicColor by mutableStateOf(true)
    
    // 应用锁状态
    private var isAppLockEnabled by mutableStateOf(false)
    private var isUnlocked by mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 读取设置
        themeMode = SettingsRepository.getThemeMode(this)
        dynamicColor = SettingsRepository.isDynamicColorEnabled(this)
        isAppLockEnabled = SettingsRepository.isAppLockEnabled(this)
        
        // 如果应用锁未开启，直接标记为已解锁
        if (!isAppLockEnabled) {
            isUnlocked = true
        }
        
        setContent {
            NzHelperTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor
            ) {
                if (isAppLockEnabled && !isUnlocked) {
                    // 显示锁屏
                    AppLockScreen(
                        onUnlocked = { isUnlocked = true }
                    )
                } else {
                    // 显示主界面
                    MainScreen(
                        onThemeChanged = { mode ->
                            themeMode = mode
                            SettingsRepository.setThemeMode(this@MainActivity, mode)
                        },
                        onDynamicColorChanged = { enabled ->
                            dynamicColor = enabled
                            SettingsRepository.setDynamicColorEnabled(this@MainActivity, enabled)
                        },
                        onAppLockChanged = { enabled ->
                            isAppLockEnabled = enabled
                            SettingsRepository.setAppLockEnabled(this@MainActivity, enabled)
                        },
                        currentThemeMode = themeMode,
                        currentDynamicColor = dynamicColor,
                        currentAppLockEnabled = isAppLockEnabled
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 确保恢复时重新读取设置
        themeMode = SettingsRepository.getThemeMode(this)
        dynamicColor = SettingsRepository.isDynamicColorEnabled(this)
        isAppLockEnabled = SettingsRepository.isAppLockEnabled(this)
    }
    
    override fun onStop() {
        super.onStop()
        // 应用进入后台时重新锁定（如果开启了应用锁）
        if (isAppLockEnabled) {
            isUnlocked = false
        }
    }
}
