package me.hiawui.hiassist

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.hiawui.hiassist.calendar.alarm.initAlarm
import me.hiawui.hiassist.calendar.alarm.setSystemAlarm
import me.hiawui.hiassist.calendar.calendarDataStore

@RequiresApi(Build.VERSION_CODES.O)
class MyApplication : Application() {
    override fun onCreate() {
        logI { "alarm app starting" }
        super.onCreate()
        initAlarm()
    }
}