package com.example.data.export

import android.content.Context
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import java.io.File

/**
 * PDF utilities via pdfbox-android: password removal, merge, and split.
 * All operations are best-effort and return null/empty on failure.
 */
object PdfTools {

    @Volatile private var inited = false

    fun init(context: Context) {
        if (!inited) {
            synchronized(this) {
                if (!inited) {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    inited = true
                }
            }
        }
    }

    /** Removes password protection. Returns the unlocked file, or null if the
     *  password is wrong / file is not a PDF. */
    fun decrypt(input: File, password: String, output: File): File? = try {
        val doc = PDDocument.load(input, password)
        if (doc.isEncrypted) doc.setAllSecurityToBeRemoved(true)
        doc.save(output)
        doc.close()
        output
    } catch (e: Exception) {
        null
    }

    /** Merges multiple PDFs into one. */
    fun merge(inputs: List<File>, output: File): File? = try {
        val merger = PDFMergerUtility()
        inputs.forEach { merger.addSource(it) }
        merger.setDestinationFileName(output.absolutePath)
        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
        output
    } catch (e: Exception) {
        null
    }

    /** Splits a PDF into per-page files (from..to, 1-based). */
    fun split(input: File, from: Int, to: Int, outDir: File): List<File> = try {
        val doc = PDDocument.load(input)
        val splitter = Splitter()
        splitter.setStartPage(from.coerceAtLeast(1))
        splitter.setEndPage(to.coerceAtMost(doc.numberOfPages))
        val parts = splitter.split(doc)
        doc.close()
        val out = mutableListOf<File>()
        parts.forEachIndexed { i, part ->
            val f = File(outDir, "page_${from + i}.pdf")
            part.save(f)
            part.close()
            out.add(f)
        }
        out
    } catch (e: Exception) {
        emptyList()
    }
}
