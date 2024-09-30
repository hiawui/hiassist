package me.hiawui.hiassist.calendar

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.nextMonth
import com.kizitonwose.calendar.core.previousMonth
import com.kizitonwose.calendar.core.yearMonth
import kotlinx.coroutines.launch
import me.hiawui.hiassist.R
import me.hiawui.hiassist.calendar.alarm.AlarmInfoBuilder
import me.hiawui.hiassist.calendar.alarm.getDisplayDate
import me.hiawui.hiassist.calendar.alarm.getDisplayDayOfWeek
import me.hiawui.hiassist.calendar.alarm.getDisplayName
import me.hiawui.hiassist.calendar.alarm.getDisplayTime
import me.hiawui.hiassist.calendar.alarm.getNextAlarmTime
import me.hiawui.hiassist.calendar.alarm.rememberAlarmInfoState
import me.hiawui.hiassist.calendar.lunar.LunarTools
import me.hiawui.hiassist.logI
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val daysOfWeek = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY
)

private const val MIN_YEAR = 1901
private const val MAX_YEAR = 2099

//@Preview
@Composable
fun MyCalendar() {
    val viewModel = viewModel<CalendarViewModel>()
    val editState by viewModel.getAlarmEditState()
    val currentMonth = YearMonth.now()
    val startMonth = YearMonth.of(MIN_YEAR, 1)
    val endMonth = YearMonth.of(MAX_YEAR, 12)
    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first(),
    )
    val scrollState = rememberScrollState()
    val yearEditState = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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

    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(Modifier.height(40.dp))
            if (yearEditState.value) {
                CalendarInputHeader(Modifier, state, onEditDone = {
                    scope.launch {
                        state.scrollToMonth(it)
                        viewModel.setSelectedDate(it.atDay(1))
                        yearEditState.value = false
                    }
                })
            } else {
                CalendarHeader(Modifier, state, onYearMonthClick = {
                    yearEditState.value = true
                })
            }
            Spacer(Modifier.height(20.dp))
            BodyView(Modifier, state)
            Spacer(Modifier.height(20.dp))
            DateInfo()
            Spacer(Modifier.height(20.dp))
            FloatingWindowSettings()
            if (editState == CalendarViewModel.AlarmEditState.None) {
                AlarmList(Modifier, isResumed)
            } else {
                AlarmEdit(Modifier)
            }
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Preview
@Composable
fun DateInfoPreview() {
    Surface {
        Column(modifier = Modifier.fillMaxSize()) {
            DateInfo()
        }
    }
}

@Composable
fun DateInfo() {
    val viewModel = viewModel<CalendarViewModel>()
    val selectedDate by viewModel.getSelectedDate()
    val luckyInfoState = remember { mutableStateOf(false) }

    val lunarInfo = LunarTools.getLunarDateInfo(selectedDate)
    val eightChars = viewModel.getSelectedDate8Chars()
    val luckyInfo = viewModel.getSelectedDateTimeLuckyList()
    val holidayInfo by viewModel.getHolidayInfo(selectedDate)
        .collectAsStateWithLifecycle(initialValue = null)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "公历 ${selectedDate.year}年${selectedDate.month.value}月${selectedDate.dayOfMonth}日 ${
                viewModel.getWesternZodiac(
                    selectedDate
                )
            }"
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "农历 ${lunarInfo.monthName}${lunarInfo.dayName}"
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        val clashed = viewModel.getClashChineseZodiac(eightChars)
        Text(
            text = "${eightChars.year.name}(${eightChars.yearZodiac.name})年 ${eightChars.month.name}月 ${eightChars.day.name}日 " +
                    "${eightChars.dayZodiac.name}冲${clashed.name}",
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = viewModel.getSolarTermInfo(selectedDate))
    }
    if (holidayInfo != null) {
        val text =
            if (holidayInfo?.holiday == true) (holidayInfo?.name ?: "") + "放假"
            else holidayInfo?.name ?: ""
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = text)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(
                id = R.string.calendar_time_lucky,
                if (!luckyInfoState.value) ">>" else "<<"
            ),
            modifier = Modifier.clickable {
                luckyInfoState.value = !luckyInfoState.value
            },
            color = MaterialTheme.colorScheme.secondary,
        )
    }
    AnimatedVisibility(
        visible = luckyInfoState.value,
        enter = slideInVertically { -it } + expandVertically() + fadeIn(),
        exit = slideOutVertically { -it } + shrinkVertically() + fadeOut()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            for (i in 0..1) {
                Column(
                    modifier = Modifier
                        .weight(.4f)
                        .padding(horizontal = 15.dp)
                ) {
                    for (j in 0..6) {
                        val idx = i * 7 + j
                        if (idx < luckyInfo.size) {
                            val rowBgColor =
                                if (selectedDate == LocalDate.now() && LocalDateTime.now().hour in (idx * 2 - 1)..(idx * 2)) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            Row(
                                modifier = Modifier
                                    .background(rowBgColor)
                                    .padding(vertical = 5.dp)
                            ) {
                                val hrs = when (idx) {
                                    0 -> "0时"
                                    12 -> "23时"
                                    else -> "${idx * 2 - 1}，${idx * 2}时"
                                }
                                Text(text = "${luckyInfo[idx].time.name}【$hrs】")
                                Spacer(modifier = Modifier.weight(1f))
                                Text(text = luckyInfo[idx].luckyName)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingWindowSettings() {
    val context = LocalContext.current
    val hasOverlayPermission = remember { mutableStateOf(false) }

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

    LaunchedEffect(isResumed, Unit) {
        hasOverlayPermission.value = Settings.canDrawOverlays(context)
    }

    if (!hasOverlayPermission.value) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = stringResource(id = R.string.floating_window_permission),
                modifier = Modifier.clickable {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                },
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

//@Preview
@Composable
fun AlarmEditPreview() {
    Surface {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            AlarmEdit(Modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEdit(modifier: Modifier) {
    val viewModel = viewModel<CalendarViewModel>()
    val alarmState = rememberAlarmInfoState(viewModel.editingAlarm)
    val initTime = alarmState.getTriggerTime().value
    val timePickerState = rememberTimePickerState(initTime.hour, initTime.minute, true)
    val alarmTypeExpanded = remember { mutableStateOf(false) }
    val selectedDate by viewModel.getSelectedDate()
    val selectedDateEffectCount = remember { mutableIntStateOf(0) }
    val showRemoveConfirm = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedDate) {
        selectedDateEffectCount.intValue++
        if (selectedDateEffectCount.intValue <= 1 && alarmState.getType().value == AlarmType.ALARM_ONE_TIME) {
            viewModel.setSelectedDate(alarmState.getTriggerDay().value)
        } else {
            alarmState.setType(alarmState.getType().value, selectedDate)
        }
    }

    BackHandler(enabled = viewModel.getAlarmEditState().value != CalendarViewModel.AlarmEditState.None) {
        viewModel.setAlarmEditState(CalendarViewModel.AlarmEditState.None)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 20.dp)
    ) {
        Text(
            text = stringResource(id = R.string.do_cancel),
            modifier = Modifier.clickable {
                viewModel.setAlarmEditState(CalendarViewModel.AlarmEditState.None)
            },
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 20.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(id = R.string.do_save),
            modifier = Modifier.clickable {
                val editState = viewModel.getAlarmEditState().value
                alarmState.setTriggerTime(
                    LocalTime.of(
                        timePickerState.hour,
                        timePickerState.minute
                    )
                )
                scope.launch {
                    if (editState == CalendarViewModel.AlarmEditState.Add) {
                        viewModel.addAlarm(alarmState.toAlarm())
                    } else {
                        viewModel.updateAlarm(alarmState.toAlarm())
                    }
                }.invokeOnCompletion {
                    viewModel.setAlarmEditState(CalendarViewModel.AlarmEditState.None)
                }
            },
            color = MaterialTheme.colorScheme.primary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        TimeInput(state = timePickerState)
    }
    Row(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .weight(0.5f)
                .padding(end = 10.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Box {
                Text(
                    text = alarmState.getType().value.getDisplayName(context),
                    modifier = Modifier.clickable { alarmTypeExpanded.value = true },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                DropdownMenu(
                    expanded = alarmTypeExpanded.value,
                    onDismissRequest = { alarmTypeExpanded.value = false },
                ) {
                    for (type in AlarmType.entries) {
                        if (type == AlarmType.UNRECOGNIZED) {
                            continue
                        }
                        DropdownMenuItem(
                            text = { Text(text = type.getDisplayName(context)) },
                            onClick = {
                                alarmState.setType(type, selectedDate)
                                alarmTypeExpanded.value = false
                            })
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(0.5f)
                .padding(start = 10.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = context.getDisplayDateText(alarmState.toAlarm()),
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
            )
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 20.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = alarmState.getTitle().value,
            onValueChange = {
                if (it.length <= 40) {
                    alarmState.setTitle(it)
                }
            },
            label = { Text(text = stringResource(id = R.string.alarm_title)) })
    }
    if (viewModel.getAlarmEditState().value == CalendarViewModel.AlarmEditState.Edit) {
        if (showRemoveConfirm.value) {
            AlertDialog(
                onDismissRequest = { showRemoveConfirm.value = false },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            viewModel.removeAlarm(alarmState.getId())
                        }.invokeOnCompletion {
                            viewModel.setAlarmEditState(CalendarViewModel.AlarmEditState.None)
                        }
                    }) {
                        Text(stringResource(id = R.string.do_remove), fontSize = 20.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveConfirm.value = false }) {
                        Text(stringResource(id = R.string.do_cancel), fontSize = 20.sp)
                    }
                },
                text = {
                    Text(
                        text = stringResource(id = R.string.alarm_remove_confirm),
                        fontSize = 20.sp
                    )
                }
            )
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(all = 30.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                showRemoveConfirm.value = true
            }) {
                Text(text = stringResource(id = R.string.alarm_remove), fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun AlarmList(modifier: Modifier, isResumed: Boolean) {
    val viewModel = viewModel<CalendarViewModel>()
    val alarmListState =
        viewModel.getAlarms()
            .collectAsStateWithLifecycle(initialValue = CalendarSettings.getDefaultInstance())
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 15.dp)
            .height(30.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = "+ ${stringResource(id = R.string.alarm_add)}",
            Modifier.clickable {
                scope.launch {
                    viewModel.setAlarmEditState(CalendarViewModel.AlarmEditState.Add)
                }
            },
            color = MaterialTheme.colorScheme.primary
        )
    }
    for (alarm in alarmListState.value.alarmsList) {
        AlarmView(modifier = Modifier, alarm = alarm, isResumed)
    }
}

//@Preview
@Composable
fun AlarmViewPreview() {
    val list = arrayListOf<AlarmInfo>()
    AlarmInfoBuilder()
        .setType(AlarmType.ALARM_ONE_TIME)
        .setTitle("起床起床起床")
        .setTriggerTime(LocalTime.of(7, 50))
        .setTriggerDay(LocalDate.of(2024, 5, 1))
        .build()
        .let {
            list.add(it)
        }
    AlarmInfoBuilder()
        .setType(AlarmType.ALARM_WORK_DAY)
        .setTitle("上班")
        .setTriggerTime(LocalTime.of(8, 50))
        .setTriggerWeekday(DayOfWeek.SATURDAY)
        .build()
        .let {
            list.add(it)
        }
    AlarmInfoBuilder()
        .setType(AlarmType.ALARM_EVERY_MONTH)
        .setTriggerTime(LocalTime.of(8, 50))
        .setTriggerMonthDay(8)
        .build()
        .let {
            list.add(it)
        }
    Surface {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box {
                Text(text = "${list.size}")
            }
            for (alarm in list) {
                AlarmView(modifier = Modifier, alarm = alarm, false)
            }
        }
    }
}

fun Context.getDisplayDateText(alarm: AlarmInfo): String {
    return when (alarm.type) {
        AlarmType.ALARM_ONE_TIME -> alarm.getDisplayDate(this)
        AlarmType.ALARM_WORK_DAY -> ""
        AlarmType.ALARM_EVERY_DAY -> ""
        AlarmType.ALARM_EVERY_WEEK -> alarm.getDisplayDayOfWeek(this)

        AlarmType.ALARM_EVERY_MONTH ->
            getString(R.string.alarm_every_month, alarm.triggerMonthDay)

        AlarmType.ALARM_EVERY_YEAR ->
            getString(R.string.alarm_every_year, alarm.triggerMonth, alarm.triggerMonthDay)

        else -> ""
    }
}

@Composable
fun AlarmView(modifier: Modifier, alarm: AlarmInfo, isResumed: Boolean) {
    val viewModel = viewModel<CalendarViewModel>()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val enabled = remember { mutableStateOf(true) }

    LaunchedEffect(isResumed) {
        enabled.value = (!alarm.disabled) && (alarm.getNextAlarmTime(context) != null)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 10.dp)
    ) {
        Text(text = alarm.title, fontSize = 14.sp)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
            .clickable {
                viewModel.setAlarmEditState(
                    CalendarViewModel.AlarmEditState.Edit,
                    alarm
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(0.4f),
        ) {
            Row(
                modifier = Modifier.padding(top = 5.dp),
            ) {
                Text(
                    text = alarm.getDisplayTime(),
                    fontSize = 24.sp,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(0.4f)
                .padding(end = 10.dp),
            horizontalAlignment = Alignment.End,
        ) {
            val dateText = context.getDisplayDateText(alarm)
            Text(
                text = "${alarm.type.getDisplayName(context)} $dateText",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column {
            Switch(checked = enabled.value, onCheckedChange = { newChecked ->
                if (newChecked && alarm.getNextAlarmTime(context) == null) {
                    return@Switch
                }
                coroutineScope.launch {
                    viewModel.toggleAlarm(alarmId = alarm.id, disabled = !newChecked)
                }
            })
        }
    }
}

@Composable
fun CalendarInputHeader(modifier: Modifier, state: CalendarState, onEditDone: (YearMonth) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val viewModel = viewModel<CalendarViewModel>()
    val selectedDate by viewModel.getSelectedDate()
    val currYearMonth = state.firstVisibleMonth(.6f)?.yearMonth ?: selectedDate.yearMonth
    val yearState = remember { mutableStateOf("") }
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Row(
        modifier = modifier.padding(start = 20.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(.25f))
        val keyboardController = LocalSoftwareKeyboardController.current
        val invalidYearMsg = stringResource(id = R.string.calendar_invalid_year, MIN_YEAR, MAX_YEAR)
        OutlinedTextField(
            value = yearState.value,
            modifier = Modifier
                .weight(.4f)
                .focusRequester(focusRequester),
            placeholder = {
                Text(text = currYearMonth.year.toString())
            },
            onValueChange = { input ->
                val year = input.filter { it.isDigit() }
                yearState.value = year
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    try {
                        val year = yearState.value.toInt()
                        if (year < MIN_YEAR || year > MAX_YEAR) {
                            scope.launch {
                                snackBarHostState.showSnackbar(invalidYearMsg)
                            }
                            return@KeyboardActions
                        }
                        keyboardController?.hide()
                        val mth = state.firstVisibleMonth(.6f)?.yearMonth?.month?.value ?: 1
                        onEditDone(YearMonth.of(year, mth))
                    } catch (e: Throwable) {
                        scope.launch {
                            snackBarHostState.showSnackbar(invalidYearMsg)
                        }
                        return@KeyboardActions
                    }
                }
            ),
            label = {
                Text(text = stringResource(id = R.string.calendar_year))
            },
        )
        Text(
            text = stringResource(id = R.string.calendar_cancel),
            modifier = Modifier
                .weight(.15f)
                .padding(horizontal = 5.dp)
                .clickable {
                    keyboardController?.hide()
                    val visibleMonth =
                        state.firstVisibleMonth(.6f)?.yearMonth ?: selectedDate.yearMonth
                    onEditDone(YearMonth.of(visibleMonth.year, visibleMonth.month.value))
                })
        Spacer(modifier = Modifier.weight(.2f))
    }
    Row {
        SnackbarHost(hostState = snackBarHostState) {
            Snackbar(
                snackbarData = it,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
fun CalendarHeader(
    modifier: Modifier,
    state: CalendarState,
    onYearMonthClick: (YearMonth) -> Unit
) {
    val viewModel = viewModel<CalendarViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val selectedDate by viewModel.getSelectedDate()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "<<",
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clickable {
                    coroutineScope.launch {
                        state.scrollToMonth(state.firstVisibleMonth.yearMonth.minusYears(1))
                    }
                },
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            fontSize = 24.sp,
        )
        Spacer(modifier = Modifier.weight(0.1f))
        Text(
            text = "<",
            modifier = Modifier
                .clickable {
                    coroutineScope.launch {
                        state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.previousMonth)
                    }
                },
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            fontSize = 24.sp,
        )
        Spacer(modifier = Modifier.weight(0.1f))
        Text(
            text = state.firstVisibleMonth(0.6f)?.yearMonth.toString(),
            modifier = Modifier
                .weight(0.4f)
                .clickable {
                    onYearMonthClick(state.firstVisibleMonth(0.6f)?.yearMonth ?: YearMonth.now())
                },
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )
        if (selectedDate != LocalDate.now() || state.firstVisibleMonth(0.6f)?.yearMonth != YearMonth.now()) {
            Text(
                text = stringResource(id = R.string.calendar_today),
                modifier = Modifier
                    .weight(0.2f)
                    .padding(end = 10.dp)
                    .clickable {
                        coroutineScope.launch {
                            state.scrollToMonth(YearMonth.now())
                            viewModel.setSelectedDate(LocalDate.now())
                        }
                    },
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )
        } else {
            Spacer(modifier = Modifier.weight(0.1f))
        }
        Text(
            text = ">",
            modifier = Modifier
                .clickable {
                    coroutineScope.launch {
                        state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.nextMonth)
                    }
                },
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.weight(0.1f))
        Text(
            text = ">>",
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clickable {
                    coroutineScope.launch {
                        state.scrollToMonth(state.firstVisibleMonth.yearMonth.plusYears(1))
                    }
                },
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            fontSize = 24.sp,
        )
    }
}

@Composable
fun BodyView(modifier: Modifier, state: CalendarState) {
    val viewModel = viewModel<CalendarViewModel>()

    HorizontalCalendar(
        modifier = modifier,
        state = state,
        dayContent = { day ->
            val holidayInfo by viewModel.getHolidayInfo(day.date)
                .collectAsStateWithLifecycle(initialValue = null)
            val isHoliday = holidayInfo?.holiday ?: false
            val isMakeupDay = holidayInfo?.holiday == false
            DayView(
                day,
                viewModel.isSelectedDate(day.date),
                isHoliday,
                isMakeupDay,
                viewModel.getDateSubtitle(day.date)
            ) {
                viewModel.setSelectedDate(day.date)
            }
        },
        monthHeader = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                daysOfWeek.forEach { dayOfWeek ->
                    Text(
                        text = dayOfWeek.getDisplayName(
                            TextStyle.SHORT,
                            Locale.ENGLISH
                        ),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
    )
}

//@Preview
@Composable
fun DayViewPreview() {
    Surface {
        Column(
            Modifier
                .width(50.dp)
                .height(60.dp)
        ) {
            DayView(
                day = CalendarDay(LocalDate.now(), DayPosition.MonthDate),
                true,
                true,
                false,
                "初三"
            ) {

            }
        }
    }
}

@Composable
fun DayView(
    day: CalendarDay,
    isSelected: Boolean,
    isHoliday: Boolean,
    isMakeupDay: Boolean,
    subTitle: String,
    onClick: (CalendarDay) -> Unit
) {
    val holidayColor = Color(0xFF339933)
    val bgColor =
        if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface
    Box(
        modifier = Modifier
            .aspectRatio(0.8f)
            .padding(all = 6.dp)
            .background(bgColor)
            .border(
                1.dp,
                if (day.isToday()) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .clickable { onClick(day) },
    ) {
        if (isMakeupDay) {
            val bgPainter: Painter = painterResource(id = R.drawable.makeup_day_tag)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(all = 2.dp)
            ) {
                Image(
                    painter = bgPainter,
                    contentDescription = null,
                    modifier = Modifier
                        .height(15.dp)
                        .let {
                            if (day.position == DayPosition.MonthDate) it
                            else it.graphicsLayer {
                                alpha = 0.3f
                            }
                        },
                )
            }
        }
        if (isHoliday) {
            val bgPainter: Painter = painterResource(id = R.drawable.holiday_tag)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(all = 2.dp)
            ) {
                Image(
                    painter = bgPainter,
                    contentDescription = null,
                    modifier = Modifier
                        .height(14.dp)
                        .let {
                            if (day.position == DayPosition.MonthDate) it
                            else it.graphicsLayer {
                                alpha = 0.3f
                            }
                        },
                )
            }
        }

        val textColor =
            if (day.date.dayOfWeek in arrayOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
                holidayColor
            } else {
                MaterialTheme.colorScheme.onSurface
            }.let {
                if (day.position == DayPosition.MonthDate) it
                else it.copy(alpha = 0.3f)
            }

        Text(
            text = day.date.dayOfMonth.toString(),
            modifier = Modifier.align(Alignment.Center),
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subTitle,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 1.dp),
            color = MaterialTheme.colorScheme.secondary.let {
                if (day.position == DayPosition.MonthDate) it
                else it.copy(alpha = 0.3f)
            },
            fontSize = 12.sp
        )
    }
}