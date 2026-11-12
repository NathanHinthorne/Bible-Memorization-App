package com.bible.sketchbook.memory

import kotlin.math.roundToLong

enum class ReviewRating(val quality: Int) {
    AGAIN(0),
    HARD(3),
    GOOD(4),
    EASY(5),
}

data class Sm2Progress(
    val intervalDays: Long = 0,
    val repetitionCount: Int = 0,
    val easinessFactor: Double = 2.5,
)

data class Sm2Review(
    val progress: Sm2Progress,
    val nextReviewAtEpochMillis: Long,
)

object Sm2Scheduler {
    private const val MIN_EASINESS_FACTOR = 1.3
    private const val MILLIS_PER_DAY = 86_400_000L

    fun review(
        progress: Sm2Progress,
        rating: ReviewRating,
        reviewedAtEpochMillis: Long,
    ): Sm2Review {
        require(progress.intervalDays >= 0) { "Interval must not be negative." }
        require(progress.repetitionCount >= 0) { "Repetition count must not be negative." }
        require(
            progress.easinessFactor.isFinite() &&
                progress.easinessFactor >= MIN_EASINESS_FACTOR
        ) { "Easiness factor must be finite and at least $MIN_EASINESS_FACTOR." }
        require(reviewedAtEpochMillis >= 0) { "Review timestamp must not be negative." }

        val quality = rating.quality
        val updatedEasinessFactor = (
            progress.easinessFactor +
                (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
            ).coerceAtLeast(MIN_EASINESS_FACTOR)

        val intervalDays: Long
        val repetitionCount: Int
        if (quality < 3) {
            intervalDays = 1
            repetitionCount = 0
        } else {
            require(progress.repetitionCount < Int.MAX_VALUE) {
                "Repetition count exceeds the supported range."
            }
            repetitionCount = progress.repetitionCount + 1
            intervalDays = when (progress.repetitionCount) {
                0 -> 1
                1 -> 6
                else -> (progress.intervalDays * progress.easinessFactor).roundToLong()
                    .coerceAtLeast(1)
            }
        }

        val remainingTimestampRange = Long.MAX_VALUE - reviewedAtEpochMillis
        require(intervalDays <= remainingTimestampRange / MILLIS_PER_DAY) {
            "Calculated review timestamp exceeds the supported range."
        }

        return Sm2Review(
            progress = Sm2Progress(
                intervalDays = intervalDays,
                repetitionCount = repetitionCount,
                easinessFactor = updatedEasinessFactor,
            ),
            nextReviewAtEpochMillis = reviewedAtEpochMillis + intervalDays * MILLIS_PER_DAY,
        )
    }
}
