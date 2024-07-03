package me.hiawui.hiassist.calendar

import android.content.Context
import com.google.gson.Gson
import fuel.Fuel
import fuel.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import me.hiawui.hiassist.logI
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object HolidayDataStore {
    data class RawHoliday(val holiday: Boolean, val name: String, val wage: Int, val date: String)

    data class HolidayRsp(val code: Int, val holiday: Map<String, RawHoliday>)

    open class HolidayException(override val message: String, val throwable: Throwable? = null) :
        Exception(message, throwable)

    class HolidayFetchingException : HolidayException(message = "fetching holidays is on going")

    private val gson = Gson()
    private var fetchingHolidays = false

    suspend fun fetchHolidays(year: Int): List<HolidayInfo> {
        if (fetchingHolidays) {
            throw HolidayFetchingException()
        }
        fetchingHolidays = true
        val rsp = Fuel.get(
            "https://timor.tech/api/holiday/year/$year", headers = mapOf(
                Pair(
                    "User-Agent",
                    "Mozilla/5.0"
                )
            )
        )
        fetchingHolidays = false
        val rspStatusCode = rsp.statusCode
        if (rspStatusCode != 200) {
            throw HolidayException("Failed to fetch holidays for year $year. status code=$rspStatusCode")
        }
        val rspBody = rsp.body.string()
        val data = gson.fromJson(rspBody, HolidayRsp::class.java)
        logI { "fetched holidays of $year" }
        return data.holiday.values.map { holidayInfoOf(it) }
    }

    private fun holidayInfoOf(raw: RawHoliday): HolidayInfo {
        return HolidayInfo.newBuilder()
            .setHoliday(raw.holiday)
            .setName(raw.name)
            .setWage(raw.wage)
            .setDate(raw.date).build()
    }
}

suspend fun Context.updateHolidays(year: Int, holidays: List<HolidayInfo>) {
    calendarDataStore.updateData { settings ->
        val refreshTime =
            if (holidays.isEmpty()) (System.currentTimeMillis() + ChronoUnit.DAYS.duration.toMillis())
            else Long.MAX_VALUE
        val holidaysBuilder = HolidaysInYear.newBuilder().setNextRefreshTime(refreshTime)
        holidays.forEach {
            val date = LocalDate.parse(it.date)
            holidaysBuilder.putHolidays("${date.month}-${date.dayOfMonth}", it)
        }
        val builder = settings.toBuilder()
        builder.putHolidays(year, holidaysBuilder.build())
        logI { "updated holidays of $year" }
        builder.build()
    }
}


fun Context.getHolidayInfo(date: LocalDate): Flow<HolidayInfo?> {
    return calendarDataStore.data.map { settings ->
        val holidaysInYear = settings.holidaysMap[date.year]
        if (holidaysInYear == null || holidaysInYear.nextRefreshTime < System.currentTimeMillis()) {
            try {
                updateHolidays(date.year, HolidayDataStore.fetchHolidays(date.year))
            } catch (e: HolidayDataStore.HolidayFetchingException) {
                logI { "fetching holidays ongoing" }
            }
        }
        holidaysInYear?.holidaysMap?.get("${date.month}-${date.dayOfMonth}")
    }
}

fun Context.isHoliday(date: LocalDate): Boolean {
    val info = runBlocking {
        getHolidayInfo(date).first()
    }
    if (info != null) {
        return info.holiday
    }
    return date.dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
}