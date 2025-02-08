package me.hiawui.hiassist

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.hiawui.hiassist.calendar.HolidayDataStore
import me.hiawui.hiassist.calendar.MyCalendar
import me.hiawui.hiassist.calendar.updateHolidays
import me.hiawui.hiassist.ui.theme.HiassistTheme
import java.time.LocalDate

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

enum class Page(val titleId: Int, val seq: Int, val display: Boolean = true) {
    Calendar(titleId = R.string.nav_calendar, seq = 0),
    Settings(titleId = R.string.nav_settings, seq = 1),
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
    val routeCalendar = stringResource(id = R.string.nav_calendar)
    val routeSettings = stringResource(id = R.string.nav_settings)
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = routeCalendar,
    ) {
        composable(routeCalendar) {
            MyCalendar()
        }
        composable(routeSettings) {
            MySettings()
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
            val route = stringResource(id = page.titleId)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = route,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterVertically)
                        .padding(horizontal = 50.dp, vertical = 10.dp)
                        .clickable {
                            coroutineScope.launch {
                                scaffoldState.drawerState.close()
                            }
                            navController.navigate(route)
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
fun MySettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val infoHolidayFetched = stringResource(id = R.string.info_holiday_fetched)
    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                val year = LocalDate.now().year
                scope.launch {
                    try {
                        context.updateHolidays(year, HolidayDataStore.fetchHolidays(year))
                    } catch (e: HolidayDataStore.HolidayFetchingException) {
                        logI { "fetching holidays ongoing" }
                    }
                    Toast.makeText(context, infoHolidayFetched, Toast.LENGTH_SHORT).show()
                }
            }) {
                Text(text = stringResource(id = R.string.settings_fetch_holiday))
            }
        }
    }
}