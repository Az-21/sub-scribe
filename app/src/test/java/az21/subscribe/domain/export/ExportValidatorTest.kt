package az21.subscribe.domain.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportValidatorTest {
  private val subscriptionId = "11111111-1111-1111-1111-111111111111"

  @Test
  fun dropsUnreadableRecords() {
    val document =
      baseDocument().copy(
        subscriptions = listOf(validSubscription(subscriptionId), validSubscription("not-a-uuid")),
        priceHistory = listOf(priceEntry("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", subscriptionId)),
      )

    val result = ExportValidator.validate(document)

    assertEquals(1, result.document.subscriptions.size)
    assertTrue(result.issues.any { it.reason == ImportIssueReason.ROW_SKIPPED })
  }

  @Test
  fun clearsMissingPaymentMethodReference() {
    val document =
      baseDocument().copy(
        subscriptions =
          listOf(
            validSubscription(subscriptionId).copy(paymentMethodId = "99999999-9999-9999-9999-999999999999"),
          ),
      )

    val result = ExportValidator.validate(document)

    assertNull(
      result.document.subscriptions
        .single()
        .paymentMethodId,
    )
    assertTrue(result.issues.any { it.reason == ImportIssueReason.REFERENCE_CLEARED })
  }

  @Test
  fun dropsPriceHistoryForMissingSubscription() {
    val document =
      baseDocument().copy(
        subscriptions = listOf(validSubscription(subscriptionId)),
        priceHistory =
          listOf(
            priceEntry("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "22222222-2222-2222-2222-222222222222"),
          ),
      )

    val result = ExportValidator.validate(document)

    assertTrue(result.document.priceHistory.isEmpty())
  }

  @Test
  fun dropsTagLinksForMissingTag() {
    val document =
      baseDocument().copy(
        subscriptions = listOf(validSubscription(subscriptionId)),
        subscriptionTags = listOf(SubscriptionTagExport(subscriptionId, "33333333-3333-3333-3333-333333333333")),
      )

    val result = ExportValidator.validate(document)

    assertTrue(result.document.subscriptionTags.isEmpty())
  }

  @Test
  fun deduplicatesSubscriptionsById() {
    val document =
      baseDocument().copy(
        subscriptions =
          listOf(
            validSubscription(subscriptionId),
            validSubscription(subscriptionId).copy(name = "Second"),
          ),
      )

    val result = ExportValidator.validate(document)

    assertEquals(1, result.document.subscriptions.size)
    assertEquals(
      "Second",
      result.document.subscriptions
        .single()
        .name,
    )
  }

  private fun baseDocument(): ExportDocument = ExportDocument(schemaVersion = EXPORT_SCHEMA_VERSION, exportedAt = "now")

  private fun validSubscription(id: String): SubscriptionExport =
    SubscriptionExport(
      id = id,
      name = "Test",
      iconId = "test",
      startDate = "2025-01-01",
      billingCycle = "MONTHLY",
      status = "ACTIVE",
      createdAt = "2025-01-01T00:00:00Z",
      updatedAt = "2025-01-01T00:00:00Z",
    )

  private fun priceEntry(
    id: String,
    subscriptionId: String,
  ): PriceHistoryExport =
    PriceHistoryExport(
      id = id,
      subscriptionId = subscriptionId,
      price = "9.99",
      effectiveFromDate = "2025-01-01",
      createdAt = "2025-01-01T00:00:00Z",
    )
}
