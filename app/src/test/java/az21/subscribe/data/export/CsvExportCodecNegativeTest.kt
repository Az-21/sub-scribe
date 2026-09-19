package az21.subscribe.data.export

import az21.subscribe.domain.export.ExportDocument
import az21.subscribe.domain.export.ImportIssueReason
import az21.subscribe.domain.export.ImportParseResult
import az21.subscribe.domain.export.sampleExportDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Malformed / incomplete zip handling for [CsvExportCodec]. */
class CsvExportCodecNegativeTest {
  private val codec = CsvExportCodec()

  @Test
  fun canDecode_requiresFullZipMagic() {
    assertFalse(codec.canDecode(ByteArray(0)))
    assertFalse(codec.canDecode(byteArrayOf(0x50)))
    assertTrue(codec.canDecode(byteArrayOf(0x50, 0x4B)))
  }

  @Test
  fun decode_nonZipBytes_isFailure() {
    val result = codec.decode(byteArrayOf(0x01, 0x02, 0x03))

    val failure = result as ImportParseResult.Failure
    assertEquals(ImportIssueReason.MALFORMED_FILE, failure.issues.single().reason)
  }

  @Test
  fun decode_zipWithoutManifest_isFailure() {
    val result = codec.decode(zip(mapOf("other.csv" to "value")))

    assertTrue(result is ImportParseResult.Failure)
  }

  @Test
  fun decode_manifestWithoutSchemaVersion_isFailure() {
    val entries = unzip(codec.encode(sampleExportDocument()))
    entries[MANIFEST] = CsvTableCodec.encode(listOf("key", "value"), listOf(listOf("exported_at", "now")))

    val result = codec.decode(zip(entries))

    assertTrue(result is ImportParseResult.Failure)
  }

  @Test
  fun decode_missingFiles_reportsEachOneAndKeepsGoing() {
    val entries = unzip(codec.encode(sampleExportDocument()))
    entries.keys.retainAll(setOf(MANIFEST, "settings.csv"))

    val result = codec.decode(zip(entries)) as ImportParseResult.Success

    assertEquals(5, result.issues.count { it.reason == ImportIssueReason.MISSING_FILE })
    assertTrue(result.document.subscriptions.isEmpty())
    assertTrue(result.document.priceHistory.isEmpty())
    assertTrue(result.document.tags.isEmpty())
    assertTrue(result.document.paymentMethods.isEmpty())
    assertTrue(result.document.subscriptionTags.isEmpty())
  }

  @Test
  fun decode_missingRequiredHeaders_reportsMalformed() {
    val entries = unzip(codec.encode(sampleExportDocument()))
    entries["subscriptions.csv"] = CsvTableCodec.encode(listOf("wrong"), listOf(listOf("value")))

    val result = codec.decode(zip(entries)) as ImportParseResult.Success

    assertTrue(result.document.subscriptions.isEmpty())
    val malformed =
      result.issues.any { issue ->
        issue.reason == ImportIssueReason.MALFORMED_FILE && issue.location.startsWith("subscriptions.csv")
      }
    assertTrue(malformed)
  }

  @Test
  fun decode_skipsBlankDataRows() {
    val entries = unzip(codec.encode(sampleExportDocument()))
    val lines = entries.getValue("subscriptions.csv").split("\r\n")
    entries["subscriptions.csv"] = (lines.take(1) + "" + lines.drop(1)).joinToString("\r\n")

    val result = codec.decode(zip(entries)) as ImportParseResult.Success

    assertEquals(2, result.document.subscriptions.size)
  }

  @Test
  fun emptyDocument_roundTripsWithNoIssues() {
    val decoded = codec.decode(codec.encode(ExportDocument())) as ImportParseResult.Success

    assertTrue(decoded.issues.isEmpty())
    assertEquals(ExportDocument(), decoded.document.copy(exportedAt = ""))
  }

  private fun unzip(bytes: ByteArray): MutableMap<String, String> {
    val files = mutableMapOf<String, String>()
    ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
      var entry = zip.nextEntry
      while (entry != null) {
        if (!entry.isDirectory) files[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
        entry = zip.nextEntry
      }
    }
    return files
  }

  private fun zip(files: Map<String, String>): ByteArray {
    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
      files.forEach { (name, content) ->
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
      }
    }
    return output.toByteArray()
  }

  private companion object {
    const val MANIFEST = "manifest.csv"
  }
}
