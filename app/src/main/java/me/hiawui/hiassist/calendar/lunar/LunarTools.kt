package me.hiawui.hiassist.calendar.lunar

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

// 算法参考 https://github.com/OPN48/cnlunar
object LunarTools {
    private const val LUNAR_MONTH_START_YEAR = 1900
    private const val LUNAR_NEW_YEAR_START_YEAR = 1900
    private const val MAX_YEAR = 2100

    private const val LEAP_MONTH_BIT = 13
    private const val LEAP_MONTH_DAYS_BIT = 12

    private val lunarDayNameList = listOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )
    private val lunarMonthNameList = listOf(
        "正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"
    )

    private val heavenlyStems =
        listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    private val heavenlyStems5ElementsList =
        listOf("木", "木", "火", "火", "土", "土", "金", "金", "水", "水")
    private val earthlyBranches =
        listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    private val earthlyBranches5ElementsList =
        listOf("水", "土", "木", "木", "土", "火", "火", "土", "金", "金", "土", "水")
    private val the60StemsBranches =
        (0..59).map { "${heavenlyStems[it % heavenlyStems.size]}${earthlyBranches[it % earthlyBranches.size]}" }

    private val chineseZodiacNameList =
        listOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")

    private val luckyTimeList = listOf(
        0x2d3, 0xcb4, 0x32d, 0x4cb, 0xd32, 0xb4c, 0x2d3, 0xcb4, 0x32d, 0x4cb, 0xd22, 0xb5c,
        0x2d3, 0xcb4, 0x32d, 0x4cb, 0xd3a, 0xb4d, 0x2d3, 0xcb4, 0x32d, 0x4cb, 0xd32, 0xb4c,
        0x2d3, 0xcb5, 0x32d, 0x4cb, 0xd32, 0xb4c, 0x2d3, 0xcb4, 0x32d, 0x4cb, 0xd32, 0xb4c,
        0x2d3, 0xcb4, 0x32d, 0x4db, 0xd32, 0xb5c, 0x2d7, 0xcb4, 0x32d, 0x4cb, 0xd32, 0xb5c,
        0x2d3, 0xcb4, 0x32d, 0x4cb, 0xd32, 0xb4c, 0x2d3, 0xcb4, 0x30d, 0x4cb, 0xd32, 0xb4c
    )

    // 农历数据 每个元素的存储格式如下：
    // 16~13    12          11~0
    // 闰几月 闰月日数  12-1 月份农历日数 0=29天 1=30天
    private val lunarMonthData = listOf(
        0x10bd2,  // 1900
        0x00752, 0x00ea5, 0x0ab2a, 0x0064b, 0x00a9b, 0x09aa6, 0x0056a, 0x00b59, 0x04baa, 0x00752,  // 1901 ~ 1910
        0x0cda5, 0x00b25, 0x00a4b, 0x0ba4b, 0x002ad, 0x0056b, 0x045b5, 0x00da9, 0x0fe92, 0x00e92,  // 1911 ~ 1920
        0x00d25, 0x0ad2d, 0x00a56, 0x002b6, 0x09ad5, 0x006d4, 0x00ea9, 0x04f4a, 0x00e92, 0x0c6a6,  // 1921 ~ 1930
        0x0052b, 0x00a57, 0x0b956, 0x00b5a, 0x006d4, 0x07761, 0x00749, 0x0fb13, 0x00a93, 0x0052b,  // 1931 ~ 1940
        0x0d51b, 0x00aad, 0x0056a, 0x09da5, 0x00ba4, 0x00b49, 0x04d4b, 0x00a95, 0x0eaad, 0x00536,  // 1941 ~ 1950
        0x00aad, 0x0baca, 0x005b2, 0x00da5, 0x07ea2, 0x00d4a, 0x10595, 0x00a97, 0x00556, 0x0c575,  // 1951 ~ 1960
        0x00ad5, 0x006d2, 0x08755, 0x00ea5, 0x0064a, 0x0664f, 0x00a9b, 0x0eada, 0x0056a, 0x00b69,  // 1961 ~ 1970
        0x0abb2, 0x00b52, 0x00b25, 0x08b2b, 0x00a4b, 0x10aab, 0x002ad, 0x0056d, 0x0d5a9, 0x00da9,  // 1971 ~ 1980
        0x00d92, 0x08e95, 0x00d25, 0x14e4d, 0x00a56, 0x002b6, 0x0c2f5, 0x006d5, 0x00ea9, 0x0af52,  // 1981 ~ 1990
        0x00e92, 0x00d26, 0x0652e, 0x00a57, 0x10ad6, 0x0035a, 0x006d5, 0x0ab69, 0x00749, 0x00693,  // 1991 ~ 2000
        0x08a9b, 0x0052b, 0x00a5b, 0x04aae, 0x0056a, 0x0edd5, 0x00ba4, 0x00b49, 0x0ad53, 0x00a95,  // 2001 ~ 2010
        0x0052d, 0x0855d, 0x00ab5, 0x12baa, 0x005d2, 0x00da5, 0x0de8a, 0x00d4a, 0x00c95, 0x08a9e,  // 2011 ~ 2020
        0x00556, 0x00ab5, 0x04ada, 0x006d2, 0x0c765, 0x00725, 0x0064b, 0x0a657, 0x00cab, 0x0055a,  // 2021 ~ 2030
        0x0656e, 0x00b69, 0x16f52, 0x00b52, 0x00b25, 0x0dd0b, 0x00a4b, 0x004ab, 0x0a2bb, 0x005ad,  // 2031 ~ 2040
        0x00b6a, 0x04daa, 0x00d92, 0x0eea5, 0x00d25, 0x00a55, 0x0ba4d, 0x004b6, 0x005b5, 0x076d2,  // 2041 ~ 2050
        0x00ec9, 0x10f92, 0x00e92, 0x00d26, 0x0d516, 0x00a57, 0x00556, 0x09365, 0x00755, 0x00749,  // 2051 ~ 2060
        0x0674b, 0x00693, 0x0eaab, 0x0052b, 0x00a5b, 0x0aaba, 0x0056a, 0x00b65, 0x08baa, 0x00b4a,  // 2061 ~ 2070
        0x10d95, 0x00a95, 0x0052d, 0x0c56d, 0x00ab5, 0x005aa, 0x085d5, 0x00da5, 0x00d4a, 0x06e4d,  // 2071 ~ 2080
        0x00c96, 0x0ecce, 0x00556, 0x00ab5, 0x0bad2, 0x006d2, 0x00ea5, 0x0872a, 0x0068b, 0x10697,  // 2081 ~ 2090
        0x004ab, 0x0055b, 0x0d556, 0x00b6a, 0x00752, 0x08b95, 0x00b45, 0x00a8b, 0x04a4f, 0x004ab,  // 2091 ~ 2100 2100十二月天数未知
    )

    // 农历数据 每个元素的存储格式如下：
    // 6~5    4~0
    // 春节月  春节日
    private val lunarNewYearList = listOf(
        0x3f, // 1900
        0x53, 0x48, 0x3d, 0x50, 0x44, 0x39, 0x4d, 0x42, 0x36, 0x4a,  // 1901 ~ 1910
        0x3e, 0x52, 0x46, 0x3a, 0x4e, 0x43, 0x37, 0x4b, 0x41, 0x54,  // 1911 ~ 1920
        0x48, 0x3c, 0x50, 0x45, 0x38, 0x4d, 0x42, 0x37, 0x4a, 0x3e,  // 1921 ~ 1930
        0x51, 0x46, 0x3a, 0x4e, 0x44, 0x38, 0x4b, 0x3f, 0x53, 0x48,  // 1931 ~ 1940
        0x3b, 0x4f, 0x45, 0x39, 0x4d, 0x42, 0x36, 0x4a, 0x3d, 0x51,  // 1941 ~ 1950
        0x46, 0x3b, 0x4e, 0x43, 0x38, 0x4c, 0x3f, 0x52, 0x48, 0x3c,  // 1951 ~ 1960
        0x4f, 0x45, 0x39, 0x4d, 0x42, 0x35, 0x49, 0x3e, 0x51, 0x46,  // 1961 ~ 1970
        0x3b, 0x4f, 0x43, 0x37, 0x4b, 0x3f, 0x52, 0x47, 0x3c, 0x50,  // 1971 ~ 1980
        0x45, 0x39, 0x4d, 0x42, 0x54, 0x49, 0x3d, 0x51, 0x46, 0x3b,  // 1981 ~ 1990
        0x4f, 0x44, 0x37, 0x4a, 0x3f, 0x53, 0x47, 0x3c, 0x50, 0x45,  // 1991 ~ 2000
        0x38, 0x4c, 0x41, 0x36, 0x49, 0x3d, 0x52, 0x47, 0x3a, 0x4e,  // 2001 ~ 2010
        0x43, 0x37, 0x4a, 0x3f, 0x53, 0x48, 0x3c, 0x50, 0x45, 0x39,  // 2011 ~ 2020
        0x4c, 0x41, 0x36, 0x4a, 0x3d, 0x51, 0x46, 0x3a, 0x4d, 0x43,  // 2021 ~ 2030
        0x37, 0x4b, 0x3f, 0x53, 0x48, 0x3c, 0x4f, 0x44, 0x38, 0x4c,  // 2031 ~ 2040
        0x41, 0x36, 0x4a, 0x3e, 0x51, 0x46, 0x3a, 0x4e, 0x42, 0x37,  // 2041 ~ 2050
        0x4b, 0x41, 0x53, 0x48, 0x3c, 0x4f, 0x44, 0x38, 0x4c, 0x42,  // 2051 ~ 2060
        0x35, 0x49, 0x3d, 0x51, 0x45, 0x3a, 0x4e, 0x43, 0x37, 0x4b,  // 2061 ~ 2070
        0x3f, 0x53, 0x47, 0x3b, 0x4f, 0x45, 0x38, 0x4c, 0x42, 0x36,  // 2071 ~ 2080
        0x49, 0x3d, 0x51, 0x46, 0x3a, 0x4e, 0x43, 0x38, 0x4a, 0x3e,  // 2081 ~ 2090
        0x52, 0x47, 0x3b, 0x4f, 0x45, 0x39, 0x4c, 0x41, 0x35, 0x49,  // 2091 ~ 2100
    )

    private const val SOLAR_TERMS_START_YEAR = 1901
    private val solarTermsNameList =
        listOf(
            "小寒", "大寒", "立春", "雨水", "惊蛰", "春分", "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
            "小暑", "大暑", "立秋", "处暑", "白露", "秋分", "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
        )

    // 1901-2100年二十节气最小数序列 向量压缩法
    private val solarTermsMinList =
        listOf(4, 19, 3, 18, 4, 19, 4, 19, 4, 20, 4, 20, 6, 22, 6, 22, 6, 22, 7, 22, 6, 21, 6, 21)

    // 1901-2100年二十节气数据 每个元素的存储格式如下：
    // https://www.hko.gov.hk/sc/gts/time/conversion.htm
    // 1-24
    // 节气所在天（减去节气最小数）
    // 1901-2100年香港天文台公布二十四节气按年存储16进制，1个16进制为4个2进制
    private val solarTermsDataList = listOf(
        0x6aaaa6aa9a5a, 0xaaaaaabaaa6a, 0xaaabbabbafaa, 0x5aa665a65aab, 0x6aaaa6aa9a5a, // 1901 ~ 1905
        0xaaaaaaaaaa6a, 0xaaabbabbafaa, 0x5aa665a65aab, 0x6aaaa6aa9a5a, 0xaaaaaaaaaa6a,
        0xaaabbabbafaa, 0x5aa665a65aab, 0x6aaaa6aa9a56, 0xaaaaaaaa9a5a, 0xaaabaabaaeaa,
        0x569665a65aaa, 0x5aa6a6a69a56, 0x6aaaaaaa9a5a, 0xaaabaabaaeaa, 0x569665a65aaa,
        0x5aa6a6a65a56, 0x6aaaaaaa9a5a, 0xaaabaabaaa6a, 0x569665a65aaa, 0x5aa6a6a65a56,
        0x6aaaa6aa9a5a, 0xaaaaaabaaa6a, 0x555665665aaa, 0x5aa665a65a56, 0x6aaaa6aa9a5a,
        0xaaaaaabaaa6a, 0x555665665aaa, 0x5aa665a65a56, 0x6aaaa6aa9a5a, 0xaaaaaaaaaa6a,
        0x555665665aaa, 0x5aa665a65a56, 0x6aaaa6aa9a5a, 0xaaaaaaaaaa6a, 0x555665665aaa,
        0x5aa665a65a56, 0x6aaaa6aa9a5a, 0xaaaaaaaaaa6a, 0x555665655aaa, 0x569665a65a56,
        0x6aa6a6aa9a56, 0xaaaaaaaa9a5a, 0x5556556559aa, 0x569665a65a55, 0x6aa6a6a65a56,
        0xaaaaaaaa9a5a, 0x5556556559aa, 0x569665a65a55, 0x5aa6a6a65a56, 0x6aaaa6aa9a5a,
        0x5556556555aa, 0x569665a65a55, 0x5aa665a65a56, 0x6aaaa6aa9a5a, 0x55555565556a,
        0x555665665a55, 0x5aa665a65a56, 0x6aaaa6aa9a5a, 0x55555565556a, 0x555665665a55,
        0x5aa665a65a56, 0x6aaaa6aa9a5a, 0x55555555556a, 0x555665665a55, 0x5aa665a65a56,
        0x6aaaa6aa9a5a, 0x55555555556a, 0x555665655a55, 0x5aa665a65a56, 0x6aa6a6aa9a5a,
        0x55555555456a, 0x555655655a55, 0x5a9665a65a56, 0x6aa6a6a69a5a, 0x55555555456a,
        0x555655655a55, 0x569665a65a56, 0x6aa6a6a65a56, 0x55555155455a, 0x555655655955,
        0x569665a65a55, 0x5aa6a5a65a56, 0x15555155455a, 0x555555655555, 0x569665665a55,
        0x5aa665a65a56, 0x15555155455a, 0x555555655515, 0x555665665a55, 0x5aa665a65a56,
        0x15555155455a, 0x555555555515, 0x555665665a55, 0x5aa665a65a56, 0x15555155455a,
        0x555555555515, 0x555665665a55, 0x5aa665a65a56, 0x15555155455a, 0x555555555515,
        0x555655655a55, 0x5aa665a65a56, 0x15515155455a, 0x555555554515, 0x555655655a55,
        0x5a9665a65a56, 0x15515151455a, 0x555551554515, 0x555655655a55, 0x569665a65a56,
        0x155151510556, 0x555551554505, 0x555655655955, 0x569665665a55, 0x155110510556,
        0x155551554505, 0x555555655555, 0x569665665a55, 0x055110510556, 0x155551554505,
        0x555555555515, 0x555665665a55, 0x055110510556, 0x155551554505, 0x555555555515,
        0x555665665a55, 0x055110510556, 0x155551554505, 0x555555555515, 0x555655655a55,
        0x055110510556, 0x155551554505, 0x555555555515, 0x555655655a55, 0x055110510556,
        0x155151514505, 0x555555554515, 0x555655655a55, 0x054110510556, 0x155151510505,
        0x555551554515, 0x555655655a55, 0x014110110556, 0x155110510501, 0x555551554505,
        0x555555655555, 0x014110110555, 0x155110510501, 0x555551554505, 0x555555555555,
        0x014110110555, 0x055110510501, 0x155551554505, 0x555555555555, 0x000110110555,
        0x055110510501, 0x155551554505, 0x555555555515, 0x000110110555, 0x055110510501,
        0x155551554505, 0x555555555515, 0x000100100555, 0x055110510501, 0x155151514505,
        0x555555555515, 0x000100100555, 0x054110510501, 0x155151514505, 0x555551554515,
        0x000100100555, 0x054110510501, 0x155150510505, 0x555551554515, 0x000100100555,
        0x014110110501, 0x155110510505, 0x555551554505, 0x000000100055, 0x014110110500,
        0x155110510501, 0x555551554505, 0x000000000055, 0x014110110500, 0x055110510501,
        0x155551554505, 0x000000000055, 0x000110110500, 0x055110510501, 0x155551554505,
        0x000000000015, 0x000100110500, 0x055110510501, 0x155551554505, 0x555555555515
    )

    data class LunarMonthInfo(val year: Int, val month: Int, val days: Int, val isLeap: Boolean)

    data class LunarDateInfo(val yearMonth: LunarMonthInfo, val day: Int) {
        val year by yearMonth::year
        val month by yearMonth::month
        val monthName: String
            get() = lunarMonthNameList[month - 1]
        val days by yearMonth::days
        val isLeap by yearMonth::isLeap
        val dayName: String
            get() = lunarDayNameList[day - 1]
    }

    fun getLeapMonthInfo(year: Int): LunarMonthInfo? {
        if (year < LUNAR_MONTH_START_YEAR || year >= LUNAR_MONTH_START_YEAR + lunarMonthData.size) {
            return null
        }
        val monthData = lunarMonthData[year - LUNAR_MONTH_START_YEAR]
        val leapMonth = monthData.ushr(LEAP_MONTH_BIT)
        val leapMonthDays = if (monthData.and(1 shl LEAP_MONTH_DAYS_BIT) > 0) 30 else 29
        if (leapMonth in 1..12)
            return LunarMonthInfo(year, leapMonth, leapMonthDays, true)
        return null
    }

    fun getLunarMonthDays(year: Int, month: Int): Int {
        if (year < LUNAR_MONTH_START_YEAR || year >= LUNAR_MONTH_START_YEAR + lunarMonthData.size) {
            return 29
        }
        val monthData = lunarMonthData[year - LUNAR_MONTH_START_YEAR]
        return if (monthData and (1 shl (month - 1)) > 0) 30 else 29
    }

    fun getLunarNewYearDate(year: Int): LocalDate? {
        if (year < LUNAR_NEW_YEAR_START_YEAR || year >= LUNAR_NEW_YEAR_START_YEAR + lunarNewYearList.size) {
            return null
        }
        val newYearData = lunarNewYearList[year - LUNAR_NEW_YEAR_START_YEAR]
        val day = newYearData.and(0x1f)
        val month = newYearData.ushr(5).and(0x3)
        return LocalDate.of(year, month, day)
    }

    fun getLunarDateInfo(date: LocalDate): LunarDateInfo {
        val newYearDate = getLunarNewYearDate(date.year)
        var spanDays = ChronoUnit.DAYS.between(newYearDate, date).toInt()
        var year = date.year
        var month = 1
        var monthDays = 0
        var day = 1
        var isLeap = false
        if (spanDays >= 0) {
            val leapMonth = getLeapMonthInfo(year)
            for (i in 1..12) {
                monthDays = getLunarMonthDays(year, month)
                if (spanDays < monthDays) {
                    break
                }
                spanDays -= monthDays
                if (month == leapMonth?.month) {
                    monthDays = leapMonth.days
                    if (spanDays < monthDays) {
                        isLeap = true
                        break
                    }
                    spanDays -= monthDays
                }
                month += 1
            }
            day += spanDays
        } else {
            year -= 1
            month = 12
            val leapMonth = getLeapMonthInfo(year)
            for (i in 1..12) {
                if (month == leapMonth?.month) {
                    monthDays = leapMonth.days
                    if (abs(spanDays) <= monthDays) {
                        isLeap = true
                        break
                    }
                    spanDays += monthDays
                }
                monthDays = getLunarMonthDays(year, month)
                if (abs(spanDays) <= monthDays) {
                    break
                }
                spanDays += monthDays
                month -= 1
            }
            day += monthDays + spanDays
        }
        return LunarDateInfo(LunarMonthInfo(year, month, monthDays, isLeap), day)
    }

    data class SolarTerm(val name: String, val date: LocalDate)

    fun getSolarTermsDateList(year: Int): List<SolarTerm> {
        if (year < SOLAR_TERMS_START_YEAR || year >= SOLAR_TERMS_START_YEAR + solarTermsDataList.size) {
            return emptyList()
        }
        val solarTermsData = solarTermsDataList[year - SOLAR_TERMS_START_YEAR]
        val dateList = mutableListOf<SolarTerm>()
        for (i in 0..23) {
            val day = (solarTermsData.ushr(i * 2).and(0x3) + solarTermsMinList[i]).toInt()
            val month = (i / 2) + 1
            dateList.add(SolarTerm(solarTermsNameList[i], LocalDate.of(year, month, day)))
        }
        return dateList
    }

    data class StemBranch(val stemIndex: Int, val branchIndex: Int) {
        val stem: String
            get() = heavenlyStems[stemIndex]
        val branch: String
            get() = earthlyBranches[branchIndex]
        val name: String
            get() = "$stem$branch"
    }

    data class ChineseZodiac(val index: Int, val tradIndex: Int) {
        val name: String
            get() = chineseZodiacNameList[index]
        val tradName: String
            get() = chineseZodiacNameList[tradIndex]
    }

    data class EightChars(
        val year: StemBranch,
        val month: StemBranch,
        val day: StemBranch,
        val time: StemBranch,
        val zodiac: ChineseZodiac,
    )

    // 八字
    fun get8Chars(dateTime: LocalDateTime): EightChars {
        val date = dateTime.toLocalDate()
        val solarTerms = getSolarTermsDateList(date.year)

        // 年柱
        val dateLichun = solarTerms[2].date
        val adjYr = if (date >= dateLichun) 0 else -1
        val yrStemIdx = (date.year + adjYr - 4) % 10
        val yrBranchIdx = (date.year + adjYr - 4) % 12
        val yearStemBranch = StemBranch(yrStemIdx, yrBranchIdx)

        // 月柱
        val solarTermIdx = solarTerms.indexOfLast { it.date <= date }.let {
            // 小寒大寒属于去年的, 所以idx要加24
            if (it < 2) it + 24 else it
        }
        val mthStemIdx = ((solarTermIdx / 2) + 1 + (yrStemIdx % 5) * 2) % 10
        val mthBranchIdx = ((solarTermIdx / 2) + 1) % 12
        val mthStemBranch = StemBranch(mthStemIdx, mthBranchIdx)

        // 日柱 2024/01/01 甲子日
        val lunarDate = if (dateTime.hour >= 23) date.plusDays(1) else date
        val spanDays = ChronoUnit.DAYS.between(LocalDate.of(2024, 1, 1), lunarDate)
        val dayStemIdx = (spanDays % 10).let { if (it < 0) it + 10 else it }.toInt()
        val dayBranchIdx = (spanDays % 12).let { if (it < 0) it + 12 else it }.toInt()
        val dayStemBranch = StemBranch(dayStemIdx, dayBranchIdx)

        // 时柱
        val timeIdx = (dateTime.hour + 1) / 2
        val timeStemIdx = (timeIdx + (dayStemIdx % 5) * 2) % 10
        val timeBranchIdx = timeIdx % 12
        val timeStemBranch = StemBranch(timeStemIdx, timeBranchIdx)

        // 生肖
        val tradZodiacIdx = yrBranchIdx
        val dateNewYear = getLunarNewYearDate(date.year)
        val zodiacIdx = if (date >= dateNewYear && date < dateLichun) {
            (yrBranchIdx + 1) % 12
        } else if (date >= dateLichun && date < dateNewYear) {
            (yrBranchIdx + 11) % 12
        } else {
            yrBranchIdx
        }

        return EightChars(
            yearStemBranch,
            mthStemBranch,
            dayStemBranch,
            timeStemBranch,
            ChineseZodiac(zodiacIdx, tradZodiacIdx)
        )
    }

    data class TimeLuckyInfo(val time: StemBranch, val isGood: Boolean) {
        val luckyName: String
            get() = if (isGood) "吉" else "凶"
    }

    fun getTimeLuckyList(date: LocalDate): List<TimeLuckyInfo> {
        val eightChars = get8Chars(date.atTime(0, 0, 0))
        val stemBranchIdx =
            the60StemsBranches.indexOf("${eightChars.day.stem}${eightChars.day.branch}")
        val luckyData0 = luckyTimeList[stemBranchIdx]
        val luckyList = mutableListOf<TimeLuckyInfo>()
        for (i in 0..11) {
            val isGood = luckyData0.and(1 shl (11 - i)) == 0
            val stemIdx = (eightChars.time.stemIndex + i) % 10
            val branchIdx = (eightChars.time.branchIndex + i) % 12
            luckyList.add(TimeLuckyInfo(StemBranch(stemIdx, branchIdx), isGood))
        }
        val luckyData1 = luckyTimeList[(stemBranchIdx + 1) % 60]
        val isGood = luckyData1.and(1 shl 11) == 0
        val stemIdx = (eightChars.time.stemIndex + 12) % 10
        val branchIdx = eightChars.time.branchIndex
        luckyList.add(TimeLuckyInfo(StemBranch(stemIdx, branchIdx), isGood))
        return luckyList
    }

    data class LunarFestival(val date: LunarDateInfo, val name: String)

    private val lunarFestivalList = listOf(
        Triple(1, -1, "除夕"),
        Triple(1, 1, "春节"),
        Triple(1, 15, "元宵节"),
        Triple(2, -1, "送穷节"),
        Triple(2, 2, "龙抬头"),
        Triple(3, 3, "上巳节"),
        Triple(5, 5, "端午节"),
        Triple(7, 7, "七夕节"),
        Triple(7, 15, "中元节"),
        Triple(8, 15, "中秋节"),
        Triple(9, 9, "重阳节"),
        Triple(10, 1, "寒衣节"),
        Triple(10, 15, "下元节"),
        Triple(12, 12, "腊八节"),
    )

    fun getLunarFestivalInfo(date: LocalDate): LunarFestival? {
        val todayInfo = getLunarDateInfo(date)
        val tmrInfo = getLunarDateInfo(date.plusDays(1))
        val festival = lunarFestivalList.flatMap {
            if (it.first == todayInfo.month && it.second == todayInfo.day) {
                listOf(LunarFestival(todayInfo, it.third))
            } else if (tmrInfo.day == 1 && it.first == tmrInfo.month && it.second == -1) {
                listOf(LunarFestival(todayInfo, it.third))
            } else {
                emptyList()
            }
        }
        return festival.firstOrNull()
    }

}