package com.example.data.export

import java.io.ByteArrayInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Reads common document types into text so the AI can understand them:
 * .xlsx (Excel), .docx (Word), .zip, .md, .txt, .csv.
 * Uses only the JDK (no extra libraries): xlsx/docx/zip are ZIP containers.
 */
object FileImportHelper {

    fun isSupported(name: String): Boolean {
        val n = name.lowercase()
        return n.endsWith(".xlsx") || n.endsWith(".docx") || n.endsWith(".zip") ||
                n.endsWith(".md") || n.endsWith(".txt") || n.endsWith(".csv") ||
                n.endsWith(".json")
    }

    /** Returns text extracted from the file, or null if unsupported/unreadable. */
    fun toText(name: String, bytes: ByteArray): String? {
        return try {
            val n = name.lowercase()
            when {
                n.endsWith(".xlsx") -> xlsxToCsv(bytes)
                n.endsWith(".docx") -> docxToText(bytes)
                n.endsWith(".zip") -> zipToText(bytes)
                else -> String(bytes, Charsets.UTF_8)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun readZip(bytes: ByteArray, match: (String) -> Boolean): String {
        val out = StringBuilder()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && match(entry.name)) {
                    out.append(zip.readBytes().toString(Charsets.UTF_8))
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return out.toString()
    }

    private fun stripTags(xml: String): String =
        xml.replace(Regex("<[^>]+>"), " ")
           .replace(Regex("&amp;"), "&")
           .replace(Regex("&lt;"), "<")
           .replace(Regex("&gt;"), ">")
           .replace(Regex("&quot;"), "\"")
           .replace(Regex("&apos;"), "'")

    private fun docxToText(bytes: ByteArray): String {
        val xml = readZip(bytes) { it == "word/document.xml" }
        return stripTags(xml).replace(Regex("\\s+"), " ").trim()
    }

    private fun xlsxToCsv(bytes: ByteArray): String {
        // shared strings
        val sharedXml = readZip(bytes) { it == "xl/sharedStrings.xml" }
        val shared = Regex("<si>(.*?)</si>", RegexOption.DOT_MATCHES_ALL)
            .findAll(sharedXml)
            .map { stripTags(it.groupValues[1]).trim() }
            .toList()

        val sheetXml = readZip(bytes) { it == "xl/worksheets/sheet1.xml" }
        val sb = StringBuilder()
        val rows = Regex("<row[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL).findAll(sheetXml)
        for (row in rows) {
            val cells = Regex("<c[^>]*?(?:t=\"(\\w+)\")?[^>]*>(.*?)</c>", RegexOption.DOT_MATCHES_ALL)
                .findAll(row.groupValues[1])
                .map { cell ->
                    val t = cell.groupValues[1]
                    val inner = cell.groupValues[2]
                    when {
                        t == "s" -> shared.getOrNull(Regex("<v>(.*?)</v>").find(inner)?.groupValues?.get(1)?.toIntOrNull() ?: -1) ?: ""
                        t == "inlineStr" -> stripTags(Regex("<t[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL).find(inner)?.groupValues?.get(1) ?: "")
                        else -> Regex("<v>(.*?)</v>").find(inner)?.groupValues?.get(1) ?: ""
                    }
                }
                .joinToString(",") { csvCell(it) }
            if (cells.isNotBlank()) sb.append(cells).append('\n')
        }
        return sb.toString()
    }

    private fun zipToText(bytes: ByteArray): String {
        val out = StringBuilder()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val n = entry.name.lowercase()
                    if (n.endsWith(".txt") || n.endsWith(".md") || n.endsWith(".csv") ||
                        n.endsWith(".json") || n.endsWith(".xml")) {
                        out.append("=== ").append(entry.name).append(" ===\n")
                        out.append(zip.readBytes().toString(Charsets.UTF_8)).append("\n\n")
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return out.toString()
    }

    private fun csvCell(v: String): String =
        if (v.contains(',') || v.contains('"') || v.contains('\n')) "\"${v.replace("\"", "\"\"")}\"" else v
}
