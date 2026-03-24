package scot.oskar.galaxyfactions.ext

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future

fun <T> CoroutineScope.futureFromSuspend(block: suspend () -> T) = future {
    block()
}
