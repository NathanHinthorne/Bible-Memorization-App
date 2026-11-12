package com.bible.sketchbook.db

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class Review_history(
  public val id: Long,
  public val card_id: String,
  public val rating: String,
  public val reviewed_at: Long,
  public val interval_days: Long,
  public val easiness_factor: Double,
)
