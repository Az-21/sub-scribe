package az21.subscribe.data.export

import org.junit.Assert.assertEquals
import org.junit.Test

/** Escaping, quoting, and separator edge cases for [CsvTableCodec]. */
class CsvTableCodecEdgeCasesTest {
  @Test
  fun encode_withNoRows_returnsHeadersOnly() {
    assertEquals("a,b", CsvTableCodec.encode(listOf("a", "b"), emptyList()))
  }

  @Test
  fun encode_quotesOnlyFieldsNeedingIt() {
    assertEquals("h,h2\r\nplain,\"a,b\"", CsvTableCodec.encode(listOf("h", "h2"), listOf(listOf("plain", "a,b"))))
  }

  @Test
  fun encode_doublesEmbeddedQuotes() {
    assertEquals("h\r\n\"he said \"\"hi\"\"\"", CsvTableCodec.encode(listOf("h"), listOf(listOf("he said \"hi\""))))
  }

  @Test
  fun encode_quotesNewlinesAndCarriageReturns() {
    assertEquals("h\r\n\"line1\nline2\"", CsvTableCodec.encode(listOf("h"), listOf(listOf("line1\nline2"))))
    assertEquals("h\r\n\"cr\rhere\"", CsvTableCodec.encode(listOf("h"), listOf(listOf("cr\rhere"))))
  }

  @Test
  fun decode_unescapesDoubledQuotes() {
    assertEquals(listOf(listOf("a\"b")), CsvTableCodec.decode("\"a\"\"b\""))
  }

  @Test
  fun decode_quotedFieldMayContainCommaAndNewline() {
    assertEquals(listOf(listOf("a,b")), CsvTableCodec.decode("\"a,b\""))
    assertEquals(listOf(listOf("a\nb")), CsvTableCodec.decode("\"a\nb\""))
  }

  @Test
  fun decode_handlesLfAndCrlfLineEndings() {
    assertEquals(listOf(listOf("h1", "h2"), listOf("v1", "v2")), CsvTableCodec.decode("h1,h2\nv1,v2"))
    assertEquals(listOf(listOf("h1", "h2"), listOf("v1", "v2")), CsvTableCodec.decode("h1,h2\r\nv1,v2\r\n"))
  }

  @Test
  fun decode_preservesEmptyFields() {
    assertEquals(listOf(listOf("a", "", "b")), CsvTableCodec.decode("a,,b"))
    assertEquals(listOf(listOf("a", "b", "")), CsvTableCodec.decode("a,b,"))
    assertEquals(listOf(listOf("a", "")), CsvTableCodec.decode("a,\"\""))
  }

  @Test
  fun decode_singleFieldWithoutSeparator() {
    assertEquals(listOf(listOf("hello")), CsvTableCodec.decode("hello"))
  }
}
