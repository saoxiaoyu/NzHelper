package me.neko.nzhelper.data

import android.content.Context
import android.net.Uri
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.neko.nzhelper.NzApplication
import me.neko.nzhelper.data.db.AppDatabase
import me.neko.nzhelper.data.db.SessionEntity
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// 公共数据类 - 保持不变，用于 UI 层
data class Session(
    val id: Long = 0,
    val timestamp: LocalDateTime,
    val duration: Int,
    val remark: String,
    val location: String,
    val watchedMovie: Boolean,
    val climax: Boolean,
    val rating: Float,
    val mood: String,
    val props: String
)

/**
 * 导入结果封装类
 */
sealed class ImportResult {
    data class Success(val sessions: List<Session>, val count: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

object SessionRepository {

    private val gson = NzApplication.gson
    private val sessionsTypeToken = object : TypeToken<List<Session>>() {}.type

    // ==================== Entity <-> Session 转换 ====================
    
    private fun SessionEntity.toSession(): Session = Session(
        id = id,
        timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()),
        duration = duration,
        remark = remark,
        location = location,
        watchedMovie = watchedMovie,
        climax = climax,
        rating = rating,
        mood = mood,
        props = props
    )

    private fun Session.toEntity(): SessionEntity = SessionEntity(
        id = id,
        timestamp = timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        duration = duration,
        remark = remark,
        location = location,
        watchedMovie = watchedMovie,
        climax = climax,
        rating = rating,
        mood = mood,
        props = props
    )

    // ==================== 数据库访问 ====================
    
    private fun getDao(context: Context) = AppDatabase.getDatabase(context).sessionDao()

    /**
     * 获取所有会话的 Flow
     */
    fun getSessionsFlow(context: Context): Flow<List<Session>> {
        return getDao(context).getAllSessionsFlow().map { entities ->
            entities.map { it.toSession() }
        }
    }

    suspend fun loadSessions(context: Context): List<Session> = withContext(Dispatchers.IO) {
        getDao(context).getAllSessions().map { it.toSession() }
    }

    suspend fun saveSessions(context: Context, sessions: List<Session>) =
        withContext(Dispatchers.IO) {
            val dao = getDao(context)
            dao.deleteAll()
            dao.insertAll(sessions.map { it.toEntity() })
        }

    suspend fun addSession(context: Context, session: Session): Long =
        withContext(Dispatchers.IO) {
            getDao(context).insert(session.toEntity())
        }

    suspend fun deleteSession(context: Context, session: Session) =
        withContext(Dispatchers.IO) {
            getDao(context).delete(session.toEntity())
        }

    suspend fun deleteAllSessions(context: Context) =
        withContext(Dispatchers.IO) {
            getDao(context).deleteAll()
        }

    // ==================== 导入/导出 ====================

    /**
     * 从 JSON 输入流导入会话数据
     * 支持新格式（对象数组）和旧格式（嵌套数组）
     */
    suspend fun importFromJson(context: Context, inputStream: InputStream): ImportResult =
        withContext(Dispatchers.IO) {
            try {
                val jsonStr = inputStream.bufferedReader().readText()
                val importedSessions = mutableListOf<Session>()

                val root = JsonParser.parseString(jsonStr).asJsonArray
                
                for (elem in root) {
                    try {
                        if (elem.isJsonObject) {
                            // 新格式：对象数组 (手动解析以处理可选的 id 字段)
                            val obj = elem.asJsonObject
                            val timestampStr = obj.get("timestamp")?.asString ?: continue
                            val timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            
                            importedSessions.add(
                                Session(
                                    id = 0, // 导入时总是使用新 id
                                    timestamp = timestamp,
                                    duration = obj.get("duration")?.asInt ?: 0,
                                    remark = if (obj.has("remark") && !obj.get("remark").isJsonNull) obj.get("remark").asString else "",
                                    location = if (obj.has("location") && !obj.get("location").isJsonNull) obj.get("location").asString else "",
                                    watchedMovie = obj.get("watchedMovie")?.asBoolean ?: false,
                                    climax = obj.get("climax")?.asBoolean ?: false,
                                    rating = if (obj.has("rating") && !obj.get("rating").isJsonNull) obj.get("rating").asFloat.coerceIn(0f, 5f) else 3f,
                                    mood = if (obj.has("mood") && !obj.get("mood").isJsonNull) obj.get("mood").asString else "平静",
                                    props = if (obj.has("props") && !obj.get("props").isJsonNull) obj.get("props").asString else "手"
                                )
                            )
                        } else if (elem.isJsonArray) {
                            // 旧格式：嵌套数组
                            val arr = elem.asJsonArray
                            val timeStr = arr[0].asString
                            val timestamp = LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            
                            importedSessions.add(
                                Session(
                                    timestamp = timestamp,
                                    duration = if (arr.size() > 1) arr[1].asInt else 0,
                                    remark = if (arr.size() > 2 && !arr[2].isJsonNull) arr[2].asString else "",
                                    location = if (arr.size() > 3 && !arr[3].isJsonNull) arr[3].asString else "",
                                    watchedMovie = if (arr.size() > 4) arr[4].asBoolean else false,
                                    climax = if (arr.size() > 5) arr[5].asBoolean else false,
                                    rating = if (arr.size() > 6 && !arr[6].isJsonNull) arr[6].asFloat.coerceIn(0f, 5f) else 3f,
                                    mood = if (arr.size() > 7 && !arr[7].isJsonNull) arr[7].asString else "平静",
                                    props = if (arr.size() > 8 && !arr[8].isJsonNull) arr[8].asString else "手"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // 跳过解析失败的单条记录，继续处理其他记录
                    }
                }

                if (importedSessions.isNotEmpty()) {
                    saveSessions(context, importedSessions)
                    ImportResult.Success(importedSessions, importedSessions.size)
                } else {
                    ImportResult.Error("文件格式不正确或没有有效数据")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ImportResult.Error(e.message ?: "导入失败")
            }
        }

    /**
     * 从 URI 导入会话数据
     */
    suspend fun importFromUri(context: Context, uri: Uri): ImportResult =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    importFromJson(context, inputStream)
                } ?: ImportResult.Error("无法打开文件")
            } catch (e: Exception) {
                e.printStackTrace()
                ImportResult.Error(e.message ?: "导入失败")
            }
        }

    /**
     * 导出会话数据到输出流
     */
    suspend fun exportToStream(context: Context, outputStream: OutputStream): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val sessions = loadSessions(context)
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(gson.toJson(sessions))
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    /**
     * 导出会话数据到 URI
     */
    suspend fun exportToUri(context: Context, uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    exportToStream(context, outputStream)
                } ?: false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    // ==================== WebDAV 备份/恢复 ====================

    private const val WEBDAV_BACKUP_FILENAME = "nzhelper_backup.json"

    /**
     * 备份数据到 WebDAV
     */
    suspend fun backupToWebDav(context: Context): WebDavResult<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val url = SettingsRepository.getWebDavUrl(context).trimEnd('/') + "/" + WEBDAV_BACKUP_FILENAME
                val username = SettingsRepository.getWebDavUsername(context)
                val password = SettingsRepository.getWebDavPassword(context)

                if (url.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    return@withContext WebDavResult.Error("WebDAV 未配置")
                }

                val sessions = loadSessions(context)
                val jsonData = gson.toJson(sessions)

                WebDavClient.upload(url, username, password, jsonData)
            } catch (e: Exception) {
                e.printStackTrace()
                WebDavResult.Error(e.message ?: "备份失败")
            }
        }

    /**
     * 从 WebDAV 恢复数据
     */
    suspend fun restoreFromWebDav(context: Context): ImportResult =
        withContext(Dispatchers.IO) {
            try {
                val url = SettingsRepository.getWebDavUrl(context).trimEnd('/') + "/" + WEBDAV_BACKUP_FILENAME
                val username = SettingsRepository.getWebDavUsername(context)
                val password = SettingsRepository.getWebDavPassword(context)

                if (url.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    return@withContext ImportResult.Error("WebDAV 未配置")
                }

                when (val result = WebDavClient.download(url, username, password)) {
                    is WebDavResult.Success -> {
                        // 解析下载的 JSON 数据
                        importFromJsonString(context, result.data)
                    }
                    is WebDavResult.Error -> {
                        ImportResult.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ImportResult.Error(e.message ?: "恢复失败")
            }
        }

    /**
     * 从 JSON 字符串导入会话数据
     */
    private suspend fun importFromJsonString(context: Context, jsonStr: String): ImportResult =
        withContext(Dispatchers.IO) {
            try {
                val importedSessions = mutableListOf<Session>()
                val root = JsonParser.parseString(jsonStr).asJsonArray

                for (elem in root) {
                    try {
                        if (elem.isJsonObject) {
                            val obj = elem.asJsonObject
                            val timestampStr = obj.get("timestamp")?.asString ?: continue
                            val timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)

                            importedSessions.add(
                                Session(
                                    id = 0,
                                    timestamp = timestamp,
                                    duration = obj.get("duration")?.asInt ?: 0,
                                    remark = if (obj.has("remark") && !obj.get("remark").isJsonNull) obj.get("remark").asString else "",
                                    location = if (obj.has("location") && !obj.get("location").isJsonNull) obj.get("location").asString else "",
                                    watchedMovie = obj.get("watchedMovie")?.asBoolean ?: false,
                                    climax = obj.get("climax")?.asBoolean ?: false,
                                    rating = if (obj.has("rating") && !obj.get("rating").isJsonNull) obj.get("rating").asFloat.coerceIn(0f, 5f) else 3f,
                                    mood = if (obj.has("mood") && !obj.get("mood").isJsonNull) obj.get("mood").asString else "平静",
                                    props = if (obj.has("props") && !obj.get("props").isJsonNull) obj.get("props").asString else "手"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (importedSessions.isNotEmpty()) {
                    saveSessions(context, importedSessions)
                    ImportResult.Success(importedSessions, importedSessions.size)
                } else {
                    ImportResult.Error("备份文件格式不正确")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ImportResult.Error(e.message ?: "解析备份数据失败")
            }
        }
}
