package com.bible.sketchbook.db

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Double
import kotlin.Long
import kotlin.String

public class BibleSketchbookQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectDueCards(now: Long, mapper: (
    id: String,
    sketchbook_id: String,
    book_id: Long,
    chapter: Long,
    start_verse: Long,
    end_verse: Long,
    interval_days: Long,
    repetition_count: Long,
    easiness_factor: Double,
    next_review_at: Long,
    created_at: Long,
  ) -> T): Query<T> = SelectDueCardsQuery(now) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getDouble(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!
    )
  }

  public fun selectDueCards(now: Long): Query<Memory_card> = selectDueCards(now) { id,
      sketchbook_id, book_id, chapter, start_verse, end_verse, interval_days, repetition_count,
      easiness_factor, next_review_at, created_at ->
    Memory_card(
      id,
      sketchbook_id,
      book_id,
      chapter,
      start_verse,
      end_verse,
      interval_days,
      repetition_count,
      easiness_factor,
      next_review_at,
      created_at
    )
  }

  public fun <T : Any> selectSketchbooks(now: Long, mapper: (
    id: String,
    title: String,
    description: String?,
    cover_style: String,
    created_at: Long,
    due_card_count: Long,
  ) -> T): Query<T> = SelectSketchbooksQuery(now) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!
    )
  }

  public fun selectSketchbooks(now: Long): Query<SelectSketchbooks> = selectSketchbooks(now) { id,
      title, description, cover_style, created_at, due_card_count ->
    SelectSketchbooks(
      id,
      title,
      description,
      cover_style,
      created_at,
      due_card_count
    )
  }

  public fun <T : Any> selectCardsForSketchbook(sketchbook_id: String, mapper: (
    id: String,
    sketchbook_id: String,
    book_id: Long,
    chapter: Long,
    start_verse: Long,
    end_verse: Long,
    interval_days: Long,
    repetition_count: Long,
    easiness_factor: Double,
    next_review_at: Long,
    created_at: Long,
    book_name: String,
  ) -> T): Query<T> = SelectCardsForSketchbookQuery(sketchbook_id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getDouble(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getString(11)!!
    )
  }

  public fun selectCardsForSketchbook(sketchbook_id: String): Query<SelectCardsForSketchbook> =
      selectCardsForSketchbook(sketchbook_id) { id, sketchbook_id_, book_id, chapter, start_verse,
      end_verse, interval_days, repetition_count, easiness_factor, next_review_at, created_at,
      book_name ->
    SelectCardsForSketchbook(
      id,
      sketchbook_id_,
      book_id,
      chapter,
      start_verse,
      end_verse,
      interval_days,
      repetition_count,
      easiness_factor,
      next_review_at,
      created_at,
      book_name
    )
  }

  public fun selectDueCardCountForSketchbook(sketchbook_id: String, now: Long): Query<Long> =
      SelectDueCardCountForSketchbookQuery(sketchbook_id, now) { cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> searchBibleVerses(
    chapter_filter: Long?,
    book_search: String,
    verse_filter: Long?,
    text_search: String,
    mapper: (
      book_id: Long,
      book_name: String,
      chapter: Long,
      verse: Long,
      text: String,
    ) -> T,
  ): Query<T> = SearchBibleVersesQuery(chapter_filter, book_search, verse_filter, text_search) {
      cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getLong(3)!!,
      cursor.getString(4)!!
    )
  }

  public fun searchBibleVerses(
    chapter_filter: Long?,
    book_search: String,
    verse_filter: Long?,
    text_search: String,
  ): Query<SearchBibleVerses> = searchBibleVerses(chapter_filter, book_search, verse_filter,
      text_search) { book_id, book_name, chapter, verse, text ->
    SearchBibleVerses(
      book_id,
      book_name,
      chapter,
      verse,
      text
    )
  }

  public fun <T : Any> selectVersesForCard(card_id: String, mapper: (
    book_id: Long,
    chapter: Long,
    verse: Long,
    text: String,
  ) -> T): Query<T> = SelectVersesForCardQuery(card_id) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!,
      cursor.getLong(2)!!,
      cursor.getString(3)!!
    )
  }

  public fun selectVersesForCard(card_id: String): Query<Bible_verse> =
      selectVersesForCard(card_id) { book_id, chapter, verse, text ->
    Bible_verse(
      book_id,
      chapter,
      verse,
      text
    )
  }

  public fun selectBibleVerseCount(): Query<Long> = Query(738_478_693, arrayOf("bible_verse"),
      driver, "BibleSketchbook.sq", "selectBibleVerseCount", """
  |SELECT COUNT(*)
  |FROM bible_verse
  """.trimMargin()) { cursor ->
    cursor.getLong(0)!!
  }

  public fun selectSketchbookCount(): Query<Long> = Query(905_710_641, arrayOf("sketchbook"),
      driver, "BibleSketchbook.sq", "selectSketchbookCount", """
  |SELECT COUNT(*)
  |FROM sketchbook
  """.trimMargin()) { cursor ->
    cursor.getLong(0)!!
  }

  public fun insertBibleBook(
    id: Long?,
    name: String,
    testament: String,
    book_order: Long,
  ) {
    driver.execute(-796_754_025, """
        |INSERT INTO bible_book (id, name, testament, book_order)
        |VALUES (?, ?, ?, ?)
        """.trimMargin(), 4) {
          bindLong(0, id)
          bindString(1, name)
          bindString(2, testament)
          bindLong(3, book_order)
        }
    notifyQueries(-796_754_025) { emit ->
      emit("bible_book")
    }
  }

  public fun insertBibleVerse(
    book_id: Long,
    chapter: Long,
    verse: Long,
    text: String,
  ) {
    driver.execute(1_088_604_743, """
        |INSERT INTO bible_verse (book_id, chapter, verse, text)
        |VALUES (?, ?, ?, ?)
        """.trimMargin(), 4) {
          bindLong(0, book_id)
          bindLong(1, chapter)
          bindLong(2, verse)
          bindString(3, text)
        }
    notifyQueries(1_088_604_743) { emit ->
      emit("bible_verse")
    }
  }

  public fun insertSketchbook(
    id: String,
    title: String,
    description: String?,
    cover_style: String,
    created_at: Long,
  ) {
    driver.execute(2_125_732_091, """
        |INSERT INTO sketchbook (id, title, description, cover_style, created_at)
        |VALUES (?, ?, ?, ?, ?)
        """.trimMargin(), 5) {
          bindString(0, id)
          bindString(1, title)
          bindString(2, description)
          bindString(3, cover_style)
          bindLong(4, created_at)
        }
    notifyQueries(2_125_732_091) { emit ->
      emit("sketchbook")
    }
  }

  public fun insertMemoryCard(
    id: String,
    sketchbook_id: String,
    book_id: Long,
    chapter: Long,
    start_verse: Long,
    end_verse: Long,
    created_at: Long,
  ) {
    driver.execute(-1_671_099_369, """
        |INSERT OR IGNORE INTO memory_card (
        |    id,
        |    sketchbook_id,
        |    book_id,
        |    chapter,
        |    start_verse,
        |    end_verse,
        |    created_at
        |)
        |VALUES (
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?
        |)
        """.trimMargin(), 7) {
          bindString(0, id)
          bindString(1, sketchbook_id)
          bindLong(2, book_id)
          bindLong(3, chapter)
          bindLong(4, start_verse)
          bindLong(5, end_verse)
          bindLong(6, created_at)
        }
    notifyQueries(-1_671_099_369) { emit ->
      emit("memory_card")
    }
  }

  public fun recordReview(
    card_id: String,
    rating: String,
    reviewed_at: Long,
    interval_days: Long,
    easiness_factor: Double,
  ) {
    driver.execute(1_059_060_310, """
        |INSERT INTO review_history (
        |    card_id,
        |    rating,
        |    reviewed_at,
        |    interval_days,
        |    easiness_factor
        |)
        |VALUES (
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?
        |)
        """.trimMargin(), 5) {
          bindString(0, card_id)
          bindString(1, rating)
          bindLong(2, reviewed_at)
          bindLong(3, interval_days)
          bindDouble(4, easiness_factor)
        }
    notifyQueries(1_059_060_310) { emit ->
      emit("review_history")
    }
  }

  public fun updateMemoryCardSchedule(
    interval_days: Long,
    repetition_count: Long,
    easiness_factor: Double,
    next_review_at: Long,
    id: String,
  ) {
    driver.execute(-612_102_690, """
        |UPDATE memory_card
        |SET interval_days = ?,
        |    repetition_count = ?,
        |    easiness_factor = ?,
        |    next_review_at = ?
        |WHERE id = ?
        """.trimMargin(), 5) {
          bindLong(0, interval_days)
          bindLong(1, repetition_count)
          bindDouble(2, easiness_factor)
          bindLong(3, next_review_at)
          bindString(4, id)
        }
    notifyQueries(-612_102_690) { emit ->
      emit("memory_card")
    }
  }

  private inner class SelectDueCardsQuery<out T : Any>(
    public val now: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("memory_card", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("memory_card", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_037_487_368, """
    |SELECT memory_card.id, memory_card.sketchbook_id, memory_card.book_id, memory_card.chapter, memory_card.start_verse, memory_card.end_verse, memory_card.interval_days, memory_card.repetition_count, memory_card.easiness_factor, memory_card.next_review_at, memory_card.created_at
    |FROM memory_card
    |WHERE next_review_at <= ?
    |ORDER BY next_review_at
    """.trimMargin(), mapper, 1) {
      bindLong(0, now)
    }

    override fun toString(): String = "BibleSketchbook.sq:selectDueCards"
  }

  private inner class SelectSketchbooksQuery<out T : Any>(
    public val now: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("sketchbook", "memory_card", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("sketchbook", "memory_card", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(564_370_613, """
    |SELECT
    |    sketchbook.id,
    |    sketchbook.title,
    |    sketchbook.description,
    |    sketchbook.cover_style,
    |    sketchbook.created_at,
    |    (
    |        SELECT COUNT(*)
    |        FROM memory_card
    |        WHERE memory_card.sketchbook_id = sketchbook.id
    |          AND memory_card.next_review_at <= ?
    |    ) AS due_card_count
    |FROM sketchbook
    |ORDER BY sketchbook.created_at DESC
    """.trimMargin(), mapper, 1) {
      bindLong(0, now)
    }

    override fun toString(): String = "BibleSketchbook.sq:selectSketchbooks"
  }

  private inner class SelectCardsForSketchbookQuery<out T : Any>(
    public val sketchbook_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("memory_card", "bible_book", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("memory_card", "bible_book", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(896_875_908, """
    |SELECT
    |    memory_card.id,
    |    memory_card.sketchbook_id,
    |    memory_card.book_id,
    |    memory_card.chapter,
    |    memory_card.start_verse,
    |    memory_card.end_verse,
    |    memory_card.interval_days,
    |    memory_card.repetition_count,
    |    memory_card.easiness_factor,
    |    memory_card.next_review_at,
    |    memory_card.created_at,
    |    bible_book.name AS book_name
    |FROM memory_card
    |JOIN bible_book ON bible_book.id = memory_card.book_id
    |WHERE memory_card.sketchbook_id = ?
    |ORDER BY memory_card.next_review_at, bible_book.book_order, memory_card.chapter
    """.trimMargin(), mapper, 1) {
      bindString(0, sketchbook_id)
    }

    override fun toString(): String = "BibleSketchbook.sq:selectCardsForSketchbook"
  }

  private inner class SelectDueCardCountForSketchbookQuery<out T : Any>(
    public val sketchbook_id: String,
    public val now: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("memory_card", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("memory_card", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_984_931_242, """
    |SELECT COUNT(*)
    |FROM memory_card
    |WHERE sketchbook_id = ?
    |  AND next_review_at <= ?
    """.trimMargin(), mapper, 2) {
      bindString(0, sketchbook_id)
      bindLong(1, now)
    }

    override fun toString(): String = "BibleSketchbook.sq:selectDueCardCountForSketchbook"
  }

  private inner class SearchBibleVersesQuery<out T : Any>(
    public val chapter_filter: Long?,
    public val book_search: String,
    public val verse_filter: Long?,
    public val text_search: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("bible_verse", "bible_book", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("bible_verse", "bible_book", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null, """
    |SELECT
    |    bible_verse.book_id,
    |    bible_book.name AS book_name,
    |    bible_verse.chapter,
    |    bible_verse.verse,
    |    bible_verse.text
    |FROM bible_verse
    |JOIN bible_book ON bible_book.id = bible_verse.book_id
    |WHERE (
    |      ? IS NOT NULL
    |      AND (
    |          bible_book.name = ? COLLATE NOCASE
    |          OR bible_book.name LIKE ? || '%' COLLATE NOCASE
    |      )
    |      AND bible_verse.chapter ${ if (chapter_filter == null) "IS" else "=" } ?
    |      AND (? IS NULL OR bible_verse.verse ${ if (verse_filter == null) "IS" else "=" } ?)
    |  )
    |  OR (
    |      ? IS NULL
    |      AND (
    |          ? = ''
    |          OR bible_book.name LIKE '%' || ? || '%' COLLATE NOCASE
    |          OR bible_verse.text LIKE '%' || ? || '%' COLLATE NOCASE
    |      )
    |  )
    |ORDER BY bible_book.book_order, bible_verse.chapter, bible_verse.verse
    |LIMIT 60
    """.trimMargin(), mapper, 10) {
      bindLong(0, chapter_filter)
      bindString(1, book_search)
      bindString(2, book_search)
      bindLong(3, chapter_filter)
      bindLong(4, verse_filter)
      bindLong(5, verse_filter)
      bindLong(6, chapter_filter)
      bindString(7, text_search)
      bindString(8, text_search)
      bindString(9, text_search)
    }

    override fun toString(): String = "BibleSketchbook.sq:searchBibleVerses"
  }

  private inner class SelectVersesForCardQuery<out T : Any>(
    public val card_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("bible_verse", "memory_card", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("bible_verse", "memory_card", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_442_925_618, """
    |SELECT bible_verse.book_id, bible_verse.chapter, bible_verse.verse, bible_verse.text
    |FROM bible_verse
    |JOIN memory_card
    |  ON bible_verse.book_id = memory_card.book_id
    | AND bible_verse.chapter = memory_card.chapter
    |WHERE memory_card.id = ?
    |  AND bible_verse.verse BETWEEN memory_card.start_verse AND memory_card.end_verse
    |ORDER BY bible_verse.verse
    """.trimMargin(), mapper, 1) {
      bindString(0, card_id)
    }

    override fun toString(): String = "BibleSketchbook.sq:selectVersesForCard"
  }
}
