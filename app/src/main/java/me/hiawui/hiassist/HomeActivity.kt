package me.hiawui.hiassist

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.hiawui.hiassist.calendar.MyCalendar
import me.hiawui.hiassist.calendar.alarm.ACTION_ALARM
import me.hiawui.hiassist.calendar.alarm.AlarmReceiver
import me.hiawui.hiassist.ui.theme.HiassistTheme

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HiassistTheme {
                AppScaffold(Modifier.fillMaxSize())
            }
        }
    }
}

enum class Page(val title: String, val seq: Int, val display: Boolean = true) {
    Calendar(title = "Calender", seq = 0),
    Greeting(title = "Set Alarm", seq = 1, display = false),
}

@Composable
fun AppScaffold(modifier: Modifier) {
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        drawerContent = {
            DrawerContent(Modifier, navController, scaffoldState, scope)
        },
        content = {
            NavigationHost(Modifier.padding(it), navController)
        },
        drawerBackgroundColor = colorScheme.background,
        drawerContentColor = colorScheme.contentColorFor(colorScheme.background),
        drawerScrimColor = colorScheme.scrim,
        backgroundColor = colorScheme.background,
        contentColor = colorScheme.contentColorFor(colorScheme.background),
    )
}

@Composable
fun NavigationHost(modifier: Modifier = Modifier, navController: NavHostController) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Page.Calendar.title
    ) {
        composable(Page.Calendar.title) {
            MyCalendar()
        }
        composable(Page.Greeting.title) {
            AlarmButton()
        }
    }
}

@Composable
fun DrawerContent(
    modifier: Modifier,
    navController: NavController,
    scaffoldState: ScaffoldState,
    coroutineScope: CoroutineScope
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 50.dp)
    ) {
        for (page in Page.entries.filter { it.display }.sortedBy { it.seq }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = page.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterVertically)
                        .padding(horizontal = 50.dp, vertical = 10.dp)
                        .clickable {
                            coroutineScope.launch {
                                scaffoldState.drawerState.close()
                            }
                            navController.navigate(page.title)
                        },
                    fontSize = 18.sp,
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun AlarmButton() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var isResumed by remember { mutableStateOf(false) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            isResumed = (event == Lifecycle.Event.ON_RESUME)
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    val hasOverlayPermission = Settings.canDrawOverlays(context)
    LaunchedEffect(isResumed, Unit) {
        if (!hasOverlayPermission) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val calendar = Calendar.getInstance().apply {
        add(Calendar.SECOND, 15)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            val pi = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_ALARM, null, context, AlarmReceiver::class.java),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            try {
                alarmManager.setAlarmClock(AlarmClockInfo(calendar.timeInMillis, pi), pi)
                logI { "setAlarmClock at ${calendar.time}" }
            } catch (e: SecurityException) {
                logE(e) { "setAlarmClock error!" }
            }
        }, enabled = hasOverlayPermission) {
            Text(text = "Try Alarm")
        }
        Spacer(modifier = Modifier.height(200.dp))
        Button(onClick = {
            val pi = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_ALARM, null, context, AlarmReceiver::class.java).apply {
                    putExtra("alarmId", 1111L)
                },
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.cancel(pi)
        }) {
            Text(text = "Cancel Alarm")
        }
    }
}