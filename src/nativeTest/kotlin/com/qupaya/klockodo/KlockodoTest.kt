package com.qupaya.klockodo

import com.qupaya.klockodo.outboundPorts.ApiError
import com.qupaya.klockodo.outboundPorts.ApiResult
import com.qupaya.klockodo.outboundPorts.ForBuildingEntryRequests
import com.qupaya.klockodo.outboundPorts.ForGettingData
import com.qupaya.klockodo.outboundPorts.ForGettingTime
import com.qupaya.klockodo.outboundPorts.ForShowingNotifications
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlin.time.*

@OptIn(ExperimentalTime::class)
class KlockodoTest : BehaviorSpec({
    class FakeTimer : ForGettingTime {
        lateinit var nextTime: Instant
        lateinit var nextDate: LocalDate

        override fun getTime(): Instant = nextTime

        override fun getCurrentDate(): LocalDate = nextDate
    }

    data class FakeEntryRequest(val id: Int) : ForBuildingEntryRequests.EntryRequest

    class FakeEntryBuilder : ForBuildingEntryRequests {
        var counter = 0

        override fun buildEntryRequest(): ForBuildingEntryRequests.EntryRequest {
            return FakeEntryRequest(counter++)
        }
    }

    data class FakeApiError(
        override val message: String,
        override val fields: List<String>? = null,
        override val path: String? = null,
    ) : ApiError

    class FakeNotifier : ForShowingNotifications {
        val messages = mutableListOf<String>()

        override fun show(message: String) {
            messages.add(message)
        }
    }

    class FakeClockingApi(val timer: FakeTimer) : ForGettingData {
        var nextOpenWorkTimeOfYear = Duration.ZERO
        var nextOpenWorkTimeOfYearFailure: List<ApiError>? = null
        override fun fetchOpenWorkTimeOfYear(year: Int): ApiResult<Duration> {
            nextOpenWorkTimeOfYearFailure?.let { return ApiResult.Failure(it) }
            return ApiResult.Success(nextOpenWorkTimeOfYear)
        }

        var nextWorkedDurationOfDay = Duration.ZERO
        var nextWorkedDurationOfDayFailure: List<ApiError>? = null
        override fun fetchWorkedTimeOfDay(day: LocalDate): ApiResult<Duration> {
            nextWorkedDurationOfDayFailure?.let { return ApiResult.Failure(it) }
            return ApiResult.Success(nextWorkedDurationOfDay)
        }

        var currentTimeEntry: ForGettingData.TimeEntry? = null
        var nextGetCurrentTimeEntryFailure: List<ApiError>? = null
        override fun getCurrentTimeEntry(): ApiResult<ForGettingData.TimeEntry?> {
            nextGetCurrentTimeEntryFailure?.let { return ApiResult.Failure(it) }
            return ApiResult.Success(currentTimeEntry)
        }

        var nextStartTimeEntryFailure: List<ApiError>? = null
        override fun startTimeEntry(request: ForBuildingEntryRequests.EntryRequest): ApiResult<ForGettingData.StartedStoppedEntries> {
            nextStartTimeEntryFailure?.let { return ApiResult.Failure(it) }

            val result = object : ForGettingData.StartedStoppedEntries {
                override val startedEntry = object : ForGettingData.TimeEntry {
                    val startTime = timer.nextTime
                    override fun getStartTime(): Instant = startTime
                    override fun getEndTime(): Instant? = null
                    override fun getDuration(): Duration? = null
                }
                override val stoppedEntry = currentTimeEntry?.let {
                    object : ForGettingData.TimeEntry {
                        override fun getStartTime(): Instant = it.getStartTime()
                        override fun getEndTime(): Instant = timer.nextTime
                        override fun getDuration(): Duration = timer.nextTime.minus(it.getStartTime())
                    }
                }
            }
            currentTimeEntry = result.startedEntry
            return ApiResult.Success(result)
        }

        var nextStopEntry: (entry: ForGettingData.TimeEntry) -> ForGettingData.TimeEntry = {
            object : ForGettingData.TimeEntry {
                override fun getStartTime(): Instant = it.getStartTime()
                override fun getEndTime(): Instant = timer.nextTime
                override fun getDuration(): Duration = timer.nextTime.minus(it.getStartTime())
            }
        }
        var nextStopTimeEntryFailure: List<ApiError>? = null

        override fun stopTimeEntry(entry: ForGettingData.TimeEntry): ApiResult<ForGettingData.TimeEntry?> {
            if (currentTimeEntry != entry) {
                throw IllegalArgumentException("Entry must be the current one.")
            }
            nextStopTimeEntryFailure?.let { return ApiResult.Failure(it) }
            currentTimeEntry = null
            return ApiResult.Success(nextStopEntry(entry))
        }
    }

    Context("Klockodo should be able to calculate the work times") {
        val workTimePerDay = 8.toDuration(DurationUnit.HOURS)

        Given("A setup with some open hours from the year") {
            val timer = FakeTimer()
            val entryBuilder = FakeEntryBuilder()
            val notifier = FakeNotifier()
            val clockingApi = FakeClockingApi(timer)
            timer.nextDate = LocalDate(2023, 1, 1)
            timer.nextTime = Instant.parse("2023-01-01T10:00:00Z")
            clockingApi.nextOpenWorkTimeOfYear = 7.toDuration(DurationUnit.HOURS)

            When("Klockodo is started") {
                val klockodo = Klockodo(workTimePerDay, clockingApi, timer, entryBuilder, notifier)

                Then("it should return the initial hours to work") {
                    val workTime = klockodo.getWorkTime()
                    workTime.year shouldBe 7.toDuration(DurationUnit.HOURS)
                    workTime.today shouldBe 8.toDuration(DurationUnit.HOURS)
                }

                And("some time has passed (without running time entry)") {
                    timer.nextTime = Instant.parse("2023-01-01T11:00:00Z")

                    Then("it should still return the initial hours to work") {
                        val workTime = klockodo.getWorkTime()
                        workTime.year shouldBe 7.toDuration(DurationUnit.HOURS)
                        workTime.today shouldBe 8.toDuration(DurationUnit.HOURS)
                    }
                }
            }
        }

        Given("A setup with some open hours from the year") {
            val timer = FakeTimer()
            val entryBuilder = FakeEntryBuilder()
            val notifier = FakeNotifier()
            val clockingApi = FakeClockingApi(timer)
            timer.nextDate = LocalDate(2023, 1, 1)
            timer.nextTime = Instant.parse("2023-01-01T10:00:00Z")
            clockingApi.nextOpenWorkTimeOfYear = 7.toDuration(DurationUnit.HOURS)

            And("some work was already done (not including ongoing work)") {
                clockingApi.nextWorkedDurationOfDay = 3.toDuration(DurationUnit.HOURS)

                When("Klockodo is started") {
                    val klockodo = Klockodo(workTimePerDay, clockingApi, timer, entryBuilder, notifier)

                    Then("it should return the initial hours to work minus the already worked hours") {
                        val workTime = klockodo.getWorkTime()
                        workTime.year shouldBe 7.toDuration(DurationUnit.HOURS)
                        workTime.today shouldBe 5.toDuration(DurationUnit.HOURS)
                    }
                }

                And("a running time entry is present") {
                    clockingApi.currentTimeEntry = object : ForGettingData.TimeEntry {
                        override fun getStartTime(): Instant = Instant.parse("2023-01-01T08:00:00Z")
                        override fun getEndTime(): Instant? = null
                        override fun getDuration(): Duration? = null
                    }

                    When("Klockodo is started") {
                        val klockodo = Klockodo(workTimePerDay, clockingApi, timer, entryBuilder, notifier)

                        Then("it should return the initial hours to work minus the already worked hours and the initial ongoing work time") {
                            val workTime = klockodo.getWorkTime()
                            workTime.year shouldBe 7.toDuration(DurationUnit.HOURS)
                            workTime.today shouldBe 3.toDuration(DurationUnit.HOURS)
                        }

                        And("some time has passed") {
                            timer.nextTime = Instant.parse("2023-01-01T11:00:00Z")

                            Then("it should return the initial hours to work minus the already worked hours and the ongoing work time") {
                                val workTime = klockodo.getWorkTime()
                                workTime.year shouldBe 6.toDuration(DurationUnit.HOURS)
                                workTime.today shouldBe 2.toDuration(DurationUnit.HOURS)
                            }
                        }
                    }
                }
            }
        }

        Given("A setup with some open hours from the year") {
            val timer = FakeTimer()
            val entryBuilder = FakeEntryBuilder()
            val notifier = FakeNotifier()
            val clockingApi = FakeClockingApi(timer)
            timer.nextDate = LocalDate(2023, 1, 1)
            timer.nextTime = Instant.parse("2023-01-01T10:00:00Z")
            clockingApi.nextOpenWorkTimeOfYear = 7.toDuration(DurationUnit.HOURS)

            And("a running time entry is present") {
                clockingApi.currentTimeEntry = object : ForGettingData.TimeEntry {
                    override fun getStartTime(): Instant = Instant.parse("2023-01-01T08:00:00Z")
                    override fun getEndTime(): Instant? = null
                    override fun getDuration(): Duration? = null
                }

                When("Klockodo is started") {
                    val klockodo = Klockodo(workTimePerDay, clockingApi, timer, entryBuilder, notifier)

                    And("some time has passed") {
                        timer.nextTime = Instant.parse("2023-01-01T11:00:00Z")

                        Then("it should return the remaining open hours with reduced day hours") {
                            val workTime = klockodo.getWorkTime()
                            workTime.year shouldBe 6.toDuration(DurationUnit.HOURS)
                            workTime.today shouldBe 5.toDuration(DurationUnit.HOURS)
                        }

                        And("a task is started") {
                            klockodo.startLog()

                            Then("it should return the remaining open hours with reduced day hours") {
                                val workTime = klockodo.getWorkTime()
                                workTime.year shouldBe 6.toDuration(DurationUnit.HOURS)
                                workTime.today shouldBe 5.toDuration(DurationUnit.HOURS)
                            }

                            And("some time has passed") {
                                timer.nextTime = Instant.parse("2023-01-01T12:00:00Z")

                                Then("it should return the remaining open hours with reduced day hours") {
                                    val workTime = klockodo.getWorkTime()
                                    workTime.year shouldBe 5.toDuration(DurationUnit.HOURS)
                                    workTime.today shouldBe 4.toDuration(DurationUnit.HOURS)
                                }

                                And("the task is stopped") {
                                    klockodo.stopLog()

                                    Then("it should return the remaining open hours with reduced day hours") {
                                        val workTime = klockodo.getWorkTime()
                                        workTime.year shouldBe 5.toDuration(DurationUnit.HOURS)
                                        workTime.today shouldBe 4.toDuration(DurationUnit.HOURS)
                                    }

                                    And("some time has passed") {
                                        timer.nextTime = Instant.parse("2023-01-01T13:00:00Z")

                                        Then("it should return the remaining open hours with reduced day hours") {
                                            val workTime = klockodo.getWorkTime()
                                            workTime.year shouldBe 5.toDuration(DurationUnit.HOURS)
                                            workTime.today shouldBe 4.toDuration(DurationUnit.HOURS)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Given("A setup with some open hours from the year") {
            val timer = FakeTimer()
            val entryBuilder = FakeEntryBuilder()
            val notifier = FakeNotifier()
            val clockingApi = FakeClockingApi(timer)
            timer.nextDate = LocalDate(2023, 1, 1)
            timer.nextTime = Instant.parse("2023-01-01T08:00:00Z")
            clockingApi.nextOpenWorkTimeOfYear = 7.toDuration(DurationUnit.HOURS)

            When("Klockodo is started and a task too") {
                val klockodo = Klockodo(workTimePerDay, clockingApi, timer, entryBuilder, notifier)
                klockodo.startLog()

                And("I switch the task a bit later") {
                    timer.nextTime = Instant.parse("2023-01-01T10:00:00Z")
                    klockodo.startLog()

                    And("I move the switch time to 1 hour earlier") {
                        clockingApi.nextStopEntry = {
                            object : ForGettingData.TimeEntry {
                                override fun getStartTime(): Instant = Instant.parse("2023-01-01T09:00:00Z")
                                override fun getEndTime(): Instant = timer.nextTime
                                override fun getDuration(): Duration =
                                    timer.nextTime.minus(Instant.parse("2023-01-01T09:00:00Z"))
                            }
                        }

                        And("I wait one hour") {
                            timer.nextTime = Instant.parse("2023-01-01T11:00:00Z")
                            clockingApi.nextOpenWorkTimeOfYear = 4.toDuration(DurationUnit.HOURS)
                            clockingApi.nextWorkedDurationOfDay = 3.toDuration(DurationUnit.HOURS)

                            Then("it should display the correct time") {
                                val workTime = klockodo.getWorkTime()
                                workTime.year shouldBe 4.toDuration(DurationUnit.HOURS)
                                workTime.today shouldBe 5.toDuration(DurationUnit.HOURS)
                            }

                            And("I stop the running task") {
                                klockodo.stopLog()

                                Then("it should still display the correct time after the adjustment") {
                                    val workTime = klockodo.getWorkTime()
                                    workTime.year shouldBe 4.toDuration(DurationUnit.HOURS)
                                    workTime.today shouldBe 5.toDuration(DurationUnit.HOURS)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Context("Klockodo should handle API errors without crashing") {
        val workTimePerDay = 8.toDuration(DurationUnit.HOURS)

        fun givenRunningEntry(): Triple<FakeClockingApi, Klockodo, FakeNotifier> {
            val timer = FakeTimer()
            val entryBuilder = FakeEntryBuilder()
            val notifier = FakeNotifier()
            val clockingApi = FakeClockingApi(timer)
            timer.nextDate = LocalDate(2023, 1, 1)
            timer.nextTime = Instant.parse("2023-01-01T10:00:00Z")
            val runningEntry = object : ForGettingData.TimeEntry {
                override fun getStartTime(): Instant = Instant.parse("2023-01-01T08:00:00Z")
                override fun getEndTime(): Instant? = null
                override fun getDuration(): Duration? = null
            }
            clockingApi.currentTimeEntry = runningEntry
            val klockodo = Klockodo(workTimePerDay, clockingApi, timer, entryBuilder, notifier)
            return Triple(clockingApi, klockodo, notifier)
        }

        Given("A running time entry, when starting a new entry fails") {
            val (clockingApi, klockodo, notifier) = givenRunningEntry()

            When("starting a new entry fails") {
                clockingApi.nextStartTimeEntryFailure = listOf(FakeApiError("The requested resource could not be found."))
                klockodo.startLog()

                Then("it should keep the previously running entry") {
                    klockodo.hasRunningLog() shouldBe true
                }

                Then("it should show exactly one notification") {
                    notifier.messages.size shouldBe 1
                    notifier.messages.first() shouldBe "The requested resource could not be found."
                }
            }
        }

        Given("A running time entry, when stopping the running entry fails") {
            val (clockingApi, klockodo, notifier) = givenRunningEntry()

            When("stopping the running entry fails") {
                clockingApi.nextStopTimeEntryFailure = listOf(FakeApiError("Internal server error"))
                klockodo.stopLog()

                Then("it should keep the entry running") {
                    klockodo.hasRunningLog() shouldBe true
                }

                Then("it should show exactly one notification") {
                    notifier.messages.size shouldBe 1
                    notifier.messages.first() shouldBe "Internal server error"
                }
            }
        }

        Given("A fresh setup where fetching the yearly open time fails") {
            val timer = FakeTimer()
            val entryBuilder = FakeEntryBuilder()
            val notifier = FakeNotifier()
            val clockingApi = FakeClockingApi(timer)
            timer.nextDate = LocalDate(2023, 1, 1)
            timer.nextTime = Instant.parse("2023-01-01T10:00:00Z")
            clockingApi.nextOpenWorkTimeOfYearFailure = listOf(FakeApiError("boom"))

            When("Klockodo is started") {
                val klockodo = Klockodo(workTimePerDay, clockingApi, timer, entryBuilder, notifier)

                Then("it should fall back to zero for the yearly time and notify") {
                    val workTime = klockodo.getWorkTime()
                    workTime.year shouldBe Duration.ZERO
                    notifier.messages.size shouldBe 1
                    notifier.messages.first() shouldBe "boom"
                }
            }
        }
    }

    Context("Formatting API errors into a notification message") {
        fun startWithFailure(errors: List<ApiError>): FakeNotifier {
            val timer = FakeTimer()
            val entryBuilder = FakeEntryBuilder()
            val notifier = FakeNotifier()
            val clockingApi = FakeClockingApi(timer)
            timer.nextDate = LocalDate(2023, 1, 1)
            timer.nextTime = Instant.parse("2023-01-01T10:00:00Z")
            clockingApi.nextOpenWorkTimeOfYearFailure = errors

            Klockodo(8.toDuration(DurationUnit.HOURS), clockingApi, timer, entryBuilder, notifier)
            return notifier
        }

        Given("An error with only a message") {
            When("Klockodo is started") {
                val notifier = startWithFailure(listOf(FakeApiError("The requested resource could not be found.")))

                Then("it should show just the message") {
                    notifier.messages.first() shouldBe "The requested resource could not be found."
                }
            }
        }

        Given("An error with a message and a path") {
            When("Klockodo is started") {
                val notifier = startWithFailure(listOf(FakeApiError("Invalid input", path = "customers_id")))

                Then("it should show the message and the path on separate lines") {
                    notifier.messages.first() shouldBe "Invalid input\ncustomers_id"
                }
            }
        }

        Given("An error with a message, a path and fields") {
            When("Klockodo is started") {
                val notifier = startWithFailure(
                    listOf(FakeApiError("Invalid input", fields = listOf("customers_id", "services_id"), path = "entries"))
                )

                Then("it should show message, path and comma-separated fields on separate lines") {
                    notifier.messages.first() shouldBe "Invalid input\nentries\ncustomers_id, services_id"
                }
            }
        }

        Given("Multiple errors") {
            When("Klockodo is started") {
                val notifier = startWithFailure(
                    listOf(FakeApiError("First problem"), FakeApiError("Second problem", path = "text"))
                )

                Then("it should join all errors with newlines") {
                    notifier.messages.first() shouldBe "First problem\nSecond problem\ntext"
                }
            }
        }
    }
})
