package az21.subscribe.data.export

import az21.subscribe.domain.export.ImportParseResult
import az21.subscribe.domain.export.sampleExportDocument
import az21.subscribe.domain.export.toImportSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportCodecTest {
  private val document = sampleExportDocument()

  @Test
  fun jsonRoundTrip_preservesDocument() {
    val codec = JsonExportCodec()

    val decoded = codec.decode(codec.encode(document))

    assertEquals(ImportParseResult.Success(document, document.toImportSummary()), decoded)
  }

  @Test
  fun jsonDetectsOnlyJson() {
    val codec = JsonExportCodec()

    assertTrue(codec.canDecode("  {\"schema_version\":1}".toByteArray()))
    assertFalse(codec.canDecode(byteArrayOf(0x50, 0x4B, 0x03, 0x04)))
  }

  @Test
  fun jsonRejectsMalformedInput() {
    val codec = JsonExportCodec()

    assertTrue(codec.decode("{not json".toByteArray()) is ImportParseResult.Failure)
    assertTrue(codec.decode("{}".toByteArray()) is ImportParseResult.Failure)
  }
}
