package az21.subscribe.data.export

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTableCodecTest {
  @Test
  fun roundTripsSpecialCharacters() {
    val rows =
      listOf(
        listOf("plain", "with,comma", "with\"quote", "with\nnewline", ""),
      )

    val decoded = CsvTableCodec.decode(CsvTableCodec.encode(listOf("a", "b", "c", "d", "e"), rows))

    assertEquals(rows, decoded.drop(1))
  }

  @Test
  fun decodesWithoutTrailingNewline() {
    val decoded = CsvTableCodec.decode("h1,h2\r\nv1,v2")

    assertEquals(listOf(listOf("h1", "h2"), listOf("v1", "v2")), decoded)
  }

  @Test
  fun decodesEmptyInput() {
    assertEquals(emptyList<List<String>>(), CsvTableCodec.decode(""))
  }
}
