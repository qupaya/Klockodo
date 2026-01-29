package com.qupaya.signal

import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import platform.posix.SA_NOCLDSTOP
import platform.posix.WNOHANG
import platform.posix.abort
import platform.posix.sigaction
import platform.posix.sigemptyset
import platform.posix.sigfillset
import platform.posix.sigset_t
import platform.posix.waitpid
import quit

@OptIn(ExperimentalForeignApi::class)
class SignalHandler {
    private val allSignals: sigset_t = nativeHeap.alloc()

    private val oldActions = mutableMapOf<Signal, sigaction>()

    init {
        val action = nativeHeap.alloc<sigaction>()

        sigfillset(allSignals.ptr)
        sigemptyset(action.sa_mask.ptr)

        action.__sigaction_handler.sa_handler = staticCFunction<Int, Unit> { abort() }
        action.sa_flags = SA_NOCLDSTOP

        Signal.CORE_DUMP_SIGNALS
            // don't register for ABRT because it will be triggered by abort from other core signals
            .filter { it != Signal.ABRT }
            .forEach {
                val oldSignal = nativeHeap.alloc<sigaction>()
                sigaction(it.signalValue, action.ptr, oldSignal.ptr)
                oldActions[it] = oldSignal
            }

        Signal.NON_CORE_DUMP_SIGNALS.forEach {
            addSignalCallback(it, staticCFunction(::handleSignal))
        }
    }

    fun addSignalCallback(signal: Signal, signalHandler: CPointer<CFunction<(Int) -> Unit>>) {
        val action = nativeHeap.alloc<sigaction>()
        sigemptyset(action.sa_mask.ptr)
        action.__sigaction_handler.sa_handler = signalHandler
        action.sa_flags = SA_NOCLDSTOP

        val oldSignal = nativeHeap.alloc<sigaction>()
        sigaction(signal.signalValue, action.ptr, oldSignal.ptr)
        oldActions[signal] = oldSignal
    }

    fun cleanup() {
        oldActions.forEach { (signalValue, action) ->
            sigaction(signalValue.signalValue, action.ptr, null)
        }
        oldActions.clear()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun handleSignal(signalValue: Int) {
    val signal = Signal.entries.single { it.signalValue == signalValue }
    println("Handle signal: $signal (${signal.signalValue})")
    when (signal) {
        Signal.USR1, Signal.USR2, Signal.TTIN, Signal.TTOU -> println("Ignoring signal $signal")
        Signal.CHLD -> while (waitpid(-1, null, WNOHANG) > 0);
        else -> quit()
    }
}
