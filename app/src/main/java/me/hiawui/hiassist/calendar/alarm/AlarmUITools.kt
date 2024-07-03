package me.hiawui.hiassist.calendar.alarm

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.hiawui.hiassist.R
import me.hiawui.hiassist.calendar.AlarmInfo
import me.hiawui.hiassist.calendar.CalendarSettings
import me.hiawui.hiassist.calendar.calendarDataStore
import me.hiawui.hiassist.logE
import me.hiawui.hiassist.logI
import java.time.OffsetDateTime

const val ACTION_ALARM = "hiawui.intent.action.ALARM"
const val ACTION_STOP_ALARM_SERVICE = "hiawui.intent.action.STOP_ALARM_SERVICE"
const val ACTION_CLOSE_ALARM_ACTIVITY = "hiawui.intent.action.CLOSE_ALARM_ACTIVITY"

const val ALARM_PARAM_ALARM_ID = "alarmId"

// seconds from epoch
const val ALARM_PARAM_ALARM_TIME = "alarmTime"
const val ALARM_PARAM_ALARM_TITLE = "alarmTitle"

@RequiresApi(Build.VERSION_CODES.O)
fun Context.initAlarm() {
    val channelId = getString(R.string.alarm_channel_id)
    val notifChannel = NotificationChannel(
        channelId,
        getString(R.string.alarm_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT
    )
    notifChannel.description = getString(R.string.alarm_channel_desc)

    val notifMgr = getSystemService(Application.NOTIFICATION_SERVICE) as NotificationManager
    notifMgr.createNotificationChannel(notifChannel)
    val exChannel = notifMgr.getNotificationChannel(channelId)
    if (exChannel != null) {
        logI { "notification channel created: $channelId" }
    } else {
        logE { "notification channel creation failed: $channelId" }
    }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    scope.launch {
        applicationContext.calendarDataStore.data.map { settings ->
            settings.alarmsList
        }.onEach { alarms ->
            alarms.forEach { alarm -> setSystemAlarm(applicationContext, alarm) }
            scope.coroutineContext.cancelChildren()
        }.collect()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun Context.startAlarmFgService(action: String, extras: Bundle? = null) {
    Intent(action, null, this, AlarmService::class.java).let {
        if (extras != null) {
            it.putExtras(extras)
        }
        startService(it)
    }
}

fun Context.findAlarm(alarmId: Long): AlarmInfo? {
    return getSettings(CalendarSettings::getAlarmsList).find { it.id == alarmId }
}

fun <T> Context.getSettings(getter: (CalendarSettings) -> T): T {
    return runBlocking {
        this@getSettings.calendarDataStore.data.map(getter).first()
    }
}

fun <T> Context.updateSettings(
    setter: (CalendarSettings.Builder, T) -> CalendarSettings.Builder,
    v: T
) {
    runBlocking {
        this@updateSettings.calendarDataStore.updateData {
            setter(it.toBuilder(), v).build()
        }
    }
}


fun setSystemAlarm(context: Context, alarm: AlarmInfo) {
    logI { "setting system alarm ${alarm.id}" }
    if (alarm.disabled) {
        logI { "alarm disabled, stop system alarm ${alarm.id}" }
        removeSystemAlarm(context, alarm.id)
        return
    }
    val nextAlarmDateTime = alarm.getNextAlarmTime(context)
    if (nextAlarmDateTime == null) {
        logI { "no more alarm time, stop setting system alarm ${alarm.id}" }
        removeSystemAlarm(context, alarm.id)
        return
    }
    val secFromEpoch = nextAlarmDateTime.toEpochSecond(OffsetDateTime.now().offset)
    val pi = PendingIntent.getBroadcast(
        context,
        alarm.id.toInt(),
        Intent(ACTION_ALARM, null, context, AlarmReceiver::class.java).apply {
            putExtra(ALARM_PARAM_ALARM_ID, alarm.id)
            putExtra(ALARM_PARAM_ALARM_TIME, secFromEpoch)
            putExtra(ALARM_PARAM_ALARM_TITLE, alarm.title)
        },
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    try {
        alarmManager.setAlarmClock(AlarmClockInfo(secFromEpoch * 1000, pi), pi)
        logI { "set system alarm ${alarm.id} at $nextAlarmDateTime" }
    } catch (e: SecurityException) {
        logE(e) { "setAlarmClock error! ${alarm.id}" }
    }
}

fun removeSystemAlarm(context: Context, alarmId: Long) {
    val pi = PendingIntent.getBroadcast(
        context,
        alarmId.toInt(),
        Intent(ACTION_ALARM, null, context, AlarmReceiver::class.java),
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(pi)
    logI { "canceled system alarm $alarmId" }
}