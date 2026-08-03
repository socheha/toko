package com.example.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DraftPersistenceManager
import com.example.data.model.DraftItem
import com.example.data.model.StockItem
import com.example.data.model.TransactionRecord
import com.example.data.model.WorkbookAnalysisResult
import com.example.data.repository.StockRepository
import com.example.data.repository.TransactionResult
import com.example.data.repository.UndoResult
import com.example.ui.util.FuzzySearchEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StockUiState(
    val isLoading: Boolean = false,
    val isSavingTransaction: Boolean = false,
    val analysisResult: WorkbookAnalysisResult? = null,
    val searchQuery: String = "",
    val selectedSheetName: String = "",
    val showOnlyTop50: Boolean = true,
    val currentUri: Uri? = null,
    val errorMessage: String? = null,
    val draftItems: List<DraftItem> = emptyList(),
    val draftHistory: List<List<DraftItem>> = emptyList(),
    val totalUangHariIniInput: String = "",
    val transactions: List<TransactionRecord> = emptyList(),
    val insufficientStockProblemItems: List<DraftItem>? = null,
    val snackbarMessage: String? = null
)

class StockViewModel(private val repository: StockRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(StockUiState())
    val uiState: StateFlow<StockUiState> = _uiState.asStateFlow()

    init {
        // Collect transactions reactively from database
        viewModelScope.launch {
            repository.allTransactions.collect { txList ->
                _uiState.update { it.copy(transactions = txList) }
            }
        }
    }

    /**
     * Memuat auto-saved draft dari SharedPreferences jika browser/aplikasi dibuka kembali.
     */
    fun loadAutoSavedDraft(context: Context) {
        if (_uiState.value.draftItems.isEmpty()) {
            val savedDraft = DraftPersistenceManager.loadDraft(context)
            if (savedDraft.isNotEmpty()) {
                _uiState.update { it.copy(draftItems = savedDraft) }
            }
        }
    }

    private fun persistDraft(context: Context, items: List<DraftItem>) {
        DraftPersistenceManager.saveDraft(context, items)
    }

    // Observe database items reactively with Fuzzy Search Engine (Relevance Scoring & Max 20 results)
    val displayedItems: StateFlow<List<StockItem>> = combine(
        repository.allStockItems,
        _uiState
    ) { items, state ->
        val filteredBySheet = items.filter { item ->
            if (state.selectedSheetName.isBlank()) true
            else item.lokasiSheet.equals(state.selectedSheetName, ignoreCase = true)
        }

        if (state.searchQuery.isNotBlank()) {
            // Fuzzy search dengan relevance scoring & maksimal 20 hasil kecocokan terbaik
            FuzzySearchEngine.search(
                items = filteredBySheet,
                query = state.searchQuery,
                maxResults = 20
            )
        } else {
            if (state.showOnlyTop50 && filteredBySheet.size > 50) {
                filteredBySheet.take(50)
            } else {
                filteredBySheet
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun importFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val (fileName, fileSizeStr) = getFileInfo(context, uri)
            val result = repository.processExcelImport(context, uri, fileName, fileSizeStr)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    analysisResult = result,
                    selectedSheetName = result.activeSheetName,
                    currentUri = uri,
                    errorMessage = result.errorMessage
                )
            }
        }
    }

    fun loadSampleData(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.generateAndImportSample(context)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    analysisResult = result,
                    selectedSheetName = result.activeSheetName,
                    currentUri = null,
                    errorMessage = result.errorMessage
                )
            }
        }
    }

    fun selectSheet(context: Context, sheetName: String) {
        val currentUri = _uiState.value.currentUri
        val currentResult = _uiState.value.analysisResult

        _uiState.update { it.copy(selectedSheetName = sheetName) }

        if (currentUri != null && currentResult != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                val targetSummary = currentResult.sheets.find { it.sheetName.equals(sheetName, ignoreCase = true) }
                val mapping = targetSummary?.currentMapping
                val (newItems, updatedSummary) = repository.switchActiveSheet(context, currentUri, sheetName, mapping)
                val totalStock = newItems.sumOf { it.stok }

                val updatedSheets = currentResult.sheets.map { sheet ->
                    if (sheet.sheetName.equals(sheetName, ignoreCase = true) && updatedSummary != null) {
                        updatedSummary
                    } else {
                        sheet
                    }
                }

                val updatedResult = currentResult.copy(
                    activeSheetName = sheetName,
                    totalItems = newItems.size,
                    totalStockSum = totalStock,
                    items = newItems,
                    sheets = updatedSheets,
                    errorMessage = if (newItems.isEmpty()) "Tidak ditemukan data pada sheet ini dengan pemetaan saat ini." else null
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        analysisResult = updatedResult,
                        errorMessage = updatedResult.errorMessage
                    )
                }
            }
        }
    }

    fun updateColumnMapping(context: Context, newMapping: com.example.data.model.ColumnMapping) {
        val currentUri = _uiState.value.currentUri
        val currentResult = _uiState.value.analysisResult
        val currentSheet = _uiState.value.selectedSheetName

        if (currentUri != null && currentResult != null && currentSheet.isNotBlank()) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                val (newItems, updatedSummary) = repository.switchActiveSheet(context, currentUri, currentSheet, newMapping)
                val totalStock = newItems.sumOf { it.stok }

                val updatedSheets = currentResult.sheets.map { sheet ->
                    if (sheet.sheetName.equals(currentSheet, ignoreCase = true) && updatedSummary != null) {
                        updatedSummary
                    } else {
                        sheet
                    }
                }

                val updatedResult = currentResult.copy(
                    activeSheetName = currentSheet,
                    totalItems = newItems.size,
                    totalStockSum = totalStock,
                    items = newItems,
                    sheets = updatedSheets,
                    errorMessage = if (newItems.isEmpty()) "Data tidak ditemukan untuk pemetaan kolom ini. Silakan coba pilih kolom lain." else null
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        analysisResult = updatedResult,
                        errorMessage = updatedResult.errorMessage
                    )
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleTop50(showTop50: Boolean) {
        _uiState.update { it.copy(showOnlyTop50 = showTop50) }
    }

    fun updateTotalUangHariIniInput(input: String) {
        _uiState.update { it.copy(totalUangHariIniInput = input) }
    }

    fun dismissInsufficientStockDialog() {
        _uiState.update { it.copy(insufficientStockProblemItems = null) }
    }

    fun clearData() {
        viewModelScope.launch {
            repository.clearCurrentData()
            _uiState.update { StockUiState() }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // --- DRAFT PENJUALAN MANAGEMENT ---

    private fun pushDraftHistory(currentList: List<DraftItem>) {
        val history = _uiState.value.draftHistory.toMutableList()
        history.add(currentList)
        if (history.size > 20) history.removeAt(0) // Keep max 20 undo states
        _uiState.update { it.copy(draftHistory = history) }
    }

    /**
     * Tambahkan barang ke draft.
     * Jika barang yang sama sudah ada di draft, jumlahnya bertambah (+1).
     */
    fun addToDraft(context: Context, item: StockItem) {
        val currentDraft = _uiState.value.draftItems.toMutableList()
        pushDraftHistory(currentDraft.toList())

        val existingIndex = currentDraft.indexOfFirst {
            it.stockItemId == item.id ||
                    (it.kodeBarang.isNotBlank() && it.kodeBarang.equals(item.kodeBarang, ignoreCase = true))
        }

        if (existingIndex >= 0) {
            val existing = currentDraft[existingIndex]
            currentDraft[existingIndex] = existing.copy(quantity = existing.quantity + 1)
        } else {
            currentDraft.add(
                DraftItem(
                    stockItemId = item.id,
                    kodeBarang = item.kodeBarang,
                    namaBarang = item.namaBarang,
                    quantity = 1,
                    stokTersedia = item.stok,
                    harga = item.harga,
                    satuan = item.satuan,
                    lokasiSheet = item.lokasiSheet
                )
            )
        }

        persistDraft(context, currentDraft)

        _uiState.update {
            it.copy(
                draftItems = currentDraft,
                snackbarMessage = "+1 ${item.namaBarang} masuk ke Draft Penjualan"
            )
        }
    }

    /**
     * Tambah jumlah (+1) pada item di draft.
     */
    fun incrementDraftQuantity(context: Context, draftItem: DraftItem) {
        val currentDraft = _uiState.value.draftItems.toMutableList()
        pushDraftHistory(currentDraft.toList())

        val index = currentDraft.indexOfFirst { it.stockItemId == draftItem.stockItemId && it.kodeBarang == draftItem.kodeBarang }
        if (index >= 0) {
            val item = currentDraft[index]
            currentDraft[index] = item.copy(quantity = item.quantity + 1)
            persistDraft(context, currentDraft)
            _uiState.update { it.copy(draftItems = currentDraft) }
        }
    }

    /**
     * Kurangi jumlah (-1) pada item di draft. Jika jumlah menjadi 0, hapus dari draft.
     */
    fun decrementDraftQuantity(context: Context, draftItem: DraftItem) {
        val currentDraft = _uiState.value.draftItems.toMutableList()
        pushDraftHistory(currentDraft.toList())

        val index = currentDraft.indexOfFirst { it.stockItemId == draftItem.stockItemId && it.kodeBarang == draftItem.kodeBarang }
        if (index >= 0) {
            val item = currentDraft[index]
            if (item.quantity > 1) {
                currentDraft[index] = item.copy(quantity = item.quantity - 1)
            } else {
                currentDraft.removeAt(index)
            }
            persistDraft(context, currentDraft)
            _uiState.update { it.copy(draftItems = currentDraft) }
        }
    }

    /**
     * Hapus barang dari draft.
     */
    fun removeDraftItem(context: Context, draftItem: DraftItem) {
        val currentDraft = _uiState.value.draftItems.toMutableList()
        pushDraftHistory(currentDraft.toList())

        val removed = currentDraft.removeAll { it.stockItemId == draftItem.stockItemId && it.kodeBarang == draftItem.kodeBarang }
        if (removed) {
            persistDraft(context, currentDraft)
            _uiState.update {
                it.copy(
                    draftItems = currentDraft,
                    snackbarMessage = "${draftItem.namaBarang} dihapus dari Draft"
                )
            }
        }
    }

    /**
     * Batalkan aksi terakhir pada draft (Undo).
     */
    fun undoDraftAction(context: Context) {
        val history = _uiState.value.draftHistory.toMutableList()
        if (history.isNotEmpty()) {
            val previousState = history.removeAt(history.lastIndex)
            persistDraft(context, previousState)
            _uiState.update {
                it.copy(
                    draftItems = previousState,
                    draftHistory = history,
                    snackbarMessage = "Aksi draft berhasil dibatalkan (Undo)"
                )
            }
        }
    }

    /**
     * Bersihkan seluruh isi draft.
     */
    fun clearDraft(context: Context) {
        val currentDraft = _uiState.value.draftItems
        if (currentDraft.isNotEmpty()) {
            pushDraftHistory(currentDraft)
            DraftPersistenceManager.clearDraft(context)
            _uiState.update {
                it.copy(
                    draftItems = emptyList(),
                    snackbarMessage = "Draft Penjualan telah dikosongkan"
                )
            }
        }
    }

    // --- TAHAP 4: SIMPAN TRANSAKSI & UNDO ---

    fun saveTransaction(context: Context) {
        val draft = _uiState.value.draftItems
        if (draft.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "Draft penjualan masih kosong.") }
            return
        }

        val omzetText = _uiState.value.totalUangHariIniInput.trim()
            .replace(".", "")
            .replace(",", ".")
        val omzetValue = omzetText.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingTransaction = true) }
            val result = repository.executeSaveTransaction(context, draft, omzetValue)
            when (result) {
                is TransactionResult.InsufficientStock -> {
                    _uiState.update {
                        it.copy(
                            isSavingTransaction = false,
                            insufficientStockProblemItems = result.problemItems
                        )
                    }
                }
                is TransactionResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSavingTransaction = false,
                            errorMessage = result.message
                        )
                    }
                }
                is TransactionResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSavingTransaction = false,
                            draftItems = emptyList(),
                            draftHistory = emptyList(),
                            totalUangHariIniInput = "",
                            snackbarMessage = "Transaksi tanggal ${result.transaction.tanggalFormatted} ${result.transaction.jamFormatted} berhasil disimpan! Stok telah berkurang."
                        )
                    }
                }
            }
        }
    }

    fun undoLastTransaction(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.executeUndoLastTransaction(context)
            when (result) {
                is UndoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is UndoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            snackbarMessage = "Transaksi ${result.undoneTransaction.tanggalFormatted} ${result.undoneTransaction.jamFormatted} berhasil dibatalkan dan stok telah dikembalikan."
                        )
                    }
                }
            }
        }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun getFileInfo(context: Context, uri: Uri): Pair<String, String> {
        var name = "File_Excel.xlsx"
        var sizeBytes = 0L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sizeFormatted = when {
            sizeBytes <= 0 -> "Ukuran tidak diketahui"
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
            else -> String.format("%.2f MB", sizeBytes.toDouble() / (1024 * 1024))
        }

        return Pair(name, sizeFormatted)
    }
}

class StockViewModelFactory(private val repository: StockRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StockViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
