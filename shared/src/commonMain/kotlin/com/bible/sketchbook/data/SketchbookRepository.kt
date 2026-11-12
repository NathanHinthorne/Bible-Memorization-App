package com.bible.sketchbook.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.bible.sketchbook.db.BibleDatabase
import com.bible.sketchbook.memory.ReviewRating
import com.bible.sketchbook.memory.Sm2Progress
import com.bible.sketchbook.memory.Sm2Scheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

data class SketchbookSummary(
    val id: String,
    val title: String,
    val description: String?,
    val coverStyle: String,
    val dueCardCount: Long,
)

data class MemoryCardSummary(
    val id: String,
    val sketchbookId: String,
    val bookId: Long,
    val bookName: String,
    val chapter: Long,
    val startVerse: Long,
    val endVerse: Long,
    val intervalDays: Long,
    val repetitionCount: Int,
    val easinessFactor: Double,
    val nextReviewAt: Long,
    val isDue: Boolean,
) {
    val reference: String
        get() = "$bookName $chapter:$startVerse" +
            if (endVerse == startVerse) "" else "–$endVerse"
}

data class VerseSearchResult(
    val bookId: Long,
    val bookName: String,
    val chapter: Long,
    val verse: Long,
    val text: String,
) {
    val reference: String
        get() = "$bookName $chapter:$verse"
}

data class BibleVerse(
    val verse: Long,
    val text: String,
)

class SketchbookRepository(private val database: BibleDatabase) {
    private val queries = database.bibleSketchbookQueries
    private val referencePattern = Regex("^(.+?)\\s+(\\d+)(?::(\\d+))?$")

    fun observeSketchbooks(nowEpochMillis: Long): Flow<List<SketchbookSummary>> =
        queries.selectSketchbooks(nowEpochMillis) { id, title, description, coverStyle, _, dueCount ->
            SketchbookSummary(id, title, description, coverStyle, dueCount)
        }.asFlow().mapToList(Dispatchers.Default)

    fun observeCards(
        sketchbookId: String,
        nowEpochMillis: Long,
    ): Flow<List<MemoryCardSummary>> =
        queries.selectCardsForSketchbook(sketchbookId) {
                id,
                notebookId,
                bookId,
                chapter,
                startVerse,
                endVerse,
                intervalDays,
                repetitionCount,
                easinessFactor,
                nextReviewAt,
                _,
                bookName,
            ->
            MemoryCardSummary(
                id = id,
                sketchbookId = notebookId,
                bookId = bookId,
                bookName = bookName,
                chapter = chapter,
                startVerse = startVerse,
                endVerse = endVerse,
                intervalDays = intervalDays,
                repetitionCount = repetitionCount.toInt(),
                easinessFactor = easinessFactor,
                nextReviewAt = nextReviewAt,
                isDue = nextReviewAt <= nowEpochMillis,
            )
        }.asFlow().mapToList(Dispatchers.Default)

    fun observeVerseSearch(search: String): Flow<List<VerseSearchResult>> {
        val cleanSearch = search.trim()
        val reference = referencePattern.matchEntire(cleanSearch)?.takeIf { match ->
            match.groupValues[2].toLongOrNull() != null &&
                (match.groupValues[3].isEmpty() || match.groupValues[3].toLongOrNull() != null)
        }
        val chapterFilter = reference?.groupValues?.get(2)?.toLongOrNull()
        val verseFilter = reference?.groupValues?.get(3)?.takeIf(String::isNotEmpty)?.toLongOrNull()
        return queries.searchBibleVerses(
            book_search = reference?.groupValues?.get(1).orEmpty(),
            chapter_filter = chapterFilter,
            verse_filter = verseFilter,
            text_search = if (reference == null) cleanSearch else "",
        ) { bookId, bookName, chapter, verse, text ->
            VerseSearchResult(bookId, bookName, chapter, verse, text)
        }.asFlow().mapToList(Dispatchers.Default)
    }

    fun observeVersesForCard(cardId: String): Flow<List<BibleVerse>> =
        queries.selectVersesForCard(cardId) { _, _, verse, text ->
            BibleVerse(verse, text)
        }.asFlow().mapToList(Dispatchers.Default)

    fun createSketchbook(title: String, nowEpochMillis: Long) {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "Sketchbook title must not be empty." }
        val count = queries.selectSketchbookCount().executeAsOne()
        queries.insertSketchbook(
            id = "sketchbook-$nowEpochMillis-$count",
            title = cleanTitle,
            description = null,
            cover_style = "paper",
            created_at = nowEpochMillis,
        )
    }

    fun addVerseCard(
        sketchbookId: String,
        verse: VerseSearchResult,
        nowEpochMillis: Long,
    ) {
        queries.insertMemoryCard(
            id = "$sketchbookId-${verse.bookId}-${verse.chapter}-${verse.verse}",
            sketchbook_id = sketchbookId,
            book_id = verse.bookId,
            chapter = verse.chapter,
            start_verse = verse.verse,
            end_verse = verse.verse,
            created_at = nowEpochMillis,
        )
    }

    fun reviewCard(card: MemoryCardSummary, rating: ReviewRating, nowEpochMillis: Long) {
        val result = Sm2Scheduler.review(
            progress = Sm2Progress(
                intervalDays = card.intervalDays,
                repetitionCount = card.repetitionCount,
                easinessFactor = card.easinessFactor,
            ),
            rating = rating,
            reviewedAtEpochMillis = nowEpochMillis,
        )

        database.transaction {
            queries.recordReview(
                card_id = card.id,
                rating = rating.name,
                reviewed_at = nowEpochMillis,
                interval_days = result.progress.intervalDays,
                easiness_factor = result.progress.easinessFactor,
            )
            queries.updateMemoryCardSchedule(
                interval_days = result.progress.intervalDays,
                repetition_count = result.progress.repetitionCount.toLong(),
                easiness_factor = result.progress.easinessFactor,
                next_review_at = result.nextReviewAtEpochMillis,
                id = card.id,
            )
        }
    }
}
