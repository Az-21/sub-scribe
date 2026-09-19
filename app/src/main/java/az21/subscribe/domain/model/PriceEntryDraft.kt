package az21.subscribe.domain.model

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * A desired price point while editing a subscription's history. A null [id] marks an entry that has
 * not been persisted yet; a non-null [id] means an existing entry to update in place.
 */
data class PriceEntryDraft(
  val id: UUID?,
  val price: BigDecimal,
  val effectiveFromDate: LocalDate,
)
