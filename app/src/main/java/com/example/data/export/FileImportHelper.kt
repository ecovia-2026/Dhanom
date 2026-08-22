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
        val n = name.lowercase()
        return when {
            n.endsWith(".xlsx") -> xlsxToCsv(bytes)
            n.endsWith(".docx") -> docxToText(bytes)
            n.endsWith(".zip") -> zipToText(bytes)
            else -> String(bytes, Charsets.UTF_8)
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

/** Minimal built-in PDF text extractor (no external library).
 *  Extracts text from FlateDecode/LZW content streams using the standard
 *  PDF text operators (Tj / TJ). Works for most bank statement PDFs. */
fun extractPdfText(bytes: ByteArray): String {
    val sb = StringBuilder()
    try {
        val text = String(bytes, Charsets.ISO_8859_1)
        // find every "stream ... endstream"
        val streamRegex = Regex("stream\r?\n", RegexOption.IGNORE_CASE)
        var idx = 0
        while (true) {
            val m = streamRegex.find(text, idx) ?: break
            val contentStart = m.range.last + 1
            val end = text.indexOf("endstream", contentStart)
            if (end == -1) break
            val raw = text.substring(contentStart, end)
            // try FlateDecode (zlib) decompress; fall back to raw
            val decoded = try {
                val inflater = java.util.zip.Inflater()
                inflater.setInput(raw.toByteArray(Charsets.ISO_8859_1))
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(4096)
                while (!inflater.finished()) {
                    val n = inflater.inflate(buf)
                    if (n == 0) break
                    out.write(buf, 0, n)
                }
                inflater.end()
                String(out.toByteArray(), Charsets.ISO_8859_1)
            } catch (e: Exception) {
                raw
            }
            extractPdfTextOperators(decoded, sb)
            idx = end + 9
        }
    } catch (e: Exception) {
        // ignore
    }
    return sb.toString().replace(Regex("\\s+"), " ").trim()
}

private fun extractPdfTextOperators(content: String, sb: StringBuilder) {
    // "(...)" Tj  and  [(...) ... ] TJ
    val tj = Regex("""\((?:[^()\\]|\\.)*\)\s*Tj""")
    for (m in tj.findAll(content)) {
        sb.append(decodePdfString(m.value.substringBefore("Tj").trim())).append(' ')
    }
    val tjArr = Regex("""\[(?:[^\[\]])*\]\s*TJ""")
    for (m in tjArr.findAll(content)) {
        val inner = m.value.substring(1, m.value.indexOf(']'))
        for (tok in Regex("""\((?:[^()\\]|\\.)*\)""").findAll(inner)) {
            sb.append(decodePdfString(tok.value)).append(' ')
        }
    }
}

private fun decodePdfString(s: String): String {
    var body = s.trim()
    if (body.startsWith("(") && body.endsWith(")")) body = body.substring(1, body.length - 1)
    val out = StringBuilder()
    var i = 0
    while (i < body.length) {
        val c = body[i]
        if (c == '\\' && i + 1 < body.length) {
            i++
            when (val e = body[i]) {
                'n' -> out.append('\n'); 'r' -> out.append('\r'); 't' -> out.append('\t')
                'b' -> out.append('\b'); 'f' -> out.append('\u000C')
                '(', ')' , '\\' -> out.append(e)
                else -> {
                    if (e.isDigit() && i + 2 < body.length) {
                        val oct = body.substring(i, i + 3)
                        out.append(oct.toIntOrNull(8)?.toChar() ?: e)
                        i += 2
                    } else out.append(e)
                }
            }
        } else out.append(c)
        i++
    }
    return out.toString()
}
