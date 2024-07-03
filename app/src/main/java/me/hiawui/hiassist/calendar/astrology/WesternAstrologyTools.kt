package me.hiawui.hiassist.calendar.astrology

import java.time.LocalDate

object WesternAstrologyTools {
    private val westernZodiacList = listOf(
        Triple("水瓶座", 1, 20), Triple("双鱼座", 2, 19),
        Triple("白羊座", 3, 21), Triple("金牛座", 4, 20),
        Triple("双子座", 5, 21), Triple("巨蟹座", 6, 22),
        Triple("狮子座", 7, 23), Triple("处女座", 8, 23),
        Triple("天秤座", 9, 23), Triple("天蝎座", 10, 24),
        Triple("射手座", 11, 23), Triple("摩羯座", 12, 22),
    )

    data class WesternZodiacInfo(val name: String, val date: LocalDate)

    fun getWesternZodiacInfo(date: LocalDate): WesternZodiacInfo {
        return westernZodiacList.findLast {
            val startDate = LocalDate.of(date.year, it.second, it.third)
            startDate <= date
        }.let {
            if (it == null) {
                val last = westernZodiacList.last()
                WesternZodiacInfo(last.first, LocalDate.of(date.year - 1, last.second, last.third))
            } else {
                WesternZodiacInfo(it.first, LocalDate.of(date.year, it.second, it.third))
            }
        }
    }
}