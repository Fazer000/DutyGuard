package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Duty
import com.example.data.DutyNote
import com.example.data.ShiftSegment
import com.example.ui.components.CircularHoursDial
import com.example.ui.components.DailyPasswordDialog
import com.example.ui.components.DailyPasswordHUDCard
import com.example.ui.components.DutyNotesCard
import com.example.ui.components.ShiftTimelineBar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveDutyHUD(
    duty: Duty,
    currentMillisFlow: StateFlow<Long>,
    notes: List<DutyNote>,
    onAddNote: (text: String, isImportant: Boolean, timeLabel: String) -> Unit,
    onToggleNote: (DutyNote) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onHandOverClick: () -> Unit,
    onDeviceOwnerIndexChange: (Int) -> Unit,
    onUpdateDailyPassword: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect the 1-second clock ticker here inside the HUD so other screens don't recompose
    val currentMillis by currentMillisFlow.collectAsStateWithLifecycle()

    val durationLeft = duty.endEpochMillis - currentMillis
    val isFinished = durationLeft <= 0

    val countdownText = remember(durationLeft, isFinished) {
        if (isFinished) {
            "НАРЯД ЗАВЕРШЕН!"
        } else {
            val totalSec = (durationLeft / 1000).coerceAtLeast(0)
            val hrs = totalSec / 3600
            val mins = (totalSec % 3600) / 60
            val secs = totalSec % 60
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        }
    }

    val totalDuration = (duty.endEpochMillis - duty.startEpochMillis).coerceAtLeast(1)
    val elapsed = (currentMillis - duty.startEpochMillis).coerceAtLeast(0)
    val progress = (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

    val formatStart = remember(duty.startEpochMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(duty.startEpochMillis)
    }
    val formatEndOnlyTime = remember(duty.endEpochMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(duty.endEpochMillis)
    }
    val hasTomorrowLabel = remember(duty.startEpochMillis, duty.endEpochMillis) {
        val calStart = Calendar.getInstance().apply { timeInMillis = duty.startEpochMillis }
        val calEnd = Calendar.getInstance().apply { timeInMillis = duty.endEpochMillis }
        calStart.get(Calendar.DAY_OF_YEAR) != calEnd.get(Calendar.DAY_OF_YEAR)
    }

    val startHour = remember(duty.startEpochMillis) {
        Calendar.getInstance().apply { timeInMillis = duty.startEpochMillis }.get(Calendar.HOUR_OF_DAY)
    }

    val currentActiveHourIndex = remember(currentMillis, duty.startEpochMillis) {
        if (currentMillis < duty.startEpochMillis) -1
        else ((currentMillis - duty.startEpochMillis) / (3600 * 1000L)).toInt().coerceIn(0, 23)
    }

    val guardNames = remember(duty) { duty.getGuardNames() }
    val hourAssignments = remember(duty) { duty.getHourAssignments() }

    // Shift segments calculation
    val segments = remember(duty) { ShiftSegment.getShiftSegments(duty) }
    val activeSegment = remember(segments, currentMillis) {
        segments.find { currentMillis >= it.startMillis && currentMillis < it.endMillis }
    }
    var showSegmentsList by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. MAIN COCKPIT BENTO BOX (Timer & Progress)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("active_duty_card"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "До конца наряда",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    // "В ПРОЦЕССЕ" Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isFinished) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            .padding(vertical = 4.dp, horizontal = 10.dp)
                    ) {
                        Text(
                            text = if (isFinished) "ГОТОВО" else "В ПРОЦЕССЕ",
                            color = if (isFinished) Color.White else Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // GIGANTIC MONOSPACE TIMER
                Text(
                    text = countdownText,
                    fontSize = 42.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-1.5).sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )

                // PROGRESS BAR
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        // 2. DAILY PASSWORD CARD (СУТОЧНЫЙ ПАРОЛЬ)
        DailyPasswordHUDCard(
            dailyPassword = duty.dailyPassword,
            onEditClick = { showPasswordDialog = true }
        )

        // 3. DUTY NOTES CARD (ЗАМЕТКИ И ОСОБЫЕ ПОРУЧЕНИЯ НА НАРЯД)
        DutyNotesCard(
            notes = notes,
            onAddNote = onAddNote,
            onToggleNote = onToggleNote,
            onDeleteNote = onDeleteNote
        )

        // 4. ACTIVE SHIFT DETAILS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "РАСПРЕДЕЛЕНИЕ СМЕН",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )

                if (activeSegment != null) {
                    val shiftLeft = activeSegment.endMillis - currentMillis
                    val shiftLeftText = if (shiftLeft <= 0) {
                        "Смена прямо сейчас!"
                    } else {
                        val totalMin = (shiftLeft / 60000).coerceAtLeast(0)
                        val min = totalMin % 60
                        val hrs = totalMin / 60
                        if (hrs > 0) "${hrs} ч. ${min} мин." else "${min} мин."
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "НА ПОСТУ СЕЙЧАС",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activeSegment.activeGuardName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Осталось до смены: $shiftLeftText",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "СВОБОДНАЯ СМЕНА / ОТДЫХАЕТ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = activeSegment.restingGuardName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        ) {
                            Text(
                                text = "ТИХИЙ СЕКТОР",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Смены не рассчитаны или наряд завершен.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 24-hour timeline bar
                ShiftTimelineBar(
                    hourAssignments = hourAssignments,
                    guardNames = guardNames
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)

                // Profile selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ТЕКУЩИЙ ПРОФИЛЬ УСТРОЙСТВА:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 0.5.sp
                    )

                    val guardList = duty.getGuardNames()
                    val choices = listOf("Общий") + guardList

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        choices.forEachIndexed { index, name ->
                            val isSelected = duty.deviceOwnerIndex == index
                            FilterChip(
                                selected = isSelected,
                                onClick = { onDeviceOwnerIndexChange(index) },
                                label = {
                                    Text(
                                        text = name + (if (index > 0 && isSelected) " (Я)" else ""),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            )
                        }
                    }

                    val ownerName = if (duty.deviceOwnerIndex in 1..guardList.size) {
                        guardList[duty.deviceOwnerIndex - 1]
                    } else null

                    Text(
                        text = if (ownerName != null) {
                            "Уведомления для $ownerName будут бесшумно пропускаться во время отдыха."
                        } else {
                            "Общий режим. Все уведомления будут воспроизводиться для всех дежурных."
                        },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // 5. EXPANDABLE 24-HOUR ROTATION SCHEDULE & CIRCULAR DIAL
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showSegmentsList = !showSegmentsList }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "КОЛЬЦЕВОЙ ГРАФИК И СМЕНЫ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Icon(
                        imageVector = if (showSegmentsList) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = showSegmentsList,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Circular Dial in Active Mode showing live active hour index
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularHoursDial(
                                startHour = startHour,
                                hourAssignments = hourAssignments,
                                onHourAssignmentsChange = {}, // Read-only during active duty
                                guardNames = guardNames,
                                selectedPaintGuardIndex = 0,
                                currentActiveHourIndex = currentActiveHourIndex,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        segments.forEach { seg ->
                            val isCurrent = currentMillis >= seg.startMillis && currentMillis < seg.endMillis
                            val isPast = currentMillis >= seg.endMillis
                            val timeLabel = "${sdf.format(seg.startMillis)} - ${sdf.format(seg.endMillis)}"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = if (isCurrent) 1.5.dp else 1.dp,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = timeLabel,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else if (isPast) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "На посту: ${seg.activeGuardName}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else if (isPast) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Отдых: ${seg.restingGuardName}",
                                        fontSize = 11.sp,
                                        color = if (isPast) MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isCurrent) MaterialTheme.colorScheme.primary
                                             else if (isPast) MaterialTheme.colorScheme.surfaceVariant
                                             else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                        .padding(vertical = 2.dp, horizontal = 6.dp)
                                ) {
                                    Text(
                                        text = if (isCurrent) "СЕЙЧАС" else if (isPast) "ПРОШЛА" else "БУДЕТ",
                                        color = if (isCurrent) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. TWO GRID ITEMS - START & END
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Start Cell
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "НАЧАЛО",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = formatStart,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // End/Handover Cell
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "СМЕНА",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = formatEndOnlyTime,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (hasTomorrowLabel) {
                            Text(
                                text = "Завтра",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Handover button
        Button(
            onClick = onHandOverClick,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("stop_duty_button")
        ) {
            Icon(Icons.Default.Clear, contentDescription = "Сдать наряд", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "СДАТЬ ДЕЖУРСТВО",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp
            )
        }
    }

    // Daily Password Edit Dialog
    if (showPasswordDialog) {
        DailyPasswordDialog(
            initialPassword = duty.dailyPassword,
            onDismissRequest = { showPasswordDialog = false },
            onSavePassword = { newPassword ->
                onUpdateDailyPassword(newPassword)
                showPasswordDialog = false
            }
        )
    }
}
