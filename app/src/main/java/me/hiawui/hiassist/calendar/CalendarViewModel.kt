package me.hiawui.hiassist.calendar

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.Flow
import me.hiawui.hiassist.calendar.alarm.removeSystemAlarm
import me.hiawui.hiassist.calendar.alarm.setSystemAlarm
import me.hiawui.hiassist.calendar.astrology.WesternAstrologyTools
import me.hiawui.hiassist.calendar.lunar.LunarTools
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    enum class AlarmEditState {
        None, Add, Edit
    }

    @SuppressLint("StaticFieldLeak")
    private val context: Context = application.applicationContext
    private val selectedDate = mutableStateOf<LocalDate>(LocalDate.now())
    private val alarmEditState = mutableStateOf(AlarmEditState.None)
    var editingAlarm: AlarmInfo? = null

    fun getAlarmEditState(): State<AlarmEditState> {
        return alarmEditState
    }

    fun setAlarmEditState(state: AlarmEditState, alarm: AlarmInfo? = null) {
        if (state != AlarmEditState.Edit && alarm != null) {
            assert(false) { "edit state ${state.name} must set editing alarm to null" }
        } else if (state == AlarmEditState.Edit && alarm == null) {
            assert(false) { "edit state ${state.name} must set editing alarm" }
        }
        alarmEditState.value = state
        editingAlarm = alarm
    }

    fun setSelectedDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun isSelectedDate(date: LocalDate): Boolean {
        return date == selectedDate.value
    }

    fun getSelectedDate(): State<LocalDate> {
        return selectedDate
    }

    fun getSelectedDate8Chars(): LunarTools.EightChars {
        return LunarTools.get8Chars(selectedDate.value.atTime(0, 0, 0))
    }

    fun getClashChineseZodiac(eightChars: LunarTools.EightChars): LunarTools.ChineseZodiac {
        return LunarTools.getClashChineseZodiac(eightChars.dayZodiac.index)
    }

    fun getSelectedDateTimeLuckyList(): List<LunarTools.TimeLuckyInfo> {
        return LunarTools.getTimeLuckyList(selectedDate.value)
    }

    fun getAlarms(): Flow<CalendarSettings> {
        return context.calendarDataStore.data
    }

    suspend fun toggleAlarm(alarmId: Long, disabled: Boolean) {
        context.calendarDataStore.updateData { settings ->
            val alarms = settings.alarmsList.map {
                if (it.id == alarmId) {
                    val newIt = it.toBuilder().setDisabled(disabled).build()
                    if (disabled) {
                        removeSystemAlarm(context, alarmId)
                    } else {
                        setSystemAlarm(context, newIt)
                    }
                    newIt
                } else {
                    it
                }
            }
            settings.toBuilder().clearAlarms().addAllAlarms(alarms).build()
        }
    }

    suspend fun addAlarm(alarm: AlarmInfo) {
        context.calendarDataStore.updateData { settings ->
            setSystemAlarm(context, alarm)
            settings.toBuilder()
                .addAlarms(alarm)
                .build()
        }
    }

    suspend fun updateAlarm(alarm: AlarmInfo) {
        context.calendarDataStore.updateData { settings ->
            setSystemAlarm(context, alarm)
            val newList = settings.alarmsList.map {
                if (it.id == alarm.id) {
                    alarm
                } else {
                    it
                }
            }
            settings.toBuilder().clearAlarms().addAllAlarms(newList).build()
        }
    }

    suspend fun removeAlarm(alarmId: Long) {
        context.calendarDataStore.updateData { settings ->
            removeSystemAlarm(context, alarmId)
            val newList = settings.alarmsList.filter { it.id != alarmId }
            settings.toBuilder().clearAlarms().addAllAlarms(newList).build()
        }
    }

    fun getHolidayInfo(date: LocalDate): Flow<HolidayInfo?> {
        return context.getHolidayInfo(date)
    }

    fun getDateSubtitle(date: LocalDate): String {
        val lunarDateInfo = LunarTools.getLunarDateInfo(date)
        val name1 = if (lunarDateInfo.day == 1) lunarDateInfo.monthName else lunarDateInfo.dayName

        val solarTerms = LunarTools.getSolarTermsDateList(date.year)
        val name2 = solarTerms.find { it.date == date }?.name

        val lunarFestival = LunarTools.getLunarFestivalInfo(date)
        return lunarFestival?.name ?: name2 ?: name1
    }

    fun getSolarTermInfo(date: LocalDate): String {
        val solarTerms = LunarTools.getSolarTermsDateList(date.year)
        var currTerm = solarTerms.findLast { it.date <= date }
        if (currTerm == null) {
            // 找去年的冬至
            val solarTermLastYear = LunarTools.getSolarTermsDateList(date.year - 1)
            currTerm = solarTermLastYear.lastOrNull()
        }
        if (currTerm == null) {
            return ""
        }
        return "${currTerm.name} 第 ${ChronoUnit.DAYS.between(currTerm.date, date) + 1} 天"
    }

    fun getWesternZodiac(date: LocalDate): String {
        return WesternAstrologyTools.getWesternZodiacInfo(date).name
    }
}