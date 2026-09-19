package az21.subscribe.ui.common

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG contrast math and the display-only inversion used to keep icon glyphs visible. */
class ColorContrastTest {
  @Test
  fun contrastRatio_blackOnWhite_isMaximum() {
    assertEquals(21f, contrastRatio(Color.Black, Color.White), 0.01f)
  }

  @Test
  fun contrastRatio_identicalColors_isOne() {
    assertEquals(1f, contrastRatio(Color.Black, Color.Black), 0.001f)
  }

  @Test
  fun contrastRatio_isSymmetric() {
    assertEquals(
      contrastRatio(Color.White, Color.Black),
      contrastRatio(Color.Black, Color.White),
      0.001f,
    )
  }

  @Test
  fun hasSufficientContrast_respectsThreshold() {
    assertTrue(hasSufficientContrast(Color.Black, Color.White, minRatio = 3f))
    assertTrue(hasSufficientContrast(Color.Black, Color.White, minRatio = 20f))
    assertFalse(hasSufficientContrast(Color.White, Color.White, minRatio = 3f))
  }

  @Test
  fun inverted_flipsEachChannel() {
    assertEquals(Color(0xFFE5CCB2), Color(0xFF1A334D).inverted())
  }

  @Test
  fun inverted_keepsAlpha() {
    assertEquals(Color(0x80E5CCB2), Color(0x801A334D).inverted())
  }

  @Test
  fun inverted_mapsWhiteToBlack() {
    assertEquals(Color.Black, Color.White.inverted())
  }
}
