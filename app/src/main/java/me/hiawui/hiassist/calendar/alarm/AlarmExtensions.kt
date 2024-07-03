package me.hiawui.hiassist.calendar.alarm

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import me.hiawui.hiassist.R
import me.hiawui.hiassist.calendar.AlarmInfo
import me.hiawui.hiassist.calendar.AlarmType
import me.hiawui.hiassist.calendar.isHoliday
import java.io.ByteArrayOutputStream
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.util.Base64
import java.util.Locale

class AlarmInfoBuilder {
    companion object {
        var baseId = 0L

        fun newDefaultInstance(): AlarmInfo {
            val dateTime = LocalDateTime.now().plusHours(1)
            return AlarmInfoBuilder()
                .setType(AlarmType.ALARM_ONE_TIME)
                .setTriggerTime(dateTime.toLocalTime())
                .setTriggerDay(dateTime.toLocalDate())
                .build()
        }

        fun mergeFrom(alarm: AlarmInfo): AlarmInfoBuilder {
            return AlarmInfoBuilder().mergeFrom(alarm)
        }
    }

    private val builder = AlarmInfo.newBuilder().setId(idGen()).setDisabled(false)

    private fun idGen(): Long {
        val low10Mask = (1L shl 10) - 1
        return (System.currentTimeMillis() and low10Mask.inv()) or ((baseId++) and low10Mask)
    }

    fun mergeFrom(alarm: AlarmInfo): AlarmInfoBuilder {
        this.builder.mergeFrom(alarm)
        return this
    }

    fun build(): AlarmInfo {
        return builder.build()
    }

    fun setId(id: Long): AlarmInfoBuilder {
        builder.id = id
        return this;
    }

    fun setDisabled(disabled: Boolean): AlarmInfoBuilder {
        builder.disabled = disabled
        return this
    }

    fun setType(type: AlarmType): AlarmInfoBuilder {
        builder.type = type
        return this
    }

    fun setTitle(title: String): AlarmInfoBuilder {
        builder.title = title
        return this
    }

    fun setTriggerTime(triggerTime: LocalTime): AlarmInfoBuilder {
        builder.triggerTime = triggerTime.toSecondOfDay()
        return this
    }

    fun setTriggerDay(triggerDay: LocalDate): AlarmInfoBuilder {
        builder.triggerDay = triggerDay.toEpochDay()
        return this
    }

    fun setTriggerWeekday(triggerWeekday: DayOfWeek): AlarmInfoBuilder {
        builder.triggerWeekday = triggerWeekday.value
        return this
    }

    fun setTriggerMonthDay(triggerMonthDay: Int): AlarmInfoBuilder {
        builder.triggerMonthDay = triggerMonthDay
        return this
    }

    fun setTriggerMonth(triggerMonth: Month): AlarmInfoBuilder {
        builder.triggerMonth = triggerMonth.value
        return this
    }
}

fun AlarmInfo.getDisplayTime(): String {
    val time = LocalTime.ofSecondOfDay(this.triggerTime.toLong())
    return String.format(
        Locale.getDefault(),
        "%02d:%02d",
        time.hour,
        time.minute
    )
}

fun getDisplayDayOfWeek(context: Context, dayOfWeek: DayOfWeek): String {
    return when (dayOfWeek) {
        DayOfWeek.MONDAY -> context.getString(R.string.alarm_monday)
        DayOfWeek.TUESDAY -> context.getString(R.string.alarm_tuesday)
        DayOfWeek.WEDNESDAY -> context.getString(R.string.alarm_wednesday)
        DayOfWeek.THURSDAY -> context.getString(R.string.alarm_thursday)
        DayOfWeek.FRIDAY -> context.getString(R.string.alarm_friday)
        DayOfWeek.SATURDAY -> context.getString(R.string.alarm_saturday)
        DayOfWeek.SUNDAY -> context.getString(R.string.alarm_sunday)
        else -> ""
    }
}

fun AlarmInfo.getDisplayDayOfWeek(context: Context): String {
    return context.getString(
        R.string.alarm_every_week, getDisplayDayOfWeek(
            context,
            DayOfWeek.of(this.triggerWeekday)
        )
    )
}

fun AlarmInfo.getDisplayDate(context: Context): String {
    val now = LocalDate.now()
    val date = LocalDate.ofEpochDay(this.triggerDay)
    if (now == date) {
        return context.getString(R.string.calendar_today)
    }
    val str = StringBuilder()
    if (date.year != now.year) {
        str.append(date.year).append('/')
    }
    val dateStr = str
        .append(String.format(Locale.getDefault(), "%02d", date.month.value))
        .append('/')
        .append(String.format(Locale.getDefault(), "%02d", date.dayOfMonth))
        .toString()
    return context.getString(
        R.string.alarm_specified_day,
        dateStr,
        getDisplayDayOfWeek(context, date.dayOfWeek)
    )
}

fun AlarmInfo.getLocalTime(): LocalTime {
    return LocalTime.ofSecondOfDay(this.triggerTime.toLong())
}

fun AlarmInfo.getLocalDate(): LocalDate {
    return LocalDate.ofEpochDay(this.triggerDay)
}

fun AlarmInfo.getWeekday(): DayOfWeek {
    return DayOfWeek.of(this.triggerWeekday)
}

fun AlarmInfo.getMonth(): Month {
    return Month.of(this.triggerMonth)
}

fun AlarmInfo.getNextAlarmTime(
    context: Context,
    after: LocalDateTime = LocalDateTime.now()
): LocalDateTime? {
    val info = this
    val day = after.toLocalDate()
    val triggerTime = LocalTime.ofSecondOfDay(info.triggerTime.toLong())
    val triggerDay =
        if (info.hasTriggerDay()) LocalDate.ofEpochDay(info.triggerDay) else null
    val triggerWeekday = if (info.hasTriggerWeekday()) info.triggerWeekday else null
    val triggerMonthDay = if (info.hasTriggerMonthDay()) info.triggerMonthDay else null
    val triggerMonth = if (info.hasTriggerMonth()) info.triggerMonth else null
    when (info.type) {
        AlarmType.ALARM_ONE_TIME -> {
            val triggerDateTime = triggerDay?.atTime(triggerTime)
            if (triggerDateTime == null || after > triggerDateTime) {
                return null
            }
            return triggerDateTime
        }

        AlarmType.ALARM_WORK_DAY -> {
            var tryDay = day
            for (i in 1..30) {
                if (context.isHoliday(tryDay)) {
                    tryDay = tryDay.plusDays(1)
                    continue
                }
                val triggerDateTime = tryDay.atTime(triggerTime)
                if (after > triggerDateTime) {
                    tryDay = tryDay.plusDays(1)
                    continue
                }
                return triggerDateTime
            }
            assert(false)
            return null
        }

        AlarmType.ALARM_EVERY_DAY -> {
            var triggerDateTime = day.atTime(triggerTime)
            if (after > triggerDateTime) {
                triggerDateTime = day.plusDays(1).atTime(triggerTime)
            }
            return triggerDateTime
        }

        AlarmType.ALARM_EVERY_WEEK -> {
            if (triggerWeekday == null) {
                return null
            }
            var triggerDateTime =
                day
                    .plusDays((triggerWeekday - day.dayOfWeek.value).toLong())
                    .atTime(triggerTime)
            if (after > triggerDateTime) {
                triggerDateTime = triggerDateTime.plusWeeks(1)
            }
            return triggerDateTime
        }

        AlarmType.ALARM_EVERY_MONTH -> {
            if (triggerMonthDay == null) {
                return null
            }
            for (m in 0L..12L) {
                val triggerDate =
                    day.plusMonths(m).plusDays((triggerMonthDay - day.dayOfMonth).toLong())
                if (triggerDate.dayOfMonth != triggerMonthDay) {
                    continue
                }
                val triggerDateTime = triggerDate.atTime(triggerTime)
                if (after > triggerDateTime) {
                    continue
                }
                return triggerDateTime
            }
            check(false)
            return null
        }

        AlarmType.ALARM_EVERY_YEAR -> {
            if (triggerMonthDay == null || triggerMonth == null) {
                return null
            }
            for (y in 0..8) {
                try {
                    val triggerDate =
                        LocalDate.of(day.year + y, triggerMonth, triggerMonthDay)
                    val triggerDateTime = triggerDate.atTime(triggerTime)
                    if (after > triggerDateTime) {
                        continue
                    }
                    return triggerDateTime
                } catch (e: DateTimeException) {
                    continue
                }
            }
            check(false)
            return null
        }

        else -> {
            check(false)
            return null
        }
    }
}

fun AlarmType.getDisplayName(context: Context): String {
    return when (this) {
        AlarmType.ALARM_ONE_TIME -> context.getString(R.string.alarm_type_one_time)
        AlarmType.ALARM_WORK_DAY -> context.getString(R.string.alarm_type_work_day)
        AlarmType.ALARM_EVERY_DAY -> context.getString(R.string.alarm_type_every_day)
        AlarmType.ALARM_EVERY_WEEK -> context.getString(R.string.alarm_type_every_week)
        AlarmType.ALARM_EVERY_MONTH -> context.getString(R.string.alarm_type_every_month)
        AlarmType.ALARM_EVERY_YEAR -> context.getString(R.string.alarm_type_every_year)
        else -> "-"
    }
}

class AlarmInfoState(
    val alarm: AlarmInfo? = null,
) {
    companion object {
        fun Saver(): Saver<AlarmInfoState, *> = Saver(
            save = {
                val byteArray = ByteArrayOutputStream()
                it.alarm?.writeTo(byteArray)
                val alarmStr = Base64.getEncoder().encodeToString(byteArray.toByteArray())
                listOf(
                    alarmStr,
                    it.id,
                    it.disabled.value,
                    it.type.value,
                    it.title.value,
                    it.triggerTime.value,
                    it.triggerDay.value,
                    it.triggerWeekday.value,
                    it.triggerMonthDay.intValue,
                    it.triggerMonth.value
                )
            },
            restore = { value ->
                var alarm: AlarmInfo? = null
                val alarmStr = value[0] as String
                if (alarmStr.isNotEmpty()) {
                    alarm = AlarmInfo.parseFrom(Base64.getDecoder().decode(alarmStr))
                }
                val state = AlarmInfoState(alarm)
                state.id = value[1] as Long
                state.setDisabled(value[2] as Boolean)
                state.setType(value[3] as AlarmType)
                state.setTitle(value[4] as String)
                state.setTriggerTime(value[5] as LocalTime)
                state.setTriggerDay(value[6] as LocalDate)
                state.setTriggerWeekday(value[7] as DayOfWeek)
                state.setTriggerMonthDay(value[8] as Int)
                state.setTriggerMonth(value[9] as Month)
                state
            }
        )
    }

    private val now = LocalDateTime.now()
    private var id = alarm?.id ?: 0
    private val disabled = mutableStateOf(alarm?.disabled ?: false)
    private val type = mutableStateOf(alarm?.type ?: AlarmType.ALARM_ONE_TIME)
    private val title = mutableStateOf(alarm?.title ?: "")
    private val triggerTime = mutableStateOf(alarm?.getLocalTime() ?: now.toLocalTime())
    private val triggerDay =
        mutableStateOf(if (alarm?.hasTriggerDay() == true) alarm.getLocalDate() else now.toLocalDate())
    private val triggerWeekday =
        mutableStateOf(if (alarm?.hasTriggerWeekday() == true) alarm.getWeekday() else now.dayOfWeek)
    private val triggerMonthDay =
        mutableIntStateOf(if (alarm?.hasTriggerMonthDay() == true) alarm.triggerMonthDay else now.dayOfMonth)
    private val triggerMonth =
        mutableStateOf(if (alarm?.hasTriggerMonth() == true) alarm.getMonth() else now.month)

    fun toAlarm(): AlarmInfo {
        val builder = AlarmInfoBuilder()

        if (this.alarm != null) {
            builder.setId(this.alarm.id)
        }
        builder.setDisabled(disabled.value)
        builder.setType(type.value)
        builder.setTitle(title.value)
        builder.setTriggerTime(triggerTime.value)
        if (type.value == AlarmType.ALARM_ONE_TIME) {
            builder.setTriggerDay(triggerDay.value)
        }
        if (type.value == AlarmType.ALARM_EVERY_WEEK) {
            builder.setTriggerWeekday(triggerWeekday.value)
        }
        if (type.value == AlarmType.ALARM_EVERY_MONTH || type.value == AlarmType.ALARM_EVERY_YEAR) {
            builder.setTriggerMonthDay(triggerMonthDay.intValue)
        }
        if (type.value == AlarmType.ALARM_EVERY_YEAR) {
            builder.setTriggerMonth(triggerMonth.value)
        }

        return builder.build()
    }

    fun getId(): Long = id
    fun getType(): State<AlarmType> = type
    fun setType(type: AlarmType, date: LocalDate? = null) {
        this.type.value = type
        if (date == null) {
            return
        }
        when (type) {
            AlarmType.ALARM_ONE_TIME -> this.setTriggerDay(date)
            AlarmType.ALARM_EVERY_WEEK -> this.setTriggerWeekday(date.dayOfWeek)
            AlarmType.ALARM_EVERY_MONTH -> this.setTriggerMonthDay(date.dayOfMonth)
            AlarmType.ALARM_EVERY_YEAR -> {
                this.setTriggerMonthDay(date.dayOfMonth)
                this.setTriggerMonth(date.month)
            }

            else -> {}
        }
    }

    fun getDisabled(): State<Boolean> = disabled
    fun setDisabled(disabled: Boolean) {
        this.disabled.value = disabled
    }

    fun getTitle(): State<String> = title
    fun setTitle(title: String) {
        this.title.value = title
    }

    fun getTriggerTime(): State<LocalTime> = triggerTime
    fun setTriggerTime(triggerTime: LocalTime) {
        this.triggerTime.value = triggerTime
    }

    fun getTriggerDay(): State<LocalDate> = triggerDay
    fun setTriggerDay(triggerDay: LocalDate) {
        this.triggerDay.value = triggerDay
    }

    fun getTriggerWeekday(): State<DayOfWeek> = triggerWeekday
    fun setTriggerWeekday(triggerWeekday: DayOfWeek) {
        this.triggerWeekday.value = triggerWeekday
    }

    fun getTriggerMonthDay(): State<Int> = triggerMonthDay
    fun setTriggerMonthDay(triggerMonthDay: Int) {
        this.triggerMonthDay.intValue = triggerMonthDay
    }

    fun getTriggerMonth(): State<Month> = triggerMonth
    fun setTriggerMonth(triggerMonth: Month) {
        this.triggerMonth.value = triggerMonth
    }
}

@Composable
fun rememberAlarmInfoState(alarm: AlarmInfo? = null): AlarmInfoState = rememberSaveable(
    saver = AlarmInfoState.Saver()
) {
    AlarmInfoState(alarm)
}