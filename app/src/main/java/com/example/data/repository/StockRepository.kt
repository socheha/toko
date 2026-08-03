package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.excel.ExcelParser
import com.example.data.excel.SampleExcelGenerator
import com.example.data.local.DraftPersistenceManager
import com.example.data.local.StockDao
import com.example.data.model.DraftItem
import com.example.data.model.StockItem
import com.example.data.model.TransactionRecord
import com.example.data.model.WorkbookAnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class TransactionResult {
    data class Success(val transaction: TransactionRecord) : TransactionResult()
    data class InsufficientStock(val problemItems: List<DraftItem>) : TransactionResult()
    data class Error(val message: String) : TransactionResult()
}

sealed class UndoResult {
    data class Success(val undoneTransaction: TransactionRecord) : UndoResult()
    data class Error(val message: String) : UndoResult()
}

class StockRepository(private val stockDao: StockDao) {

    val allStockItems: Flow<List<StockItem>> = stockDao.getAllItems()
    val allTransactions: Flow<List<TransactionRecord>> = stockDao.getAllTransactions()

    fun getTop50ItemsBySheet(sheetName: String): Flow<List<StockItem>> =
        stockDao.getTop50ItemsBySheet(sheetName)

    fun getAllItemsBySheet(sheetName: String): Flow<List<StockItem>> =
        stockDao.getAllItemsBySheet(sheetName)

    fun searchItems(query: String): Flow<List<StockItem>> =
        stockDao.searchItems(query)

    suspend fun executeSaveTransaction(
        context: Context,
        draftItems: List<DraftItem>,
        totalOmzet: Double
    ): TransactionResult = withContext(Dispatchers.IO) {
        if (draftItems.isEmpty()) {
            return@withContext TransactionResult.Error("Draft penjualan masih kosong.")
        }

        // 1. Validasi kecukupan stok seluruh barang
        val problemItems = mutableListOf<DraftItem>()
        for (draftItem in draftItems) {
            val currentDbItem = if (draftItem.stockItemId > 0) {
                stockDao.getItemById(draftItem.stockItemId)
            } else {
                stockDao.getItemByKode(draftItem.kodeBarang)
            }

            val availableStock = currentDbItem?.stok ?: draftItem.stokTersedia
            if (draftItem.quantity > availableStock) {
                problemItems.add(draftItem.copy(stokTersedia = availableStock))
            }
        }

        // 2. Jika ada yang kurang stok -> Batalkan dan kembalikan daftar bermasalah
        if (problemItems.isNotEmpty()) {
            return@withContext TransactionResult.InsufficientStock(problemItems)
        }

        // 3. Stok cukup -> Kurangi stok & Simpan ke Riwayat Transaksi
        for (draftItem in draftItems) {
            if (draftItem.stockItemId > 0) {
                stockDao.reduceStockById(draftItem.stockItemId, draftItem.quantity.toDouble())
            } else {
                stockDao.reduceStockByKode(draftItem.kodeBarang, draftItem.quantity.toDouble())
            }
        }

        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale("id", "ID"))

        val dateStr = dateFormat.format(Date(now))
        val timeStr = timeFormat.format(Date(now))

        val itemsJson = DraftPersistenceManager.serializeDraftItems(draftItems)
        val totalQtySum = draftItems.sumOf { it.quantity }

        val record = TransactionRecord(
            timestamp = now,
            tanggalFormatted = dateStr,
            jamFormatted = timeStr,
            totalItemCount = totalQtySum,
            totalOmzet = totalOmzet,
            itemsJson = itemsJson
        )

        val insertedId = stockDao.insertTransaction(record)
        DraftPersistenceManager.clearDraft(context)

        TransactionResult.Success(record.copy(id = insertedId))
    }

    suspend fun executeUndoLastTransaction(context: Context): UndoResult = withContext(Dispatchers.IO) {
        val lastTx = stockDao.getLastTransaction()
            ?: return@withContext UndoResult.Error("Belum ada transaksi di riwayat yang dapat dibatalkan.")

        val items = DraftPersistenceManager.deserializeDraftItems(lastTx.itemsJson)

        // Kembalikan stok barang
        for (item in items) {
            if (item.stockItemId > 0) {
                stockDao.restoreStockById(item.stockItemId, item.quantity.toDouble())
            } else {
                stockDao.restoreStockByKode(item.kodeBarang, item.quantity.toDouble())
            }
        }

        // Hapus transaksi terakhir dari DB
        stockDao.deleteTransaction(lastTx.id)

        UndoResult.Success(lastTx)
    }

    suspend fun processExcelImport(
        context: Context,
        uri: Uri,
        fileName: String,
        fileSizeFormatted: String
    ): WorkbookAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val result = ExcelParser.parseExcelWorkbook(context, uri, fileName, fileSizeFormatted)
            if (result.items.isNotEmpty()) {
                stockDao.clearAll()
                stockDao.insertAll(result.items)
            }
            result
        } catch (e: Throwable) {
            e.printStackTrace()
            WorkbookAnalysisResult(
                fileName = fileName,
                fileSizeFormatted = fileSizeFormatted,
                totalSheets = 0,
                sheets = emptyList(),
                activeSheetName = "-",
                totalItems = 0,
                totalStockSum = 0.0,
                items = emptyList(),
                errorMessage = "Gagal memproses file Excel: ${e.localizedMessage ?: e.javaClass.simpleName}"
            )
        }
    }

    suspend fun switchActiveSheet(
        context: Context,
        uri: Uri,
        sheetName: String,
        mapping: com.example.data.model.ColumnMapping? = null
    ): Pair<List<StockItem>, com.example.data.model.ExcelSheetSummary?> = withContext(Dispatchers.IO) {
        val (newItems, summary) = ExcelParser.parseSingleSheet(context, uri, sheetName, mapping)
        if (newItems.isNotEmpty()) {
            stockDao.clearAll()
            stockDao.insertAll(newItems)
        }
        Pair(newItems, summary)
    }

    suspend fun generateAndImportSample(context: Context): WorkbookAnalysisResult = withContext(Dispatchers.IO) {
        val (sampleUri, fileName) = SampleExcelGenerator.createSampleStockExcel(context)
        processExcelImport(context, sampleUri, fileName, "~15 KB")
    }

    suspend fun clearCurrentData() = withContext(Dispatchers.IO) {
        stockDao.clearAll()
    }
}

