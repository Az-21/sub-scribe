package az21.subscribe.domain.usecase

import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.repository.SubscriptionRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Archives a subscription so it leaves the active list but keeps contributing to historic metrics.
 *
 * @throws az21.subscribe.domain.model.SubscriptionTransitionException unless the subscription is
 *   already cancelled.
 */
class ArchiveSubscriptionUseCase
  @Inject
  constructor(
    private val repository: SubscriptionRepository,
  ) {
    suspend operator fun invoke(id: UUID): Subscription = repository.archiveSubscription(id)
  }
