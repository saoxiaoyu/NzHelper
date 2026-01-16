package me.neko.nzhelper.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.neko.nzhelper.data.Session
import me.neko.nzhelper.data.SessionRepository
import me.neko.nzhelper.ui.service.TimerService
import java.time.LocalDateTime

/**
 * HomeScreen 的 ViewModel
 * 管理计时器状态、会话数据和对话框状态
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // ==================== Timer Service ====================
    
    private var timerService: TimerService? = null
    private val serviceIntent = Intent(application, TimerService::class.java)
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            timerService = (binder as TimerService.LocalBinder).getService()
            // 订阅服务的计时状态
            viewModelScope.launch {
                timerService?.elapsedSec?.collect { seconds ->
                    _elapsedSeconds.value = seconds
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
        }
    }

    // ==================== 计时器状态 ====================
    
    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // ==================== 会话数据 ====================
    
    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    // ==================== 对话框状态 ====================
    
    private val _showConfirmDialog = MutableStateFlow(false)
    val showConfirmDialog: StateFlow<Boolean> = _showConfirmDialog.asStateFlow()

    private val _showDetailsDialog = MutableStateFlow(false)
    val showDetailsDialog: StateFlow<Boolean> = _showDetailsDialog.asStateFlow()

    // ==================== 表单输入状态 ====================
    
    private val _remarkInput = MutableStateFlow("")
    val remarkInput: StateFlow<String> = _remarkInput.asStateFlow()

    private val _locationInput = MutableStateFlow("")
    val locationInput: StateFlow<String> = _locationInput.asStateFlow()

    private val _watchedMovie = MutableStateFlow(false)
    val watchedMovie: StateFlow<Boolean> = _watchedMovie.asStateFlow()

    private val _climax = MutableStateFlow(false)
    val climax: StateFlow<Boolean> = _climax.asStateFlow()

    private val _rating = MutableStateFlow(3f)
    val rating: StateFlow<Float> = _rating.asStateFlow()

    private val _mood = MutableStateFlow("平静")
    val mood: StateFlow<String> = _mood.asStateFlow()

    private val _props = MutableStateFlow("手")
    val props: StateFlow<String> = _props.asStateFlow()

    // ==================== 初始化 ====================
    
    init {
        bindService()
        loadSessions()
    }

    private fun bindService() {
        val context = getApplication<Application>()
        ContextCompat.startForegroundService(
            context,
            serviceIntent.apply { action = TimerService.ACTION_START }
        )
        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun loadSessions() {
        viewModelScope.launch {
            val loaded = SessionRepository.loadSessions(getApplication())
            _sessions.value = loaded
        }
    }

    // ==================== 计时器控制 ====================
    
    fun toggleTimer() {
        _isRunning.value = !_isRunning.value
        val action = if (_isRunning.value) TimerService.ACTION_START else TimerService.ACTION_PAUSE
        getApplication<Application>().startService(serviceIntent.apply { this.action = action })
    }

    fun requestStop() {
        if (_elapsedSeconds.value > 0) {
            _showConfirmDialog.value = true
        }
    }

    fun confirmStop() {
        _showConfirmDialog.value = false
        _showDetailsDialog.value = true
        _isRunning.value = false
    }

    fun cancelStop() {
        _showConfirmDialog.value = false
    }

    // ==================== 表单更新方法 ====================
    
    fun updateRemark(value: String) { _remarkInput.value = value }
    fun updateLocation(value: String) { _locationInput.value = value }
    fun updateWatchedMovie(value: Boolean) { _watchedMovie.value = value }
    fun updateClimax(value: Boolean) { _climax.value = value }
    fun updateRating(value: Float) { _rating.value = value }
    fun updateMood(value: String) { _mood.value = value }
    fun updateProps(value: String) { _props.value = value }

    // ==================== 保存会话 ====================
    
    fun saveSession() {
        val session = Session(
            timestamp = LocalDateTime.now(),
            duration = _elapsedSeconds.value,
            remark = _remarkInput.value,
            location = _locationInput.value,
            watchedMovie = _watchedMovie.value,
            climax = _climax.value,
            rating = _rating.value,
            mood = _mood.value,
            props = _props.value
        )

        viewModelScope.launch {
            val updatedSessions = _sessions.value + session
            _sessions.value = updatedSessions
            SessionRepository.saveSessions(getApplication(), updatedSessions)
        }

        // 重置状态
        resetForm()
        _showDetailsDialog.value = false

        // 停止计时服务
        getApplication<Application>().startService(
            serviceIntent.apply { action = TimerService.ACTION_STOP }
        )
    }

    fun dismissDetailsDialog() {
        _showDetailsDialog.value = false
    }

    private fun resetForm() {
        _isRunning.value = false
        _remarkInput.value = ""
        _locationInput.value = ""
        _watchedMovie.value = false
        _climax.value = false
        _rating.value = 3f
        _mood.value = "平静"
        _props.value = "手"
    }

    // ==================== 生命周期 ====================
    
    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unbindService(serviceConnection)
        } catch (_: Exception) {
            // Service may not be bound
        }
    }
}
