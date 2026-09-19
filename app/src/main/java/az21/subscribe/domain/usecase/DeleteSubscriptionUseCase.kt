package az21.subscribe.domain.usecase

import az21.subscribe.domain.repository.SubscriptionRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Permanently deletes an archived subscription. This is irreversible: the subscription and its
 * price history stop contributing to metrics, so callers should recommend archiving instead.
 *
 * @throws az21.subscribe.domain.model.SubscriptionTransitionException unless the subscription is
 *   already archived.
 */
class DeleteSubscriptionUseCase
  @Inject
  constructor(
    private val repository: SubscriptionRepository,
  ) {
    suspend operator fun invoke(id: UUID) {
      repository.deleteSubscription(id)
    }
  }
