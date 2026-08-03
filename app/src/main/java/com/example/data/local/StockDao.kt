package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.StockItem
import com.example.data.model.TransactionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stock_items ORDER BY id ASC")
    fun getAllItems(): Flow<List<StockItem>>

    @Query("SELECT * FROM stock_items WHERE lokasiSheet = :sheetName ORDER BY nomorBaris ASC LIMIT 50")
    fun getTop50ItemsBySheet(sheetName: String): Flow<List<StockItem>>

    @Query("SELECT * FROM stock_items WHERE lokasiSheet = :sheetName ORDER BY nomorBaris ASC")
    fun getAllItemsBySheet(sheetName: String): Flow<List<StockItem>>

    @Query("SELECT * FROM stock_items WHERE (kodeBarang LIKE '%' || :query || '%' OR namaBarang LIKE '%' || :query || '%') ORDER BY nomorBaris ASC")
    fun searchItems(query: String): Flow<List<StockItem>>

    @Query("SELECT * FROM stock_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): StockItem?

    @Query("SELECT * FROM stock_items WHERE kodeBarang = :kode LIMIT 1")
    suspend fun getItemByKode(kode: String): StockItem?

    @Query("UPDATE stock_items SET stok = stok - :qty WHERE id = :id")
    suspend fun reduceStockById(id: Long, qty: Double)

    @Query("UPDATE stock_items SET stok = stok - :qty WHERE kodeBarang = :kode")
    suspend fun reduceStockByKode(kode: String, qty: Double)

    @Query("UPDATE stock_items SET stok = stok + :qty WHERE id = :id")
    suspend fun restoreStockById(id: Long, qty: Double)

    @Query("UPDATE stock_items SET stok = stok + :qty WHERE kodeBarang = :kode")
    suspend fun restoreStockByKode(kode: String, qty: Double)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<StockItem>)

    @Query("DELETE FROM stock_items")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM stock_items")
    suspend fun getItemCount(): Int

    @Query("SELECT SUM(stok) FROM stock_items")
    suspend fun getTotalStockSum(): Double?

    // --- TRANSACTION HISTORY DAOS ---
    @Query("SELECT * FROM transaction_records ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transaction_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastTransaction(): TransactionRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionRecord): Long

    @Query("DELETE FROM transaction_records WHERE id = :id")
    suspend fun deleteTransaction(id: Long)
}

