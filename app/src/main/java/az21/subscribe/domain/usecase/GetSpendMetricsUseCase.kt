package az21.subscribe.domain.usecase

import az21.subscribe.domain.metrics.SpendCalculator
import az21.subscribe.domain.metrics.YearlySpend
import az21.subscribe.domain.repository.PriceHistoryRepository
import az21.subscribe.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Computes the true-monthly, divvied-annual, and true-total spend metrics for a calendar year,
 * including archived subscriptions and using the historically-effective price at each charge date.
 */
class GetSpendMetricsUseCase
  @Inject
  constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val priceHistoryRepository: PriceHistoryRepository,
  ) {
    suspend operator fun invoke(year: Int): YearlySpend {
      val subscriptions = subscriptionRepository.observeSubscriptions().first()
      val timelines = subscriptions.associate { it.id to priceHistoryRepository.getTimeline(it.id) }
      return SpendCalculator.yearly(subscriptions, timelines, year)
    }
  }
