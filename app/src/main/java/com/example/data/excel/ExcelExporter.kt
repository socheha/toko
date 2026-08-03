package com.example.data.excel

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.ExportResult
import com.example.data.model.StockItem
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExporter {

    /**
     * Memperbarui sel nilai stok pada workbook Excel asli berdasarkan smart mapping
     * (lokasiSheet, nomorBaris, kolomStok) tanpa mengubah struktur, format, warna, border,
     * merge cell, atau rumus workbook asli.
     */
    fun exportUpdatedExcel(
        context: Context,
        originalUri: Uri,
        originalFileName: String,
        updatedStockItems: List<StockItem>
    ): ExportResult {
        // Set temp directory
        try {
            System.setProperty("java.io.tmpdir", context.cacheDir.absolutePath)
        } catch (ignored: Throwable) {}

        var tempOriginalFile: File? = null
        try {
            // 1. Salin file original dari Uri ke temp file
            tempOriginalFile = File.createTempFile("export_orig_", ".tmp", context.cacheDir)
            val inputStream = context.contentResolver.openInputStream(originalUri)
                ?: throw IllegalStateException("Tidak dapat membaca file asal dari URI.")

            inputStream.use { input ->
                FileOutputStream(tempOriginalFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 2. Buka workbook dengan Apache POI (Mempertahankan seluruh struktur, style, dsb.)
            val workbook = WorkbookFactory.create(tempOriginalFile)

            var changedItemCount = 0
            var totalStockReduced = 0.0

            // 3. Iterasi seluruh item stok dan perbarui sel yang sesuai
            for (item in updatedStockItems) {
                val sheetName = item.lokasiSheet
                val sheet = workbook.getSheet(sheetName) ?: continue

                // nomorBaris is 1-indexed (Row 1 in Excel = Index 0 in POI)
                val rowIndex = item.nomorBaris - 1
                if (rowIndex < 0) continue

                val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)

                // Ekstrak indeks kolom stok dari string kolomStok (e.g. "Kolom C [Stok]")
                val colIndex = extractColumnIndex(item.kolomStok)
                val cell = row.getCell(colIndex) ?: row.createCell(colIndex)

                val originalCellValue = getCellValueAsDouble(cell)
                val diff = originalCellValue - item.stok

                if (Math.abs(diff) > 0.0001) {
                    changedItemCount++
                    if (diff > 0) {
                        totalStockReduced += diff
                    }
                }

                // Perbarui nilai sel saja (mempertahankan cellStyle)
                cell.setCellValue(item.stok)
            }

            // 4. Tentukan Nama File Baru
            // Format: MULTI_ELEKTRINDO_2026_UPDATE_YYYYMMDD_HHMM.xlsx
            val rawStem = originalFileName.substringBeforeLast(".")
                .replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                .trim('_')
            val fileStem = if (rawStem.isNotBlank() && rawStem != "sample_stock_data") rawStem else "MULTI_ELEKTRINDO_2026"

            val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
            val timestampStr = timestampFormat.format(Date())
            val exportFileName = "${fileStem}_UPDATE_${timestampStr}.xlsx"

            // 5. Simpan ke direktori dokumen ekspor
            val exportDir = File(
                context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
                "ExcelExports"
            )
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val outputFile = File(exportDir, exportFileName)
            FileOutputStream(outputFile).use { fos ->
                workbook.write(fos)
            }
            workbook.close()

            // 6. Buat FileProvider URI untuk sharing / membuka file
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )

            val displayTimeFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale("id", "ID"))
            val displayTimeStr = displayTimeFormat.format(Date())

            return ExportResult(
                filePath = outputFile.absolutePath,
                fileName = exportFileName,
                fileUri = fileUri,
                exportTimestampFormatted = displayTimeStr,
                changedItemCount = changedItemCount,
                totalStockReduced = totalStockReduced,
                totalItemsExported = updatedStockItems.size
            )

        } finally {
            try {
                tempOriginalFile?.delete()
            } catch (ignored: Throwable) {}
        }
    }

    private fun extractColumnIndex(kolomStokStr: String): Int {
        val regexMatch = Regex("""Kolom\s+([A-Z]+)""", RegexOption.IGNORE_CASE).find(kolomStokStr)
        if (regexMatch != null) {
            val letter = regexMatch.groupValues[1]
            return convertColStringToNum(letter)
        }

        val letterMatch = Regex("""\b([A-Z]{1,3})\b""").find(kolomStokStr)
        if (letterMatch != null) {
            return convertColStringToNum(letterMatch.groupValues[1])
        }

        return 2 // default column index
    }

    private fun convertColStringToNum(colStr: String): Int {
        var result = 0
        val uppercase = colStr.uppercase(Locale.ROOT)
        for (char in uppercase) {
            if (char in 'A'..'Z') {
                result = result * 26 + (char - 'A' + 1)
            }
        }
        return if (result > 0) result - 1 else 0
    }

    private fun getCellValueAsDouble(cell: Cell?): Double {
        if (cell == null) return 0.0
        return try {
            when (cell.cellType) {
                CellType.NUMERIC -> cell.numericCellValue
                CellType.STRING -> {
                    val text = cell.stringCellValue.replace(",", ".").replace("[^0-9.]".toRegex(), "")
                    text.toDoubleOrNull() ?: 0.0
                }
                CellType.FORMULA -> {
                    try {
                        cell.numericCellValue
                    } catch (e: Throwable) {
                        0.0
                    }
                }
                else -> 0.0
            }
        } catch (e: Throwable) {
            0.0
        }
    }
}
