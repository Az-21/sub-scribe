package az21.subscribe.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az21.subscribe.domain.metrics.SpendCalculator
import az21.subscribe.domain.model.SubscriptionStatus
import az21.subscribe.domain.repository.PriceHistoryRepository
import az21.subscribe.domain.repository.SettingsRepository
import az21.subscribe.domain.repository.SubscriptionRepository
import az21.subscribe.domain.repository.TagRepository
import az21.subscribe.domain.usecase.ArchiveSubscriptionUseCase
import az21.subscribe.domain.usecase.DeleteSubscriptionUseCase
import az21.subscribe.ui.common.SubscriptionSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Drives the archive screen: lists cancelled and archived subscriptions and offers the only delete
 * entry point, behind a destructive confirmation.
 */
@HiltViewModel
class ArchiveViewModel
  @Inject
  constructor(
    subscriptionRepository: SubscriptionRepository,
    tagRepository: TagRepository,
    priceHistoryRepository: PriceHistoryRepository,
    settingsRepository: SettingsRepository,
    private val archiveSubscription: ArchiveSubscriptionUseCase,
    private val deleteSubscription: DeleteSubscriptionUseCase,
    private val clock: Clock,
  ) : ViewModel() {
    private val pendingDelete = MutableStateFlow<SubscriptionSummary?>(null)

    val uiState: StateFlow<ArchiveUiState> =
      combine(
        subscriptionRepository.observeSubscriptions(),
        tagRepository.observeTagsBySubscription(),
        priceHistoryRepository.observeAllTimelines(),
        settingsRepository.settings,
        pendingDelete,
      ) { subscriptions, tagsBySubscription, timelines, settings, pending ->
        val today: LocalDate = LocalDate.now(clock)
        val items =
          subscriptions
            .filter { subscription -> subscription.status != SubscriptionStatus.ACTIVE }
            .sortedBy { subscription -> subscription.name.lowercase() }
            .map { subscription ->
              SubscriptionSummary(
                subscription = subscription,
                price = SpendCalculator.resolvePrice(timelines[subscription.id].orEmpty(), today),
                nextBillingDate = null,
                tags = tagsBySubscription[subscription.id].orEmpty(),
              )
            }
        ArchiveUiState(isLoading = false, items = items, currency = settings.currency, pendingDelete = pending)
      }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        ArchiveUiState(),
      )

    fun archive(id: UUID) {
      viewModelScope.launch { runCatching { archiveSubscription(id) } }
    }

    fun requestDelete(item: SubscriptionSummary) {
      pendingDelete.value = item
    }

    fun cancelDelete() {
      pendingDelete.value = null
    }

    fun confirmDelete() {
      val item = pendingDelete.value ?: return
      pendingDelete.value = null
      viewModelScope.launch { runCatching { deleteSubscription(item.subscription.id) } }
    }

    private companion object {
      const val STOP_TIMEOUT_MILLIS = 5_000L
    }
  }
