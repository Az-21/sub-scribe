package az21.subscribe.domain.export

private const val STREAMING_ID = "11111111-1111-1111-1111-111111111111"
private const val MUSIC_ID = "22222222-2222-2222-2222-222222222222"
private const val TAG_ID = "33333333-3333-3333-3333-333333333333"
private const val PAYMENT_ID = "44444444-4444-4444-4444-444444444444"

fun sampleExportDocument(): ExportDocument =
  ExportDocument(
    schemaVersion = EXPORT_SCHEMA_VERSION,
    exportedAt = "2026-01-02T03:04:05Z",
    subscriptions = sampleSubscriptions(),
    priceHistory = samplePriceHistory(),
    tags = listOf(TagExport(id = TAG_ID, name = "Entertainment", color = -16776961)),
    subscriptionTags = listOf(SubscriptionTagExport(subscriptionId = STREAMING_ID, tagId = TAG_ID)),
    paymentMethods = listOf(PaymentMethodExport(id = PAYMENT_ID, label = "Visa ...1234", color = -65536)),
    settings = sampleSettings(),
  )

private fun sampleSubscriptions(): List<SubscriptionExport> =
  listOf(
    SubscriptionExport(
      id = STREAMING_ID,
      name = "Streaming, Plus \"4K\"",
      iconId = "netflix",
      startDate = "2025-01-15",
      billingCycle = "MONTHLY",
      freeTrialMonths = 1,
      status = "ACTIVE",
      reminderDaysBefore = 3,
      trialReminderEnabled = true,
      paymentMethodId = PAYMENT_ID,
      notes = "shared with family",
      createdAt = "2025-01-15T08:00:00Z",
      updatedAt = "2025-06-01T09:30:00Z",
    ),
    SubscriptionExport(
      id = MUSIC_ID,
      name = "Music",
      iconId = "spotify",
      startDate = "2024-03-01",
      billingCycle = "ANNUAL",
      status = "CANCELLED",
      endDate = "2025-03-01",
      trialReminderEnabled = false,
      createdAt = "2024-03-01T00:00:00Z",
      updatedAt = "2025-03-02T00:00:00Z",
    ),
  )

private fun samplePriceHistory(): List<PriceHistoryExport> =
  listOf(
    PriceHistoryExport(
      id = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      subscriptionId = STREAMING_ID,
      price = "9.99",
      effectiveFromDate = "2025-01-15",
      createdAt = "2025-01-15T08:00:00Z",
    ),
    PriceHistoryExport(
      id = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
      subscriptionId = MUSIC_ID,
      price = "99.00",
      effectiveFromDate = "2024-03-01",
      createdAt = "2024-03-01T00:00:00Z",
    ),
  )

private fun sampleSettings(): SettingsExport =
  SettingsExport(
    currency = "EUR",
    themeSeedSource = "MANUAL",
    themeSeedColor = -16776961,
    themeVariant = "EXPRESSIVE",
    themeMode = "DARK",
  )
