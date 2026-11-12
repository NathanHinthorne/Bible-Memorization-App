package com.bible.sketchbook.db

import kotlin.Long
import kotlin.String

public data class Sketchbook(
  public val id: String,
  public val title: String,
  public val description: String?,
  public val cover_style: String,
  public val created_at: Long,
)
