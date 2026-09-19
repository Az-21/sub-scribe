package az21.subscribe.domain.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Value semantics and metadata for [ExportedFile] and [ExportFormat]. */
class ExportedFileTest {
  @Test
  fun equalsAndHashCode_compareByteContent() {
    val first = ExportedFile("export.json", "application/json", byteArrayOf(1, 2, 3))
    val second = ExportedFile("export.json", "application/json", byteArrayOf(1, 2, 3))

    assertEquals(first, second)
    assertEquals(first.hashCode(), second.hashCode())
  }

  @Test
  fun differsOnNameMimeOrBytes() {
    val base = ExportedFile("export.json", "application/json", byteArrayOf(1, 2, 3))

    assertNotEquals(base, ExportedFile("other.json", "application/json", byteArrayOf(1, 2, 3)))
    assertNotEquals(base, ExportedFile("export.json", "text/plain", byteArrayOf(1, 2, 3)))
    assertNotEquals(base, ExportedFile("export.json", "application/json", byteArrayOf(3, 2, 1)))
  }

  @Test
  fun exportFormats_exposeMetadata() {
    assertEquals("application/json", ExportFormat.JSON.mimeType)
    assertEquals("json", ExportFormat.JSON.fileExtension)
  }
}
