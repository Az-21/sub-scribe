package az21.subscribe.domain.repository

import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionDraft
import az21.subscribe.domain.model.SubscriptionStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

/**
 * Single source of truth for subscriptions.
 *
 * Lifecycle rules are enforced here: cancel sets status + end date, archive requires a cancelled
 * subscription, and delete requires an archived one.
 */
interface SubscriptionRepository {
  fun observeSubscriptions(): Flow<List<Subscription>>

  fun observeSubscriptionsByStatus(status: SubscriptionStatus): Flow<List<Subscription>>

  fun observeSubscription(id: UUID): Flow<Subscription?>

  suspend fun getSubscription(id: UUID): Subscription?

  /** Creates a subscription, generating its id and timestamps and defaulting a missing start date. */
  suspend fun createSubscription(draft: SubscriptionDraft): Subscription

  suspend fun updateSubscription(subscription: Subscription)

  /**
   * Marks the subscription cancelled and stamps [endDate], defaulting to today when null.
   *
   * @throws az21.subscribe.domain.model.SubscriptionTransitionException if the subscription is archived.
   */
  suspend fun cancelSubscription(
    id: UUID,
    endDate: LocalDate? = null,
  ): Subscription

  /** @throws az21.subscribe.domain.model.SubscriptionTransitionException unless status is CANCELLED. */
  suspend fun archiveSubscription(id: UUID): Subscription

  /** @throws az21.subscribe.domain.model.SubscriptionTransitionException unless status is ARCHIVED. */
  suspend fun deleteSubscription(id: UUID)

  fun observeTagIds(subscriptionId: UUID): Flow<List<UUID>>

  suspend fun setTags(
    subscriptionId: UUID,
    tagIds: Set<UUID>,
  )
}
