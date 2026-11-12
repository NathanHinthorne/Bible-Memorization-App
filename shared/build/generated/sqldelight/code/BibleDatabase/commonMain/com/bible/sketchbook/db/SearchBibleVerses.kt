package com.bible.sketchbook.db

import kotlin.Long
import kotlin.String

public data class SearchBibleVerses(
  public val book_id: Long,
  public val book_name: String,
  public val chapter: Long,
  public val verse: Long,
  public val text: String,
)
