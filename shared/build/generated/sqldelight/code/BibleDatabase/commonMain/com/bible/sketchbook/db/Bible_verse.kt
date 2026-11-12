package com.bible.sketchbook.db

import kotlin.Long
import kotlin.String

public data class Bible_verse(
  public val book_id: Long,
  public val chapter: Long,
  public val verse: Long,
  public val text: String,
)
