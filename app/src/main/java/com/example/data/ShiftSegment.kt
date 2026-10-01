package com.example.data

import java.util.Calendar

data class GuardInterval(val start: Int, val end: Int)

data class ShiftSegment(
    val startMillis: Long,
    val endMillis: Long,
    val activeGuardName: String,
    val restingGuardName: String,
    val activeGuardIndex: Int // 1 for Guard 1, 2 for Guard 2
) {
    companion object {
        fun parseIntervals(intervalsStr: String): List<GuardInterval> {
            if (intervalsStr.isBlank()) return emptyList()
            
            // Handle legacy comma-separated single integers format if present
            if (!intervalsStr.contains("-")) {
                val splits = intervalsStr.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .filter { it in 1..24 }
                    .sorted()
                    .distinct()
                
                val list = mutableListOf<GuardInterval>()
                var prev = 0
                var isGuardOne = true
                for (s in splits) {
                    if (isGuardOne && prev < s) {
                        list.add(GuardInterval(prev, s))
                    }
                    prev = s
                    isGuardOne = !isGuardOne
                }
                return list
            }

            val list = mutableListOf<GuardInterval>()
            val parts = intervalsStr.split(",")
            for (part in parts) {
                val range = part.trim().split("-")
                if (range.size == 2) {
                    val s = range[0].toIntOrNull()
                    val e = range[1].toIntOrNull()
                    if (s != null && e != null && s in 0..24 && e in 0..24 && s < e) {
                        list.add(GuardInterval(s, e))
                    }
                }
            }
            return list.sortedBy { it.start }
        }

        fun getShiftSegments(duty: Duty): List<ShiftSegment> {
            val segments = mutableListOf<ShiftSegment>()
            val start = duty.startEpochMillis
            val guardNames = duty.getGuardNames()
            val hourAssignments = duty.getHourAssignments()
            
            var currentGuardIdx = -1
            var segmentStartHour = 0
            
            for (h in 0..24) {
                val guardIdxAtHour = if (h < 24) {
                    hourAssignments.getOrElse(h) { 0 }.coerceIn(0, guardNames.size - 1)
                } else {
                    -2 // Sentinel for end of 24h
                }
                
                if (h == 0) {
                    currentGuardIdx = guardIdxAtHour
                    segmentStartHour = 0
                } else if (guardIdxAtHour != currentGuardIdx) {
                    val startMillis = start + segmentStartHour * 3600 * 1000L
                    val endMillis = start + h * 3600 * 1000L
                    
                    val activeGuardName = guardNames.getOrElse(currentGuardIdx) { "Дежурный" }
                    val activeGuardIndex = currentGuardIdx + 1
                    
                    val restingGuards = guardNames.filterIndexed { index, _ -> index != currentGuardIdx }
                    val restingGuardName = if (restingGuards.isEmpty()) "Нет" else restingGuards.joinToString(", ")
                    
                    segments.add(
                        ShiftSegment(
                            startMillis = startMillis,
                            endMillis = endMillis,
                            activeGuardName = activeGuardName,
                            restingGuardName = restingGuardName,
                            activeGuardIndex = activeGuardIndex
                        )
                    )
                    
                    currentGuardIdx = guardIdxAtHour
                    segmentStartHour = h
                }
            }
            return segments
        }
    }
}
