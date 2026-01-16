package me.neko.nzhelper.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * WebDAV 操作结果
 */
sealed class WebDavResult<out T> {
    data class Success<T>(val data: T) : WebDavResult<T>()
    data class Error(val message: String, val code: Int = -1) : WebDavResult<Nothing>()
}

/**
 * WebDAV 客户端
 * 使用 OkHttp 实现 WebDAV 协议的基本操作
 */
object WebDavClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

    /**
     * 测试 WebDAV 连接
     */
    suspend fun testConnection(
        url: String,
        username: String,
        password: String
    ): WebDavResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", Credentials.basic(username, password))
                .method("PROPFIND", null)
                .header("Depth", "0")
                .build()

            val response = client.newCall(request).execute()
            response.use {
                if (it.isSuccessful || it.code == 207) { // 207 Multi-Status is success for PROPFIND
                    WebDavResult.Success(true)
                } else {
                    WebDavResult.Error("连接失败: ${it.code} ${it.message}", it.code)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            WebDavResult.Error(e.message ?: "连接失败")
        }
    }

    /**
     * 上传数据到 WebDAV
     */
    suspend fun upload(
        url: String,
        username: String,
        password: String,
        data: String
    ): WebDavResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val requestBody = data.toRequestBody(JSON_MEDIA_TYPE.toMediaType())
            
            val request = Request.Builder()
                .url(url)
                .header("Authorization", Credentials.basic(username, password))
                .put(requestBody)
                .build()

            val response = client.newCall(request).execute()
            response.use {
                if (it.isSuccessful || it.code == 201 || it.code == 204) {
                    WebDavResult.Success(true)
                } else {
                    WebDavResult.Error("上传失败: ${it.code} ${it.message}", it.code)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            WebDavResult.Error(e.message ?: "上传失败")
        }
    }

    /**
     * 从 WebDAV 下载数据
     */
    suspend fun download(
        url: String,
        username: String,
        password: String
    ): WebDavResult<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", Credentials.basic(username, password))
                .get()
                .build()

            val response = client.newCall(request).execute()
            response.use {
                if (it.isSuccessful) {
                    val body = it.body?.string()
                    if (body != null) {
                        WebDavResult.Success(body)
                    } else {
                        WebDavResult.Error("下载数据为空")
                    }
                } else if (it.code == 404) {
                    WebDavResult.Error("备份文件不存在", 404)
                } else {
                    WebDavResult.Error("下载失败: ${it.code} ${it.message}", it.code)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            WebDavResult.Error(e.message ?: "下载失败")
        }
    }
}
