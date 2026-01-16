package me.neko.nzhelper.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import me.neko.nzhelper.data.ImportResult
import me.neko.nzhelper.data.SessionRepository
import me.neko.nzhelper.data.SettingsRepository.ThemeMode
import me.neko.nzhelper.ui.dialog.CustomAppAlertDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeChanged: (ThemeMode) -> Unit = {},
    currentDynamicColor: Boolean = true,
    onDynamicColorChanged: (Boolean) -> Unit = {},
    currentAppLockEnabled: Boolean = false,
    onAppLockChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    
    // 通知权限状态
    val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    
    // 导入数据
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { importUri ->
            scope.launch {
                when (val result = SessionRepository.importFromUri(context, importUri)) {
                    is ImportResult.Success -> {
                        Toast.makeText(
                            context,
                            "成功导入 ${result.count} 条记录",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is ImportResult.Error -> {
                        Toast.makeText(context, "导入失败：${result.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // 导出数据
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { exportUri ->
            scope.launch {
                val success = SessionRepository.exportToUri(context, exportUri)
                if (success) {
                    Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 打开通知设置
    fun openNotificationSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra("app_uid", context.applicationInfo.uid)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("设置") },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
        ) {
            // ==================== 外观设置分组 ====================
            SettingsSectionHeader("外观")

            // 主题模式
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showThemeDialog = true },
                leadingContent = {
                    Icon(
                        imageVector = when (currentThemeMode) {
                            ThemeMode.SYSTEM -> Icons.Outlined.BrightnessMedium
                            ThemeMode.DARK -> Icons.Outlined.DarkMode
                            ThemeMode.LIGHT -> Icons.Outlined.LightMode
                        },
                        contentDescription = null,
                    )
                },
                headlineContent = {
                    Text(text = "主题模式", style = MaterialTheme.typography.titleMedium)
                },
                supportingContent = {
                    Text(
                        text = when (currentThemeMode) {
                            ThemeMode.SYSTEM -> "跟随系统"
                            ThemeMode.DARK -> "深色模式"
                            ThemeMode.LIGHT -> "浅色模式"
                        }
                    )
                }
            )

            // 动态颜色（仅 Android 12+ 显示）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    leadingContent = {
                        Icon(imageVector = Icons.Outlined.Palette, contentDescription = null)
                    },
                    headlineContent = {
                        Text(text = "动态颜色", style = MaterialTheme.typography.titleMedium)
                    },
                    supportingContent = {
                        Text(text = "使用壁纸颜色生成主题色")
                    },
                    trailingContent = {
                        Switch(
                            checked = currentDynamicColor,
                            onCheckedChange = onDynamicColorChanged
                        )
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ==================== 隐私安全分组 ====================
            SettingsSectionHeader("隐私安全")

            // 应用锁
            ListItem(
                modifier = Modifier.fillMaxWidth(),
                leadingContent = {
                    Icon(imageVector = Icons.Outlined.Fingerprint, contentDescription = null)
                },
                headlineContent = {
                    Text(text = "应用锁", style = MaterialTheme.typography.titleMedium)
                },
                supportingContent = {
                    Text(text = "启动应用时需要验证身份")
                },
                trailingContent = {
                    Switch(
                        checked = currentAppLockEnabled,
                        onCheckedChange = onAppLockChanged
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ==================== 通知分组 ====================
            SettingsSectionHeader("通知")

            // 通知设置
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openNotificationSettings() },
                leadingContent = {
                    Icon(
                        imageVector = if (notificationsEnabled) 
                            Icons.Outlined.Notifications 
                        else 
                            Icons.Outlined.NotificationsOff,
                        contentDescription = null
                    )
                },
                headlineContent = {
                    Text(text = "通知权限", style = MaterialTheme.typography.titleMedium)
                },
                supportingContent = {
                    Text(
                        text = if (notificationsEnabled) "已开启" else "未开启，点击前往设置",
                        color = if (notificationsEnabled) 
                            MaterialTheme.colorScheme.onSurfaceVariant 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ==================== 数据管理分组 ====================
            SettingsSectionHeader("数据管理")

            // 导出数据
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        exportLauncher.launch("NzHelper_export_${System.currentTimeMillis()}.json")
                    },
                leadingContent = {
                    Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = null)
                },
                headlineContent = {
                    Text(text = "导出数据", style = MaterialTheme.typography.titleMedium)
                },
                supportingContent = {
                    Text(text = "将记录导出为 JSON 文件")
                }
            )

            // 导入数据
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        importLauncher.launch(arrayOf("application/json"))
                    },
                leadingContent = {
                    Icon(imageVector = Icons.Outlined.FileUpload, contentDescription = null)
                },
                headlineContent = {
                    Text(text = "导入数据", style = MaterialTheme.typography.titleMedium)
                },
                supportingContent = {
                    Text(text = "从 JSON 文件导入（将覆盖当前数据）")
                }
            )

            // 清除数据
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showClearDialog = true },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                headlineContent = {
                    Text(
                        text = "清除所有数据",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                supportingContent = {
                    Text(text = "删除所有记录，此操作不可撤销")
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ==================== 其他设置分组 ====================
            SettingsSectionHeader("其他")

            // 关于
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("about") },
                leadingContent = {
                    Icon(imageVector = Icons.Outlined.Info, contentDescription = null)
                },
                headlineContent = {
                    Text(text = "关于", style = MaterialTheme.typography.titleMedium)
                }
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        // 主题选择对话框
        if (showThemeDialog) {
            ThemeModeDialog(
                currentMode = currentThemeMode,
                onModeSelected = { mode ->
                    onThemeChanged(mode)
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }

        // 清除数据确认对话框
        if (showClearDialog) {
            CustomAppAlertDialog(
                onDismissRequest = { showClearDialog = false },
                iconVector = Icons.Rounded.Warning,
                title = "清除全部数据",
                message = "此操作不可撤销，确定要删除所有记录吗？",
                confirmText = "删除全部",
                confirmIcon = Icons.Rounded.DeleteForever,
                dismissText = "取消",
                onConfirm = {
                    scope.launch {
                        SessionRepository.saveSessions(context, emptyList())
                        Toast.makeText(context, "已清除所有数据", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showClearDialog = false }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun ThemeModeDialog(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "选择主题模式",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = { onModeSelected(mode) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = when (mode) {
                                ThemeMode.SYSTEM -> Icons.Outlined.BrightnessMedium
                                ThemeMode.DARK -> Icons.Outlined.DarkMode
                                ThemeMode.LIGHT -> Icons.Outlined.LightMode
                            },
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (mode) {
                                ThemeMode.SYSTEM -> "跟随系统"
                                ThemeMode.DARK -> "深色模式"
                                ThemeMode.LIGHT -> "浅色模式"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        navController = rememberNavController()
    )
}
