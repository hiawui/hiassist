package me.hiawui.hiassist.calendar

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import java.io.InputStream
import java.io.OutputStream

object CalendarSettingsSerializer : Serializer<CalendarSettings> {
    override val defaultValue: CalendarSettings
        get() = CalendarSettings.newBuilder().setAlarmServiceState(0).build()

    override suspend fun readFrom(input: InputStream): CalendarSettings {
        return CalendarSettings.parseFrom(input)
    }

    override suspend fun writeTo(t: CalendarSettings, output: OutputStream) {
        t.writeTo(output)
    }
}

val Context.calendarDataStore: DataStore<CalendarSettings> by dataStore(
    fileName = "calendar.pb",
    serializer = CalendarSettingsSerializer
)