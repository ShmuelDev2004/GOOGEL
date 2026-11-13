package app.sederyom.di

import android.content.Context
import androidx.room.Room
import app.sederyom.data.backup.ContentResolverBackupFileAccess
import app.sederyom.data.backup.JsonBackupSerializer
import app.sederyom.data.db.AppDatabase
import app.sederyom.data.db.RoomScheduleRepository
import app.sederyom.data.settings.SettingsRepository
import app.sederyom.domain.repository.ScheduleRepository
import app.sederyom.domain.usecase.ApplyImport
import app.sederyom.domain.usecase.DeleteTemplateBlock
import app.sederyom.domain.usecase.ExportBackup
import app.sederyom.domain.usecase.FindNowAndNext
import app.sederyom.domain.usecase.ObserveSchedule
import app.sederyom.domain.usecase.ObserveSettings
import app.sederyom.domain.usecase.PrepareImport
import app.sederyom.domain.usecase.ReminderRescheduler
import app.sederyom.domain.usecase.ResetDayOverride
import app.sederyom.domain.usecase.ResolveDayPlan
import app.sederyom.domain.usecase.SaveDayOverride
import app.sederyom.domain.usecase.SaveTemplateBlock
import app.sederyom.domain.usecase.SuggestNewBlock
import app.sederyom.domain.usecase.UpdateSettings
import app.sederyom.domain.usecase.ValidateBlock
import app.sederyom.reminders.AlarmScheduler
import app.sederyom.reminders.ReminderCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency container. Use cases and reminders are wired here. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Lives as long as the process; used for work that must outlive any screen. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: AppDatabase =
        Room.databaseBuilder(appContext, AppDatabase::class.java, "sederyom.db").build()

    val scheduleRepository: ScheduleRepository = RoomScheduleRepository(database)

    val settingsRepository = SettingsRepository(appContext)

    val reminderRescheduler: ReminderRescheduler =
        ReminderCoordinator(scheduleRepository, settingsRepository, AlarmScheduler(appContext))

    val validateBlock = ValidateBlock()
    val resolveDayPlan = ResolveDayPlan()
    val suggestNewBlock = SuggestNewBlock(validateBlock)
    val observeSchedule = ObserveSchedule(scheduleRepository)
    val saveTemplateBlock = SaveTemplateBlock(scheduleRepository, validateBlock, reminderRescheduler)
    val deleteTemplateBlock = DeleteTemplateBlock(scheduleRepository, reminderRescheduler)
    val findNowAndNext = FindNowAndNext()
    val saveDayOverride = SaveDayOverride(scheduleRepository, validateBlock, reminderRescheduler)
    val resetDayOverride = ResetDayOverride(scheduleRepository, reminderRescheduler)

    private val backupSerializer = JsonBackupSerializer()
    private val backupFiles = ContentResolverBackupFileAccess(appContext)

    val observeSettings = ObserveSettings(settingsRepository)
    val updateSettings = UpdateSettings(settingsRepository, reminderRescheduler)
    val exportBackup = ExportBackup(scheduleRepository, backupSerializer, backupFiles)
    val prepareImport = PrepareImport(backupSerializer, validateBlock, backupFiles)
    val applyImport = ApplyImport(scheduleRepository, reminderRescheduler)
}
