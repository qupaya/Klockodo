package com.qupaya.klockodo

import com.qupaya.klockodo.outboundPorts.ForBuildingEntryRequests
import com.qupaya.klockodo.outboundPorts.ForGettingData
import com.qupaya.klockodo.outboundPorts.ForGettingTime
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

    class FakeClockingApi(val timer: FakeTimer) : ForGettingData {
        var nextOpenWorkTimeOfYear = Duration.ZERO
        override fun fetchOpenWorkTimeOfYear(year: Int): Duration {
            return nextOpenWorkTimeOfYear
        }

        var nextWorkedDurationOfDay = Duration.ZERO
        override fun fetchWorkedTimeOfDay(day: LocalDate): Duration {
            return nextWorkedDurationOfDay
        }

        var currentTimeEntry: ForGettingData.TimeEntry? = null
        override fun getCurrentTimeEntry(): ForGettingData.TimeEntry? {
            return currentTimeEntry
        }

        override fun startTimeEntry(request: ForBuildingEntryRequests.EntryRequest): ForGettingData.StartedStoppedEntries {
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
            return result
        }

        var nextStopEntry: (entry: ForGettingData.TimeEntry) -> ForGettingData.TimeEntry = {
            object : ForGettingData.TimeEntry {
                override fun getStartTime(): Instant = it.getStartTime()
                override fun getEndTime(): Instant = timer.nextTime
                override fun getDuration(): Duration = timer.nextTime.minus(it.getStartTime())
            }
        }

        override fun stopTimeEntry(entry: ForGettingData.TimeEntry): ForGettingData.TimeEntry {
            if (currentTimeEntry != entry) {
                throw IllegalArgumentException("Entry must be the current one.")
            }
            currentTimeEntry = null
            return nextStopEntry(entry)
        }
    }

    Context("Klockodo should be able to calculate the work times") {
        val workTimePerDay = 8.toDuration(DurationUnit.HOURS)

        Given("A setup with some open hours from the year") {
            val timer = FakeTimer()
            val entryBuilder = FakeEntryBuilder()
            val clockingApi = FakeClockingApi(timer)
            timer.nextDate = LocalDate(2023, 1, 1)
            timer.nextTime = Instant.parse("2023-01-01T10:00:00Z")
            clockingApi.nextOpenWorkTimeOfYear = 7.toDuration(DurationUnit.HOURS)

            When("Klockodo is started") {
                val klockodo = Klockodo(workTimePerDay, clockingApi, timer, entryBuilder)

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
            val clockingApi = FakeClockingApi(timer)
            timer.nextDate = LocalDate(2023, 1, 1)
            timer.nextTime = Instant.parse("2023-01-01T10:00:00Z")
            clockingApi.nextOpenWorkTimeOfYear = 7.toDuration(DurationUnit.HOURS)

            And("some work was already done (not including ongoing work)") {
                clockingApi.nextWorkedDurationOfDay = 3.toDuration(DurationUnit.HOURS)

                When("Klockodo is started") {
                    val klockodo = Klockodo(workTimePerDay, clockingApi, timer, entryBuilder)

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
                        val klockodo = Klockodo(workTimePerDay, clockingApi, timer, entryBuilder)

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
                    val klockodo = Klockodo(workTimePerDay, clockingApi, timer, entryBuilder)

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
            val clockingApi = FakeClockingApi(timer)
            timer.nextDate = LocalDate(2023, 1, 1)
            timer.nextTime = Instant.parse("2023-01-01T08:00:00Z")
            clockingApi.nextOpenWorkTimeOfYear = 7.toDuration(DurationUnit.HOURS)

            When("Klockodo is started and a task too") {
                val klockodo = Klockodo(workTimePerDay, clockingApi, timer, entryBuilder)
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
})