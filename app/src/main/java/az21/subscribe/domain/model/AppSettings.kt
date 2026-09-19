package az21.subscribe.domain.model

/**
 * User preferences persisted in DataStore.
 *
 * @param themeSeedColor an ARGB color used when [themeSeedSource] is [ThemeSeedSource.MANUAL],
 *   otherwise null and the system/wallpaper color is used.
 */
data class AppSettings(
  val currency: Currency = Currency.DEFAULT,
  val themeSeedSource: ThemeSeedSource = ThemeSeedSource.SYSTEM,
  val themeSeedColor: Int? = null,
  val themeVariant: ThemeVariant = ThemeVariant.TONAL_SPOT,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

/** Whether the theme seed comes from the wallpaper/system or a user-picked color. */
enum class ThemeSeedSource {
  SYSTEM,
  MANUAL,
}

/** Material 3 Expressive color scheme variants. */
enum class ThemeVariant {
  TONAL_SPOT,
  NEUTRAL,
  VIBRANT,
  EXPRESSIVE,
  MONOCHROME,
}

/** Light/dark preference, defaulting to the system setting. */
enum class ThemeMode {
  SYSTEM,
  LIGHT,
  DARK,
}

/**
 * App-wide display currency. Values are stored as plain numerics; this only changes the symbol and
 * code shown in the UI.
 */
enum class Currency(
  val code: String,
  val symbol: String,
) {
  USD("USD", "$"),
  EUR("EUR", "€"),
  GBP("GBP", "£"),
  JPY("JPY", "¥"),
  CNY("CNY", "¥"),
  INR("INR", "₹"),
  AUD("AUD", "A$"),
  CAD("CAD", "C$"),
  CHF("CHF", "CHF"),
  BRL("BRL", "R$"),
  MXN("MXN", "MX$"),
  SEK("SEK", "kr"),
  NOK("NOK", "kr"),
  PLN("PLN", "zł"),
  ZAR("ZAR", "R"),
  NZD("NZD", "NZ$"),
  SGD("SGD", "S$"),
  HKD("HKD", "HK$"),
  KRW("KRW", "₩"),
  TRY("TRY", "₺"),
  ;

  companion object {
    val DEFAULT: Currency = USD

    fun fromCode(code: String?): Currency = entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: DEFAULT
  }
}
