package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// High-contrast vibrant tactical hues for up to 6 guards
fun getPaintGuardColor(index: Int, total: Int): Color {
    val hues = listOf(145f, 210f, 38f, 280f, 0f, 185f) // Emerald Green, Cyber Blue, Amber Gold, Purple, Crimson, Cyan
    val h = hues.getOrElse(index) { (index * 360f / total.coerceAtLeast(1)) % 360f }
    return Color.hsv(h, 0.88f, 0.96f)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CircularHoursDial(
    startHour: Int,
    hourAssignments: List<Int>,
    onHourAssignmentsChange: (List<Int>) -> Unit,
    guardNames: List<String>,
    selectedPaintGuardIndex: Int,
    modifier: Modifier = Modifier,
    currentActiveHourIndex: Int? = null // Optional current running hour for live HUD
) {
    val haptic = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()
    val textColor = MaterialTheme.colorScheme.onSurface
    val strokeColor = MaterialTheme.colorScheme.outline

    // All 24 hour markers (0 to 23)
    val hourLabels = remember(startHour) {
        (0..23).map { h ->
            val labelHour = (startHour + h) % 24
            val labelText = String.format("%02d", labelHour)
            val isMajor = h % 2 == 0
            val theta = -90f + h * 15f + 7.5f // Center of each 15 deg slice
            val rad = Math.toRadians(theta.toDouble())
            Triple(labelText, isMajor, rad)
        }
    }

    val headerLayout = remember(textColor) {
        textMeasurer.measure(
            text = "СМЕНЫ 24 Ч",
            style = TextStyle(
                fontSize = 8.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp,
                color = textColor.copy(alpha = 0.6f)
            )
        )
    }

    val guardStatsLayouts = remember(hourAssignments, guardNames) {
        val totalGuards = guardNames.size
        val items = mutableListOf<Pair<androidx.compose.ui.text.TextLayoutResult, Color>>()

        guardNames.take(4).forEachIndexed { idx, name ->
            val totalHours = hourAssignments.count { it == idx }
            val displayName = if (name.length > 7) name.take(6) + "…" else name.ifBlank { "Деж.${idx + 1}" }
            val color = getPaintGuardColor(idx, totalGuards)
            val layout = textMeasurer.measure(
                text = "● $displayName: $totalHours ч",
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = color
                )
            )
            items.add(Pair(layout, color))
        }

        if (totalGuards > 4) {
            val othersCount = hourAssignments.count { it >= 4 }
            val layout = textMeasurer.measure(
                text = "● Другие: $othersCount ч",
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    color = textColor.copy(alpha = 0.7f)
                )
            )
            items.add(Pair(layout, textColor.copy(alpha = 0.7f)))
        }
        items
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Segmented Dial Canvas
        Box(
            modifier = Modifier
                .size(270.dp)
                .pointerInput(startHour, hourAssignments, selectedPaintGuardIndex, guardNames) {
                    // Handle tap to paint or toggle an hour
                    detectTapGestures { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val strokeWidthPx = 30.dp.toPx()
                        val outerRadius = size.width / 2f - strokeWidthPx / 2f - 18.dp.toPx()

                        val touchedHour = getHourFromOffset(offset, cx, cy, outerRadius, strokeWidthPx)
                        if (touchedHour in 0..23) {
                            val arr = hourAssignments.toMutableList()
                            val currentAssigned = arr.getOrElse(touchedHour) { 0 }
                            // If same as selected guard, cycle to next guard; otherwise set selected guard
                            val newGuard = if (currentAssigned == selectedPaintGuardIndex) {
                                (currentAssigned + 1) % guardNames.size.coerceAtLeast(1)
                            } else {
                                selectedPaintGuardIndex
                            }
                            arr[touchedHour] = newGuard
                            onHourAssignmentsChange(arr)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                }
                .pointerInput(startHour, hourAssignments, selectedPaintGuardIndex, guardNames) {
                    // Handle continuous drag to paint multiple hours
                    var lastProcessedHour = -1
                    detectDragGestures(
                        onDragStart = { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val strokeWidthPx = 30.dp.toPx()
                            val outerRadius = size.width / 2f - strokeWidthPx / 2f - 18.dp.toPx()

                            val touchedHour = getHourFromOffset(offset, cx, cy, outerRadius, strokeWidthPx)
                            if (touchedHour in 0..23) {
                                lastProcessedHour = touchedHour
                                if (hourAssignments.getOrNull(touchedHour) != selectedPaintGuardIndex) {
                                    val arr = hourAssignments.toMutableList()
                                    arr[touchedHour] = selectedPaintGuardIndex
                                    onHourAssignmentsChange(arr)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val strokeWidthPx = 30.dp.toPx()
                            val outerRadius = size.width / 2f - strokeWidthPx / 2f - 18.dp.toPx()

                            val currentHour = getHourFromOffset(change.position, cx, cy, outerRadius, strokeWidthPx)
                            if (currentHour in 0..23 && currentHour != lastProcessedHour) {
                                lastProcessedHour = currentHour
                                if (hourAssignments.getOrNull(currentHour) != selectedPaintGuardIndex) {
                                    val arr = hourAssignments.toMutableList()
                                    arr[currentHour] = selectedPaintGuardIndex
                                    onHourAssignmentsChange(arr)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                            change.consume()
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.size(270.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val strokeWidthPx = 30.dp.toPx()
                val outerRadius = size.width / 2f - strokeWidthPx / 2f - 18.dp.toPx()

                // 1. Draw subtle background track ring
                drawCircle(
                    color = Color(0xFF101712),
                    radius = outerRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = strokeWidthPx)
                )

                // 2. Draw 24 distinct individual hour segments with clear gaps and contrasting colors
                for (h in 0..23) {
                    val assignedGuardIdx = hourAssignments.getOrElse(h) { 0 }.coerceIn(0, guardNames.size - 1)
                    val baseColor = getPaintGuardColor(assignedGuardIdx, guardNames.size)
                    val isCurrentRunning = currentActiveHourIndex == h

                    // 15 degrees per hour; leave 2.2 degrees visible gap between sectors
                    val startAngle = -90f + h * 15f + 1.2f
                    val sweepAngle = 12.6f

                    // Draw the colored block for hour h
                    drawArc(
                        color = if (isCurrentRunning) Color.White else baseColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(cx - outerRadius, cy - outerRadius),
                        size = Size(outerRadius * 2f, outerRadius * 2f),
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt)
                    )

                    // Draw radial division lines between segments
                    val divisionAngle = -90f + h * 15f
                    val rad = Math.toRadians(divisionAngle.toDouble())
                    val r1 = outerRadius - strokeWidthPx / 2f - 1.dp.toPx()
                    val r2 = outerRadius + strokeWidthPx / 2f + 1.dp.toPx()
                    drawLine(
                        color = Color.Black,
                        start = Offset(cx + r1 * cos(rad).toFloat(), cy + r1 * sin(rad).toFloat()),
                        end = Offset(cx + r2 * cos(rad).toFloat(), cy + r2 * sin(rad).toFloat()),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // 3. Draw 24 Outer Hour Numbers around the perimeter
                for (i in hourLabels.indices) {
                    val (labelText, isMajor, rad) = hourLabels[i]
                    val dist = outerRadius + strokeWidthPx / 2f + 10.dp.toPx()
                    val rx = cx + dist * cos(rad).toFloat()
                    val ry = cy + dist * sin(rad).toFloat()

                    val isCurrent = currentActiveHourIndex == i
                    val textLayout = textMeasurer.measure(
                        text = labelText,
                        style = TextStyle(
                            fontSize = if (isMajor) 7.5.sp else 6.5.sp,
                            fontWeight = if (isCurrent || isMajor) FontWeight.ExtraBold else FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isCurrent) Color(0xFF00FF88) else if (isMajor) textColor else textColor.copy(alpha = 0.5f)
                        )
                    )

                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(rx - textLayout.size.width / 2f, ry - textLayout.size.height / 2f)
                    )
                }

                // 4. Central Dashboard Display
                val totalHeight = headerLayout.size.height + 4.dp.toPx() + guardStatsLayouts.sumOf { it.first.size.height + 2.dp.toPx().toInt() }
                var currentY = cy - totalHeight / 2f

                drawText(headerLayout, topLeft = Offset(cx - headerLayout.size.width / 2f, currentY))
                currentY += headerLayout.size.height + 4.dp.toPx()

                for ((statLayout, _) in guardStatsLayouts) {
                    drawText(statLayout, topLeft = Offset(cx - statLayout.size.width / 2f, currentY))
                    currentY += statLayout.size.height + 2.dp.toPx()
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 24-Hour Interactive Grid: Tap any hour to change its guard
        Text(
            text = "ВЫБОР ПО ЧАСАМ (Нажмите на час для переключения):",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (h in 0..23) {
                val absHour = (startHour + h) % 24
                val nextAbsHour = (startHour + h + 1) % 24
                val assignedGuardIdx = hourAssignments.getOrElse(h) { 0 }.coerceIn(0, guardNames.size - 1)
                val color = getPaintGuardColor(assignedGuardIdx, guardNames.size)
                val isCurrent = currentActiveHourIndex == h
                val timeLabel = String.format("%02d:00", absHour)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCurrent) color.copy(alpha = 0.45f) else color.copy(alpha = 0.18f))
                        .border(
                            width = if (isCurrent) 1.5.dp else 1.dp,
                            color = if (isCurrent) Color.White else color.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            val arr = hourAssignments.toMutableList()
                            // If already selected paint guard, cycle to next guard
                            val currentVal = arr.getOrElse(h) { 0 }
                            val nextVal = if (currentVal == selectedPaintGuardIndex) {
                                (currentVal + 1) % guardNames.size.coerceAtLeast(1)
                            } else {
                                selectedPaintGuardIndex
                            }
                            arr[h] = nextVal
                            onHourAssignmentsChange(arr)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        .padding(horizontal = 7.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = timeLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private fun getHourFromOffset(
    offset: Offset,
    cx: Float,
    cy: Float,
    outerRadius: Float,
    strokeWidthPx: Float
): Int {
    val dx = offset.x - cx
    val dy = offset.y - cy
    val distance = sqrt(dx * dx + dy * dy)

    // Touch ring around the dial (from inner radius to outer radius)
    val minThreshold = (outerRadius - strokeWidthPx - 30f).coerceAtLeast(10f)
    val maxThreshold = outerRadius + strokeWidthPx + 40f

    if (distance in minThreshold..maxThreshold) {
        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (angle < 0) {
            angle += 360f
        }
        val rotatedAngle = (angle + 90f) % 360f
        return (rotatedAngle / 15f).toInt().coerceIn(0, 23)
    }
    return -1
}
