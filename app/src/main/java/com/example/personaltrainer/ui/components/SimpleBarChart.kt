package com.example.personaltrainer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun SimpleBarChart(
    title: String,
    values: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    val maxValue = max(1, values.maxOrNull() ?: 1)
    val barColor: Color = MaterialTheme.colorScheme.primary
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val n = values.size.coerceAtLeast(1)
            val gap = 10.dp.toPx()
            val barW = (size.width - gap * (n - 1)) / n
            val maxH = size.height

            values.forEachIndexed { i, v ->
                val h = (v.toFloat() / maxValue) * maxH
                val x = i * (barW + gap)
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, maxH - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (labels.isNotEmpty()) Text(labels.first(), style = MaterialTheme.typography.bodySmall)
            if (labels.size > 1) Text(labels.last(), style = MaterialTheme.typography.bodySmall)
        }
    }
}
