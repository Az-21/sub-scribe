package az21.subscribe.domain.model

import java.util.UUID

/** A user-defined label that can be attached to any number of subscriptions. */
data class Tag(
  val id: UUID,
  val name: String,
  val color: Int?,
)
