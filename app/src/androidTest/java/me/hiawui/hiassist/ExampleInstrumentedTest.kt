package me.hiawui.hiassist

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.hiawui.hiassist.calendar.AlarmType
import me.hiawui.hiassist.calendar.alarm.AlarmInfoBuilder
import me.hiawui.hiassist.calendar.alarm.getNextAlarmTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val alarmDisabled = AlarmInfoBuilder()
            .setDisabled(true)
            .setType(AlarmType.ALARM_ONE_TIME)
            .setTriggerDay(LocalDate.of(2024, 6, 26))
            .setTriggerTime(LocalTime.of(7, 50))
            .build()
        assertEquals(
            alarmDisabled.getNextAlarmTime(appContext, LocalDateTime.of(2024, 6, 26, 7, 50)),
            LocalDateTime.of(2024, 6, 26, 7, 50)
        )

        val alarmOT = AlarmInfoBuilder()
            .setType(AlarmType.ALARM_ONE_TIME)
            .setTriggerDay(LocalDate.of(2024, 6, 26))
            .setTriggerTime(LocalTime.of(7, 50))
            .build()
        assertEquals(
            alarmOT.getNextAlarmTime(appContext, LocalDateTime.of(2024, 6, 26, 7, 49)),
            LocalDateTime.of(2024, 6, 26, 7, 50)
        )
        assertEquals(
            alarmOT.getNextAlarmTime(appContext, LocalDateTime.of(2024, 6, 26, 7, 51)),
            null
        )

        val alarmED = AlarmInfoBuilder()
            .setType(AlarmType.ALARM_EVERY_DAY)
            .setTriggerTime(LocalTime.of(7, 50))
            .build()
        assertEquals(
            alarmED.getNextAlarmTime(appContext, LocalDateTime.of(2024, 6, 26, 7, 49)),
            LocalDateTime.of(2024, 6, 26, 7, 50)
        )
        assertEquals(
            alarmED.getNextAlarmTime(appContext, LocalDateTime.of(2024, 6, 26, 7, 51)),
            LocalDateTime.of(2024, 6, 27, 7, 50)
        )

        val alarmEW = AlarmInfoBuilder()
            .setType(AlarmType.ALARM_EVERY_WEEK)
            .setTriggerTime(LocalTime.of(7, 50))
            .setTriggerWeekday(DayOfWeek.WEDNESDAY)
            .build()
        assertEquals(
            alarmEW.getNextAlarmTime(appContext, LocalDateTime.of(2024, 6, 26, 7, 49)),
            LocalDateTime.of(2024, 6, 26, 7, 50)
        )
        assertEquals(
            alarmEW.getNextAlarmTime(appContext, LocalDateTime.of(2024, 6, 26, 7, 51)),
            LocalDateTime.of(2024, 7, 3, 7, 50)
        )
        val alarmEW2 = AlarmInfoBuilder()
            .setType(AlarmType.ALARM_EVERY_WEEK)
            .setTriggerTime(LocalTime.of(7, 50))
            .build()
        assertNull(alarmEW2.getNextAlarmTime(appContext, LocalDateTime.now()))

        val alarmEM = AlarmInfoBuilder()
            .setType(AlarmType.ALARM_EVERY_MONTH)
            .setTriggerTime(LocalTime.of(7, 50))
            .setTriggerMonthDay(30)
            .build()
        assertEquals(
            alarmEM.getNextAlarmTime(appContext, LocalDateTime.of(2024, 1, 30, 7, 50)),
            LocalDateTime.of(2024, 1, 30, 7, 50)
        )
        assertEquals(
            alarmEM.getNextAlarmTime(appContext, LocalDateTime.of(2024, 1, 30, 7, 51)),
            LocalDateTime.of(2024, 3, 30, 7, 50)
        )
        val alarmEM2 = AlarmInfoBuilder()
            .setType(AlarmType.ALARM_EVERY_MONTH)
            .setTriggerTime(LocalTime.of(7, 50))
            .build()
        assertNull(alarmEM2.getNextAlarmTime(appContext, LocalDateTime.now()))

        val alarmEY = AlarmInfoBuilder()
            .setType(AlarmType.ALARM_EVERY_YEAR)
            .setTriggerTime(LocalTime.of(7, 50))
            .setTriggerMonthDay(29)
            .setTriggerMonth(Month.of(2))
            .build()
        assertEquals(
            alarmEY.getNextAlarmTime(appContext, LocalDateTime.of(2024, 2, 29, 7, 50)),
            LocalDateTime.of(2024, 2, 29, 7, 50)
        )
        assertEquals(
            alarmEY.getNextAlarmTime(appContext, LocalDateTime.of(1896, 2, 29, 7, 51)),
            LocalDateTime.of(1904, 2, 29, 7, 50)
        )
        val alarmEY2 = AlarmInfoBuilder()
            .setType(AlarmType.ALARM_EVERY_YEAR)
            .setTriggerTime(LocalTime.of(7, 50))
            .build()
        assertNull(alarmEY2.getNextAlarmTime(appContext, LocalDateTime.now()))
    }
}