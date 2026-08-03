package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.StockItem
import com.example.ui.StockUiState
import com.example.ui.StockViewModel
import com.example.ui.components.ColumnMappingCard
import com.example.ui.components.DraftViewCard
import com.example.ui.components.FileImportHeader
import com.example.ui.components.InsufficientStockDialog
import com.example.ui.components.SheetSelectorTab
import com.example.ui.components.StockTableView
import com.example.ui.components.SummaryCards
import com.example.ui.components.TransactionHistoryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: StockViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val displayedItems by viewModel.displayedItems.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Auto-load saved draft on screen enter
    LaunchedEffect(Unit) {
        viewModel.loadAutoSavedDraft(context)
    }

    // Show error message if present
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar("Error: $error")
            viewModel.dismissError()
        }
    }

    // Show snackbar message if present (e.g. "+1 barang masuk ke draft" or "Transaksi berhasil disimpan")
    uiState.snackbarMessage?.let { msg ->
        LaunchedEffect(msg) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    // Insufficient Stock Alert Dialog
    uiState.insufficientStockProblemItems?.let { problemItems ->
        InsufficientStockDialog(
            problemItems = problemItems,
            onDismiss = { viewModel.dismissInsufficientStockDialog() }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Pencatatan Stok & Transaksi",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    text = "Tahap 4",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Simpan Transaksi, Pengurangan Stok & Riwayat",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (uiState.analysisResult != null) {
                        IconButton(
                            onClick = { viewModel.clearData() },
                            modifier = Modifier.testTag("action_reset_data")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Data",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Artwork (when no file loaded yet)
            if (uiState.analysisResult == null && !uiState.isLoading) {
                item {
                    HeroHeaderCard()
                }
            }

            // 2. File Upload Dropzone Header
            item {
                FileImportHeader(
                    analysisResult = uiState.analysisResult,
                    onFileSelected = { uri -> viewModel.importFile(context, uri) },
                    onLoadSample = { viewModel.loadSampleData(context) },
                    onClearData = { viewModel.clearData() }
                )
            }

            // 3. Loading Indicator
            if (uiState.isLoading) {
                item {
                    LoadingCard()
                }
            }

            // 4. Error Card if file read failed
            if (uiState.analysisResult?.errorMessage != null) {
                item {
                    ErrorCard(errorMessage = uiState.analysisResult!!.errorMessage!!)
                }
            }

            // 5. Analysis Result Summary, Draft, Riwayat & Table
            uiState.analysisResult?.let { result ->
                if (result.errorMessage == null) {
                    // Summary Metrics Cards (Jumlah barang, Nama sheet, Total stok, Preview info)
                    item {
                        SummaryCards(
                            result = result,
                            displayedCount = displayedItems.size,
                            isShowingTop50Only = uiState.showOnlyTop50
                        )
                    }

                    // Draft Penjualan Card with Total Uang Hari Ini & Simpan Transaksi Button
                    item {
                        DraftViewCard(
                            draftItems = uiState.draftItems,
                            canUndo = uiState.draftHistory.isNotEmpty(),
                            totalUangHariIniInput = uiState.totalUangHariIniInput,
                            onTotalUangHariIniChange = { input -> viewModel.updateTotalUangHariIniInput(input) },
                            onIncrement = { item -> viewModel.incrementDraftQuantity(context, item) },
                            onDecrement = { item -> viewModel.decrementDraftQuantity(context, item) },
                            onRemove = { item -> viewModel.removeDraftItem(context, item) },
                            onUndo = { viewModel.undoDraftAction(context) },
                            onClearAll = { viewModel.clearDraft(context) },
                            onSaveTransaction = { viewModel.saveTransaction(context) },
                            isSaving = uiState.isSavingTransaction
                        )
                    }

                    // Riwayat Transaksi Card
                    item {
                        TransactionHistoryCard(
                            transactions = uiState.transactions,
                            onUndoLastTransaction = { viewModel.undoLastTransaction(context) }
                        )
                    }

                    // Sheet Selector Tabs if multiple sheets
                    if (result.sheets.isNotEmpty()) {
                        item {
                            SheetSelectorTab(
                                sheets = result.sheets,
                                selectedSheetName = uiState.selectedSheetName,
                                onSheetSelected = { sheetName ->
                                    viewModel.selectSheet(context, sheetName)
                                }
                            )
                        }
                    }

                    // Interactive Column Mapping Selector Card
                    val activeSheetSummary = result.sheets.find {
                        it.sheetName.equals(uiState.selectedSheetName, ignoreCase = true)
                    } ?: result.sheets.firstOrNull()

                    if (activeSheetSummary != null && activeSheetSummary.availableColumns.isNotEmpty()) {
                        item {
                            ColumnMappingCard(
                                sheetSummary = activeSheetSummary,
                                onMappingChanged = { newMapping ->
                                    viewModel.updateColumnMapping(context, newMapping)
                                }
                            )
                        }
                    }

                    // Stock Data Table
                    item {
                        StockTableView(
                            items = displayedItems,
                            totalItemsInSheet = result.totalItems,
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChange = { query -> viewModel.updateSearchQuery(query) },
                            isShowingTop50Only = uiState.showOnlyTop50,
                            onToggleTop50 = { toggle -> viewModel.toggleTop50(toggle) },
                            onItemClick = { item -> viewModel.addToDraft(context, item) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun HeroHeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_excel_hero_1785768213722),
                    contentDescription = "Excel Import Hero Artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sistem Impor Stok Excel Otomatis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Aplikasi akan otomatis membaca workbook Excel Anda, mencari sheet stok barang, mengekstrak data kode, nama, jumlah stok, lokasi sheet, serta nomor baris secara presisi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Membaca Workbook Excel...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Menganalisis sheet, mendeteksi kolom stok & mengekstrak baris data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(errorMessage: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gagal Membaca File Excel",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
