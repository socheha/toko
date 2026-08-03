package com.example.data.excel

import android.content.Context
import android.net.Uri
import com.example.data.model.ColumnMapping
import com.example.data.model.ExcelColumnInfo
import com.example.data.model.ExcelSheetSummary
import com.example.data.model.StockItem
import com.example.data.model.WorkbookAnalysisResult
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object ExcelParser {

    private val dataFormatter = DataFormatter(Locale.getDefault())

    /**
     * Membaca workbook Excel dari URI dengan menyalin ke temp file terlebih dahulu
     * untuk mencegah OutOfMemoryError / Stream Corruption / Crashes pada Android.
     */
    fun parseExcelWorkbook(
        context: Context,
        uri: Uri,
        fileName: String,
        fileSizeFormatted: String,
        customMappings: Map<String, ColumnMapping> = emptyMap()
    ): WorkbookAnalysisResult {
        try {
            System.setProperty("java.io.tmpdir", context.cacheDir.absolutePath)
        } catch (ignored: Throwable) {}

        var tempFile: File? = null
        var workbook: Workbook? = null

        try {
            tempFile = File.createTempFile("excel_import_", ".tmp", context.cacheDir)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return WorkbookAnalysisResult(
                    fileName = fileName,
                    fileSizeFormatted = fileSizeFormatted,
                    totalSheets = 0,
                    sheets = emptyList(),
                    activeSheetName = "-",
                    totalItems = 0,
                    totalStockSum = 0.0,
                    items = emptyList(),
                    errorMessage = "Gagal membuka file Excel. Stream input tidak dapat diakses."
                )

            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            workbook = WorkbookFactory.create(tempFile)
            val totalSheets = workbook.numberOfSheets

            if (totalSheets == 0) {
                return WorkbookAnalysisResult(
                    fileName = fileName,
                    fileSizeFormatted = fileSizeFormatted,
                    totalSheets = 0,
                    sheets = emptyList(),
                    activeSheetName = "-",
                    totalItems = 0,
                    totalStockSum = 0.0,
                    items = emptyList(),
                    errorMessage = "Workbook Excel tidak memiliki sheet data."
                )
            }

            val sheetSummaries = mutableListOf<ExcelSheetSummary>()
            val allParsedItemsBySheet = mutableMapOf<String, List<StockItem>>()

            for (i in 0 until totalSheets) {
                val sheet = workbook.getSheetAt(i)
                val sheetName = sheet.sheetName ?: "Sheet${i + 1}"
                val userMapping = customMappings[sheetName]
                val (items, summary) = parseSheetData(sheet, i, userMapping)
                sheetSummaries.add(summary)
                allParsedItemsBySheet[sheetName] = items
            }

            // Pilih sheet dengan data stok terbanyak sebagai sheet aktif utama jika belum ada yang terpilih
            val activeSummary = sheetSummaries.maxByOrNull { it.itemCount }
                ?: sheetSummaries.first()

            val activeItems = allParsedItemsBySheet[activeSummary.sheetName] ?: emptyList()
            val totalStockSum = activeItems.sumOf { it.stok }

            return WorkbookAnalysisResult(
                fileName = fileName,
                fileSizeFormatted = fileSizeFormatted,
                totalSheets = totalSheets,
                sheets = sheetSummaries,
                activeSheetName = activeSummary.sheetName,
                totalItems = activeItems.size,
                totalStockSum = totalStockSum,
                items = activeItems,
                errorMessage = if (activeItems.isEmpty()) "Tidak ditemukan data stok barang dalam sheet ini. Silakan atur 'Pemetaan Kolom' di bawah." else null
            )

        } catch (e: Throwable) {
            e.printStackTrace()
            return WorkbookAnalysisResult(
                fileName = fileName,
                fileSizeFormatted = fileSizeFormatted,
                totalSheets = 0,
                sheets = emptyList(),
                activeSheetName = "-",
                totalItems = 0,
                totalStockSum = 0.0,
                items = emptyList(),
                errorMessage = "Gagal membaca file Excel: ${e.localizedMessage ?: e.javaClass.simpleName}"
            )
        } finally {
            try {
                workbook?.close()
            } catch (ignored: Throwable) {}
            try {
                tempFile?.delete()
            } catch (ignored: Throwable) {}
        }
    }

    /**
     * Membaca data dari spesifik sheet dengan dukungan pemetaan kolom custom.
     */
    fun parseSingleSheet(
        context: Context,
        uri: Uri,
        targetSheetName: String,
        customMapping: ColumnMapping? = null
    ): Pair<List<StockItem>, ExcelSheetSummary?> {
        try {
            System.setProperty("java.io.tmpdir", context.cacheDir.absolutePath)
        } catch (ignored: Throwable) {}

        var tempFile: File? = null
        var workbook: Workbook? = null
        try {
            tempFile = File.createTempFile("excel_sheet_", ".tmp", context.cacheDir)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return Pair(emptyList(), null)
            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            workbook = WorkbookFactory.create(tempFile)
            val sheet = workbook.getSheet(targetSheetName) ?: return Pair(emptyList(), null)
            val sheetIdx = workbook.getSheetIndex(sheet)
            val (items, summary) = parseSheetData(sheet, sheetIdx, customMapping)
            return Pair(items, summary)
        } catch (e: Throwable) {
            e.printStackTrace()
            return Pair(emptyList(), null)
        } finally {
            try {
                workbook?.close()
            } catch (ignored: Throwable) {}
            try {
                tempFile?.delete()
            } catch (ignored: Throwable) {}
        }
    }

    private fun parseSheetData(
        sheet: Sheet,
        sheetIdx: Int,
        customMapping: ColumnMapping? = null
    ): Pair<List<StockItem>, ExcelSheetSummary> {
        val sheetName = sheet.sheetName ?: "Sheet${sheetIdx + 1}"
        val firstRowNum = sheet.firstRowNum
        val lastRowNum = sheet.lastRowNum

        val availableColumns = extractAvailableColumns(sheet)

        if (lastRowNum < 0 || firstRowNum > lastRowNum) {
            return Pair(
                emptyList(),
                ExcelSheetSummary(
                    sheetName = sheetName,
                    sheetIndex = sheetIdx,
                    isStockSheet = false,
                    itemCount = 0,
                    totalStock = 0.0,
                    availableColumns = availableColumns,
                    currentMapping = customMapping ?: ColumnMapping()
                )
            )
        }

        // Auto-detect header row dan kolom
        var autoHeaderRowIndex = -1
        var autoKodeColIdx = -1
        var autoNamaColIdx = -1
        var autoStokColIdx = -1
        var autoSatuanColIdx = -1
        var autoHargaColIdx = -1
        var autoKategoriColIdx = -1
        var autoStockColumnHeader = "Stok"

        val maxHeaderScan = minOf(firstRowNum + 25, lastRowNum)
        for (r in firstRowNum..maxHeaderScan) {
            val row = sheet.getRow(r) ?: continue
            val indices = detectColumnIndices(row)

            if (indices.namaIdx != -1 && indices.stokIdx != -1) {
                autoHeaderRowIndex = r
                autoKodeColIdx = indices.kodeIdx
                autoNamaColIdx = indices.namaIdx
                autoStokColIdx = indices.stokIdx
                autoSatuanColIdx = indices.satuanIdx
                autoHargaColIdx = indices.hargaIdx
                autoKategoriColIdx = indices.kategoriIdx
                autoStockColumnHeader = indices.stockHeaderName
                break
            }
        }

        // Fallback jika tidak ditemukan header berpasangan
        if (autoHeaderRowIndex == -1) {
            for (r in firstRowNum..maxHeaderScan) {
                val row = sheet.getRow(r) ?: continue
                val indices = detectColumnIndices(row)
                if (indices.stokIdx != -1 || indices.namaIdx != -1) {
                    autoHeaderRowIndex = r
                    autoKodeColIdx = indices.kodeIdx
                    autoNamaColIdx = if (indices.namaIdx != -1) indices.namaIdx else 1
                    autoStokColIdx = if (indices.stokIdx != -1) indices.stokIdx else 2
                    autoStockColumnHeader = indices.stockHeaderName
                    break
                }
            }
        }

        if (autoHeaderRowIndex == -1) {
            autoHeaderRowIndex = firstRowNum
        }

        // Tentukan kolom akhir yang dipakai (Prioritas: customMapping dari pengguna > auto-detected)
        val finalKodeColIdx = customMapping?.kodeColIndex?.takeIf { it >= 0 } ?: autoKodeColIdx
        val finalNamaColIdx = customMapping?.namaColIndex?.takeIf { it >= 0 } ?: autoNamaColIdx
        val finalStokColIdx = customMapping?.stokColIndex?.takeIf { it >= 0 } ?: autoStokColIdx
        val finalStartRow = customMapping?.startDataRowIndex?.takeIf { it >= 0 }
            ?: (autoHeaderRowIndex + 1)

        val activeStockColIdx = if (finalStokColIdx >= 0) finalStokColIdx else 2
        val stockColLetter = convertNumToColString(activeStockColIdx)

        val stockColumnHeader = availableColumns.find { it.index == activeStockColIdx }?.headerName
            ?.takeIf { it.isNotBlank() } ?: autoStockColumnHeader

        val effectiveMapping = ColumnMapping(
            kodeColIndex = finalKodeColIdx,
            namaColIndex = finalNamaColIdx,
            stokColIndex = finalStokColIdx,
            headerRowIndex = autoHeaderRowIndex,
            startDataRowIndex = finalStartRow
        )

        val items = mutableListOf<StockItem>()

        // Mulai membaca data dari baris awal data
        if (finalStartRow <= lastRowNum) {
            for (r in finalStartRow..lastRowNum) {
                val row = sheet.getRow(r) ?: continue

                val namaVal = if (finalNamaColIdx >= 0) getCellValueAsString(row.getCell(finalNamaColIdx)) else ""
                val stokCell = if (finalStokColIdx >= 0) row.getCell(finalStokColIdx) else null
                val stokVal = getCellValueAsDouble(stokCell)

                // Skip baris yang benar-benar kosong
                if (namaVal.isBlank() && (stokCell == null || stokCell.cellType == CellType.BLANK)) {
                    val allCellsBlank = (0 until row.lastCellNum).all { c ->
                        row.getCell(c)?.cellType == CellType.BLANK || getCellValueAsString(row.getCell(c)).isBlank()
                    }
                    if (allCellsBlank) continue
                }

                val kodeVal = if (finalKodeColIdx >= 0) getCellValueAsString(row.getCell(finalKodeColIdx)) else ""
                val finalKode = if (kodeVal.isNotBlank()) kodeVal else "KDB-${String.format("%04d", r + 1)}"
                val finalNama = if (namaVal.isNotBlank()) namaVal else "Barang Baris ${r + 1}"

                val satuanVal = if (autoSatuanColIdx >= 0) getCellValueAsString(row.getCell(autoSatuanColIdx)) else "Pcs"
                val hargaVal = if (autoHargaColIdx >= 0) getCellValueAsDouble(row.getCell(autoHargaColIdx)) else 0.0
                val katVal = if (autoKategoriColIdx >= 0) getCellValueAsString(row.getCell(autoKategoriColIdx)) else "Umum"

                val item = StockItem(
                    kodeBarang = finalKode,
                    namaBarang = finalNama,
                    stok = stokVal,
                    lokasiSheet = sheetName,
                    nomorBaris = r + 1, // 1-indexed
                    kolomStok = "Kolom $stockColLetter [$stockColumnHeader]",
                    kategori = if (katVal.isNotBlank()) katVal else "Umum",
                    harga = hargaVal,
                    satuan = if (satuanVal.isNotBlank()) satuanVal else "Pcs"
                )
                items.add(item)
            }
        }

        val totalStock = items.sumOf { it.stok }
        val summary = ExcelSheetSummary(
            sheetName = sheetName,
            sheetIndex = sheetIdx,
            isStockSheet = items.isNotEmpty(),
            itemCount = items.size,
            totalStock = totalStock,
            stockColumnName = stockColumnHeader,
            stockColumnLetter = stockColLetter,
            headerRowIndex = autoHeaderRowIndex + 1,
            availableColumns = availableColumns,
            currentMapping = effectiveMapping
        )

        return Pair(items, summary)
    }

    private fun extractAvailableColumns(sheet: Sheet): List<ExcelColumnInfo> {
        val columns = mutableListOf<ExcelColumnInfo>()
        var maxColIndex = -1

        val firstRowNum = sheet.firstRowNum
        val lastRowNum = sheet.lastRowNum
        if (lastRowNum < 0) return emptyList()

        val scanLimit = minOf(firstRowNum + 20, lastRowNum)
        for (r in firstRowNum..scanLimit) {
            val row = sheet.getRow(r) ?: continue
            if (row.lastCellNum > maxColIndex) {
                maxColIndex = row.lastCellNum.toInt()
            }
        }

        if (maxColIndex <= 0) maxColIndex = 10 // default 10 columns A-J

        for (colIdx in 0 until maxColIndex) {
            val colLetter = convertNumToColString(colIdx)
            var headerName = ""

            // Cari nama header terbaik dari 15 baris pertama di kolom ini
            for (r in firstRowNum..scanLimit) {
                val cell = sheet.getRow(r)?.getCell(colIdx) ?: continue
                val strVal = getCellValueAsString(cell)
                if (strVal.isNotBlank() && strVal.length < 50 && !strVal.matches("^[0-9.,\\-]+$".toRegex())) {
                    headerName = strVal
                    break
                }
            }

            columns.add(
                ExcelColumnInfo(
                    index = colIdx,
                    letter = colLetter,
                    headerName = headerName
                )
            )
        }

        return columns
    }

    private data class ColumnIndices(
        val kodeIdx: Int,
        val namaIdx: Int,
        val stokIdx: Int,
        val satuanIdx: Int,
        val hargaIdx: Int,
        val kategoriIdx: Int,
        val stockHeaderName: String
    )

    private fun detectColumnIndices(row: Row): ColumnIndices {
        var kIdx = -1
        var nIdx = -1
        var sIdx = -1
        var satIdx = -1
        var hIdx = -1
        var katIdx = -1
        var sName = "Stok"

        val lastCell = row.lastCellNum
        for (c in 0 until lastCell) {
            val cell = row.getCell(c) ?: continue
            val cellText = getCellValueAsString(cell).lowercase(Locale.getDefault()).trim()
            if (cellText.isBlank()) continue

            when {
                // Priority 1 for Stock: Qty / Quantity (Seperti pada screenshot: Col E = Qty)
                sIdx == -1 && (cellText == "qty" || cellText.startsWith("qty") || cellText.contains("quantity")) -> {
                    sIdx = c
                    sName = getCellValueAsString(cell)
                }
                // Priority 2 for Stock: Stok / Stock / Kuantitas / Sisa / Fisik / Saldo
                sIdx == -1 && (cellText.contains("stok") || cellText.contains("stock") ||
                        cellText.contains("kuantitas") || cellText.contains("sisa") ||
                        cellText.contains("fisik") || cellText.contains("saldo")) -> {
                    sIdx = c
                    sName = getCellValueAsString(cell)
                }
                // Nama Barang
                nIdx == -1 && (cellText.contains("nama") || cellText.contains("deskripsi") ||
                        cellText.contains("description") || cellText.contains("item") ||
                        cellText.contains("produk") || cellText.contains("product") ||
                        cellText.contains("barang")) -> {
                    nIdx = c
                }
                // Kode Barang
                kIdx == -1 && (cellText.contains("kode") || cellText.contains("code") ||
                        cellText.contains("sku") || cellText.contains("barcode") ||
                        cellText.contains("part no") || cellText == "id" ||
                        cellText.contains("no. barang") || cellText == "no") -> {
                    kIdx = c
                }
                // Satuan
                satIdx == -1 && (cellText.contains("satuan") || cellText.contains("unit") ||
                        cellText.contains("uom")) -> {
                    satIdx = c
                }
                // Harga Satuan
                hIdx == -1 && (cellText.contains("harga") || cellText.contains("price") ||
                        cellText.contains("cost")) -> {
                    hIdx = c
                }
                // Kategori
                katIdx == -1 && (cellText.contains("kategori") || cellText.contains("category") ||
                        cellText.contains("kelompok") || cellText.contains("jenis")) -> {
                    katIdx = c
                }
            }
        }

        // Secondary check for Stock if Qty/Stok not found: "jumlah"
        if (sIdx == -1) {
            for (c in 0 until lastCell) {
                val cell = row.getCell(c) ?: continue
                val cellText = getCellValueAsString(cell).lowercase(Locale.getDefault()).trim()
                if (cellText.contains("jumlah")) {
                    sIdx = c
                    sName = getCellValueAsString(cell)
                    break
                }
            }
        }

        return ColumnIndices(kIdx, nIdx, sIdx, satIdx, hIdx, katIdx, sName)
    }

    private fun getCellValueAsString(cell: Cell?): String {
        if (cell == null) return ""
        return try {
            when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue.trim()
                CellType.NUMERIC -> {
                    val formatted = dataFormatter.formatCellValue(cell).trim()
                    if (formatted.endsWith(".0")) formatted.substringBefore(".0") else formatted
                }
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.FORMULA -> {
                    try {
                        when (cell.cachedFormulaResultType) {
                            CellType.NUMERIC -> {
                                val v = cell.numericCellValue
                                if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
                            }
                            CellType.STRING -> cell.stringCellValue.trim()
                            CellType.BOOLEAN -> cell.booleanCellValue.toString()
                            else -> dataFormatter.formatCellValue(cell).trim()
                        }
                    } catch (e: Throwable) {
                        dataFormatter.formatCellValue(cell).trim()
                    }
                }
                else -> ""
            }
        } catch (e: Throwable) {
            ""
        }
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
                        val str = cell.stringCellValue.replace(",", ".").replace("[^0-9.]".toRegex(), "")
                        str.toDoubleOrNull() ?: 0.0
                    }
                }
                else -> 0.0
            }
        } catch (e: Throwable) {
            0.0
        }
    }

    private fun convertNumToColString(colIndex: Int): String {
        var excelCol = ""
        var col = colIndex
        while (col >= 0) {
            val rem = col % 26
            excelCol = (('A'.code + rem).toChar()) + excelCol
            col = (col / 26) - 1
        }
        return if (excelCol.isEmpty()) "A" else excelCol
    }
}
