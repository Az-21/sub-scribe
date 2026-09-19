package az21.subscribe.data.repository

import az21.subscribe.data.local.dao.SubscriptionDao
import az21.subscribe.data.local.entity.SubscriptionEntity
import az21.subscribe.data.local.entity.SubscriptionReminderEntity
import az21.subscribe.data.local.entity.SubscriptionWithReminders
import az21.subscribe.data.mapper.toDomain
import az21.subscribe.data.mapper.toEntity
import az21.subscribe.domain.model.ReminderSpec
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionDraft
import az21.subscribe.domain.model.SubscriptionStatus
import az21.subscribe.domain.model.SubscriptionTransitionException
import az21.subscribe.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl
  @Inject
  constructor(
    private val subscriptionDao: SubscriptionDao,
    private val clock: Clock,
  ) : SubscriptionRepository {
    override fun observeSubscriptions(): Flow<List<Subscription>> =
      subscriptionDao.observeAll().map { entities -> entities.map(SubscriptionWithReminders::toDomain) }

    override fun observeSubscriptionsByStatus(status: SubscriptionStatus): Flow<List<Subscription>> =
      subscriptionDao.observeByStatus(status).map { entities ->
        entities.map(SubscriptionWithReminders::toDomain)
      }

    override fun observeSubscription(id: UUID): Flow<Subscription?> =
      subscriptionDao.observeById(id).map { it?.toDomain() }

    override suspend fun getSubscription(id: UUID): Subscription? = subscriptionDao.getById(id)?.toDomain()

    override suspend fun createSubscription(draft: SubscriptionDraft): Subscription {
      val now = clock.instant()
      val id = UUID.randomUUID()
      val entity =
        SubscriptionEntity(
          id = id,
          name = draft.name,
          iconId = draft.iconId,
          startDate = draft.startDate ?: LocalDate.now(clock),
          billingCycle = draft.billingCycle,
          status = SubscriptionStatus.ACTIVE,
          endDate = draft.endDate,
          paymentMethodId = draft.paymentMethodId,
          notes = draft.notes,
          createdAt = now,
          updatedAt = now,
          iconColor = draft.iconColor,
        )
      subscriptionDao.upsertWithReminders(entity, draft.reminders.toEntities(id))
      return entity.toDomain().copy(reminders = draft.reminders.sortedWith(REMINDER_ORDER))
    }

    override suspend fun updateSubscription(subscription: Subscription) {
      val entity = subscription.copy(updatedAt = clock.instant()).toEntity()
      subscriptionDao.upsertWithReminders(entity, subscription.reminders.toEntities(subscription.id))
    }

    override suspend fun cancelSubscription(
      id: UUID,
      endDate: LocalDate?,
    ): Subscription {
      val entity = requireSubscription(id)
      if (entity.status == SubscriptionStatus.ARCHIVED) {
        throw SubscriptionTransitionException.CannotCancel(entity.status)
      }
      val updated =
        entity.copy(
          status = SubscriptionStatus.CANCELLED,
          endDate = endDate ?: LocalDate.now(clock),
          updatedAt = clock.instant(),
        )
      subscriptionDao.upsert(updated)
      return updated.toDomain()
    }

    override suspend fun archiveSubscription(id: UUID): Subscription {
      val entity = requireSubscription(id)
      if (entity.status != SubscriptionStatus.CANCELLED) {
        throw SubscriptionTransitionException.CannotArchive(entity.status)
      }
      val updated =
        entity.copy(status = SubscriptionStatus.ARCHIVED, updatedAt = clock.instant())
      subscriptionDao.upsert(updated)
      return updated.toDomain()
    }

    override suspend fun deleteSubscription(id: UUID) {
      val entity = requireSubscription(id)
      if (entity.status != SubscriptionStatus.ARCHIVED) {
        throw SubscriptionTransitionException.CannotDelete(entity.status)
      }
      subscriptionDao.deleteById(id)
    }

    override fun observeTagIds(subscriptionId: UUID): Flow<List<UUID>> = subscriptionDao.observeTagIds(subscriptionId)

    override suspend fun setTags(
      subscriptionId: UUID,
      tagIds: Set<UUID>,
    ) {
      subscriptionDao.setTags(subscriptionId, tagIds)
    }

    private suspend fun requireSubscription(id: UUID): SubscriptionEntity =
      subscriptionDao.getById(id)?.subscription ?: throw SubscriptionTransitionException.NotFound(id)

    private fun List<ReminderSpec>.toEntities(subscriptionId: UUID): List<SubscriptionReminderEntity> =
      map { spec -> spec.toEntity(subscriptionId = subscriptionId, id = UUID.randomUUID()) }

    private companion object {
      val REMINDER_ORDER: Comparator<ReminderSpec> = compareBy({ it.daysBefore }, { it.time })
    }
  }
