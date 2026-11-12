package com.bible.sketchbook.db

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class Memory_card(
  public val id: String,
  public val sketchbook_id: String,
  public val book_id: Long,
  public val chapter: Long,
  public val start_verse: Long,
  public val end_verse: Long,
  public val interval_days: Long,
  public val repetition_count: Long,
  public val easiness_factor: Double,
  public val next_review_at: Long,
  public val created_at: Long,
)
