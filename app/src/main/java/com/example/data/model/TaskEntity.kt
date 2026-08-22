package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

/**
 * A scheduled / recurring task the brain remembers to follow (the "daily fix
 * task" memory). Tasks can be one-off or recurring and can expire (after which
 * the brain stops nudging). The AI proposes them as {"action":"add_task"} and
 * the app tracks usage (timesDone) + next due date.
 */
enum class TaskRecurrence { ONCE, DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY }

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double = 0.0,             // ₹ involved, if any
    val recurrence: TaskRecurrence = TaskRecurrence.ONCE,
    val nextDueDateMillis: Long = 0L,     // 0 = no specific date (do today)
    val expiresAtMillis: Long = 0L,       // 0 = never expires
    val category: String = "General",
    val notes: String = "",
    val timesDone: Int = 0,
    val isActive: Boolean = true
) {
    /** Human-readable schedule label (e.g. "Monthly · due 1st"). */
    fun scheduleLabel(): String = when (recurrence) {
        TaskRecurrence.ONCE -> "Once"
        TaskRecurrence.DAILY -> "Daily"
        TaskRecurrence.WEEKLY -> "Weekly"
        TaskRecurrence.MONTHLY -> "Monthly"
        TaskRecurrence.QUARTERLY -> "Quarterly (every 3 months)"
        TaskRecurrence.YEARLY -> "Yearly"
    }

    /** Computes the next due date after marking the task done. */
    fun nextOccurrence(): Long {
        val cal = Calendar.getInstance()
        val base = if (nextDueDateMillis > 0 && nextDueDateMillis > System.currentTimeMillis())
            nextDueDateMillis else System.currentTimeMillis()
        cal.timeInMillis = base
        when (recurrence) {
            TaskRecurrence.ONCE -> return Long.MAX_VALUE // done forever
            TaskRecurrence.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            TaskRecurrence.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            TaskRecurrence.MONTHLY -> cal.add(Calendar.MONTH, 1)
            TaskRecurrence.QUARTERLY -> cal.add(Calendar.MONTH, 3)
            TaskRecurrence.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }
}
