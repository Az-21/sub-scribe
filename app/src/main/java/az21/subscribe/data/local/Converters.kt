package az21.subscribe.data.local

import androidx.room.TypeConverter
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.SubscriptionStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Room type converters for the value types used by the entities.
 *
 * One converter per direction per type is inherent to a Room TypeConverters class, hence the
 * [Suppress] on the function count.
 */
@Suppress("TooManyFunctions")
class Converters {
  @TypeConverter
  fun fromUuid(value: UUID?): String? = value?.toString()

  @TypeConverter
  fun toUuid(value: String?): UUID? = value?.let(UUID::fromString)

  @TypeConverter
  fun fromLocalDate(value: LocalDate?): String? = value?.toString()

  @TypeConverter
  fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

  @TypeConverter
  fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

  @TypeConverter
  fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

  @TypeConverter
  fun fromLocalTime(value: LocalTime?): String? = value?.toString()

  @TypeConverter
  fun toLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

  @TypeConverter
  fun fromBigDecimal(value: BigDecimal?): String? = value?.toPlainString()

  @TypeConverter
  fun toBigDecimal(value: String?): BigDecimal? = value?.let(::BigDecimal)

  @TypeConverter
  fun fromBillingCycle(value: BillingCycle?): String? = value?.name

  @TypeConverter
  fun toBillingCycle(value: String?): BillingCycle? = value?.let(BillingCycle::valueOf)

  @TypeConverter
  fun fromSubscriptionStatus(value: SubscriptionStatus?): String? = value?.name

  @TypeConverter
  fun toSubscriptionStatus(value: String?): SubscriptionStatus? = value?.let(SubscriptionStatus::valueOf)
}
