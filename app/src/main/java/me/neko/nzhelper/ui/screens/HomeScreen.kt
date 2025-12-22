package me.neko.nzhelper.ui.screens

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import me.neko.nzhelper.data.Session
import me.neko.nzhelper.data.SessionRepository
import me.neko.nzhelper.ui.service.TimerService
import me.neko.nzhelper.ui.util.CalendarHeatMapStyled
import me.neko.nzhelper.ui.util.buildDayStats
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    // 绑定 Service
    val serviceIntent = remember { Intent(context, TimerService::class.java) }
    var timerService by remember { mutableStateOf<TimerService?>(null) }
    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                timerService = (binder as TimerService.LocalBinder).getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                timerService = null
            }
        }
    }

    // 启动并绑定服务
    LaunchedEffect(Unit) {
        ContextCompat.startForegroundService(
            context,
            serviceIntent.apply { action = TimerService.ACTION_START }
        )
        context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }
    DisposableEffect(Unit) {
        onDispose { context.unbindService(connection) }
    }

    // 订阅 elapsedSec
    val elapsedSeconds by timerService
        ?.elapsedSec
        ?.collectAsState(initial = 0)
        ?: remember { mutableIntStateOf(0) }

    var isRunning by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    var showCalendar by remember { mutableStateOf(false) }

    var remarkInput by remember { mutableStateOf("") }
    var locationInput by remember { mutableStateOf("") }
    var watchedMovie by remember { mutableStateOf(false) }
    var climax by remember { mutableStateOf(false) }
    var rating by remember { mutableFloatStateOf(3f) }
    var mood by remember { mutableStateOf("平静") }
    var props by remember { mutableStateOf("手") }

    val sessions = remember { mutableStateListOf<Session>() }

    // 加载历史
    LaunchedEffect(Unit) {
        val loaded = SessionRepository.loadSessions(context)
        sessions.clear()
        sessions.addAll(loaded)
    }

    // 控制 Service 启停
    LaunchedEffect(isRunning) {
        val action = if (isRunning) TimerService.ACTION_START else TimerService.ACTION_PAUSE
        context.startService(serviceIntent.apply { this.action = action })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "牛子小助手") },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentAlignment = Alignment.Center // Box 控制 LazyColumn 居中
        ) {

            // 统计展示
            val totalCount = sessions.size
            val averageTimeMin = if (totalCount > 0)
                sessions.sumOf { it.duration }.toDouble() / totalCount / 60
            else 0.0
            val now = LocalDateTime.now()
            val weekCount = sessions.count { it.timestamp.isAfter(now.minusWeeks(1)) }
            val monthCount = sessions.count { it.timestamp.isAfter(now.minusMonths(1)) }
            val yearCount = sessions.count { it.timestamp.isAfter(now.minusYears(1)) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(), // 避免占满整个高度
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {

                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    ) {
                        Text(
                            text = "记录新的手艺活",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "准备开始",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = formatTime(elapsedSeconds),
                            style = MaterialTheme.typography.displayMedium
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            Button(onClick = {
                                isRunning = !isRunning
                            }) {
                                Text(if (isRunning) "暂停" else "开始")
                            }
                            Button(onClick = {
                                if (elapsedSeconds > 0) showConfirmDialog = true
                                else Toast.makeText(context, "计时尚未开始", Toast.LENGTH_SHORT)
                                    .show()
                            }) {
                                Text("结束")
                            }
                        }
                    }
                }

                val stats = listOf(
                    "总次数: $totalCount 次",
                    "平均时间: ${"%.1f".format(averageTimeMin)} 分钟",
                    "本周次数: $weekCount 次",
                    "本月次数: $monthCount 次",
                    "今年次数: $yearCount 次"
                )
                items(stats) { info ->
                    OutlinedCard(
                        onClick = {
                            showCalendar = true
                        },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Text(
                            text = info,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // 日历热力图
            if (showCalendar) {
                val dayStats = remember { buildDayStats(sessions) }
                AlertDialog(
                    onDismissRequest = { showCalendar = false },
                    title = { Text(text = "发射日历") },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            CalendarHeatMapStyled(dayStats)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCalendar = false }) {
                            Text("关闭")
                        }
                    }
                )
            }

            // 结束对话框
            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = { Text("结束") },
                    text = { Text("要结束对牛牛的爱抚了吗？") },
                    confirmButton = {
                        TextButton(onClick = {
                            showConfirmDialog = false
                            showDetailsDialog = true
                            isRunning = false // 自动暂停计时
                        }) { Text("燃尽了") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDialog = false }) { Text("再坚持一下") }
                    }
                )
            }

            // 使用 DetailsDialog
            DetailsDialog(
                show = showDetailsDialog,
                remark = remarkInput,
                onRemarkChange = { remarkInput = it },
                location = locationInput,
                onLocationChange = { locationInput = it },
                watchedMovie = watchedMovie,
                onWatchedMovieChange = { watchedMovie = it },
                climax = climax,
                onClimaxChange = { climax = it },
                props = props,
                onPropsChange = { props = it },
                rating = rating,
                onRatingChange = { rating = it },
                mood = mood,
                onMoodChange = { mood = it },
                onConfirm = {
                    val now = LocalDateTime.now()
                    val session = Session(
                        timestamp = now,
                        duration = elapsedSeconds,
                        remark = remarkInput,
                        location = locationInput,
                        watchedMovie = watchedMovie,
                        climax = climax,
                        rating = rating,
                        mood = mood,
                        props = props
                    )
                    sessions.add(session)
                    scope.launch { SessionRepository.saveSessions(context, sessions) }

                    // 重置状态
                    isRunning = false
                    remarkInput = ""
                    locationInput = ""
                    watchedMovie = false
                    climax = false
                    rating = 3f
                    mood = "平静"
                    props = "手"
                    showDetailsDialog = false

                    // 停止服务
                    context.startService(
                        serviceIntent.apply { action = TimerService.ACTION_STOP }
                    )
                },
                onDismiss = { showDetailsDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsDialog(
    show: Boolean,
    remark: String,
    onRemarkChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    watchedMovie: Boolean,
    onWatchedMovieChange: (Boolean) -> Unit,
    climax: Boolean,
    onClimaxChange: (Boolean) -> Unit,
    props: String,
    onPropsChange: (String) -> Unit,
    rating: Float,
    onRatingChange: (Float) -> Unit,
    mood: String,
    onMoodChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("填写本次信息") },
        text = {
            val scrollState = rememberScrollState()
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // 备注
                Text("备注（可选）")
                Spacer(Modifier.height(2.dp))
                OutlinedTextField(
                    value = remark,
                    onValueChange = onRemarkChange,
                    placeholder = { Text("有什么想说的？") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                // 起飞地点
                Text("起飞地点（可选）")
                Spacer(Modifier.height(2.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = onLocationChange,
                    placeholder = { Text("例如：卧室") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                // 选项：是否观看小电影 / 是否发射（高潮）
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onWatchedMovieChange(!watchedMovie) }
                            .padding(vertical = 8.dp)
                    ) {
                        Checkbox(checked = watchedMovie, onCheckedChange = null)
                        Spacer(Modifier.width(4.dp))
                        Text("是否观看小电影")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClimaxChange(!climax) }
                            .padding(vertical = 8.dp)
                    ) {
                        Checkbox(checked = climax, onCheckedChange = null)
                        Spacer(Modifier.width(4.dp))
                        Text("是否发射")
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 道具
                Text(
                    text = "道具：",
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                val propList = listOf("手", "斐济杯", "小胶妻")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    propList.forEach { p ->
                        val isSelected = props == p

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                                )
                                .clickable { onPropsChange(p) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null, // 交由 Row 处理点击
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = p,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // 评分
                Text("评分：${"%.1f".format(rating)}")
                Slider(
                    value = rating,
                    onValueChange = onRatingChange,
                    valueRange = 0f..5f,
                    steps = 25,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0")
                    Text("5.0")
                }
                Spacer(Modifier.height(12.dp))

                // 心情
                Text(
                    text = "心情：",
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                val moods = listOf("平静", "愉悦", "兴奋", "疲惫", "这是最后一次！")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    moods.forEach { m ->
                        val isSelected = mood == m

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                                )
                                .clickable { onMoodChange(m) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = m,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@SuppressLint("DefaultLocale")
private fun formatTime(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return buildString {
        if (h > 0) append(String.format("%02d:", h))
        append(String.format("%02d:%02d", m, s))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}