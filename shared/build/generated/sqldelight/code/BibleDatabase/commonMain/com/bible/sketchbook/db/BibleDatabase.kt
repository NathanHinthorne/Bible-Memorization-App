package com.bible.sketchbook.db

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.bible.sketchbook.db.shared.newInstance
import com.bible.sketchbook.db.shared.schema
import kotlin.Unit

public interface BibleDatabase : Transacter {
  public val bibleSketchbookQueries: BibleSketchbookQueries

  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = BibleDatabase::class.schema

    public operator fun invoke(driver: SqlDriver): BibleDatabase =
        BibleDatabase::class.newInstance(driver)
  }
}
