import com.qupaya.toggl.Configuration
import com.qupaya.toggl.Toggl
import com.qupaya.toggl.signal.SignalHandler
import com.qupaya.toggl.ui.GtkApplication
import com.qupaya.toggl.ui.Indicator
import com.qupaya.toggl.ui.Menu
import com.qupaya.toggl.ui.Notification
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import togglKlient.GtkWidget
import kotlin.math.abs


var app: GtkApplication? = null

val props = Configuration.load()
val toggl = Toggl(props)
val signalHandler = SignalHandler()

@OptIn(ExperimentalForeignApi::class)
var toggleMenuItem: CPointer<GtkWidget>? = null
@OptIn(ExperimentalForeignApi::class)
var infoMenuItem: CPointer<GtkWidget>? = null
var infoJob: Job? = null
var isDoneSent = false

fun switchTimeEntry() {
  toggl.switch()
  println("Switch: ${toggl.currentEntry?.id}")
}

@OptIn(ExperimentalForeignApi::class)
fun toggleRunPause() {
  val menuItem = toggleMenuItem ?: return

  if (toggl.hasRunningTimeEntry()) {
    toggl.stop()
    Indicator.showInactive()
    Menu.changeMenuButtonLabelAndIcon(menuItem, "Continue", "player_play")
    println("Pause")
  } else {
    toggl.start()
    Indicator.showActive()
    Menu.changeMenuButtonLabelAndIcon(menuItem, "Pause", "player_pause")
    println("Continue: ${toggl.currentEntry?.id}")
  }
}

fun quit() {
  println("Quit")
  signalHandler.cleanup()
  toggl.stop()
  infoJob?.cancel()
  app?.quitMainLoop()
  app = null
}

fun cancel() {
  println("Cancel")
  signalHandler.cleanup()
  infoJob?.cancel()
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
  Menu.appendMenuButton("Cancel", "process-stop", staticCFunction(::cancel))
  Menu.appendMenuButton("Quit", "exit", staticCFunction(::quit))

  Indicator.init(Menu.REF?.reinterpret())

  if (!toggl.hasRunningTimeEntry()) {
    toggl.start()
    println("Started: ${toggl.currentEntry?.id}")
  } else {
    println("Found: ${toggl.currentEntry?.id}")
  }

  val toDoTime = toggl.getTimeToDo()
  Notification.show("Today you need to work for ${toDoTime.formatTime()}")

  infoJob = runInfoLoop()

  runMainLoop()
}

@OptIn(ExperimentalForeignApi::class)
fun CoroutineScope.runInfoLoop(): Job = launch {
  while (true) {
    val toDoTime = toggl.getTimeToDo()
    infoMenuItem?.run { Menu.changeMenuLabel(this, "To work: ${toDoTime.formatTime()}") }
    Indicator.setIndicatorTitle("To work: ${toDoTime.formatTime()}")
    if (toDoTime <= 0.0 && !isDoneSent) {
      Notification.show("You finished your work for today.")
      isDoneSent = true
    }
    delay(200)
  }
}

fun Double.formatTime(): String {
  val sign = if (this < 0) "-" else ""
  val hours = this.toInt()
  val minutes = ((this - hours) * 60).toInt()
  return "$sign${abs(hours).toString().padStart(2, '0')}:${abs(minutes).toString().padStart(2, '0')}"
}
