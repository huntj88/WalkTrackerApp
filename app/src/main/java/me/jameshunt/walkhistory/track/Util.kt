package me.jameshunt.walkhistory.track

typealias Seconds = Long
fun Seconds.elapsedTimeString(): String {
    val renderedSeconds = this % 60
    val minutes = this / 60
    val renderedMinutes = minutes % 60
    val hours = minutes / 60

    return listOf(hours, renderedMinutes, renderedSeconds).joinToString(" : ") {
        when (it.toString().length == 1) {
            true -> "0$it"
            false -> "$it"
        }
    }
}
