package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Model data barang hasil pembacaan dari file Excel.
 */
@Entity(tableName = "stock_items")
data class StockItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val kodeBarang: String,
    val namaBarang: String,
    val stok: Double,
    val lokasiSheet: String,
    val nomorBaris: Int, // 1-indexed row number in the sheet
    val kolomStok: String, // Column letter/name e.g. "C" or "Stok"
    val kategori: String = "-",
    val harga: Double = 0.0,
    val satuan: String = "Pcs",
    val importTimestamp: Long = System.currentTimeMillis()
)

/**
 * Informasi kolom pada sheet Excel untuk pemetaan manual.
 */
data class ExcelColumnInfo(
    val index: Int, // 0-indexed column index (0 = A, 1 = B, etc.)
    val letter: String, // "A", "B", "C", etc.
    val headerName: String // "kode", "Nama Barang", "Qty", "Harga", etc.
) {
    val displayLabel: String
        get() = if (headerName.isNotBlank() && headerName != "-") {
            "Kolom $letter • $headerName"
        } else {
            "Kolom $letter"
        }
}

/**
 * Konfigurasi pemetaan kolom manual oleh pengguna.
 */
data class ColumnMapping(
    val kodeColIndex: Int = -1, // -1 means auto-detect
    val namaColIndex: Int = -1,
    val stokColIndex: Int = -1,
    val headerRowIndex: Int = -1, // 0-indexed header row (-1 = auto)
    val startDataRowIndex: Int = -1 // 0-indexed data start row (-1 = auto)
)

/**
 * Ringkasan data untuk setiap sheet yang terdapat dalam workbook Excel.
 */
data class ExcelSheetSummary(
    val sheetName: String,
    val sheetIndex: Int,
    val isStockSheet: Boolean,
    val itemCount: Int,
    val totalStock: Double,
    val stockColumnName: String = "-",
    val stockColumnLetter: String = "-",
    val headerRowIndex: Int = 0,
    val availableColumns: List<ExcelColumnInfo> = emptyList(),
    val currentMapping: ColumnMapping = ColumnMapping()
)

/**
 * Hasil analisis menyeluruh terhadap file workbook Excel yang diimpor.
 */
data class WorkbookAnalysisResult(
    val fileName: String,
    val fileSizeFormatted: String,
    val totalSheets: Int,
    val sheets: List<ExcelSheetSummary>,
    val activeSheetName: String,
    val totalItems: Int,
    val totalStockSum: Double,
    val items: List<StockItem>,
    val errorMessage: String? = null
)

/**
 * Model item barang yang dimasukkan ke dalam Draft Penjualan (Keranjang).
 */
data class DraftItem(
    val stockItemId: Long,
    val kodeBarang: String,
    val namaBarang: String,
    val quantity: Int = 1,
    val stokTersedia: Double = 0.0,
    val harga: Double = 0.0,
    val satuan: String = "Pcs",
    val lokasiSheet: String = ""
)

/**
 * Model data transaksi penjualan yang telah disimpan ke dalam Riwayat Transaksi.
 */
@Entity(tableName = "transaction_records")
data class TransactionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val tanggalFormatted: String, // e.g. "03 Aug 2026"
    val jamFormatted: String,     // e.g. "15:44:12"
    val totalItemCount: Int,      // Total jumlah kuantitas item
    val totalOmzet: Double,       // Omzet ("Total Uang Hari Ini")
    val itemsJson: String         // Serialized JSON of List<DraftItem>
)

/**
 * Hasil ekspor file Excel baru (Tahap 5).
 */
data class ExportResult(
    val filePath: String,
    val fileName: String,
    val fileUri: android.net.Uri,
    val exportTimestampFormatted: String,
    val changedItemCount: Int,
    val totalStockReduced: Double,
    val totalItemsExported: Int
)

/**
 * Model riwayat scan nota AI (Tahap 6).
 */
@Entity(tableName = "scan_records")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imagePath: String,              // Path ke file foto nota di penyimpanan lokal
    val rawOcrJson: String,             // JSON mentah hasil pembacaan OCR Gemini
    val matchedItemsJson: String,        // JSON terstruktur dari hasil pencocokan item
    val timestamp: Long = System.currentTimeMillis(),
    val dateFormatted: String,          // Format tanggal "03 Aug 2026, 15:30"
    val itemCount: Int                  // Jumlah barang yang berhasil terdeteksi
)

/**
 * Item hasil OCR Gemini Vision.
 */
data class ScannedOcrItem(
    val namaBarang: String,
    val jumlah: Int = 1
)

/**
 * Hasil pencocokan item nota hasil OCR dengan data barang Excel.
 */
data class ScanMatchResultItem(
    val rawOcrName: String,
    val quantity: Int,
    val selectedStockItem: StockItem?,
    val candidateStockItems: List<StockItem> = emptyList(),
    val isUserConfirmed: Boolean = true
)

