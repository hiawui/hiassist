package me.hiawui.hiassist.calendar.alarm

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.hiawui.hiassist.HomeActivity
import me.hiawui.hiassist.R
import me.hiawui.hiassist.calendar.CalendarSettings
import me.hiawui.hiassist.logI
import me.hiawui.hiassist.ui.theme.HiassistTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : ComponentActivity() {
    private var myReceiver: MyReceiver? = null

    inner class MyReceiver : BroadcastReceiver() {
        init {
            logI { "alarm activity receiver created" }
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            logI { "closing alarm activity" }
            this@AlarmActivity.finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        myReceiver = MyReceiver()
        registerReceiver(myReceiver, IntentFilter(ACTION_CLOSE_ALARM_ACTIVITY))

        val alarmTitle = intent.getStringExtra(ALARM_PARAM_ALARM_TITLE) ?: "闹钟"

        enableEdgeToEdge()
        setContent {
            HiassistTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxSize()) {
                        MainInfo(
                            alarmTitle,
                            modifier = Modifier
                                .weight(6f)
                                .fillMaxWidth()
                        )
                        DismissButton(
                            modifier = Modifier
                                .weight(4f)
                                .fillMaxWidth()
                        ) {
                            startAlarmFgService(ACTION_STOP_ALARM_SERVICE)
                            finish()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (getSettings(CalendarSettings::getAlarmServiceState) != 1) {
            logI { "alarm service not active, jump to main activity" }
            Intent(this, HomeActivity::class.java).let {
                it.addFlags(FLAG_ACTIVITY_CLEAR_TASK)
                it.addFlags(FLAG_ACTIVITY_NEW_TASK)
                startActivity(it)
            }
            finish()
            return
        }
    }

    override fun onDestroy() {
        logI { "alarm activity destroying" }
        myReceiver?.let { unregisterReceiver(myReceiver) }
        super.onDestroy()
    }
}

fun timeFlow(): Flow<String> = flow {
    val sdf = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
    while (true) {
        val str = sdf.format(Date())
        emit(str)
        delay(200)
    }
}

@Composable
fun MainInfo(title: String, modifier: Modifier) {
    val time by remember { timeFlow() }.collectAsState(initial = "")
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(.6f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                title,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Bottom)
                    .padding(bottom = 10.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(.4f),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = time,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Top),
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DismissButton(modifier: Modifier, onDismiss: () -> Unit) {
    val context = LocalContext.current

    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        Button(
            modifier = Modifier.align(Alignment.CenterVertically),
            onClick = onDismiss
        ) {
            Text(text = context.getString(R.string.dismiss_alarm))
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun MyPreview() {
    HiassistTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            MainInfo(
                "闹钟",
                modifier = Modifier
                    .weight(6f)
                    .fillMaxWidth()
            )
            DismissButton(
                modifier = Modifier
                    .weight(4f)
                    .fillMaxWidth()
            ) {
            }
        }
    }
}