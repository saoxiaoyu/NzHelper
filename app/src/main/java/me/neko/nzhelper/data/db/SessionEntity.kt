package me.neko.nzhelper.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity for Session data
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    val timestamp: Long,  // 存储为毫秒时间戳
    val duration: Int,
    val remark: String,
    val location: String,
    val watchedMovie: Boolean,
    val climax: Boolean,
    val rating: Float,
    val mood: String,
    val props: String
)
