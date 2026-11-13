package app.sederyom.domain.usecase

/** Recomputes and re-registers all reminders. Implemented in the reminders package. */
fun interface ReminderRescheduler {
    suspend fun reschedule()
}
