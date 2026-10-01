package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.DutyNote
import com.example.data.GuardInterval
import com.example.data.TaskPreset
import com.example.ui.components.BeautifulTimePickerDialog
import com.example.ui.components.CircularHoursDial
import com.example.ui.components.DutyNotesCard
import com.example.ui.components.ShiftTimelineBar
import com.example.ui.components.getPaintGuardColor
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InactiveSetupCard(
    dutyTitle: String,
    onTitleChange: (String) -> Unit,
    startHour: Int,
    startMinute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    presets: List<TaskPreset>,
    selectedPresets: Set<Int>,
    onTogglePreset: (Int) -> Unit,
    showPresetCreator: Boolean,
    onTogglePresetCreator: () -> Unit,
    newPresetTitle: String,
    onPresetTitleChange: (String) -> Unit,
    newPresetInterval: Int,
    onPresetIntervalChange: (Int) -> Unit,
    newPresetDisableInFreeShift: Boolean,
    onNewPresetDisableInFreeShiftChange: (Boolean) -> Unit,
    isNewPresetSpecificTime: Boolean,
    onNewPresetSpecificTimeChange: (Boolean) -> Unit,
    newPresetSpecificHour: Int,
    onNewPresetSpecificHourChange: (Int) -> Unit,
    newPresetSpecificMinute: Int,
    onNewPresetSpecificMinuteChange: (Int) -> Unit,
    onAddPreset: () -> Unit,
    onDeletePreset: (TaskPreset) -> Unit,
    onTogglePresetFreeShiftDisable: (TaskPreset) -> Unit,
    guardNames: List<String>,
    onGuardNamesChange: (List<String>) -> Unit,
    hourAssignments: List<Int>,
    onHourAssignmentsChange: (List<Int>) -> Unit,
    deviceOwnerIndex: Int,
    onDeviceOwnerIndexChange: (Int) -> Unit,
    dailyPassword: String,
    onDailyPasswordChange: (String) -> Unit,
    notes: List<DutyNote> = emptyList(),
    onAddNote: (String, Boolean, String) -> Unit = { _, _, _ -> },
    onToggleNote: (DutyNote) -> Unit = {},
    onDeleteNote: (Long) -> Unit = {},
    onStartDuty: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartTimePicker by remember { mutableStateOf(false) }
    var selectedPaintGuardIndex by remember { mutableIntStateOf(0) }
    var showIntervalDialog by remember { mutableStateOf(false) }
    var intervalFromHour by remember { mutableIntStateOf(0) }
    var intervalToHour by remember { mutableIntStateOf(4) }

    if (selectedPaintGuardIndex >= guardNames.size) {
        selectedPaintGuardIndex = 0
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("setup_duty_card")
    ) {
        // Bento Panel 1: Post Name Input & Daily Password
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "ЗАСТУПЛЕНИЕ НА ДЕЖУРСТВО",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dutyTitle,
                    onValueChange = onTitleChange,
                    label = { Text("Название наряда / пост") },
                    placeholder = { Text("например: Дежурный по части") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("duty_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                val parts = remember(dailyPassword) {
                    val split = if (dailyPassword.contains(" - ")) {
                        dailyPassword.split(" - ")
                    } else if (dailyPassword.contains("-")) {
                        dailyPassword.split("-")
                    } else if (dailyPassword.contains(" — ")) {
                        dailyPassword.split(" — ")
                    } else {
                        listOf(dailyPassword)
                    }
                    Pair(split.getOrNull(0)?.trim() ?: "", split.getOrNull(1)?.trim() ?: "")
                }

                var callPart by remember(dailyPassword) { mutableStateOf(parts.first) }
                var responsePart by remember(dailyPassword) { mutableStateOf(parts.second) }

                Text(
                    text = "СУТОЧНЫЙ ПАРОЛЬ (СЛОВО - СЛОВО):",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = callPart,
                        onValueChange = {
                            callPart = it
                            val final = if (it.isNotBlank() && responsePart.isNotBlank()) "${it.trim()} - ${responsePart.trim()}" else if (it.isNotBlank()) it.trim() else responsePart.trim()
                            onDailyPasswordChange(final)
                        },
                        label = { Text("Пароль") },
                        placeholder = { Text("Рязань") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("setup_password_call"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Text(
                        text = "—",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 16.sp
                    )

                    OutlinedTextField(
                        value = responsePart,
                        onValueChange = {
                            responsePart = it
                            val final = if (callPart.isNotBlank() && it.isNotBlank()) "${callPart.trim()} - ${it.trim()}" else if (it.isNotBlank()) it.trim() else callPart.trim()
                            onDailyPasswordChange(final)
                        },
                        label = { Text("Отзыв") },
                        placeholder = { Text("Пенза") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("setup_password_response"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }

        // Bento Panel 2: Start Time & Quick Adjustments
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "ВРЕМЯ НАЧАЛА НАРЯДА",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { showStartTimePicker = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Time",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Начало дежурства (нажмите для выбора)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = String.format("%02d : %02d", startHour, startMinute),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ИЗМЕНИТЬ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fast Time Adjustments Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Reset to NOW
                    FilterChip(
                        selected = false,
                        onClick = {
                            val now = Calendar.getInstance()
                            onHourChange(now.get(Calendar.HOUR_OF_DAY))
                            onMinuteChange(now.get(Calendar.MINUTE))
                        },
                        label = { Text("Текущее", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )

                    // 08:00 Standard
                    FilterChip(
                        selected = startHour == 8 && startMinute == 0,
                        onClick = {
                            onHourChange(8)
                            onMinuteChange(0)
                        },
                        label = { Text("08:00", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )

                    // 09:00 Standard
                    FilterChip(
                        selected = startHour == 9 && startMinute == 0,
                        onClick = {
                            onHourChange(9)
                            onMinuteChange(0)
                        },
                        label = { Text("09:00", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )

                    // 18:00 Evening
                    FilterChip(
                        selected = startHour == 18 && startMinute == 0,
                        onClick = {
                            onHourChange(18)
                            onMinuteChange(0)
                        },
                        label = { Text("18:00", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Bento Panel 3: Guard List & Circular Shift Schedule
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "ЛИЧНЫЙ СОСТАВ И ГРАФИК СМЕН",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )

                // Editable Guard Names
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    guardNames.forEachIndexed { idx, name ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(getPaintGuardColor(idx, guardNames.size))
                            )

                            OutlinedTextField(
                                value = name,
                                onValueChange = { newName ->
                                    val updated = guardNames.toMutableList()
                                    updated[idx] = newName
                                    onGuardNamesChange(updated)
                                },
                                label = { Text("Дежурный ${idx + 1}") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("guard_${idx + 1}_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            if (guardNames.size > 2) {
                                IconButton(
                                    onClick = {
                                        val updated = guardNames.toMutableList()
                                        updated.removeAt(idx)
                                        onGuardNamesChange(updated)

                                        val updatedAssignments = hourAssignments.map { assignment ->
                                            if (assignment >= updated.size) {
                                                0
                                            } else if (assignment == idx) {
                                                0
                                            } else if (assignment > idx) {
                                                assignment - 1
                                            } else {
                                                assignment
                                            }
                                        }
                                        onHourAssignmentsChange(updatedAssignments)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Удалить дежурного",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (guardNames.size < 6) {
                        Button(
                            onClick = {
                                val updated = guardNames.toMutableList()
                                val nextNum = updated.size + 1
                                updated.add("Дежурный $nextNum")
                                onGuardNamesChange(updated)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Добавить дежурного", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Добавить дежурного", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)

                // Predefined Preset Buttons
                Text(
                    text = "БЫСТРЫЕ ШАБЛОНЫ РАСПРЕДЕЛЕНИЯ:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.secondary
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Army Preset (2-1-4)
                    Button(
                        onClick = {
                            val armySplits = getArmyPresetSplits(startHour)
                            val arr = IntArray(24) { 1 }
                            var prev = 0
                            var isG1 = true
                            for (s in armySplits) {
                                for (h in prev until s) {
                                    if (h in 0..23) {
                                        arr[h] = if (isG1) 0 else 1
                                    }
                                }
                                prev = s
                                isG1 = !isG1
                            }
                            onHourAssignmentsChange(arr.toList())
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Армия (2-1-4)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // By 2 Hours
                    Button(
                        onClick = {
                            val count = guardNames.size
                            val arr = IntArray(24) { (it / 2) % count }
                            onHourAssignmentsChange(arr.toList())
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("По 2 часа", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // By 3 Hours
                    Button(
                        onClick = {
                            val count = guardNames.size
                            val arr = IntArray(24) { (it / 3) % count }
                            onHourAssignmentsChange(arr.toList())
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("По 3 часа", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // By 4 Hours
                    Button(
                        onClick = {
                            val count = guardNames.size
                            val arr = IntArray(24) { (it / 4) % count }
                            onHourAssignmentsChange(arr.toList())
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("По 4 часа", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // 12 / 12 (Day / Night)
                    Button(
                        onClick = {
                            val count = guardNames.size
                            val arr = IntArray(24) { (it / 12) % count }
                            onHourAssignmentsChange(arr.toList())
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("12 / 12 (День/Ночь)", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Even split
                    Button(
                        onClick = {
                            val count = guardNames.size
                            val chunk = 24 / count
                            val arr = IntArray(24) { (it / chunk).coerceIn(0, count - 1) }
                            onHourAssignmentsChange(arr.toList())
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Равномерно", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)

                // Paint Brush Selection
                Text(
                    text = "ВЫБЕРИТЕ ДЕЖУРНОГО ДЛЯ ОКРАСКИ СМЕНЫ:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.secondary
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    guardNames.forEachIndexed { idx, name ->
                        val isSelected = selectedPaintGuardIndex == idx
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPaintGuardIndex = idx },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(
                                                color = getPaintGuardColor(idx, guardNames.size),
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = name.ifBlank { "Дежурный ${idx + 1}" },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        )
                    }
                }

                Text(
                    text = "КОЛЬЦЕВОЙ ГРАФИК (Проведите по кольцу или нажмите на нужный час):",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.secondary
                )

                // Render High-Performance Circular Dial (Full Width)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularHoursDial(
                        startHour = startHour,
                        hourAssignments = hourAssignments,
                        onHourAssignmentsChange = onHourAssignmentsChange,
                        guardNames = guardNames,
                        selectedPaintGuardIndex = selectedPaintGuardIndex,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                // 24h Timeline preview bar
                Text(
                    text = "ВИЗУАЛЬНАЯ ШКАЛА СМЕН (24 ЧАСА):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )

                ShiftTimelineBar(
                    hourAssignments = hourAssignments,
                    guardNames = guardNames
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                // Device owner index selection beforehand
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Владелец этого устройства (для бесшумных уведомлений во время отдыха):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val choices = listOf("Общий режим") + guardNames
                        choices.forEachIndexed { index, name ->
                            val isSelected = deviceOwnerIndex == index
                            FilterChip(
                                selected = isSelected,
                                onClick = { onDeviceOwnerIndexChange(index) },
                                label = { Text(name, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                }
            }
        }

        // Bento Panel 4: Notes on Duty (Заметки и поручения на наряд)
        DutyNotesCard(
            notes = notes,
            onAddNote = onAddNote,
            onToggleNote = onToggleNote,
            onDeleteNote = onDeleteNote,
            titleText = "ПРЕДВАРИТЕЛЬНЫЕ ЗАМЕТКИ НА НАРЯД"
        )

        // Bento Panel 5: Recurring Rules & Tasks
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ПЕРИОДИЧЕСКИЕ ЗАДАЧИ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.sp
                    )

                    TextButton(
                        onClick = onTogglePresetCreator,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (showPresetCreator) Icons.Default.Clear else Icons.Default.Add,
                            contentDescription = "New Preset",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showPresetCreator) "Закрыть" else "Создать",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showPresetCreator,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Новый шаблон задачи:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            OutlinedTextField(
                                value = newPresetTitle,
                                onValueChange = onPresetTitleChange,
                                label = { Text("Название задачи") },
                                placeholder = { Text("например: Доклад дежурному") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_preset_title"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Specific Time vs Periodic Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = !isNewPresetSpecificTime,
                                    onClick = { onNewPresetSpecificTimeChange(false) },
                                    label = { Text("Регулярно (интервал)", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = isNewPresetSpecificTime,
                                    onClick = { onNewPresetSpecificTimeChange(true) },
                                    label = { Text("Точное время (раз в сутки)", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (!isNewPresetSpecificTime) {
                                Text(
                                    text = "Периодичность:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val options = listOf(15 to "15 мин", 30 to "30 мин", 60 to "1 час", 120 to "2 часа", 240 to "4 часа")
                                    options.forEach { (mins, label) ->
                                        val isSel = newPresetInterval == mins
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { onPresetIntervalChange(mins) },
                                            label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                                        )
                                    }
                                }
                            } else {
                                var showPresetTimePicker by remember { mutableStateOf(false) }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { showPresetTimePicker = true }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = "Время срабатывания",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Время выполнения задачи",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = String.format("%02d : %02d", newPresetSpecificHour, newPresetSpecificMinute),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { showPresetTimePicker = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Выбрать", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (showPresetTimePicker) {
                                    BeautifulTimePickerDialog(
                                        title = "Время выполнения задачи",
                                        initialHour = newPresetSpecificHour,
                                        initialMinute = newPresetSpecificMinute,
                                        onDismissRequest = { showPresetTimePicker = false },
                                        onTimeSelected = { h, m ->
                                            onNewPresetSpecificHourChange(h)
                                            onNewPresetSpecificMinuteChange(m)
                                            showPresetTimePicker = false
                                        }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Отключать в свободную смену",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Бесшумный режим при Вашем отдыхе",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Switch(
                                    checked = newPresetDisableInFreeShift,
                                    onCheckedChange = onNewPresetDisableInFreeShiftChange,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = Color(0xFFC5D1C6),
                                        uncheckedTrackColor = Color(0xFF222F24),
                                        uncheckedBorderColor = Color(0xFF384E3C),
                                        checkedBorderColor = Color.Transparent
                                    )
                                )
                            }

                            Button(
                                onClick = onAddPreset,
                                enabled = newPresetTitle.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Добавить в список", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (presets.isEmpty()) {
                    Text(
                        text = "Задачи отсутствуют. Нажмите «Создать» чтобы добавить регулярный обход или доклад.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 8.dp)
                    )
                } else {
                    presets.forEach { preset ->
                        val isSelected = selectedPresets.contains(preset.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onTogglePreset(preset.id) }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(
                                        text = preset.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val unitLabel = if (preset.isSpecificTime) {
                                        String.format("Точное время %02d:%02d", preset.specificHour, preset.specificMinute)
                                    } else if (preset.intervalMinutes >= 60) {
                                        val h = preset.intervalMinutes / 60
                                        "каждые $h ч."
                                    } else {
                                        "каждые ${preset.intervalMinutes} мин."
                                    }
                                    Text(
                                        text = if (preset.isSpecificTime) unitLabel else "Периодичность: $unitLabel",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                    )
                                    // Switch-status indicator for easy toggle
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onTogglePresetFreeShiftDisable(preset) }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = null,
                                            tint = if (preset.disableInFreeShift) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = if (preset.disableInFreeShift) "Откл. во время отдыха (нажмите переключить)" else "Активна всегда (нажмите переключить)",
                                            fontSize = 10.sp,
                                            color = if (preset.disableInFreeShift) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            IconButton(onClick = { onDeletePreset(preset) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete preset",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.82f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bento deploying main action button
        Button(
            onClick = onStartDuty,
            enabled = true,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.outline,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("start_duty_button")
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Start duty",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ЗАСТУПИТЬ НА НАРЯД",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
        }
    }

    if (showStartTimePicker) {
        BeautifulTimePickerDialog(
            title = "Начало дежурства",
            initialHour = startHour,
            initialMinute = startMinute,
            onDismissRequest = { showStartTimePicker = false },
            onTimeSelected = { h, m ->
                onHourChange(h)
                onMinuteChange(m)
                showStartTimePicker = false
            }
        )
    }
}

private fun getArmyPresetSplits(startHour: Int): List<Int> {
    val splits = mutableListOf<Int>()
    var cursor = 0
    while (cursor < 24) {
        val absHour = (startHour + cursor) % 24
        val duration = when {
            absHour in 0..5 -> 4 // Night: 00:00 - 05:59
            absHour in 6..17 -> 2 // Day: 06:00 - 17:59
            else -> 1 // Evening: 18:00 - 23:59
        }
        val next = cursor + duration
        if (next <= 24) {
            splits.add(next)
        } else {
            splits.add(24)
        }
        cursor = next
    }
    return splits.distinct().sorted()
}
