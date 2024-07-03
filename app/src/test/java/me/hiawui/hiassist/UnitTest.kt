package me.hiawui.hiassist

import com.google.gson.Gson
import fuel.Fuel
import fuel.get
import kotlinx.coroutines.runBlocking
import me.hiawui.hiassist.calendar.CalendarSettings
import me.hiawui.hiassist.calendar.HolidayDataStore
import me.hiawui.hiassist.calendar.alarm.AlarmInfoBuilder
import me.hiawui.hiassist.calendar.lunar.LunarTools.LunarDateInfo
import me.hiawui.hiassist.calendar.lunar.LunarTools.LunarMonthInfo
import me.hiawui.hiassist.calendar.lunar.LunarTools.get8Chars
import me.hiawui.hiassist.calendar.lunar.LunarTools.getLeapMonthInfo
import me.hiawui.hiassist.calendar.lunar.LunarTools.getLunarDateInfo
import me.hiawui.hiassist.calendar.lunar.LunarTools.getLunarNewYearDate
import me.hiawui.hiassist.calendar.lunar.LunarTools.getSolarTermsDateList
import me.hiawui.hiassist.calendar.lunar.LunarTools.getTimeLuckyList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class UnitTest {
    @Test
    fun anything() {
        println(-5 % 60)
    }

    @Test
    fun date() {
        println(AlarmInfoBuilder().build().id)
        println(AlarmInfoBuilder().build().id)
        println(AlarmInfoBuilder().build().id)
        println(AlarmInfoBuilder().build().id)
    }

    @Test
    fun holidays() {
        val d = CalendarSettings.newBuilder().build()
        println(d.holidaysMap)
        println(d.holidaysMap[2024])
        println(LocalDate.parse("2024-02-23"))
    }

    @Test
    fun fuel() {
        val rsp = runBlocking {
            Fuel.get(
                "https://timor.tech/api/holiday/year/2025",
                headers = mapOf(
                    Pair(
                        "User-Agent",
                        "Mozilla/5.0"
                    )
                )
            )
        }
        println("\n--------")
        val rspCode = rsp.statusCode
        val rspBody = rsp.body.string()
        println("$rspCode, $rspBody")
        val data = Gson().fromJson(rspBody, HolidayDataStore.HolidayRsp::class.java)
        println(data.holiday.values)
    }

    @Test
    fun lunar() {
        assertNull(getLeapMonthInfo(1902))
        assertEquals(getLeapMonthInfo(1903), LunarMonthInfo(1903, 5, 29, true))

        assertEquals(getLunarNewYearDate(1903), LocalDate.of(1903, 1, 29))

        assertEquals(
            getLunarDateInfo(LocalDate.of(1903, 1, 2)), LunarDateInfo(LunarMonthInfo(1902, 12, 30, false), 4)
        )
        assertEquals(getLunarDateInfo(LocalDate.of(1903, 2, 3)), LunarDateInfo(LunarMonthInfo(1903, 1, 29, false), 6))
        assertEquals(getLunarDateInfo(LocalDate.of(1903, 3, 4)), LunarDateInfo(LunarMonthInfo(1903, 2, 30, false), 6))
        assertEquals(getLunarDateInfo(LocalDate.of(1903, 4, 5)), LunarDateInfo(LunarMonthInfo(1903, 3, 29, false), 8))
        assertEquals(getLunarDateInfo(LocalDate.of(1903, 5, 6)), LunarDateInfo(LunarMonthInfo(1903, 4, 30, false), 10))
        assertEquals(getLunarDateInfo(LocalDate.of(1903, 6, 7)), LunarDateInfo(LunarMonthInfo(1903, 5, 29, false), 12))
        assertEquals(getLunarDateInfo(LocalDate.of(1903, 7, 8)), LunarDateInfo(LunarMonthInfo(1903, 5, 29, true), 14))
        assertEquals(getLunarDateInfo(LocalDate.of(1903, 8, 9)), LunarDateInfo(LunarMonthInfo(1903, 6, 30, false), 17))
        assertEquals(getLunarDateInfo(LocalDate.of(1903, 9, 10)), LunarDateInfo(LunarMonthInfo(1903, 7, 29, false), 19))
        assertEquals(getLunarDateInfo(LocalDate.of(2034, 1, 1)), LunarDateInfo(LunarMonthInfo(2033, 11, 29, true), 11))
        assertEquals(getLunarDateInfo(LocalDate.of(2034, 2, 1)), LunarDateInfo(LunarMonthInfo(2033, 12, 30, false), 13))
        assertEquals(getLunarDateInfo(LocalDate.of(2034, 7, 1)), LunarDateInfo(LunarMonthInfo(2034, 5, 30, false), 16))

        assertEquals(getLunarDateInfo(LocalDate.of(2022, 1, 1)), LunarDateInfo(LunarMonthInfo(2021, 11, 30, false), 29))
        assertEquals(getLunarDateInfo(LocalDate.of(2022, 2, 2)), LunarDateInfo(LunarMonthInfo(2022, 1, 30, false), 2))
        assertEquals(getLunarDateInfo(LocalDate.of(2022, 3, 3)), LunarDateInfo(LunarMonthInfo(2022, 2, 29, false), 1))
        assertEquals(getLunarDateInfo(LocalDate.of(2022, 4, 4)), LunarDateInfo(LunarMonthInfo(2022, 3, 30, false), 4))

        assertEquals(getLunarDateInfo(LocalDate.of(2020, 7, 1)), LunarDateInfo(LunarMonthInfo(2020, 5, 30, false), 11))
    }

    @Test
    fun solarTerms() {
        assertEquals(
            getSolarTermsDateList(1901).map { it.date.dayOfMonth }.joinToString(","),
            "6,21,4,19,6,21,5,21,6,22,6,22,8,23,8,24,8,24,9,24,8,23,8,22"
        )
        assertEquals(
            getSolarTermsDateList(2024).map { it.date.dayOfMonth }.joinToString(","),
            "6,20,4,19,5,20,4,19,5,20,5,21,6,22,7,22,7,22,8,23,7,22,6,21"
        )
        assertEquals(
            getSolarTermsDateList(2100).map { it.date.dayOfMonth }.joinToString(","),
            "5,20,4,18,5,20,5,20,5,21,5,21,7,23,7,23,7,23,8,23,7,22,7,22"
        )
    }

    @Test
    fun eightChars() {
        assertEquals(
            get8Chars(LocalDateTime.of(2023, 2, 3, 0, 0)).let {
                "${it.year.name},${it.month.name},${it.day.name},${it.time.name}" +
                        ",${it.zodiac.name}-${it.zodiac.tradName}"
            },
            "壬寅,癸丑,壬辰,庚子,兔-虎"
        )
        assertEquals(
            get8Chars(
                LocalDateTime.of(2023, 2, 4, 0, 0)
            ).let {
                "${it.year.name},${it.month.name},${it.day.name},${it.time.name}" +
                        ",${it.zodiac.name}-${it.zodiac.tradName}"
            },
            "癸卯,甲寅,癸巳,壬子,兔-兔"
        )
        assertEquals(
            get8Chars(
                LocalDateTime.of(2024, 1, 6, 0, 0)
            ).let {
                "${it.year.name},${it.month.name},${it.day.name},${it.time.name}" +
                        ",${it.zodiac.name}-${it.zodiac.tradName}"
            },
            "癸卯,乙丑,己巳,甲子,兔-兔"
        )
        assertEquals(
            get8Chars(
                LocalDateTime.of(2024, 2, 9, 1, 0)
            ).let {
                "${it.year.name},${it.month.name},${it.day.name},${it.time.name}" +
                        ",${it.zodiac.name}-${it.zodiac.tradName}"
            },
            "甲辰,丙寅,癸卯,癸丑,兔-龙"
        )
    }

    @Test
    fun timeLuckyList() {
        assertEquals(getTimeLuckyList(LocalDate.of(2024, 7, 2)).joinToString("") {
            "${it.time.name}${it.luckyName}"
        }, "庚子吉辛丑凶壬寅吉癸卯吉甲辰凶乙巳凶丙午吉丁未吉戊申凶己酉吉庚戌凶辛亥凶壬子凶")
        assertEquals(getTimeLuckyList(LocalDate.of(2025, 7, 3)).joinToString("") {
            "${it.time.name}${it.luckyName}"
        }, "壬子吉癸丑凶甲寅吉乙卯吉丙辰凶丁巳凶戊午吉己未吉庚申凶辛酉吉壬戌凶癸亥凶甲子凶")
        assertEquals(getTimeLuckyList(LocalDate.of(2024, 7, 20)).joinToString("") {
            "${it.time.name}${it.luckyName}"
        }, "丙子吉丁丑凶戊寅吉己卯吉庚辰凶辛巳凶壬午吉癸未吉甲申凶乙酉吉丙戌凶丁亥凶戊子凶")
        assertEquals(getTimeLuckyList(LocalDate.of(2024, 7, 21)).joinToString("") {
            "${it.time.name}${it.luckyName}"
        }, "戊子凶己丑凶庚寅吉辛卯凶壬辰吉癸巳吉甲午凶乙未凶丙申吉丁酉吉戊戌凶己亥吉庚子凶")
    }
}