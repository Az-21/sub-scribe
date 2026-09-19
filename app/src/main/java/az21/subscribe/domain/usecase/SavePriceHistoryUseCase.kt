package az21.subscribe.domain.usecase

import az21.subscribe.domain.model.PriceEntryDraft
import az21.subscribe.domain.model.PriceHistory
import az21.subscribe.domain.model.SubscriptionTransitionException
import az21.subscribe.domain.repository.PriceHistoryRepository
import az21.subscribe.domain.repository.SubscriptionRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Reconciles a subscription's price timeline with the desired [entries]: deleted entries are
 * removed, changed entries are updated in place (preserving their creation time), and new entries
 * are appended. This lets the add/edit form treat the whole timeline as one editable list.
 *
 * @throws IllegalArgumentException when [entries] is empty.
 * @throws SubscriptionTransitionException.NotFound when the subscription does not exist.
 */
class SavePriceHistoryUseCase
  @Inject
  constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val priceHistoryRepository: PriceHistoryRepository,
    private val addPriceChange: AddPriceChangeUseCase,
  ) {
    suspend operator fun invoke(
      subscriptionId: UUID,
      entries: List<PriceEntryDraft>,
    ): List<PriceHistory> {
      require(entries.isNotEmpty()) { "At least one price entry is required" }
      require(entries.all { entry -> entry.price.signum() >= 0 }) { "Price must not be negative" }
      subscriptionRepository.getSubscription(subscriptionId)
        ?: throw SubscriptionTransitionException.NotFound(subscriptionId)

      val existingById = priceHistoryRepository.getTimeline(subscriptionId).associateBy { it.id }
      val keptIds = entries.mapNotNull { it.id }.toSet()
      existingById.values
        .filterNot { it.id in keptIds }
        .forEach { entry -> priceHistoryRepository.deleteEntry(entry.id) }

      entries.forEach { draft ->
        val current = draft.id?.let(existingById::get)
        when {
          current == null -> {
            addPriceChange(subscriptionId, draft.price, draft.effectiveFromDate)
          }

          current.price.compareTo(draft.price) != 0 ||
            current.effectiveFromDate != draft.effectiveFromDate -> {
            priceHistoryRepository.updateEntry(current.id, draft.price, draft.effectiveFromDate)
          }
        }
      }
      return priceHistoryRepository.getTimeline(subscriptionId)
    }
  }
