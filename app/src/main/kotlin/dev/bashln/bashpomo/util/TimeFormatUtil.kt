package dev.bashln.bashpomo.util

object TimeFormatUtil {

    /**
     * Formats [totalSeconds] as "MM:SS".
     * Works for values >= 0. If [totalSeconds] < 0 the absolute value is used.
     */
    fun formatMMSS(totalSeconds: Int): String {
        val abs = kotlin.math.abs(totalSeconds)
        val minutes = abs / 60
        val seconds = abs % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
