package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ShiftTimelineBar(
    hourAssignments: List<Int>,
    guardNames: List<String>,
    modifier: Modifier = Modifier
) {
    // Memoize the calculated segments from 24h assignments
    val previewSegments = remember(hourAssignments, guardNames) {
        val segs = mutableListOf<Pair<Int, Int>>()
        var currentGuard = -1
        var currentCount = 0
        val totalGuards = guardNames.size.coerceAtLeast(1)

        for (h in 0..23) {
            val assigned = hourAssignments.getOrElse(h) { 0 }.coerceIn(0, totalGuards - 1)
            if (currentGuard == -1) {
                currentGuard = assigned
                currentCount = 1
            } else if (assigned == currentGuard) {
                currentCount++
            } else {
                segs.add(Pair(currentGuard, currentCount))
                currentGuard = assigned
                currentCount = 1
            }
        }
        if (currentGuard != -1) {
            segs.add(Pair(currentGuard, currentCount))
        }
        segs
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        previewSegments.forEach { (guardIdx, duration) ->
            val color = getPaintGuardColor(guardIdx, guardNames.size)
            val name = guardNames.getOrElse(guardIdx) { "Дежурный" }
            val shortName = if (name.length > 5) name.take(4) + ".." else name

            Box(
                modifier = Modifier
                    .weight(duration.toFloat())
                    .fillMaxHeight()
                    .background(color.copy(alpha = 0.88f))
                    .border(0.5.dp, Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$shortName (${duration}ч)",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}
