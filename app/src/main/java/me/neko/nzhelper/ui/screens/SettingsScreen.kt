package me.neko.nzhelper.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.neko.nzhelper.data.ImportResult
import me.neko.nzhelper.data.SessionRepository
import me.neko.nzhelper.data.SettingsRepository
import me.neko.nzhelper.data.SettingsRepository.ThemeMode
import me.neko.nzhelper.data.WebDavClient
import me.neko.nzhelper.data.WebDavResult
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
    var showWebDavDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // WebDAV 配置状态
    var webdavUrl by remember { mutableStateOf(SettingsRepository.getWebDavUrl(context)) }
    var webdavUsername by remember { mutableStateOf(SettingsRepository.getWebDavUsername(context)) }
    var webdavPassword by remember { mutableStateOf(SettingsRepository.getWebDavPassword(context)) }
    val isWebDavConfigured = webdavUrl.isNotEmpty() && webdavUsername.isNotEmpty() && webdavPassword.isNotEmpty()
    
    // 通知权限状态
    val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

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
            SettingsSectionHeader("云备份")

            // WebDAV 配置
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWebDavDialog = true },
                leadingContent = {
                    Icon(imageVector = Icons.Outlined.Cloud, contentDescription = null)
                },
                headlineContent = {
                    Text(text = "WebDAV 设置", style = MaterialTheme.typography.titleMedium)
                },
                supportingContent = {
                    Text(
                        text = if (isWebDavConfigured) "已配置" else "点击配置 WebDAV 服务器",
                        color = if (isWebDavConfigured) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            // 备份到云端
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = isWebDavConfigured && !isLoading) {
                        scope.launch {
                            isLoading = true
                            val result = SessionRepository.backupToWebDav(context)
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                when (result) {
                                    is WebDavResult.Success -> {
                                        Toast.makeText(context, "备份成功", Toast.LENGTH_SHORT).show()
                                    }
                                    is WebDavResult.Error -> {
                                        Toast.makeText(context, "备份失败：${result.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    },
                leadingContent = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Outlined.CloudUpload, contentDescription = null)
                    }
                },
                headlineContent = {
                    Text(
                        text = "备份到云端", 
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isWebDavConfigured) 
                            MaterialTheme.colorScheme.onSurface 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                supportingContent = {
                    Text(
                        text = if (isWebDavConfigured) "将数据上传到 WebDAV 服务器" else "请先配置 WebDAV"
                    )
                }
            )

            // 从云端恢复
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = isWebDavConfigured && !isLoading) {
                        scope.launch {
                            isLoading = true
                            val result = SessionRepository.restoreFromWebDav(context)
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                when (result) {
                                    is ImportResult.Success -> {
                                        Toast.makeText(context, "恢复成功，共 ${result.count} 条记录", Toast.LENGTH_SHORT).show()
                                    }
                                    is ImportResult.Error -> {
                                        Toast.makeText(context, "恢复失败：${result.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    },
                leadingContent = {
                    Icon(imageVector = Icons.Outlined.CloudDownload, contentDescription = null)
                },
                headlineContent = {
                    Text(
                        text = "从云端恢复", 
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isWebDavConfigured) 
                            MaterialTheme.colorScheme.onSurface 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                supportingContent = {
                    Text(
                        text = if (isWebDavConfigured) "从 WebDAV 服务器下载数据（将覆盖当前数据）" else "请先配置 WebDAV"
                    )
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

        // WebDAV 配置对话框
        if (showWebDavDialog) {
            var dialogUrl by remember { mutableStateOf(webdavUrl) }
            var dialogUsername by remember { mutableStateOf(webdavUsername) }
            var dialogPassword by remember { mutableStateOf(webdavPassword) }
            var testing by remember { mutableStateOf(false) }
            var testResult by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { showWebDavDialog = false },
                title = { Text("WebDAV 设置") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "请输入 WebDAV 服务器信息",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        OutlinedTextField(
                            value = dialogUrl,
                            onValueChange = { dialogUrl = it },
                            label = { Text("服务器地址") },
                            placeholder = { Text("https://dav.example.com/") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = dialogUsername,
                            onValueChange = { dialogUsername = it },
                            label = { Text("用户名") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = dialogPassword,
                            onValueChange = { dialogPassword = it },
                            label = { Text("密码") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        if (testResult != null) {
                            Text(
                                text = testResult!!,
                                color = if (testResult!!.startsWith("连接成功")) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Row {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    testing = true
                                    testResult = null
                                    val result = WebDavClient.testConnection(dialogUrl, dialogUsername, dialogPassword)
                                    testing = false
                                    testResult = when (result) {
                                        is WebDavResult.Success -> "连接成功！"
                                        is WebDavResult.Error -> "连接失败：${result.message}"
                                    }
                                }
                            },
                            enabled = !testing && dialogUrl.isNotEmpty() && dialogUsername.isNotEmpty() && dialogPassword.isNotEmpty()
                        ) {
                            if (testing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.width(16.dp).height(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("测试")
                            }
                        }
                        
                        TextButton(
                            onClick = {
                                SettingsRepository.saveWebDavConfig(context, dialogUrl, dialogUsername, dialogPassword)
                                webdavUrl = dialogUrl
                                webdavUsername = dialogUsername
                                webdavPassword = dialogPassword
                                showWebDavDialog = false
                                Toast.makeText(context, "WebDAV 配置已保存", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("保存")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWebDavDialog = false }) {
                        Text("取消")
                    }
                }
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
