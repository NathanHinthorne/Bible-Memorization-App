package com.bible.sketchbook.data

import biblesketchbook.shared.generated.resources.Res
import com.bible.sketchbook.db.BibleDatabase
import org.jetbrains.compose.resources.ExperimentalResourceApi

data class BibleBookInfo(val name: String, val testament: String)

private val canonicalBooks = listOf(
    "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy", "Joshua", "Judges", "Ruth",
    "1 Samuel", "2 Samuel", "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra",
    "Nehemiah", "Esther", "Job", "Psalms", "Proverbs", "Ecclesiastes", "Song of Solomon",
    "Isaiah", "Jeremiah", "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel", "Amos",
    "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk", "Zephaniah", "Haggai", "Zechariah",
    "Malachi", "Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1 Corinthians",
    "2 Corinthians", "Galatians", "Ephesians", "Philippians", "Colossians", "1 Thessalonians",
    "2 Thessalonians", "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews", "James",
    "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude", "Revelation",
)

class OfflineBibleSeeder(private val database: BibleDatabase) {
    @OptIn(ExperimentalResourceApi::class)
    suspend fun seedIfNeeded(nowEpochMillis: Long) {
        val queries = database.bibleSketchbookQueries
        if (queries.selectBibleVerseCount().executeAsOne() == 0L) {
            val corpus = Res.readBytes("files/web.tsv").decodeToString()
            val rows = corpus.lineSequence().filter(String::isNotBlank)

            database.transaction {
                canonicalBooks.forEachIndexed { index, name ->
                    queries.insertBibleBook(
                        id = index + 1L,
                        name = name,
                        testament = if (index < 39) "OLD" else "NEW",
                        book_order = index + 1L,
                    )
                }

                rows.forEachIndexed { index, line ->
                    val columns = line.split('\t', limit = 4)
                    require(columns.size == 4) {
                        "Invalid bundled WEB verse row ${index + 1}."
                    }
                    val bookId = columns[0].toLongOrNull()
                        ?: error("Invalid book id in bundled WEB verse row ${index + 1}.")
                    val chapter = columns[1].toLongOrNull()
                        ?: error("Invalid chapter in bundled WEB verse row ${index + 1}.")
                    val verse = columns[2].toLongOrNull()
                        ?: error("Invalid verse number in bundled WEB verse row ${index + 1}.")
                    val text = columns[3].trim()
                    require(bookId in 1L..canonicalBooks.size.toLong() && text.isNotEmpty()) {
                        "Invalid bundled WEB verse row ${index + 1}."
                    }
                    queries.insertBibleVerse(bookId, chapter, verse, text)
                }
            }
        }

        if (queries.selectSketchbookCount().executeAsOne() == 0L) {
            database.transaction {
                queries.insertSketchbook(
                    id = STARTER_SKETCHBOOK_ID,
                    title = "Peace & Rest",
                    description = "A few words to carry with you.",
                    cover_style = "sage",
                    created_at = nowEpochMillis,
                )
                STARTER_VERSES.forEach { verse ->
                    queries.insertMemoryCard(
                        id = "starter-${verse.bookId}-${verse.chapter}-${verse.verse}",
                        sketchbook_id = STARTER_SKETCHBOOK_ID,
                        book_id = verse.bookId,
                        chapter = verse.chapter,
                        start_verse = verse.verse,
                        end_verse = verse.verse,
                        created_at = nowEpochMillis,
                    )
                }
            }
        }
    }

    private data class StarterVerse(val bookId: Long, val chapter: Long, val verse: Long)

    private companion object {
        const val STARTER_SKETCHBOOK_ID = "starter-peace-rest"
        val STARTER_VERSES = listOf(
            StarterVerse(bookId = 19, chapter = 23, verse = 1),
            StarterVerse(bookId = 43, chapter = 3, verse = 16),
            StarterVerse(bookId = 50, chapter = 4, verse = 6),
        )
    }
}
