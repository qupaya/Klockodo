import com.qupaya.Configuration
import com.qupaya.klockodo.Klockodo
import com.qupaya.outbound.forBuildingEntryRequest.EntryRequestBuilder
import com.qupaya.outbound.forGettingData.KlockodoApi
import com.qupaya.outbound.forGettingTime.LocalTimer
import com.qupaya.signal.SignalHandler
import com.qupaya.ui.GtkApplication
import com.qupaya.ui.Indicator
import com.qupaya.ui.Menu
import com.qupaya.ui.Notification
import klockodo.GtkWidget
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


var app: GtkApplication? = null

val config = Configuration.load()
val forGettingTime = LocalTimer()
val api = KlockodoApi(config.apiKey, config.apiUser)
val entryRequestBuilder = EntryRequestBuilder()
val klockodo = Klockodo(config.workTimePerDay, api, forGettingTime, entryRequestBuilder)
val signalHandler = SignalHandler()

@OptIn(ExperimentalForeignApi::class)
var toggleMenuItem: CPointer<GtkWidget>? = null

@OptIn(ExperimentalForeignApi::class)
var infoMenuItem: CPointer<GtkWidget>? = null
var infoJob: Job? = null
var isDone = false
var reachedDailyMin = false

val MIN_WORK_HOURS = 4.toDuration(DurationUnit.HOURS)

fun switchTimeEntry() {
    klockodo.startLog()
}

@OptIn(ExperimentalForeignApi::class)
fun toggleRunPause() {
    val menuItem = toggleMenuItem ?: return

    if (klockodo.hasRunningLog()) {
        klockodo.stopLog()
        Indicator.showInactive()
        Menu.changeMenuButtonLabelAndIcon(menuItem, "Continue", "player_play")
    } else {
        klockodo.startLog()
        setActiveIndicator()
        Menu.changeMenuButtonLabelAndIcon(menuItem, "Pause", "player_pause")
    }
}

fun setActiveIndicator() {
    when {
        !reachedDailyMin -> Indicator.showMustWork()
        isDone -> Indicator.showDone()
        else -> Indicator.showMinDailyDone()
    }
}

fun quit() {
    app?.quitMainLoop()
    app = null
}

@OptIn(ExperimentalForeignApi::class)
fun main() = GtkApplication.create {
    app = this

    infoMenuItem = Menu.appendMenuLabel("Loading time ...")
    Menu.appendSeparator()
    Menu.appendMenuButton("Switch", "appointment-new", staticCFunction(::switchTimeEntry))
    toggleMenuItem = Menu.appendMenuButton("Pause", "player_pause", staticCFunction(::toggleRunPause))
    Menu.appendSeparator()
    Menu.appendMenuButton("Quit", "exit", staticCFunction(::quit))

    Indicator.init(Menu.REF?.reinterpret())

    val today = forGettingTime.getCurrentDate()
    val isSupposedToWorkToday = !setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(today.dayOfWeek)
    if (!isSupposedToWorkToday) {
        Indicator.showInactive()
    } else if (!klockodo.hasRunningLog()) {
        klockodo.startLog()
        setActiveIndicator()
    } else {
        setActiveIndicator()
    }

    val (wtToday, wtYear) = klockodo.getWorkTime()
    Notification.show("Today you need to work for\nD: ${wtToday.toHourMinuteString()} / Y: ${wtYear.toHourMinuteString()}")

    infoJob = runInfoLoop()

    runMainLoop {
        signalHandler.cleanup()
        klockodo.stopLog()
        infoJob?.cancel()
    }
}

@OptIn(ExperimentalForeignApi::class)
fun CoroutineScope.runInfoLoop(): Job = launch {
    while (true) {
        val (wtToday, wtYear) = klockodo.getWorkTime()
        infoMenuItem?.run {
            Menu.changeMenuLabel(
                this,
                "To work: D: ${wtToday.toHourMinuteString()} / Y: ${wtYear.toHourMinuteString()}"
            )
        }
        var statusChanged = false
        if (wtToday <= MIN_WORK_HOURS && !reachedDailyMin) {
            reachedDailyMin = true
            statusChanged = true
        }
        if (wtYear <= Duration.ZERO && !isDone) {
            isDone = true
            statusChanged = true
        }
        if (statusChanged && reachedDailyMin && isDone) {
            Notification.show("You finished your work for today.")
        }
        if (statusChanged) {
            setActiveIndicator()
        }
        delay(1000)
    }
}

fun Duration.toHourMinuteString(): String {
    val absoluteDuration = this.absoluteValue
    val hours = "${absoluteDuration.inWholeHours}"
    val minutes = "${absoluteDuration.inWholeMinutes % 60}"
    val sign = if (this.isNegative()) "-" else ""
    return "${sign}${hours.padStart(2, '0')}:${minutes.padStart(2, '0')}"
}
