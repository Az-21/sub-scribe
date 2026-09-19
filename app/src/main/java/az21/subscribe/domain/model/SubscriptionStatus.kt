package az21.subscribe.domain.model

/**
 * Lifecycle of a subscription.
 *
 * Transitions are one-way: ACTIVE -> CANCELLED -> ARCHIVED. A subscription must be archived before
 * it can be deleted, so historic metrics are never lost by accident.
 */
enum class SubscriptionStatus {
  ACTIVE,
  CANCELLED,
  ARCHIVED,
}
