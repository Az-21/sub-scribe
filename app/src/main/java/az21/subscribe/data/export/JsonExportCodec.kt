package az21.subscribe.data.export

import az21.subscribe.domain.export.ExportCodec
import az21.subscribe.domain.export.ExportDocument
import az21.subscribe.domain.export.ExportFormat
import az21.subscribe.domain.export.ImportIssue
import az21.subscribe.domain.export.ImportIssueReason
import az21.subscribe.domain.export.ImportParseResult
import az21.subscribe.domain.export.toImportSummary
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

/** Reads and writes the full-fidelity, versioned JSON export. */
class JsonExportCodec
  @Inject
  constructor() : ExportCodec {
    override val format: ExportFormat = ExportFormat.JSON

    override fun encode(document: ExportDocument): ByteArray = json.encodeToString(document).toByteArray(Charsets.UTF_8)

    override fun canDecode(bytes: ByteArray): Boolean = bytes.firstContentByte() == '{'.code.toByte()

    override fun decode(bytes: ByteArray): ImportParseResult =
      decodeDocument(bytes.toString(Charsets.UTF_8)) ?: malformed()

    private fun decodeDocument(text: String): ImportParseResult.Success? {
      val jsonObject = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
      val hasVersion =
        jsonObject
          ?.get("schema_version")
          ?.jsonPrimitive
          ?.content
          ?.isNotBlank() == true
      val decoded = if (hasVersion) runCatching { json.decodeFromString<ExportDocument>(text) }.getOrNull() else null
      return decoded?.let { document -> ImportParseResult.Success(document, document.toImportSummary(), emptyList()) }
    }

    private fun malformed(): ImportParseResult.Failure =
      ImportParseResult.Failure(listOf(ImportIssue(ImportIssueReason.MALFORMED_FILE, "")))

    private companion object {
      val json =
        Json {
          prettyPrint = true
          encodeDefaults = true
          ignoreUnknownKeys = true
        }
    }
  }

private fun ByteArray.firstContentByte(): Byte? = firstOrNull { byte -> !byte.toInt().toChar().isWhitespace() }
