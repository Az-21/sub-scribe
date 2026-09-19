package az21.subscribe.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HexColorTest {
  @Test
  fun formatsRgbChannelsAsUppercaseHex_andIgnoresAlpha() {
    assertEquals("FF0000", formatRgbHex(0xFFFF0000.toInt()))
    assertEquals("123456", formatRgbHex(0x80123456.toInt()))
    assertEquals("000000", formatRgbHex(0xFF000000.toInt()))
    assertEquals("FFFFFF", formatRgbHex(0xFFFFFFFF.toInt()))
  }

  @Test
  fun parsesSixDigitHex_withOrWithoutHash() {
    assertEquals(0xFFFF0000.toInt(), parseRgbHex("FF0000"))
    assertEquals(0xFFFF0000.toInt(), parseRgbHex("#ff0000"))
    assertEquals(0xFF123456.toInt(), parseRgbHex("123456"))
  }

  @Test
  fun returnsNull_whenHexIsInvalid() {
    assertNull(parseRgbHex(""))
    assertNull(parseRgbHex("#12345"))
    assertNull(parseRgbHex("1234567"))
    assertNull(parseRgbHex("GGGGGG"))
    assertNull(parseRgbHex("#12 34 56"))
  }
}
