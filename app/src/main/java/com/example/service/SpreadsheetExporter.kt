package com.example.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.ColumnType
import com.example.data.model.SpreadsheetDocument
import com.example.data.model.SpreadsheetSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class SpreadsheetExporter(private val context: Context) {

    enum class ExportFormat(val extension: String, val mimeType: String, val displayName: String) {
        EXCEL_XML("xls", "application/vnd.ms-excel", "Excel Workbook (.xls)"),
        CSV("csv", "text/csv", "Comma-Separated Values (.csv)"),
        TSV("tsv", "text/tab-separated-values", "Tab-Separated Values (.tsv)"),
        HTML("html", "text/html", "Interactive HTML Table (.html)")
    }

    suspend fun exportToFile(
        document: SpreadsheetDocument,
        format: ExportFormat,
        activeSheetIndex: Int = 0
    ): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val cleanBaseName = document.fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val outputFile = File(exportDir, "${cleanBaseName}_${System.currentTimeMillis()}.${format.extension}")

        val content = when (format) {
            ExportFormat.EXCEL_XML -> generateExcelXml(document)
            ExportFormat.CSV -> generateCsv(document.sheets.getOrElse(activeSheetIndex) { document.sheets.first() })
            ExportFormat.TSV -> generateTsv(document.sheets.getOrElse(activeSheetIndex) { document.sheets.first() })
            ExportFormat.HTML -> generateHtml(document)
        }

        FileOutputStream(outputFile).use { out ->
            if (format == ExportFormat.CSV || format == ExportFormat.TSV) {
                // Add UTF-8 BOM so Excel opens UTF-8 files correctly
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            }
            out.write(content.toByteArray(Charsets.UTF_8))
        }

        outputFile
    }

    fun shareFile(file: File, mimeType: String, title: String = "Share Spreadsheet") {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            putExtra(Intent.EXTRA_TEXT, "Spreadsheet extracted from PDF using AI: ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun copyToClipboard(sheet: SpreadsheetSheet, asTsv: Boolean = true): String {
        val text = if (asTsv) generateTsv(sheet) else generateCsv(sheet)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Extracted Spreadsheet", text)
        clipboard.setPrimaryClip(clip)
        return text
    }

    fun generateCsv(sheet: SpreadsheetSheet): String {
        val sb = StringBuilder()
        // Headers
        sb.append(sheet.headers.joinToString(",") { escapeCsvCell(it) }).append("\n")
        // Rows
        for (row in sheet.rows) {
            sb.append(row.joinToString(",") { escapeCsvCell(it) }).append("\n")
        }
        // Summary if present
        if (sheet.summaryInfo.isNotEmpty()) {
            sb.append("\nSummary Metadata\n")
            for ((k, v) in sheet.summaryInfo) {
                sb.append(escapeCsvCell(k)).append(",").append(escapeCsvCell(v)).append("\n")
            }
        }
        return sb.toString()
    }

    fun generateTsv(sheet: SpreadsheetSheet): String {
        val sb = StringBuilder()
        sb.append(sheet.headers.joinToString("\t")).append("\n")
        for (row in sheet.rows) {
            sb.append(row.joinToString("\t")).append("\n")
        }
        return sb.toString()
    }

    fun generateExcelXml(document: SpreadsheetDocument): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:html="http://www.w3.org/TR/REC-html40">
 <DocumentProperties xmlns="urn:schemas-microsoft-com:office:office">
  <Author>AI PDF to Excel Engine</Author>
  <Created>${java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date())}</Created>
 </DocumentProperties>
 <Styles>
  <Style ss:ID="Default" ss:Name="Normal">
   <Alignment ss:Vertical="Center"/>
   <Borders/>
   <Font ss:FontName="Calibri" x:Family="Swiss" ss:Size="11" ss:Color="#0F172A"/>
   <Interior/>
   <NumberFormat/>
   <Protection/>
  </Style>
  <Style ss:ID="HeaderStyle">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#0D9488"/>
   </Borders>
   <Font ss:FontName="Calibri" x:Family="Swiss" ss:Size="11" ss:Color="#FFFFFF" ss:Bold="1"/>
   <Interior ss:Color="#0F766E" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="DataStyle">
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
   </Borders>
   <Font ss:FontName="Calibri" ss:Size="10" ss:Color="#1E293B"/>
  </Style>
  <Style ss:ID="NumberStyle">
   <Alignment ss:Horizontal="Right" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
   </Borders>
   <Font ss:FontName="Calibri" ss:Size="10" ss:Color="#1E293B"/>
   <NumberFormat ss:Format="#,##0.00"/>
  </Style>
  <Style ss:ID="CurrencyStyle">
   <Alignment ss:Horizontal="Right" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
   </Borders>
   <Font ss:FontName="Calibri" ss:Size="10" ss:Color="#0F766E" ss:Bold="1"/>
   <NumberFormat ss:Format="$#,##0.00"/>
  </Style>
 </Styles>
""")

        for (sheet in document.sheets) {
            val safeSheetName = sheet.name.replace("[^a-zA-Z0-9 ]".toRegex(), "").take(31).ifBlank { "Sheet" }
            sb.append(" <Worksheet ss:Name=\"$safeSheetName\">\n")
            sb.append("  <Table ss:DefaultColumnWidth=\"110\" ss:DefaultRowHeight=\"20\">\n")

            // Column definitions with auto width hints
            for (colIndex in sheet.headers.indices) {
                val headerLength = sheet.headers[colIndex].length
                val maxCellLength = sheet.rows.maxOfOrNull { it.getOrNull(colIndex)?.length ?: 0 } ?: 0
                val colWidth = maxOf(80, maxOf(headerLength, maxCellLength) * 9 + 30)
                sb.append("   <Column ss:Index=\"${colIndex + 1}\" ss:Width=\"$colWidth\"/>\n")
            }

            // Header Row
            sb.append("   <Row ss:Height=\"24\">\n")
            for (header in sheet.headers) {
                sb.append("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">${escapeXml(header)}</Data></Cell>\n")
            }
            sb.append("   </Row>\n")

            // Data Rows
            for (row in sheet.rows) {
                sb.append("   <Row ss:Height=\"19\">\n")
                for (colIndex in sheet.headers.indices) {
                    val rawValue = row.getOrNull(colIndex) ?: ""
                    val colType = sheet.columnTypes.getOrElse(colIndex) { ColumnType.TEXT }
                    val cellXml = formatXmlCell(rawValue, colType)
                    sb.append(cellXml)
                }
                sb.append("   </Row>\n")
            }

            // Summary Rows if present
            if (sheet.summaryInfo.isNotEmpty()) {
                sb.append("   <Row ss:Height=\"15\"/>\n")
                sb.append("   <Row ss:Height=\"22\">\n")
                sb.append("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">Summary Field</Data></Cell>\n")
                sb.append("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">Value</Data></Cell>\n")
                sb.append("   </Row>\n")
                for ((k, v) in sheet.summaryInfo) {
                    sb.append("   <Row ss:Height=\"19\">\n")
                    sb.append("    <Cell ss:StyleID=\"DataStyle\"><Data ss:Type=\"String\">${escapeXml(k)}</Data></Cell>\n")
                    sb.append("    <Cell ss:StyleID=\"DataStyle\"><Data ss:Type=\"String\">${escapeXml(v)}</Data></Cell>\n")
                    sb.append("   </Row>\n")
                }
            }

            sb.append("  </Table>\n")
            sb.append("  <WorksheetOptions xmlns=\"urn:schemas-microsoft-com:office:excel\">\n")
            sb.append("   <Selected/>\n")
            sb.append("   <FreezePanes/>\n")
            sb.append("   <FrozenNoSplit/>\n")
            sb.append("   <SplitHorizontal>1</SplitHorizontal>\n")
            sb.append("   <TopRowBottomPane>1</TopRowBottomPane>\n")
            sb.append("  </WorksheetOptions>\n")
            sb.append(" </Worksheet>\n")
        }

        sb.append("</Workbook>")
        return sb.toString()
    }

    private fun generateHtml(document: SpreadsheetDocument): String {
        val sb = StringBuilder()
        sb.append("""<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>${escapeXml(document.fileName)}</title>
<style>
  body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #F8FAFC; color: #0F172A; padding: 24px; }
  h2 { color: #0F766E; margin-bottom: 8px; }
  .sheet-title { color: #0D9488; margin-top: 32px; border-bottom: 2px solid #0D9488; padding-bottom: 6px; }
  table { border-collapse: collapse; width: 100%; margin-top: 12px; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
  th { background: #0F766E; color: white; text-align: left; padding: 12px 16px; font-size: 13px; font-weight: 600; }
  td { padding: 10px 16px; border-bottom: 1px solid #E2E8F0; font-size: 13px; }
  tr:nth-child(even) { background: #F8FAFC; }
  .meta { color: #64748B; font-size: 12px; margin-bottom: 24px; }
</style>
</head>
<body>
<h2>${escapeXml(document.fileName)}</h2>
<div class="meta">Extracted on ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(document.createdAt))} | AI Confidence: ${(document.confidenceScore * 100).toInt()}%</div>
""")
        for (sheet in document.sheets) {
            sb.append("<h3 class=\"sheet-title\">${escapeXml(sheet.name)} (${sheet.rowCount} rows)</h3>\n")
            sb.append("<table>\n<thead>\n<tr>\n")
            for (h in sheet.headers) {
                sb.append("<th>${escapeXml(h)}</th>\n")
            }
            sb.append("</tr>\n</thead>\n<tbody>\n")
            for (r in sheet.rows) {
                sb.append("<tr>\n")
                for (c in r) {
                    sb.append("<td>${escapeXml(c)}</td>\n")
                }
                sb.append("</tr>\n")
            }
            sb.append("</tbody>\n</table>\n")
        }
        sb.append("</body>\n</html>")
        return sb.toString()
    }

    private fun formatXmlCell(rawValue: String, colType: ColumnType): String {
        val trimmed = rawValue.trim()
        val numClean = trimmed.replace("$", "").replace("€", "").replace(",", "").replace("%", "")
        val numVal = numClean.toDoubleOrNull()

        return if (numVal != null && (colType == ColumnType.NUMBER || colType == ColumnType.CURRENCY || colType == ColumnType.PERCENTAGE)) {
            val styleId = if (colType == ColumnType.CURRENCY || trimmed.startsWith("$")) "CurrencyStyle" else "NumberStyle"
            "    <Cell ss:StyleID=\"$styleId\"><Data ss:Type=\"Number\">$numVal</Data></Cell>\n"
        } else {
            "    <Cell ss:StyleID=\"DataStyle\"><Data ss:Type=\"String\">${escapeXml(rawValue)}</Data></Cell>\n"
        }
    }

    private fun escapeCsvCell(value: String): String {
        val containsSpecial = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        return if (containsSpecial) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
