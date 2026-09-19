package az21.subscribe.domain.usecase

import az21.subscribe.domain.model.PriceHistory
import az21.subscribe.domain.model.SubscriptionTransitionException
import az21.subscribe.domain.repository.PriceHistoryRepository
import az21.subscribe.domain.repository.SubscriptionRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Adds a price point to a subscription's timeline. [effectiveFromDate] may be backdated (to correct
 * history) or future-dated (to schedule an upcoming change).
 *
 * @throws IllegalArgumentException when [price] is negative.
 * @throws SubscriptionTransitionException.NotFound when the subscription does not exist.
 */
class AddPriceChangeUseCase
  @Inject
  constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val priceHistoryRepository: PriceHistoryRepository,
  ) {
    suspend operator fun invoke(
      subscriptionId: UUID,
      price: BigDecimal,
      effectiveFromDate: LocalDate,
    ): PriceHistory {
      require(price.signum() >= 0) { "Price must not be negative" }
      subscriptionRepository.getSubscription(subscriptionId)
        ?: throw SubscriptionTransitionException.NotFound(subscriptionId)
      return priceHistoryRepository.addPriceChange(subscriptionId, price, effectiveFromDate)
    }
  }
