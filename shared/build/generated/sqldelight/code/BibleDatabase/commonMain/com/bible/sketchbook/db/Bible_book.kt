package com.bible.sketchbook.db

import kotlin.Long
import kotlin.String

public data class Bible_book(
  public val id: Long,
  public val name: String,
  public val testament: String,
  public val book_order: Long,
)
