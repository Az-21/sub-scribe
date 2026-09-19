package az21.subscribe.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az21.subscribe.domain.billing.BillingSchedule
import az21.subscribe.domain.metrics.SpendCalculator
import az21.subscribe.domain.model.Currency
import az21.subscribe.domain.model.PriceHistory
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.Tag
import az21.subscribe.domain.repository.PaymentMethodRepository
import az21.subscribe.domain.repository.PriceHistoryRepository
import az21.subscribe.domain.repository.SettingsRepository
import az21.subscribe.domain.repository.SubscriptionRepository
import az21.subscribe.domain.repository.TagRepository
import az21.subscribe.domain.usecase.AddPriceChangeUseCase
import az21.subscribe.domain.usecase.ArchiveSubscriptionUseCase
import az21.subscribe.domain.usecase.CancelSubscriptionUseCase
import az21.subscribe.domain.usecase.DeleteSubscriptionUseCase
import az21.subscribe.domain.usecase.GetNextBillingDateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Drives the subscription detail screen: current values, the price timeline with deltas, and the
 * status-gated lifecycle actions.
 */
@HiltViewModel
class SubscriptionDetailViewModel
  @Inject
  constructor(
    private val subscriptionRepository: SubscriptionRepository,
    priceHistoryRepository: PriceHistoryRepository,
    tagRepository: TagRepository,
    paymentMethodRepository: PaymentMethodRepository,
    settingsRepository: SettingsRepository,
    private val getNextBillingDate: GetNextBillingDateUseCase,
    private val addPriceChange: AddPriceChangeUseCase,
    private val cancelSubscription: CancelSubscriptionUseCase,
    private val archiveSubscription: ArchiveSubscriptionUseCase,
    private val deleteSubscription: DeleteSubscriptionUseCase,
    private val clock: Clock,
  ) : ViewModel() {
    private val subscriptionId = MutableStateFlow<UUID?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SubscriptionDetailUiState> =
      subscriptionId
        .flatMapLatest { id ->
          if (id == null) {
            flowOf(SubscriptionDetailUiState())
          } else {
            combine(
              subscriptionRepository.observeSubscription(id),
              priceHistoryRepository.observeTimeline(id),
              tagRepository.observeTagsForSubscription(id),
              paymentMethodRepository.observePaymentMethods(),
              settingsRepository.settings,
            ) { subscription, timeline, tags, paymentMethods, settings ->
              buildState(
                subscription = subscription,
                timeline = timeline,
                tags = tags,
                paymentMethodLabel =
                  paymentMethods.firstOrNull { method -> method.id == subscription?.paymentMethodId }?.label,
                currency = settings.currency,
              )
            }
          }
        }.stateIn(
          viewModelScope,
          SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
          SubscriptionDetailUiState(),
        )

    /** Binds the screen to a subscription. Safe to call repeatedly: only the first id sticks. */
    fun initialize(subscriptionId: String) {
      if (this.subscriptionId.value != null) return
      this.subscriptionId.value = UUID.fromString(subscriptionId)
    }

    fun cancel(endDate: LocalDate? = null) = withSubscription { id -> cancelSubscription(id, endDate) }

    fun archive() = withSubscription { id -> archiveSubscription(id) }

    fun delete() = withSubscription { id -> deleteSubscription(id) }

    fun addPrice(
      price: BigDecimal,
      effectiveFromDate: LocalDate,
    ) = withSubscription { id -> addPriceChange(id, price, effectiveFromDate) }

    private fun withSubscription(action: suspend (UUID) -> Unit) {
      val id = subscriptionId.value ?: return
      viewModelScope.launch { runCatching { action(id) } }
    }

    private fun buildState(
      subscription: Subscription?,
      timeline: List<PriceHistory>,
      tags: List<Tag>,
      paymentMethodLabel: String?,
      currency: Currency,
    ): SubscriptionDetailUiState {
      val today = LocalDate.now(clock)
      val items =
        timeline.mapIndexed { index, entry ->
          PriceHistoryItem(
            entry = entry,
            delta = timeline.getOrNull(index + 1)?.let { previous -> entry.price - previous.price },
          )
        }
      return SubscriptionDetailUiState(
        isLoading = subscription == null,
        subscription = subscription,
        currentPrice = subscription?.let { SpendCalculator.resolvePrice(timeline, today) },
        nextBillingDate = subscription?.let { getNextBillingDate(it, today) },
        trialEndDate =
          subscription
            ?.takeIf { (it.freeTrialMonths ?: 0) > 0 }
            ?.let { BillingSchedule.billingStartDate(it) },
        timeline = items,
        tags = tags,
        paymentMethodLabel = paymentMethodLabel,
        currency = currency,
      )
    }

    private companion object {
      const val STOP_TIMEOUT_MILLIS = 5_000L
    }
  }
