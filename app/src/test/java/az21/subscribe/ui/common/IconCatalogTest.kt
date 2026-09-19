package az21.subscribe.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Namespacing behavior of the combined icon catalog. */
class IconCatalogTest {
  @Test
  fun normalize_treatsLegacyBareIdsAsSimple() {
    assertEquals("simple:netflix", IconCatalog.normalize("netflix"))
  }

  @Test
  fun normalize_stripsSeparatorsAndCase() {
    assertEquals("simple:applemusic", IconCatalog.normalize("Apple Music"))
  }

  @Test
  fun normalize_preservesExplicitNamespace() {
    assertEquals("simple:netflix", IconCatalog.normalize("simple:netflix"))
    assertEquals("material:home", IconCatalog.normalize("material:home"))
  }

  @Test
  fun normalize_sameNameAcrossSets_yieldsDistinctKeys() {
    assertNotEquals(IconCatalog.normalize("home"), IconCatalog.normalize("material:home"))
  }

  @Test
  fun findEntry_unknownKey_returnsNull() {
    assertNull(IconCatalog.findEntry("simple:does-not-exist"))
    assertNull(IconCatalog.findEntry("material:does-not-exist"))
    assertNull(IconCatalog.findEntry("does-not-exist"))
  }

  @Test
  fun find_unknownKey_returnsNull() {
    assertNull(IconCatalog.find("simple:does-not-exist"))
  }

  @Test
  fun findEntry_blankOrNullKey_returnsNull() {
    assertNull(IconCatalog.findEntry(""))
    assertNull(IconCatalog.findEntry(null))
  }

  @Test
  fun findEntry_absorbsSeparatorAndCaseOnlyRenames() {
    val canonical = IconCatalog.findEntry("simple:netflix")

    assertNotNull(canonical)
    assertEquals(canonical, IconCatalog.findEntry("simple:Net-Flix"))
    assertEquals(canonical, IconCatalog.findEntry("netflix"))
  }

  @Test
  fun findEntry_simpleIcon_exposesBrandDefaultColor() {
    val entry = IconCatalog.findEntry("simple:netflix")

    assertEquals(IconSource.SIMPLE, entry?.source)
    assertEquals(0xFFE50914.toInt(), entry?.defaultColor)
  }

  @Test
  fun findEntry_materialIcon_exposesCuratedDefaultColor() {
    val entry = IconCatalog.findEntry("material:home")

    assertEquals(IconSource.MATERIAL, entry?.source)
    assertEquals(0xFF3949AB.toInt(), entry?.defaultColor)
  }
}
