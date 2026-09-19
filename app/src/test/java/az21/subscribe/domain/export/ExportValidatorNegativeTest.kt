package az21.subscribe.domain.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Negative and referential-integrity cases for [ExportValidator]. */
class ExportValidatorNegativeTest {
  private val subscriptionId = "11111111-1111-1111-1111-111111111111"
  private val tagId = "33333333-3333-3333-3333-333333333333"
  private val paymentId = "44444444-4444-4444-4444-444444444444"

  @Test
  fun emptyDocument_producesNoIssues() {
    val result = ExportValidator.validate(baseDocument())

    assertTrue(result.issues.isEmpty())
    assertTrue(result.document.subscriptions.isEmpty())
  }

  @Test
  fun dropsSubscriptionWithInvalidBillingCycle() {
    val result = validate(subscriptions = listOf(validSubscription().copy(billingCycle = "WEEKLY")))

    assertTrue(result.document.subscriptions.isEmpty())
    assertSkipped(result)
  }

  @Test
  fun dropsSubscriptionWithInvalidStatus() {
    val result = validate(subscriptions = listOf(validSubscription().copy(status = "PAUSED")))

    assertTrue(result.document.subscriptions.isEmpty())
    assertSkipped(result)
  }

  @Test
  fun dropsSubscriptionWithBlankName() {
    val result = validate(subscriptions = listOf(validSubscription().copy(name = "   ")))

    assertTrue(result.document.subscriptions.isEmpty())
    assertSkipped(result)
  }

  @Test
  fun dropsSubscriptionWithInvalidStartDate() {
    val result = validate(subscriptions = listOf(validSubscription().copy(startDate = "2025-13-40")))

    assertTrue(result.document.subscriptions.isEmpty())
    assertSkipped(result)
  }

  @Test
  fun dropsSubscriptionWithInvalidTimestamp() {
    val result = validate(subscriptions = listOf(validSubscription().copy(createdAt = "yesterday")))

    assertTrue(result.document.subscriptions.isEmpty())
    assertSkipped(result)
  }

  @Test
  fun dropsSubscriptionWithNegativeReminderDays() {
    val result = validate(subscriptions = listOf(validSubscription().copy(reminderDaysBefore = -1)))

    assertTrue(result.document.subscriptions.isEmpty())
    assertSkipped(result)
  }

  @Test
  fun dropsSubscriptionWithMalformedPaymentMethodId() {
    val result = validate(subscriptions = listOf(validSubscription().copy(paymentMethodId = "not-a-uuid")))

    assertTrue(result.document.subscriptions.isEmpty())
    assertSkipped(result)
  }

  @Test
  fun keepsSubscriptionWithNullOptionalFields() {
    val result =
      validate(
        subscriptions =
          listOf(
            validSubscription().copy(
              endDate = null,
              reminderDaysBefore = null,
              paymentMethodId = null,
              notes = null,
            ),
          ),
      )

    assertEquals(1, result.document.subscriptions.size)
    assertNull(
      result.document.subscriptions
        .single()
        .paymentMethodId,
    )
    assertTrue(result.issues.isEmpty())
  }

  @Test
  fun dropsPriceHistoryWithNonNumericPrice() {
    val result =
      validate(
        subscriptions = listOf(validSubscription()),
        priceHistory = listOf(validPrice().copy(price = "free")),
      )

    assertTrue(result.document.priceHistory.isEmpty())
    assertSkipped(result)
  }

  @Test
  fun dropsPriceHistoryWithNegativePrice() {
    val result =
      validate(
        subscriptions = listOf(validSubscription()),
        priceHistory = listOf(validPrice().copy(price = "-0.01")),
      )

    assertTrue(result.document.priceHistory.isEmpty())
    assertSkipped(result)
  }

  @Test
  fun dropsPriceHistoryWithInvalidEffectiveDate() {
    val result =
      validate(
        subscriptions = listOf(validSubscription()),
        priceHistory = listOf(validPrice().copy(effectiveFromDate = "not-a-date")),
      )

    assertTrue(result.document.priceHistory.isEmpty())
    assertSkipped(result)
  }

  @Test
  fun dropsTagWithInvalidIdOrBlankName() {
    val result =
      validate(
        tags =
          listOf(
            TagExport(id = "nope", name = "Entertainment"),
            TagExport(id = tagId, name = "  "),
          ),
      )

    assertTrue(result.document.tags.isEmpty())
  }

  @Test
  fun dropsPaymentMethodWithInvalidIdOrBlankLabel() {
    val result =
      validate(
        paymentMethods =
          listOf(
            PaymentMethodExport(id = "nope", label = "Visa"),
            PaymentMethodExport(id = paymentId, label = ""),
          ),
      )

    assertTrue(result.document.paymentMethods.isEmpty())
  }

  @Test
  fun deduplicatesRepeatedTagLinks() {
    val result =
      validate(
        subscriptions = listOf(validSubscription()),
        tags = listOf(TagExport(id = tagId, name = "Entertainment")),
        subscriptionTags =
          listOf(
            SubscriptionTagExport(subscriptionId, tagId),
            SubscriptionTagExport(subscriptionId, tagId),
          ),
      )

    assertEquals(1, result.document.subscriptionTags.size)
  }

  @Test
  fun keepsValidTagLink() {
    val result =
      validate(
        subscriptions = listOf(validSubscription()),
        tags = listOf(TagExport(id = tagId, name = "Entertainment")),
        subscriptionTags = listOf(SubscriptionTagExport(subscriptionId, tagId)),
      )

    assertEquals(1, result.document.subscriptionTags.size)
    assertTrue(result.issues.isEmpty())
  }

  private fun validate(
    subscriptions: List<SubscriptionExport> = emptyList(),
    priceHistory: List<PriceHistoryExport> = emptyList(),
    tags: List<TagExport> = emptyList(),
    subscriptionTags: List<SubscriptionTagExport> = emptyList(),
    paymentMethods: List<PaymentMethodExport> = emptyList(),
  ): ValidatedExport =
    ExportValidator.validate(
      baseDocument().copy(
        subscriptions = subscriptions,
        priceHistory = priceHistory,
        tags = tags,
        subscriptionTags = subscriptionTags,
        paymentMethods = paymentMethods,
      ),
    )

  private fun assertSkipped(result: ValidatedExport) {
    assertTrue(result.issues.any { it.reason == ImportIssueReason.ROW_SKIPPED })
  }

  private fun baseDocument(): ExportDocument = ExportDocument(schemaVersion = EXPORT_SCHEMA_VERSION, exportedAt = "now")

  private fun validSubscription(): SubscriptionExport =
    SubscriptionExport(
      id = subscriptionId,
      name = "Test",
      iconId = "test",
      startDate = "2025-01-01",
      billingCycle = "MONTHLY",
      status = "ACTIVE",
      createdAt = "2025-01-01T00:00:00Z",
      updatedAt = "2025-01-01T00:00:00Z",
    )

  private fun validPrice(): PriceHistoryExport =
    PriceHistoryExport(
      id = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      subscriptionId = subscriptionId,
      price = "9.99",
      effectiveFromDate = "2025-01-01",
      createdAt = "2025-01-01T00:00:00Z",
    )
}
