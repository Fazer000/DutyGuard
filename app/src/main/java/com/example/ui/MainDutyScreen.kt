package com.example.ui

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Duty
import com.example.data.TaskPreset
import com.example.ui.components.ActiveTaskItem
import com.example.ui.components.BeautifulTimePickerDialog
import com.example.ui.screens.ActiveDutyHUD
import com.example.ui.screens.InactiveSetupCard
import com.example.ui.screens.PersonnelTerritoryScreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainDutyScreen(
    viewModel: DutyViewModel,
    modifier: Modifier = Modifier
) {
    val activeDuty by viewModel.activeDuty.collectAsStateWithLifecycle()
    val allDuties by viewModel.allDuties.collectAsStateWithLifecycle()
    val presets by viewModel.taskPresets.collectAsStateWithLifecycle()
    val occurrences by viewModel.activeOccurrences.collectAsStateWithLifecycle()
    val dutyNotes by viewModel.activeDutyNotes.collectAsStateWithLifecycle()

    val activePresets = remember(occurrences, presets) {
        val activePresetIds = occurrences.map { it.presetId }.toSet()
        presets.filter { activePresetIds.contains(it.id) }
    }

    val context = LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("duty_settings_prefs", Context.MODE_PRIVATE) }

    // Form states for creating a duty
    var dutyTitle by remember { mutableStateOf(sharedPrefs.getString("duty_title", "Суточный наряд") ?: "Суточный наряд") }
    var dailyPassword by remember { mutableStateOf(sharedPrefs.getString("daily_password", "") ?: "") }
    var selectedPresets by remember { mutableStateOf(emptySet<Int>()) }

    val savedGuardNamesStr = remember(sharedPrefs) {
        sharedPrefs.getString("guard_names_list", "") ?: ""
    }
    val decodedGuardNames = remember(savedGuardNamesStr) {
        if (savedGuardNamesStr.isNotBlank()) {
            savedGuardNamesStr.split(",")
        } else {
            listOf("Дежурный 1", "Дежурный 2")
        }
    }
    var guardNames by remember { mutableStateOf(decodedGuardNames) }

    val savedHourAssignmentsStr = remember(sharedPrefs) {
        sharedPrefs.getString("hour_assignments", "") ?: ""
    }
    val decodedHourAssignments = remember(savedHourAssignmentsStr, guardNames) {
        if (savedHourAssignmentsStr.isNotBlank()) {
            val list = savedHourAssignmentsStr.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (list.size == 24) list else null
        } else {
            null
        }
    } ?: remember(guardNames) {
        val count = guardNames.size.coerceAtLeast(2)
        IntArray(24) { (it / 2) % count }.toList()
    }
    var hourAssignments by remember { mutableStateOf(decodedHourAssignments) }

    var deviceOwnerIndex by remember { mutableIntStateOf(sharedPrefs.getInt("device_owner_index", 1)) }

    // Starting hour/minute picking state
    val calendar = remember { Calendar.getInstance() }
    var startHour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var startMinute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }

    // Preset creator states
    var showPresetCreator by remember { mutableStateOf(false) }
    var newPresetTitle by remember { mutableStateOf("") }
    var newPresetInterval by remember { mutableIntStateOf(60) }
    var newPresetDisableInFreeShift by remember { mutableStateOf(false) }
    var isNewPresetSpecificTime by remember { mutableStateOf(false) }
    var newPresetSpecificHour by remember { mutableIntStateOf(12) }
    var newPresetSpecificMinute by remember { mutableIntStateOf(0) }

    // Add task during active duty states
    var showActiveDutyAddTaskDialog by remember { mutableStateOf(false) }
    var activeAddTaskTitle by remember { mutableStateOf("") }
    var activeAddTaskInterval by remember { mutableIntStateOf(60) }
    var activeAddTaskDisableInRest by remember { mutableStateOf(false) }
    var isActiveAddTaskSpecificTime by remember { mutableStateOf(false) }
    var activeAddTaskSpecificHour by remember { mutableIntStateOf(12) }
    var activeAddTaskSpecificMinute by remember { mutableIntStateOf(0) }
    var activeAddTaskDialogTab by remember { mutableIntStateOf(0) }
    var showActiveTimePicker by remember { mutableStateOf(false) }

    var currentTab by remember { mutableIntStateOf(0) } // 0 = Duty, 1 = Personnel
    var showAddPersonDialog by remember { mutableStateOf(false) }

    // Sync selected presets configuration on database updates
    LaunchedEffect(presets) {
        if (presets.isEmpty()) {
            selectedPresets = emptySet()
            sharedPrefs.edit().putString("selected_presets", "").apply()
        } else {
            val saved = sharedPrefs.getString("selected_presets", null)
            if (saved != null) {
                val decoded = saved.split(",").mapNotNull { it.toIntOrNull() }.toSet()
                selectedPresets = decoded.filter { dId -> presets.any { it.id == dId } }.toSet()
            } else if (selectedPresets.isEmpty()) {
                selectedPresets = presets.map { it.id }.toSet()
            } else {
                selectedPresets = selectedPresets.filter { dId -> presets.any { it.id == dId } }.toSet()
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (activeDuty != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                )
                                Text(
                                    text = if (activeDuty != null) "ДЕЖУРСТВО АКТИВНО" else "ОЖИДАНИЕ ЗАСТУПЛЕНИЯ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp,
                                    color = if (activeDuty != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (activeDuty != null) activeDuty?.title ?: "Наряд на посту" else "Контроль наряда",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Vibration preview beacon
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { viewModel.testVibration() }
                                    .testTag("vibration_test_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Test Vibro",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Command badge
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ПП",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Tactical Tab Bar Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tab 0: Duties (Наряд)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (currentTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { currentTab = 0 }
                                .testTag("tab_duty"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Наряд",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Tab 1: Territory (Территория)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (currentTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { currentTab = 1 }
                                .testTag("tab_personnel"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Территория",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)
                }
            }
        },
        floatingActionButton = {
            if (currentTab == 0) {
                if (activeDuty != null) {
                    ExtendedFloatingActionButton(
                        text = { Text("Добавить задачу", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Add, contentDescription = "Добавить задачу") },
                        onClick = { showActiveDutyAddTaskDialog = true },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("fab_add_task_during_active")
                    )
                }
            } else {
                ExtendedFloatingActionButton(
                    text = { Text("Вписать человека", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Вписать человека") },
                    onClick = { showAddPersonDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag("fab_add_person")
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "MainTabsNavigationState"
        ) { tabSelection ->
            if (tabSelection == 0) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
                ) {
                    // ====================== SECTION 1: DUTY CONTROLS & HUD ======================
                    item {
                        AnimatedContent(
                            targetState = activeDuty,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "ActiveDutyState"
                        ) { duty ->
                            if (duty != null) {
                                ActiveDutyHUD(
                                    duty = duty,
                                    currentMillisFlow = viewModel.currentMillis,
                                    notes = dutyNotes,
                                    onAddNote = { text, isImportant, timeLabel -> viewModel.addDutyNote(text, isImportant, timeLabel) },
                                    onToggleNote = { note -> viewModel.toggleDutyNoteCompletion(note) },
                                    onDeleteNote = { noteId -> viewModel.deleteDutyNote(noteId) },
                                    onHandOverClick = { viewModel.stopActiveDutyFlow() },
                                    onDeviceOwnerIndexChange = { viewModel.updateDeviceOwnerIndex(it) },
                                    onUpdateDailyPassword = { viewModel.updateDailyPassword(it) }
                                )
                            } else {
                                InactiveSetupCard(
                                    dutyTitle = dutyTitle,
                                    onTitleChange = { dutyTitle = it },
                                    startHour = startHour,
                                    startMinute = startMinute,
                                    onHourChange = { startHour = it.coerceIn(0, 23) },
                                    onMinuteChange = { startMinute = it.coerceIn(0, 59) },
                                    presets = presets,
                                    selectedPresets = selectedPresets,
                                    onTogglePreset = { id ->
                                        selectedPresets = if (selectedPresets.contains(id)) {
                                            selectedPresets - id
                                        } else {
                                            selectedPresets + id
                                        }
                                    },
                                    showPresetCreator = showPresetCreator,
                                    onTogglePresetCreator = { showPresetCreator = !showPresetCreator },
                                    newPresetTitle = newPresetTitle,
                                    onPresetTitleChange = { newPresetTitle = it },
                                    newPresetInterval = newPresetInterval,
                                    onPresetIntervalChange = { newPresetInterval = it.coerceAtLeast(1) },
                                    newPresetDisableInFreeShift = newPresetDisableInFreeShift,
                                    onNewPresetDisableInFreeShiftChange = { newPresetDisableInFreeShift = it },
                                    isNewPresetSpecificTime = isNewPresetSpecificTime,
                                    onNewPresetSpecificTimeChange = { isNewPresetSpecificTime = it },
                                    newPresetSpecificHour = newPresetSpecificHour,
                                    onNewPresetSpecificHourChange = { newPresetSpecificHour = it },
                                    newPresetSpecificMinute = newPresetSpecificMinute,
                                    onNewPresetSpecificMinuteChange = { newPresetSpecificMinute = it },
                                    onAddPreset = {
                                        if (newPresetTitle.isNotBlank()) {
                                            viewModel.addPreset(
                                                title = newPresetTitle,
                                                intervalMinutes = if (isNewPresetSpecificTime) 0 else newPresetInterval,
                                                disableInFreeShift = newPresetDisableInFreeShift,
                                                isSpecificTime = isNewPresetSpecificTime,
                                                specificHour = newPresetSpecificHour,
                                                specificMinute = newPresetSpecificMinute
                                            )
                                            newPresetTitle = ""
                                            newPresetDisableInFreeShift = false
                                            isNewPresetSpecificTime = false
                                            showPresetCreator = false
                                        }
                                    },
                                    onDeletePreset = { viewModel.deletePreset(it) },
                                    onTogglePresetFreeShiftDisable = { viewModel.togglePresetFreeShiftDisable(it) },
                                    guardNames = guardNames,
                                    onGuardNamesChange = { guardNames = it },
                                    hourAssignments = hourAssignments,
                                    onHourAssignmentsChange = { hourAssignments = it },
                                    deviceOwnerIndex = deviceOwnerIndex,
                                    onDeviceOwnerIndexChange = { deviceOwnerIndex = it },
                                    dailyPassword = dailyPassword,
                                    onDailyPasswordChange = { dailyPassword = it },
                                    notes = dutyNotes,
                                    onAddNote = { text, isImportant, timeLabel -> viewModel.addDutyNote(text, isImportant, timeLabel) },
                                    onToggleNote = { note -> viewModel.toggleDutyNoteCompletion(note) },
                                    onDeleteNote = { noteId -> viewModel.deleteDutyNote(noteId) },
                                    onStartDuty = {
                                        sharedPrefs.edit().apply {
                                            putString("duty_title", dutyTitle)
                                            putString("daily_password", dailyPassword)
                                            putString("guard_names_list", guardNames.joinToString(","))
                                            putString("hour_assignments", hourAssignments.joinToString(","))
                                            putInt("device_owner_index", deviceOwnerIndex)
                                            putString("selected_presets", selectedPresets.joinToString(","))
                                            apply()
                                        }

                                        val cal = Calendar.getInstance().apply {
                                            set(Calendar.HOUR_OF_DAY, startHour)
                                            set(Calendar.MINUTE, startMinute)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        viewModel.startNewDuty(
                                            title = dutyTitle,
                                            startEpochMillis = cal.timeInMillis,
                                            selectedPresetIds = selectedPresets,
                                            guardOneName = guardNames.getOrElse(0) { "Дежурный 1" },
                                            guardTwoName = guardNames.getOrElse(1) { "Дежурный 2" },
                                            deviceOwnerIndex = deviceOwnerIndex,
                                            guardNamesStr = guardNames.joinToString(","),
                                            hourAssignmentsStr = hourAssignments.joinToString(","),
                                            dailyPassword = dailyPassword
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // ====================== SECTION 2: GENERAL SHIFT TASKS OR HISTORY ======================
                    if (activeDuty != null) {
                        item {
                            Text(
                                text = "ОБЩИЙ СПИСОК ЗАДАЧ НА НАРЯД",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (activePresets.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(
                                        text = "Нет подключенных задач к текущему дежурству.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        textAlign = TextAlign.Center,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            items(activePresets, key = { it.id }) { preset ->
                                ActiveTaskItem(preset = preset)
                            }
                        }
                    } else {
                        // HISTORICAL RECORD LIST
                        item {
                            Text(
                                text = "ИСТОРИЯ ДЕЖУРСТВ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        val historicalDuties = allDuties.filter { !it.isActive }
                        if (historicalDuties.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(
                                        text = "История пуста. Ваши завершенные дежурства будут отображаться здесь.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        textAlign = TextAlign.Center,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            items(historicalDuties, key = { it.id }) { historyDuty ->
                                val formatStart = remember(historyDuty.startEpochMillis) {
                                    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(historyDuty.startEpochMillis)
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = historyDuty.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Начало: $formatStart",
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteHistoryDuty(historyDuty) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Удалить архив",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                PersonnelTerritoryScreen(
                    viewModel = viewModel,
                    innerPadding = innerPadding
                )
            }
        }
    }

    // Add Person Dialog
    if (showAddPersonDialog) {
        var personRank by remember { mutableStateOf("Рядовой") }
        var personCallsign by remember { mutableStateOf("") }
        var personIsPresent by remember { mutableStateOf(true) }
        var expandedDropdown by remember { mutableStateOf(false) }

        val ranks = remember {
            listOf(
                "Рядовой", "Ефрейтор", "Мл. сержант", "Сержант", "Ст. сержант", "Старшина",
                "Прапорщик", "Ст. прапорщик", "Мл. лейтенант", "Лейтенант", "Ст. лейтенант",
                "Капитан", "Майор", "Подполковник", "Полковник"
            )
        }

        AlertDialog(
            onDismissRequest = { showAddPersonDialog = false },
            title = { Text("Вписать человека", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box {
                        OutlinedTextField(
                            value = personRank,
                            onValueChange = {},
                            label = { Text("Воинское звание") },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expandedDropdown = !expandedDropdown }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Выбор звания"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedDropdown = !expandedDropdown }
                                .testTag("person_rank_field")
                        )
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            ranks.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        personRank = r
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = personCallsign,
                        onValueChange = { personCallsign = it },
                        label = { Text("Позывной (или Фамилия)") },
                        placeholder = { Text("например, Сахалин / Петров") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("person_callsign_field")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (personIsPresent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { personIsPresent = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (personIsPresent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "В части",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (personIsPresent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!personIsPresent) MaterialTheme.colorScheme.errorContainer else Color.Transparent)
                                .clickable { personIsPresent = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (!personIsPresent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Уехал",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!personIsPresent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (personCallsign.isNotBlank()) {
                            viewModel.addTerritoryPerson(
                                rank = personRank,
                                callsign = personCallsign.trim(),
                                isPresent = personIsPresent
                            )
                            showAddPersonDialog = false
                        }
                    },
                    enabled = personCallsign.isNotBlank()
                ) {
                    Text("Вписать", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPersonDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Add task during active duty dialog
    if (showActiveDutyAddTaskDialog) {
        AlertDialog(
            onDismissRequest = {
                showActiveDutyAddTaskDialog = false
                activeAddTaskTitle = ""
                activeAddTaskInterval = 60
                activeAddTaskDisableInRest = false
                isActiveAddTaskSpecificTime = false
                activeAddTaskSpecificHour = 12
                activeAddTaskSpecificMinute = 0
                activeAddTaskDialogTab = 0
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            title = {
                Text(
                    text = "Добавить задачу в наряд",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TabRow(
                        selectedTabIndex = activeAddTaskDialogTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Tab(
                            selected = activeAddTaskDialogTab == 0,
                            onClick = { activeAddTaskDialogTab = 0 },
                            text = { Text("Шаблоны", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        )
                        Tab(
                            selected = activeAddTaskDialogTab == 1,
                            onClick = { activeAddTaskDialogTab = 1 },
                            text = { Text("Новая задача", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (activeAddTaskDialogTab == 0) {
                        val inactivePresets = remember(occurrences, presets) {
                            val activePresetIds = occurrences.map { it.presetId }.toSet()
                            presets.filter { !activePresetIds.contains(it.id) }
                        }

                        if (inactivePresets.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Все шаблоны уже добавлены. Создайте новую задачу во вкладке выше!",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(inactivePresets) { preset ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewModel.addPresetToActiveDuty(preset)
                                                showActiveDutyAddTaskDialog = false
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = preset.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
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
                                                    text = unitLabel,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Добавить в наряд",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = activeAddTaskTitle,
                            onValueChange = { activeAddTaskTitle = it },
                            label = { Text("Название задачи", fontSize = 12.sp) },
                            placeholder = { Text("Например, Обход территории", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            textStyle = TextStyle(fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Тип задачи",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !isActiveAddTaskSpecificTime,
                                onClick = { isActiveAddTaskSpecificTime = false },
                                label = { Text("Интервал", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = isActiveAddTaskSpecificTime,
                                onClick = { isActiveAddTaskSpecificTime = true },
                                label = { Text("Точное время", fontSize = 11.sp) }
                            )
                        }

                        if (!isActiveAddTaskSpecificTime) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Интервал: $activeAddTaskInterval мин",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val commonIntervals = listOf(5, 10, 15, 30, 45, 60, 120, 180, 240)
                                commonIntervals.forEach { mins ->
                                    val hrsLabel = if (mins >= 60) " (${mins / 60} ч)" else ""
                                    FilterChip(
                                        selected = activeAddTaskInterval == mins,
                                        onClick = { activeAddTaskInterval = mins },
                                        label = { Text("$mins мин$hrsLabel", fontSize = 11.sp) }
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { showActiveTimePicker = true }
                                    .padding(12.dp),
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
                                            contentDescription = "Select Time",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text("Время срабатывания", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        Text(
                                            text = String.format("%02d : %02d", activeAddTaskSpecificHour, activeAddTaskSpecificMinute),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Button(
                                    onClick = { showActiveTimePicker = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    elevation = null,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Выбрать", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (showActiveTimePicker) {
                                BeautifulTimePickerDialog(
                                    title = "Время срабатывания",
                                    initialHour = activeAddTaskSpecificHour,
                                    initialMinute = activeAddTaskSpecificMinute,
                                    onDismissRequest = { showActiveTimePicker = false },
                                    onTimeSelected = { h, m ->
                                        activeAddTaskSpecificHour = h
                                        activeAddTaskSpecificMinute = m
                                        showActiveTimePicker = false
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Отключать во время отдыха",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Пропускать оповещение во время отдыха",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = activeAddTaskDisableInRest,
                                onCheckedChange = { activeAddTaskDisableInRest = it },
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
                    }
                }
            },
            confirmButton = {
                if (activeAddTaskDialogTab == 1) {
                    Button(
                        onClick = {
                            if (activeAddTaskTitle.isNotBlank()) {
                                viewModel.addPresetAndAddToActiveDuty(
                                    title = activeAddTaskTitle,
                                    intervalMinutes = if (isActiveAddTaskSpecificTime) 0 else activeAddTaskInterval,
                                    disableInFreeShift = activeAddTaskDisableInRest,
                                    isSpecificTime = isActiveAddTaskSpecificTime,
                                    specificHour = activeAddTaskSpecificHour,
                                    specificMinute = activeAddTaskSpecificMinute
                                )
                                activeAddTaskTitle = ""
                                activeAddTaskInterval = 60
                                activeAddTaskDisableInRest = false
                                isActiveAddTaskSpecificTime = false
                                activeAddTaskSpecificHour = 12
                                activeAddTaskSpecificMinute = 0
                                activeAddTaskDialogTab = 0
                                showActiveDutyAddTaskDialog = false
                            }
                        },
                        enabled = activeAddTaskTitle.isNotBlank()
                    ) {
                        Text("Создать и Добавить")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showActiveDutyAddTaskDialog = false
                        activeAddTaskTitle = ""
                        activeAddTaskInterval = 60
                        activeAddTaskDisableInRest = false
                        isActiveAddTaskSpecificTime = false
                        activeAddTaskSpecificHour = 12
                        activeAddTaskSpecificMinute = 0
                        activeAddTaskDialogTab = 0
                    }
                ) {
                    Text(if (activeAddTaskDialogTab == 0) "Закрыть" else "Отмена")
                }
            }
        )
    }
}
