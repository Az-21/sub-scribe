package az21.subscribe.data.repository

import az21.subscribe.data.local.TransactionRunner
import az21.subscribe.data.local.dao.PaymentMethodDao
import az21.subscribe.data.local.dao.PriceHistoryDao
import az21.subscribe.data.local.dao.SubscriptionDao
import az21.subscribe.data.local.dao.TagDao
import az21.subscribe.data.local.entity.SubscriptionTagEntity
import az21.subscribe.data.local.entity.SubscriptionWithReminders
import az21.subscribe.data.mapper.toDomain
import az21.subscribe.data.mapper.toEntity
import az21.subscribe.data.mapper.toEntityWithReminders
import az21.subscribe.domain.export.EXPORT_SCHEMA_VERSION
import az21.subscribe.domain.export.ExportDocument
import az21.subscribe.domain.export.ImportSummary
import az21.subscribe.domain.export.SubscriptionTagExport
import az21.subscribe.domain.export.toDomain
import az21.subscribe.domain.export.toExport
import az21.subscribe.domain.repository.DataTransferRepository
import az21.subscribe.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataTransferRepositoryImpl
  @Inject
  constructor(
    private val subscriptionDao: SubscriptionDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val tagDao: TagDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val settingsRepository: SettingsRepository,
    private val transactionRunner: TransactionRunner,
    private val clock: Clock,
  ) : DataTransferRepository {
    override suspend fun exportSnapshot(): ExportDocument =
      ExportDocument(
        schemaVersion = EXPORT_SCHEMA_VERSION,
        exportedAt = clock.instant().toString(),
        subscriptions = subscriptionDao.getAll().map { entity -> entity.toDomain().toExport() },
        priceHistory = priceHistoryDao.getAll().map { entity -> entity.toDomain().toExport() },
        tags = tagDao.getAll().map { entity -> entity.toDomain().toExport() },
        subscriptionTags = subscriptionDao.getAllTagAssignments().map { entity -> entity.toExport() },
        paymentMethods = paymentMethodDao.getAll().map { entity -> entity.toDomain().toExport() },
        settings = settingsRepository.settings.first().toExport(),
      )

    override suspend fun merge(document: ExportDocument): ImportSummary =
      transactionRunner.runTransaction {
        paymentMethodDao.upsertAll(document.paymentMethods.map { entry -> entry.toEntity() })
        tagDao.upsertAll(document.tags.map { entry -> entry.toEntity() })
        subscriptionDao.upsertAllWithReminders(
          document.subscriptions.map { entry -> entry.toEntityWithReminders() },
        )

        val importedSubscriptionIds = document.subscriptions.map { entry -> UUID.fromString(entry.id) }
        if (importedSubscriptionIds.isNotEmpty()) {
          subscriptionDao.clearTagsFor(importedSubscriptionIds)
        }
        subscriptionDao.insertTags(document.subscriptionTags.map { entry -> entry.toEntity() })
        priceHistoryDao.upsertAll(document.priceHistory.map { entry -> entry.toEntity() })
        settingsRepository.updateSettings(document.settings.toDomain())

        ImportSummary(
          subscriptions = document.subscriptions.size,
          priceHistory = document.priceHistory.size,
          tags = document.tags.size,
          subscriptionTags = document.subscriptionTags.size,
          paymentMethods = document.paymentMethods.size,
          settingsApplied = true,
        )
      }
  }

private fun SubscriptionTagEntity.toExport(): SubscriptionTagExport =
  SubscriptionTagExport(subscriptionId = subscriptionId.toString(), tagId = tagId.toString())

private fun SubscriptionTagExport.toEntity(): SubscriptionTagEntity =
  SubscriptionTagEntity(subscriptionId = UUID.fromString(subscriptionId), tagId = UUID.fromString(tagId))
