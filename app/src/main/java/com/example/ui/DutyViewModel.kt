package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Duty
import com.example.data.DutyNote
import com.example.data.TaskOccurrence
import com.example.data.TaskPreset
import com.example.data.TerritoryPerson
import com.example.utils.AlarmScheduler
import com.example.utils.NotificationHelper
import com.example.utils.VibrationHelper
import java.util.Calendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DutyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dutyDao = db.dutyDao()
    private val presetDao = db.taskPresetDao()
    private val occurrenceDao = db.taskOccurrenceDao()
    private val territoryPersonDao = db.territoryPersonDao()
    private val noteDao = db.dutyNoteDao()
    private val seedPrefs = application.getSharedPreferences("app_seed_preferences", Context.MODE_PRIVATE)

    // 1. Reactive Clock Ticker for countdown timer (only listened to by timer composables)
    val currentMillis: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

    // 2. Flows for Room Database Queries
    val activeDuty: StateFlow<Duty?> = dutyDao.getActiveDutyFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allDuties: StateFlow<List<Duty>> = dutyDao.getAllDutiesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val taskPresets: StateFlow<List<TaskPreset>> = presetDao.getAllPresetsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPersonnel: StateFlow<List<TerritoryPerson>> = territoryPersonDao.getAllPersonnelFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic Flow of occurrences for the active duty using flatMapLatest
    val activeOccurrences: StateFlow<List<TaskOccurrence>> = activeDuty.flatMapLatest { duty ->
        if (duty != null) {
            occurrenceDao.getOccurrencesForDutyFlow(duty.id)
        } else {
            NotificationHelper.cancelTaskNotification(getApplication())
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic Flow of notes for the active duty or draft setup
    val activeDutyNotes: StateFlow<List<DutyNote>> = activeDuty.flatMapLatest { duty ->
        if (duty != null) {
            noteDao.getNotesForDutyFlow(duty.id)
        } else {
            noteDao.getNotesForDutyFlow(0L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Pre-populate standard military presets ONLY ONCE on very first launch
        val hasSeededPresets = seedPrefs.getBoolean("has_seeded_military_presets_v1", false)
        if (!hasSeededPresets) {
            viewModelScope.launch {
                val existing = presetDao.getAllPresetsFlow().first()
                if (existing.isEmpty()) {
                    presetDao.insertPreset(TaskPreset(title = "Доклад дежурному по роте", intervalMinutes = 60))
                    presetDao.insertPreset(TaskPreset(title = "Осмотр территории и постов", intervalMinutes = 120))
                    presetDao.insertPreset(TaskPreset(title = "Смена караульных / часовых", intervalMinutes = 120))
                    presetDao.insertPreset(TaskPreset(title = "Проверка сигнализации КХО", intervalMinutes = 30))
                    presetDao.insertPreset(TaskPreset(title = "Контрольное время связи", intervalMinutes = 15))
                }
                seedPrefs.edit().putBoolean("has_seeded_military_presets_v1", true).apply()
            }
        }

        // Pre-populate standard personnel list ONLY ONCE on very first launch
        val hasSeededPersonnel = seedPrefs.getBoolean("has_seeded_military_personnel_v1", false)
        if (!hasSeededPersonnel) {
            viewModelScope.launch {
                val existing = territoryPersonDao.getAllPersonnelFlow().first()
                if (existing.isEmpty()) {
                    territoryPersonDao.insertPerson(TerritoryPerson(rank = "Капитан", callsign = "Сокол", isPresent = true))
                    territoryPersonDao.insertPerson(TerritoryPerson(rank = "Лейтенант", callsign = "Гриф", isPresent = true))
                    territoryPersonDao.insertPerson(TerritoryPerson(rank = "Сержант", callsign = "Гром", isPresent = false))
                    territoryPersonDao.insertPerson(TerritoryPerson(rank = "Ефрейтор", callsign = "Скиф", isPresent = true))
                    territoryPersonDao.insertPerson(TerritoryPerson(rank = "Рядовой", callsign = "Ветер", isPresent = true))
                }
                seedPrefs.edit().putBoolean("has_seeded_military_personnel_v1", true).apply()
            }
        }
    }

    // 3. User operations for Duty and Presets

    fun addPreset(
        title: String,
        intervalMinutes: Int,
        disableInFreeShift: Boolean = false,
        isSpecificTime: Boolean = false,
        specificHour: Int = 0,
        specificMinute: Int = 0
    ) {
        viewModelScope.launch {
            presetDao.insertPreset(
                TaskPreset(
                    title = title.trim(),
                    intervalMinutes = intervalMinutes,
                    disableInFreeShift = disableInFreeShift,
                    isSpecificTime = isSpecificTime,
                    specificHour = specificHour,
                    specificMinute = specificMinute
                )
            )
        }
    }

    fun addPresetToActiveDuty(preset: TaskPreset) {
        viewModelScope.launch {
            val active = dutyDao.getActiveDuty() ?: return@launch
            val occurrences = generateOccurrencesForPreset(
                dutyId = active.id,
                dutyStart = active.startEpochMillis,
                dutyEnd = endEpochMillis(active.startEpochMillis),
                preset = preset
            )
            occurrenceDao.insertOccurrences(occurrences)

            val now = System.currentTimeMillis()
            occurrences.forEach { occurrence ->
                if (occurrence.targetTimeEpochMillis > now) {
                    AlarmScheduler.scheduleAlarm(getApplication(), occurrence)
                }
            }
        }
    }

    fun addPresetAndAddToActiveDuty(
        title: String,
        intervalMinutes: Int,
        disableInFreeShift: Boolean = false,
        isSpecificTime: Boolean = false,
        specificHour: Int = 0,
        specificMinute: Int = 0
    ) {
        viewModelScope.launch {
            val active = dutyDao.getActiveDuty() ?: return@launch
            val newPreset = TaskPreset(
                title = title.trim(),
                intervalMinutes = intervalMinutes,
                disableInFreeShift = disableInFreeShift,
                isSpecificTime = isSpecificTime,
                specificHour = specificHour,
                specificMinute = specificMinute
            )
            val insertedId = presetDao.insertPreset(newPreset)
            val presetWithId = newPreset.copy(id = insertedId.toInt())
            val occurrences = generateOccurrencesForPreset(
                dutyId = active.id,
                dutyStart = active.startEpochMillis,
                dutyEnd = endEpochMillis(active.startEpochMillis),
                preset = presetWithId
            )
            occurrenceDao.insertOccurrences(occurrences)

            val now = System.currentTimeMillis()
            occurrences.forEach { occurrence ->
                if (occurrence.targetTimeEpochMillis > now) {
                    AlarmScheduler.scheduleAlarm(getApplication(), occurrence)
                }
            }
        }
    }

    fun deletePreset(preset: TaskPreset) {
        viewModelScope.launch {
            presetDao.deletePreset(preset)
            occurrenceDao.deleteOccurrencesByPreset(preset.id)
        }
    }

    fun togglePresetFreeShiftDisable(preset: TaskPreset) {
        viewModelScope.launch {
            presetDao.insertPreset(preset.copy(disableInFreeShift = !preset.disableInFreeShift))
        }
    }

    private fun endEpochMillis(start: Long): Long = start + 24 * 60 * 60 * 1000L

    /**
     * Starts a new 24-hour military duty.
     */
    fun startNewDuty(
        title: String, 
        startEpochMillis: Long, 
        selectedPresetIds: Set<Int>,
        guardOneName: String = "Дежурный 1",
        guardTwoName: String = "Дежурный 2",
        shiftCustomIntervals: String = "0-2,4-6,8-10,12-14,16-18,20-22",
        deviceOwnerIndex: Int = 1,
        guardNamesStr: String = "Дежурный 1,Дежурный 2",
        hourAssignmentsStr: String = "",
        dailyPassword: String = ""
    ) {
        viewModelScope.launch {
            val endEpochMillis = startEpochMillis + (24 * 60 * 60 * 1000) // 24-hour shift
            val newDuty = Duty(
                id = startEpochMillis,
                title = title.ifBlank { "Суточный наряд" },
                startEpochMillis = startEpochMillis,
                endEpochMillis = endEpochMillis,
                isActive = true,
                guardOneName = guardOneName.ifBlank { "Дежурный 1" },
                guardTwoName = guardTwoName.ifBlank { "Дежурный 2" },
                shiftCustomIntervals = shiftCustomIntervals,
                deviceOwnerIndex = deviceOwnerIndex,
                guardNamesStr = guardNamesStr,
                hourAssignmentsStr = hourAssignmentsStr,
                dailyPassword = dailyPassword.trim()
            )

            // Deactivate existing duty active states
            dutyDao.insertDuty(newDuty)
            dutyDao.deactivateAllBut(newDuty.id)

            // Transfer any draft notes created before starting duty (dutyId = 0) to this duty
            val draftNotes = noteDao.getNotesForDuty(0L)
            draftNotes.forEach { draft ->
                noteDao.insertNote(draft.copy(id = 0, dutyId = newDuty.id))
            }
            noteDao.deleteNotesForDuty(0L)

            // Cancel any old active alarms
            activeOccurrences.value.forEach { occurrence ->
                AlarmScheduler.cancelAlarm(getApplication(), occurrence)
            }

            // Fetch selected presets
            val allPresets = presetDao.getAllPresetsFlow().first()
            val selectedPresets = allPresets.filter { selectedPresetIds.contains(it.id) }

            // Generate occurrences
            val occurrences = mutableListOf<TaskOccurrence>()
            selectedPresets.forEach { preset ->
                val generated = generateOccurrencesForPreset(
                    dutyId = newDuty.id,
                    dutyStart = startEpochMillis,
                    dutyEnd = endEpochMillis,
                    preset = preset
                )
                occurrences.addAll(generated)
            }

            // Save in database
            occurrenceDao.insertOccurrences(occurrences)

            // Register background alerts in AlarmManager for all future tasks
            val now = System.currentTimeMillis()
            occurrences.forEach { occurrence ->
                if (occurrence.targetTimeEpochMillis > now) {
                    AlarmScheduler.scheduleAlarm(getApplication(), occurrence)
                }
            }
        }
    }

    fun updateDeviceOwnerIndex(index: Int) {
        viewModelScope.launch {
            val active = dutyDao.getActiveDuty()
            if (active != null) {
                val updated = active.copy(deviceOwnerIndex = index)
                dutyDao.updateDuty(updated)
            }
        }
    }

    /**
     * Updates the daily password during an active duty.
     */
    fun updateDailyPassword(password: String) {
        viewModelScope.launch {
            val active = dutyDao.getActiveDuty()
            if (active != null) {
                dutyDao.updateDailyPassword(active.id, password.trim())
            }
        }
    }

    /**
     * Terminates the current active duty, automatically removing and cancelling all impending alarms.
     */
    fun stopActiveDutyFlow() {
        viewModelScope.launch {
            val active = dutyDao.getActiveDuty()
            if (active != null) {
                // Cancel scheduled alarms in AlarmManager
                val occurrences = occurrenceDao.getOccurrencesForDuty(active.id)
                occurrences.forEach { occurrence ->
                    AlarmScheduler.cancelAlarm(getApplication(), occurrence)
                }

                // Deactivate the duty
                val updated = active.copy(isActive = false)
                dutyDao.updateDuty(updated)

                // Cancel active task notifications
                NotificationHelper.cancelTaskNotification(getApplication())
            }
        }
    }

    fun deleteHistoryDuty(duty: Duty) {
        viewModelScope.launch {
            if (duty.isActive) {
                stopActiveDutyFlow()
            }
            dutyDao.deleteDutyById(duty.id)
            occurrenceDao.deleteOccurrencesForDuty(duty.id)
            noteDao.deleteNotesForDuty(duty.id)
        }
    }

    // ================== DUTY NOTES CRUD ==================

    fun addDutyNote(text: String, isImportant: Boolean = false, timeLabel: String = "") {
        if (text.isBlank()) return
        viewModelScope.launch {
            val active = dutyDao.getActiveDuty()
            val targetDutyId = active?.id ?: 0L
            noteDao.insertNote(
                DutyNote(
                    dutyId = targetDutyId,
                    text = text.trim(),
                    isImportant = isImportant,
                    timeLabel = timeLabel.trim()
                )
            )
        }
    }

    fun toggleDutyNoteCompletion(note: DutyNote) {
        viewModelScope.launch {
            noteDao.updateNoteCompletion(note.id, !note.isCompleted)
        }
    }

    fun deleteDutyNote(noteId: Long) {
        viewModelScope.launch {
            noteDao.deleteNoteById(noteId)
        }
    }

    /**
     * Toggles the completion checkbox of a scheduled task occurrence.
     */
    fun toggleOccurrenceCompletion(occurrence: TaskOccurrence) {
        viewModelScope.launch {
            val newCompleted = !occurrence.isCompleted
            occurrenceDao.updateCompletion(
                dutyId = occurrence.dutyId,
                presetId = occurrence.presetId,
                targetTime = occurrence.targetTimeEpochMillis,
                isCompleted = newCompleted
            )

            val now = System.currentTimeMillis()
            val updatedOccurrence = occurrence.copy(isCompleted = newCompleted)
            if (newCompleted) {
                AlarmScheduler.cancelAlarm(getApplication(), updatedOccurrence)
            } else if (occurrence.targetTimeEpochMillis > now) {
                AlarmScheduler.scheduleAlarm(getApplication(), updatedOccurrence)
            }
        }
    }

    fun testVibration() {
        VibrationHelper.triggerLongVibration(getApplication())
    }

    private fun generateOccurrencesForPreset(
        dutyId: Long,
        dutyStart: Long,
        dutyEnd: Long,
        preset: TaskPreset
    ): List<TaskOccurrence> {
        val list = mutableListOf<TaskOccurrence>()
        val roundedStart = (dutyStart / 60000) * 60000
        var clockCursor = roundedStart
        val calendar = Calendar.getInstance()

        while (clockCursor <= dutyEnd) {
            calendar.timeInMillis = clockCursor
            val m = calendar.get(Calendar.MINUTE)
            val h = calendar.get(Calendar.HOUR_OF_DAY)

            val matchesRules = if (preset.isSpecificTime) {
                h == preset.specificHour && m == preset.specificMinute
            } else {
                if (preset.intervalMinutes < 60) {
                    m % preset.intervalMinutes == 0
                } else {
                    val hrs = preset.intervalMinutes / 60
                    m == 0 && (h % hrs == 0)
                }
            }

            if (matchesRules && clockCursor >= System.currentTimeMillis() - 60000L) {
                list.add(
                    TaskOccurrence(
                        dutyId = dutyId,
                        presetId = preset.id,
                        title = preset.title,
                        targetTimeEpochMillis = clockCursor,
                        isCompleted = false,
                        disableInFreeShift = preset.disableInFreeShift
                    )
                )
            }
            clockCursor += 60000L
        }

        return list
    }

    // 4. Territory Personnel CRUD & Toggles
    fun addTerritoryPerson(rank: String, callsign: String, isPresent: Boolean) {
        viewModelScope.launch {
            territoryPersonDao.insertPerson(
                TerritoryPerson(
                    rank = rank,
                    callsign = callsign,
                    isPresent = isPresent,
                    lastUpdatedMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun togglePersonPresence(person: TerritoryPerson) {
        viewModelScope.launch {
            territoryPersonDao.updatePerson(
                person.copy(
                    isPresent = !person.isPresent,
                    lastUpdatedMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun updatePersonDetails(person: TerritoryPerson, rank: String, callsign: String, isPresent: Boolean) {
        viewModelScope.launch {
            territoryPersonDao.updatePerson(
                person.copy(
                    rank = rank,
                    callsign = callsign,
                    isPresent = isPresent,
                    lastUpdatedMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun deletePerson(id: Long) {
        viewModelScope.launch {
            territoryPersonDao.deletePersonById(id)
        }
    }
}
