package me.neko.nzhelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import me.neko.nzhelper.data.SettingsRepository
import me.neko.nzhelper.data.SettingsRepository.ThemeMode
import me.neko.nzhelper.ui.theme.NzHelperTheme
import me.neko.nzhelper.ui.screens.MainScreen

class MainActivity : ComponentActivity() {
    
    // 主题状态，使用 mutableStateOf 以便 Compose 响应变化
    private var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    private var dynamicColor by mutableStateOf(true)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 读取设置
        themeMode = SettingsRepository.getThemeMode(this)
        dynamicColor = SettingsRepository.isDynamicColorEnabled(this)
        
        setContent {
            NzHelperTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor
            ) {
                MainScreen(
                    onThemeChanged = { mode ->
                        themeMode = mode
                        SettingsRepository.setThemeMode(this@MainActivity, mode)
                    },
                    onDynamicColorChanged = { enabled ->
                        dynamicColor = enabled
                        SettingsRepository.setDynamicColorEnabled(this@MainActivity, enabled)
                    },
                    currentThemeMode = themeMode,
                    currentDynamicColor = dynamicColor
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 确保恢复时重新读取设置（以防其他方式修改）
        themeMode = SettingsRepository.getThemeMode(this)
        dynamicColor = SettingsRepository.isDynamicColorEnabled(this)
    }
}
