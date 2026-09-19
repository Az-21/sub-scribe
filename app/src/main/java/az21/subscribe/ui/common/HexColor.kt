package az21.subscribe.ui.common

import java.util.Locale

private const val HEX_DIGIT_COUNT = 6
private const val HEX_RADIX = 16
private const val RGB_MASK = 0xFFFFFF
private const val OPAQUE_ALPHA = 0xFF000000.toInt()

/** Formats the RGB channels of [color] as an uppercase `RRGGBB` string, without a leading `#`. */
fun formatRgbHex(color: Int): String = String.format(Locale.ROOT, "%06X", color and RGB_MASK)

/**
 * Parses an optional `#`-prefixed six-digit hex string into an opaque ARGB color, or returns `null`
 * when [hex] is not a valid RGB hex value.
 */
fun parseRgbHex(hex: String): Int? {
  val digits = hex.trim().removePrefix("#")
  if (digits.length != HEX_DIGIT_COUNT || !digits.all { it.isHexDigit() }) return null
  return digits.toLong(HEX_RADIX).toInt() or OPAQUE_ALPHA
}

internal fun Char.isHexDigit(): Boolean = isDigit() || this in 'a'..'f' || this in 'A'..'F'
