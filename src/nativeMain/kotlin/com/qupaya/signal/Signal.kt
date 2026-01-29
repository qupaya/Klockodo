package com.qupaya.signal

import platform.posix.SIGABRT
import platform.posix.SIGBUS
import platform.posix.SIGCHLD
import platform.posix.SIGFPE
import platform.posix.SIGHUP
import platform.posix.SIGILL
import platform.posix.SIGINT
import platform.posix.SIGPIPE
import platform.posix.SIGQUIT
import platform.posix.SIGSEGV
import platform.posix.SIGSYS
import platform.posix.SIGTERM
import platform.posix.SIGTRAP
import platform.posix.SIGTTIN
import platform.posix.SIGTTOU
import platform.posix.SIGUSR1
import platform.posix.SIGUSR2
import platform.posix.SIGXCPU
import platform.posix.SIGXFSZ

enum class Signal(val signalValue: Int) {
    ABRT(SIGABRT),
    SEGV(SIGSEGV),
    FPE(SIGFPE),
    ILL(SIGILL),
    QUIT(SIGQUIT),
    TRAP(SIGTRAP),
    SYS(SIGSYS),
    BUS(SIGBUS),
    XCPU(SIGXCPU),
    XFSZ(SIGXFSZ),
    USR1(SIGUSR1),
    USR2(SIGUSR2),
    TERM(SIGTERM),
    INT(SIGINT),
    HUP(SIGHUP),
    PIPE(SIGPIPE),
    CHLD(SIGCHLD),
    TTIN(SIGTTIN),
    TTOU(SIGTTOU);

    companion object {
        /**
         * Signals that trigger us to core-dump
         */
        val CORE_DUMP_SIGNALS = setOf(ABRT, SEGV, FPE, ILL, QUIT, TRAP, SYS, BUS, XCPU, XFSZ)
        val NON_CORE_DUMP_SIGNALS = setOf(USR1, USR2, TERM, INT, HUP, PIPE, CHLD, TTIN, TTOU)
    }
}