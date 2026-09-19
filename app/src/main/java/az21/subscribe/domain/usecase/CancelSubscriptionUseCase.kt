package az21.subscribe.domain.usecase

import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.repository.SubscriptionRepository
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Cancels a subscription, stamping its end date (today when [endDate] is not supplied).
 *
 * @throws az21.subscribe.domain.model.SubscriptionTransitionException if the subscription is
 *   already archived or missing.
 */
class CancelSubscriptionUseCase
  @Inject
  constructor(
    private val repository: SubscriptionRepository,
  ) {
    suspend operator fun invoke(
      id: UUID,
      endDate: LocalDate? = null,
    ): Subscription = repository.cancelSubscription(id, endDate)
  }
