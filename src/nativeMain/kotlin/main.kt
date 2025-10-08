import com.qupaya.klockodo.Configuration
import com.qupaya.klockodo.Klockodo
import com.qupaya.klockodo.signal.SignalHandler
import com.qupaya.klockodo.ui.GtkApplication
import com.qupaya.klockodo.ui.Indicator
import com.qupaya.klockodo.ui.Menu
import com.qupaya.klockodo.ui.Notification
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import klockodo.GtkWidget
import kotlin.math.abs


var app: GtkApplication? = null

val config = Configuration.load()
val klockodo = Klockodo(config)
val signalHandler = SignalHandler()

@OptIn(ExperimentalForeignApi::class)
var toggleMenuItem: CPointer<GtkWidget>? = null
@OptIn(ExperimentalForeignApi::class)
var infoMenuItem: CPointer<GtkWidget>? = null
var infoJob: Job? = null
var isDone = false
var reachedDailyMin = false

const val MIN_WORK_HOURS_DAILY_SEC = 4 * 60 * 60

fun switchTimeEntry() {
  klockodo.switch()
  println("Switch: ${klockodo.currentEntry?.id}")
}

@OptIn(ExperimentalForeignApi::class)
fun toggleRunPause() {
  val menuItem = toggleMenuItem ?: return

  if (klockodo.hasRunningTimeEntry()) {
    klockodo.stop()
    Indicator.showInactive()
    Menu.changeMenuButtonLabelAndIcon(menuItem, "Continue", "player_play")
    println("Pause")
  } else {
    klockodo.start()
    setActiveIndicator()
    Menu.changeMenuButtonLabelAndIcon(menuItem, "Pause", "player_pause")
    println("Continue: ${klockodo.currentEntry?.id}")
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
  println("Quit")
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

  if (!klockodo.hasRunningTimeEntry()) {
    klockodo.start()
    println("Started: ${klockodo.currentEntry?.id}")
  } else {
    println("Found: ${klockodo.currentEntry?.id}")
  }

  val (secondsOpenToday, secondsOpenFromYear) = klockodo.getTimeToDo()
  Notification.show("Today you need to work for ${secondsOpenToday.formatTime()} / ${secondsOpenFromYear.formatTime()}")

  infoJob = runInfoLoop()

  runMainLoop {
    signalHandler.cleanup()
    klockodo.stop()
    infoJob?.cancel()
  }
}

@OptIn(ExperimentalForeignApi::class)
fun CoroutineScope.runInfoLoop(): Job = launch {
  while (true) {
    val (secondsOpenToday, secondsOpenFromYear) = klockodo.getTimeToDo()
    infoMenuItem?.run { Menu.changeMenuLabel(this, "To work: ${secondsOpenToday.formatTime()} / ${secondsOpenFromYear.formatTime()}") }
    Indicator.setIndicatorTitle("To work: ${secondsOpenToday.formatTime()} / ${secondsOpenFromYear.formatTime()}")
    var statusChanged = false
    if (secondsOpenToday < MIN_WORK_HOURS_DAILY_SEC && !reachedDailyMin) {
      reachedDailyMin = true
      statusChanged = true
    }
    if (secondsOpenFromYear <= 0 && !isDone) {
      isDone = true
      statusChanged = true
    }
    if (reachedDailyMin && isDone) {
      Notification.show("You finished your work for today.")
    }
    if (statusChanged) {
      setActiveIndicator()
    }
    delay(200)
  }
}

fun Long.formatTime(): String {
  val sign = if (this < 0) "-" else ""
  val hours = this.div(3600)
  val minutes = this.div(60).mod(60)
  return "$sign${abs(hours).toString().padStart(2, '0')}:${abs(minutes).toString().padStart(2, '0')}"
}
