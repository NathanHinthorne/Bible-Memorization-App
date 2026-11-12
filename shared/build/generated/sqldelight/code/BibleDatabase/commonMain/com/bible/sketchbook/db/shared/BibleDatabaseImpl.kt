package com.bible.sketchbook.db.shared

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.bible.sketchbook.db.BibleDatabase
import com.bible.sketchbook.db.BibleSketchbookQueries
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<BibleDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = BibleDatabaseImpl.Schema

internal fun KClass<BibleDatabase>.newInstance(driver: SqlDriver): BibleDatabase =
    BibleDatabaseImpl(driver)

private class BibleDatabaseImpl(
  driver: SqlDriver,
) : TransacterImpl(driver), BibleDatabase {
  override val bibleSketchbookQueries: BibleSketchbookQueries = BibleSketchbookQueries(driver)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE bible_book (
          |    id INTEGER NOT NULL PRIMARY KEY,
          |    name TEXT NOT NULL,
          |    testament TEXT NOT NULL CHECK (testament IN ('OLD', 'NEW')),
          |    book_order INTEGER NOT NULL UNIQUE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE bible_verse (
          |    book_id INTEGER NOT NULL,
          |    chapter INTEGER NOT NULL CHECK (chapter > 0),
          |    verse INTEGER NOT NULL CHECK (verse > 0),
          |    text TEXT NOT NULL,
          |    PRIMARY KEY (book_id, chapter, verse),
          |    FOREIGN KEY (book_id) REFERENCES bible_book(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE sketchbook (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    title TEXT NOT NULL,
          |    description TEXT,
          |    cover_style TEXT NOT NULL,
          |    created_at INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE memory_card (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    sketchbook_id TEXT NOT NULL,
          |    book_id INTEGER NOT NULL,
          |    chapter INTEGER NOT NULL CHECK (chapter > 0),
          |    start_verse INTEGER NOT NULL CHECK (start_verse > 0),
          |    end_verse INTEGER NOT NULL CHECK (end_verse >= start_verse),
          |    interval_days INTEGER NOT NULL DEFAULT 0 CHECK (interval_days >= 0),
          |    repetition_count INTEGER NOT NULL DEFAULT 0 CHECK (repetition_count >= 0),
          |    easiness_factor REAL NOT NULL DEFAULT 2.5 CHECK (easiness_factor >= 1.3),
          |    next_review_at INTEGER NOT NULL DEFAULT 0,
          |    created_at INTEGER NOT NULL,
          |    FOREIGN KEY (sketchbook_id) REFERENCES sketchbook(id) ON DELETE CASCADE,
          |    FOREIGN KEY (book_id, chapter, start_verse)
          |        REFERENCES bible_verse(book_id, chapter, verse),
          |    FOREIGN KEY (book_id, chapter, end_verse)
          |        REFERENCES bible_verse(book_id, chapter, verse)
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE review_history (
          |    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
          |    card_id TEXT NOT NULL,
          |    rating TEXT NOT NULL CHECK (rating IN ('AGAIN', 'HARD', 'GOOD', 'EASY')),
          |    reviewed_at INTEGER NOT NULL,
          |    interval_days INTEGER NOT NULL CHECK (interval_days >= 0),
          |    easiness_factor REAL NOT NULL CHECK (easiness_factor >= 1.3),
          |    FOREIGN KEY (card_id) REFERENCES memory_card(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
