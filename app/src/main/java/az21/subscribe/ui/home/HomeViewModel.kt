package az21.subscribe.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az21.subscribe.domain.metrics.SpendCalculator
import az21.subscribe.domain.model.Currency
import az21.subscribe.domain.model.PaymentMethod
import az21.subscribe.domain.model.PriceHistory
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionStatus
import az21.subscribe.domain.model.Tag
import az21.subscribe.domain.repository.PaymentMethodRepository
import az21.subscribe.domain.repository.PriceHistoryRepository
import az21.subscribe.domain.repository.SettingsRepository
import az21.subscribe.domain.repository.SubscriptionRepository
import az21.subscribe.domain.repository.TagRepository
import az21.subscribe.domain.usecase.GetNextBillingDateUseCase
import az21.subscribe.ui.common.SubscriptionSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * Drives the subscription list: combines subscriptions with their prices, next billing dates and
 * tags, then applies the search, filter and sort controls.
 */
@HiltViewModel
class HomeViewModel
  @Inject
  constructor(
    subscriptionRepository: SubscriptionRepository,
    tagRepository: TagRepository,
    priceHistoryRepository: PriceHistoryRepository,
    paymentMethodRepository: PaymentMethodRepository,
    settingsRepository: SettingsRepository,
    private val getNextBillingDate: GetNextBillingDateUseCase,
    private val clock: Clock,
  ) : ViewModel() {
    private val filters = MutableStateFlow(HomeFilters())

    private val catalog =
      combine(
        subscriptionRepository.observeSubscriptions(),
        tagRepository.observeTagsBySubscription(),
        priceHistoryRepository.observeAllTimelines(),
        tagRepository.observeTags(),
      ) { subscriptions, tagsBySubscription, timelines, tags ->
        Catalog(
          subscriptions = subscriptions,
          tagsBySubscription = tagsBySubscription,
          timelines = timelines,
          allTags = tags,
        )
      }

    val uiState: StateFlow<HomeUiState> =
      combine(
        catalog,
        paymentMethodRepository.observePaymentMethods(),
        settingsRepository.settings,
        filters,
      ) { catalog, paymentMethods, settings, filter ->
        buildState(catalog, paymentMethods, settings.currency, filter)
      }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        HomeUiState(),
      )

    fun onQueryChange(query: String) {
      filters.value = filters.value.copy(query = query)
    }

    fun onStatusChange(status: SubscriptionStatus?) {
      filters.value = filters.value.copy(status = status)
    }

    fun onTagChange(tagId: UUID?) {
      filters.value = filters.value.copy(tagId = tagId)
    }

    fun onPaymentMethodChange(paymentMethodId: UUID?) {
      filters.value = filters.value.copy(paymentMethodId = paymentMethodId)
    }

    fun onSortChange(sort: SubscriptionSort) {
      filters.value = filters.value.copy(sort = sort)
    }

    private fun buildState(
      catalog: Catalog,
      paymentMethods: List<PaymentMethod>,
      currency: Currency,
      filter: HomeFilters,
    ): HomeUiState {
      val today = LocalDate.now(clock)
      val summaries =
        catalog.subscriptions.map { subscription ->
          SubscriptionSummary(
            subscription = subscription,
            price = SpendCalculator.resolvePrice(catalog.timelines[subscription.id].orEmpty(), today),
            nextBillingDate = getNextBillingDate(subscription, today),
            tags = catalog.tagsBySubscription[subscription.id].orEmpty(),
          )
        }

      val matching = summaries.filter { summary -> summary.matches(filter) }
      return HomeUiState(
        isLoading = false,
        items = matching.sortedWith(filter.sort.comparator()),
        allTags = catalog.allTags,
        paymentMethods = paymentMethods,
        currency = currency,
        query = filter.query,
        status = filter.status,
        tagId = filter.tagId,
        paymentMethodId = filter.paymentMethodId,
        sort = filter.sort,
        hasSubscriptions = summaries.isNotEmpty(),
      )
    }

    private fun SubscriptionSummary.matches(filter: HomeFilters): Boolean {
      val matchesStatus = filter.status == null || subscription.status == filter.status
      val query = filter.query.trim()
      val matchesQuery = query.isEmpty() || subscription.name.contains(query, ignoreCase = true)
      val matchesTag = filter.tagId == null || tags.any { tag -> tag.id == filter.tagId }
      val matchesPayment =
        filter.paymentMethodId == null || subscription.paymentMethodId == filter.paymentMethodId
      return matchesStatus && matchesQuery && matchesTag && matchesPayment
    }

    private fun SubscriptionSort.comparator(): Comparator<SubscriptionSummary> =
      when (this) {
        SubscriptionSort.NAME -> compareBy { summary -> summary.subscription.name.lowercase(Locale.ROOT) }
        SubscriptionSort.PRICE -> compareByDescending { summary -> summary.price ?: BigDecimal.ZERO }
        SubscriptionSort.NEXT_BILLING -> compareBy(nullsLast<LocalDate>()) { summary -> summary.nextBillingDate }
      }

    private data class Catalog(
      val subscriptions: List<Subscription>,
      val tagsBySubscription: Map<UUID, List<Tag>>,
      val timelines: Map<UUID, List<PriceHistory>>,
      val allTags: List<Tag>,
    )

    private data class HomeFilters(
      val query: String = "",
      val status: SubscriptionStatus? = SubscriptionStatus.ACTIVE,
      val tagId: UUID? = null,
      val paymentMethodId: UUID? = null,
      val sort: SubscriptionSort = SubscriptionSort.NAME,
    )

    private companion object {
      const val STOP_TIMEOUT_MILLIS = 5_000L
    }
  }
