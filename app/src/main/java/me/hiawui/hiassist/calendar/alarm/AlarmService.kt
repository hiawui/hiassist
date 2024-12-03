package me.hiawui.hiassist.calendar.alarm

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.VibratorManager
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.hiawui.hiassist.HomeActivity
import me.hiawui.hiassist.R
import me.hiawui.hiassist.calendar.AlarmInfo
import me.hiawui.hiassist.calendar.CalendarSettings
import me.hiawui.hiassist.logE
import me.hiawui.hiassist.logI
import java.time.LocalDateTime
import java.time.OffsetDateTime
import android.provider.Settings as SysSettings


class AlarmReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context?, intent: Intent?) {
        logI { "received broadcast action: ${intent?.action}" }
        context?.startAlarmFgService(intent?.action ?: "", intent?.extras)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
class AlarmService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var vibrateManager: VibratorManager
    private lateinit var mediaPlayer: MediaPlayer
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        vibrateManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        mediaPlayer = MediaPlayer().let {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            it.setAudioAttributes(attributes)
            it.setDataSource(
                this,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            )
            it.prepare()
            it
        }
        this.updateSettings(CalendarSettings.Builder::setAlarmServiceState, 1)
        logI { "alarm service onCreate" }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onDestroy() {
        try {
            coroutineScope.cancel()
        } catch (e: Throwable) {
            logE(e) { "coroutine cancel error" }
        }
        try {
            vibrateManager.cancel()
        } catch (e: Throwable) {
            logE(e) { "vibrate cancel error" }
        }
        try {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
        } catch (e: Throwable) {
            logE(e) { "media player release error" }
        }
        this.updateSettings(CalendarSettings.Builder::setAlarmServiceState, 0)
        super.onDestroy()
    }

    private fun findAndCheckAlarm(intent: Intent?): AlarmInfo? {
        if (intent?.hasExtra(ALARM_PARAM_ALARM_ID) != true) {
            logI { "no alarm id" }
            return null
        }
        val alarmId = intent.getLongExtra(ALARM_PARAM_ALARM_ID, 0)
        val alarm = applicationContext.findAlarm(alarmId)
        if (alarm == null) {
            logI { "alarm not found. id=$alarmId" }
            return null
        }
        if (alarm.disabled) {
            logI { "alarm disabled. id=$alarmId" }
            return null
        }
        val alarmTime = intent.getLongExtra(ALARM_PARAM_ALARM_TIME, 0)
        val expectedDateTime =
            LocalDateTime.ofEpochSecond(alarmTime, 0, OffsetDateTime.now().offset)
        val nextDateTime = alarm.getNextAlarmTime(applicationContext, expectedDateTime)
        if (expectedDateTime != nextDateTime) {
            logI { "alarm time mismatch. id=$alarmId. expected=$expectedDateTime, actual=$nextDateTime" }
            return null
        }
        return alarm
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logI { "onStartCommand. action: action=${intent?.action}" }
        when (intent?.action) {
            ACTION_ALARM -> {
                val alarm = findAndCheckAlarm(intent)
                if (alarm == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                setSystemAlarm(applicationContext, alarm)
                initService(alarm)
                showAlarmWindow(alarm)
                doVibrate()
                playRing()
            }

            else -> {
                logI { "shutting down alarm activity & service" }
                sendBroadcast(Intent(ACTION_CLOSE_ALARM_ACTIVITY))
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun initService(alarm: AlarmInfo) {
        val context = applicationContext
        // 添加一个PendingIntent，当用户点击通知时可以执行的动作
        val pendingIntent = Intent(this, HomeActivity::class.java).let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val dismissPendingIntent =
            PendingIntent.getService(
                this,
                0,
                Intent(ACTION_STOP_ALARM_SERVICE, null, this, AlarmService::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        val notificationBuilder =
            NotificationCompat.Builder(this, getString(R.string.alarm_channel_id))
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(context.getString(R.string.alarm_notification_title))
                .setContentText(context.getString(R.string.alarm_notification_content, alarm.title))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .addAction(
                    R.drawable.ic_launcher,
                    context.getString(R.string.dismiss_alarm),
                    dismissPendingIntent
                )

        val notification = notificationBuilder.build()
        startForeground(1, notification)
    }

    private fun playRing() {
        val step = 0.3f
        var volume = 0.2f
        var lastTime = System.currentTimeMillis()
        mediaPlayer.setVolume(volume, volume)
        mediaPlayer.isLooping = true
        mediaPlayer.start()
        coroutineScope.launch {
            for (i in 1..100) {
                delay(500)
                if (!mediaPlayer.isPlaying || volume >= 1) {
                    break
                }
                val now = System.currentTimeMillis()
                if (now - lastTime < 10000) {
                    continue
                }
                lastTime = now
                volume = (volume + step).coerceAtMost(1f)
                mediaPlayer.setVolume(volume, volume)
                logI { "media volume $volume" }
            }
            logI { "media volume adjust completed" }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun doVibrate() {
        val vibrator = vibrateManager.defaultVibrator
        if (!vibrator.hasVibrator()) {
            logI { "no vibrator" }
            return
        }
        val effect = VibrationEffect.createWaveform(longArrayOf(1000, 1000), intArrayOf(200, 0), 0)
        vibrator.vibrate(effect)
        logI { "vibrating..." }
    }

    private fun showAlarmWindow(alarm: AlarmInfo) {
        if (!SysSettings.canDrawOverlays(this)) {
            logI { "alarm window not allowed" }
            return
        }

        Intent(this, AlarmActivity::class.java).let {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            it.putExtra(ALARM_PARAM_ALARM_TITLE, alarm.title)
            startActivity(it)
        }
    }
}