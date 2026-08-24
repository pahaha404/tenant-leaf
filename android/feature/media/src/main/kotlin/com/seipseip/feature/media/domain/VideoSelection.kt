package com.seipseip.feature.media.domain

sealed interface VideoSelection<out T> {
    data object None : VideoSelection<Nothing>
    data class Automatic<T>(val value: T) : VideoSelection<T>
    data class ConfirmationRequired<T>(val newestFirst: List<T>) : VideoSelection<T>

    companion object {
        fun <T> from(candidates: List<T>, createdAtMillis: (T) -> Long): VideoSelection<T> {
            val sorted = candidates.sortedByDescending(createdAtMillis)
            return when (sorted.size) {
                0 -> None
                1 -> Automatic(sorted.first())
                else -> ConfirmationRequired(sorted)
            }
        }
    }
}

