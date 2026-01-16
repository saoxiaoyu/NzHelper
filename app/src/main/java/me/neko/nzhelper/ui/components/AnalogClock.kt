package me.neko.nzhelper.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

/**
 * 模拟时钟组件
 * 显示当前时间的模拟时钟
 */
@Composable
fun AnalogClock(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    hourHandColor: Color = MaterialTheme.colorScheme.primary,
    minuteHandColor: Color = MaterialTheme.colorScheme.primary,
    secondHandColor: Color = MaterialTheme.colorScheme.error,
    dialColor: Color = MaterialTheme.colorScheme.outline,
    centerDotColor: Color = MaterialTheme.colorScheme.primary
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // 每秒更新一次
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    val calendar = remember(currentTime) {
        Calendar.getInstance().apply { timeInMillis = currentTime }
    }

    val hours = calendar.get(Calendar.HOUR)
    val minutes = calendar.get(Calendar.MINUTE)
    val seconds = calendar.get(Calendar.SECOND)

    Canvas(modifier = modifier.size(size)) {
        val centerX = this.size.width / 2
        val centerY = this.size.height / 2
        val radius = minOf(centerX, centerY)

        // 绘制表盘刻度
        drawClockDial(centerX, centerY, radius, dialColor)

        // 绘制时针
        val hourAngle = Math.toRadians(((hours % 12) * 30 + minutes * 0.5 - 90).toDouble())
        drawHand(
            centerX = centerX,
            centerY = centerY,
            length = radius * 0.5f,
            angle = hourAngle,
            color = hourHandColor,
            strokeWidth = 8f
        )

        // 绘制分针
        val minuteAngle = Math.toRadians((minutes * 6 + seconds * 0.1 - 90).toDouble())
        drawHand(
            centerX = centerX,
            centerY = centerY,
            length = radius * 0.7f,
            angle = minuteAngle,
            color = minuteHandColor,
            strokeWidth = 5f
        )

        // 绘制秒针
        val secondAngle = Math.toRadians((seconds * 6 - 90).toDouble())
        drawHand(
            centerX = centerX,
            centerY = centerY,
            length = radius * 0.85f,
            angle = secondAngle,
            color = secondHandColor,
            strokeWidth = 2f
        )

        // 绘制中心点
        drawCircle(
            color = centerDotColor,
            radius = 8f,
            center = Offset(centerX, centerY)
        )
    }
}

private fun DrawScope.drawClockDial(
    centerX: Float,
    centerY: Float,
    radius: Float,
    color: Color
) {
    // 绘制外圈
    drawCircle(
        color = color,
        radius = radius,
        center = Offset(centerX, centerY),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
    )

    // 绘制 12 个小时刻度
    for (i in 0 until 12) {
        val angle = Math.toRadians((i * 30 - 90).toDouble())
        val isMainMark = i % 3 == 0
        val innerRadius = if (isMainMark) radius * 0.8f else radius * 0.88f
        val outerRadius = radius * 0.95f
        val strokeWidth = if (isMainMark) 4f else 2f

        val startX = centerX + innerRadius * cos(angle).toFloat()
        val startY = centerY + innerRadius * sin(angle).toFloat()
        val endX = centerX + outerRadius * cos(angle).toFloat()
        val endY = centerY + outerRadius * sin(angle).toFloat()

        drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawHand(
    centerX: Float,
    centerY: Float,
    length: Float,
    angle: Double,
    color: Color,
    strokeWidth: Float
) {
    val endX = centerX + length * cos(angle).toFloat()
    val endY = centerY + length * sin(angle).toFloat()

    drawLine(
        color = color,
        start = Offset(centerX, centerY),
        end = Offset(endX, endY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}
