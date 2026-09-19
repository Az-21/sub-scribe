package az21.subscribe.domain.usecase

import az21.subscribe.domain.billing.BillingSchedule
import az21.subscribe.domain.model.Subscription
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Resolves the next billing date of a subscription, or null when it is cancelled or archived.
 *
 * Defaults [from] to today so callers do not need to supply the current date.
 */
class GetNextBillingDateUseCase
  @Inject
  constructor(
    private val clock: Clock,
  ) {
    operator fun invoke(
      subscription: Subscription,
      from: LocalDate = LocalDate.now(clock),
    ): LocalDate? = BillingSchedule.nextBillingDate(subscription, from)
  }
