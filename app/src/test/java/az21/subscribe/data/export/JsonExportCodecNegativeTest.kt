package az21.subscribe.data.export

import az21.subscribe.domain.export.ImportParseResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Malformed and permissive input handling for [JsonExportCodec]. */
class JsonExportCodecNegativeTest {
  private val codec = JsonExportCodec()

  @Test
  fun canDecode_requiresAnObjectOpeningBrace() {
    assertFalse(codec.canDecode(ByteArray(0)))
    assertFalse(codec.canDecode("[]".toByteArray()))
    assertTrue(codec.canDecode("   \n{\"schema_version\":1}".toByteArray()))
  }

  @Test
  fun decode_emptyOrNonObject_isFailure() {
    assertTrue(codec.decode(ByteArray(0)) is ImportParseResult.Failure)
    assertTrue(codec.decode("[]".toByteArray()) is ImportParseResult.Failure)
    assertTrue(codec.decode("not json".toByteArray()) is ImportParseResult.Failure)
  }

  @Test
  fun decode_missingOrBlankSchemaVersion_isFailure() {
    assertTrue(codec.decode("{}".toByteArray()) is ImportParseResult.Failure)
    assertTrue(codec.decode("{\"schema_version\":\"\"}".toByteArray()) is ImportParseResult.Failure)
  }

  @Test
  fun decode_minimalDocument_succeeds() {
    val result = codec.decode("{\"schema_version\":1}".toByteArray())

    val success = result as ImportParseResult.Success
    assertEquals(1, success.document.schemaVersion)
    assertTrue(success.document.subscriptions.isEmpty())
    assertTrue(success.issues.isEmpty())
  }

  @Test
  fun decode_ignoresUnknownKeys() {
    val result = codec.decode("{\"schema_version\":1,\"unexpected\":\"value\"}".toByteArray())

    assertTrue(result is ImportParseResult.Success)
  }

  @Test
  fun decode_wrongFieldType_isFailure() {
    val result = codec.decode("{\"schema_version\":1,\"subscriptions\":\"nope\"}".toByteArray())

    assertTrue(result is ImportParseResult.Failure)
  }
}
