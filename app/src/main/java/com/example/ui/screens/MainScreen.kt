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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import com.example.ui.components.ColumnMappingCard
import com.example.ui.components.DashboardSummaryCard
import com.example.ui.components.DraftViewCard
import com.example.ui.components.ExportExcelCard
import com.example.ui.components.FileImportHeader
import com.example.ui.components.InsufficientStockDialog
import com.example.ui.components.KeyboardShortcutsBanner
import com.example.ui.components.ScanHistoryDialog
import com.example.ui.components.ScanReceiptCard
import com.example.ui.components.ScanResultsDialog
import com.example.ui.components.SheetSelectorTab
import com.example.ui.components.StockTableView
import com.example.ui.components.SummaryCards
import com.example.ui.components.TransactionHistoryCard
import com.example.ui.theme.MyApplicationTheme

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

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Auto-load saved draft on screen enter
    LaunchedEffect(Unit) {
        viewModel.loadAutoSavedDraft(context)
    }

    MyApplicationTheme(darkTheme = uiState.isDarkMode) {
        // Keyboard event handling for Desktop/Tablet/Windows Mode
        val keyboardModifier = modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                when (event.key) {
                    Key.Escape -> {
                        viewModel.clearSearchOrDismiss()
                        true
                    }
                    Key.DirectionUp -> {
                        viewModel.moveHighlightUp()
                        true
                    }
                    Key.DirectionDown -> {
                        viewModel.moveHighlightDown(displayedItems.size)
                        true
                    }
                    Key.Enter -> {
                        viewModel.addHighlightedToDraft(context, displayedItems)
                        true
                    }
                    Key.Z -> {
                        if (event.isCtrlPressed || event.isMetaPressed) {
                            viewModel.undoDraftAction(context)
                            true
                        } else false
                    }
                    else -> false
                }
            }

        // Show error message if present
        uiState.errorMessage?.let { error ->
            LaunchedEffect(error) {
                snackbarHostState.showSnackbar("Error: $error")
                viewModel.dismissError()
            }
        }

        // Show snackbar message if present
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

        // Scan Results Dialog (Tahap 6)
        uiState.scanMatchResults?.let { results ->
            ScanResultsDialog(
                matchResults = results,
                allStockItems = displayedItems,
                onSelectStockItem = { index, stock -> viewModel.updateScanMatchSelection(index, stock) },
                onUpdateQuantity = { index, newQty -> viewModel.updateScanItemQuantity(index, newQty) },
                onRemoveItem = { index -> viewModel.removeScanResultItem(index) },
                onApplyToDraft = { viewModel.applyScanResultsToDraft(context) },
                onDismiss = { viewModel.dismissScanResults() }
            )
        }

        // Scan History Dialog (Tahap 6)
        if (uiState.showScanHistoryDialog) {
            ScanHistoryDialog(
                scanRecords = uiState.scanHistoryRecords,
                onDeleteRecord = { id -> viewModel.deleteScanHistoryRecord(id) },
                onDismiss = { viewModel.closeScanHistory() }
            )
        }

        Scaffold(
            modifier = keyboardModifier,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Pencatatan Stok & Kasir",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Text(
                                        text = "Simple",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Kelola Stok, Draft Penjualan, & Scan Nota AI",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        // Theme Toggle Button
                        IconButton(
                            onClick = { viewModel.toggleDarkMode(context) },
                            modifier = Modifier.testTag("action_toggle_theme")
                        ) {
                            Icon(
                                imageVector = if (uiState.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Ganti Tema Mode",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

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
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(Icons.Default.Storefront, contentDescription = "Stok Barang")
                        },
                        label = { Text("Stok Barang") },
                        modifier = Modifier.testTag("tab_stok_barang")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (uiState.draftItems.isNotEmpty()) {
                                        Badge {
                                            Text("${uiState.draftItems.sumOf { it.quantity }}")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Draft Penjualan")
                            }
                        },
                        label = { Text("Draft") },
                        modifier = Modifier.testTag("tab_draft_penjualan")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(Icons.Default.BarChart, contentDescription = "Dashboard")
                        },
                        label = { Text("Dashboard") },
                        modifier = Modifier.testTag("tab_dashboard")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (uiState.scanHistoryRecords.isNotEmpty()) {
                                        Badge {
                                            Text("${uiState.scanHistoryRecords.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = "Scan & File")
                            }
                        },
                        label = { Text("Scan & File") },
                        modifier = Modifier.testTag("tab_scan_file")
                    )
                }
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
                when (selectedTab) {
                    0 -> {
                        // TAB 0: STOK BARANG
                        if (uiState.isLoading) {
                            item { LoadingCard() }
                        }

                        if (uiState.analysisResult?.errorMessage != null) {
                            item { ErrorCard(errorMessage = uiState.analysisResult!!.errorMessage!!) }
                        }

                        if (uiState.analysisResult == null && displayedItems.isEmpty() && !uiState.isLoading) {
                            item { HeroHeaderCard() }
                            item {
                                FileImportHeader(
                                    analysisResult = uiState.analysisResult,
                                    onFileSelected = { uri -> viewModel.importFile(context, uri) },
                                    onLoadSample = { viewModel.loadSampleData(context) },
                                    onClearData = { viewModel.clearData() }
                                )
                            }
                        } else {
                            // Sheet Selector Tabs if multiple sheets
                            uiState.analysisResult?.let { result ->
                                if (result.errorMessage == null && result.sheets.isNotEmpty()) {
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
                            }

                            // Stock Data Table View
                            item {
                                StockTableView(
                                    items = displayedItems,
                                    totalItemsInSheet = uiState.analysisResult?.totalItems ?: displayedItems.size,
                                    searchQuery = uiState.searchQuery,
                                    onSearchQueryChange = { query -> viewModel.updateSearchQuery(query) },
                                    isShowingTop50Only = uiState.showOnlyTop50,
                                    onToggleTop50 = { toggle -> viewModel.toggleTop50(toggle) },
                                    onItemClick = { item -> viewModel.addToDraft(context, item) }
                                )
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: DRAFT PENJUALAN (Dedicated Menu)
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
                    }

                    2 -> {
                        // TAB 2: DASHBOARD & REPORT
                        item {
                            DashboardSummaryCard(
                                analysisResult = uiState.analysisResult,
                                omzetHariIni = uiState.omzetHariIni,
                                transaksiHariIniCount = uiState.transaksiHariIniCount,
                                favoriteItems = uiState.favoriteItems,
                                lowStockItems = uiState.lowStockItems,
                                onAddItemToDraft = { item -> viewModel.addToDraft(context, item) }
                            )
                        }

                        uiState.analysisResult?.let { result ->
                            if (result.errorMessage == null) {
                                item {
                                    SummaryCards(
                                        result = result,
                                        displayedCount = displayedItems.size,
                                        isShowingTop50Only = uiState.showOnlyTop50
                                    )
                                }
                            }
                        }

                        item {
                            TransactionHistoryCard(
                                transactions = uiState.transactions,
                                onUndoLastTransaction = { viewModel.undoLastTransaction(context) }
                            )
                        }
                    }

                    3 -> {
                        // TAB 3: SCAN & FILE MANAGEMENT
                        item {
                            FileImportHeader(
                                analysisResult = uiState.analysisResult,
                                onFileSelected = { uri -> viewModel.importFile(context, uri) },
                                onLoadSample = { viewModel.loadSampleData(context) },
                                onClearData = { viewModel.clearData() }
                            )
                        }

                        item {
                            ScanReceiptCard(
                                isScanning = uiState.isScanningReceipt,
                                scanHistoryCount = uiState.scanHistoryRecords.size,
                                onReceiptCaptured = { bitmap -> viewModel.scanReceiptImage(context, bitmap) },
                                onOpenHistory = { viewModel.openScanHistory() }
                            )
                        }

                        if (uiState.analysisResult != null) {
                            item {
                                ExportExcelCard(
                                    exportResult = uiState.exportResult,
                                    isExporting = uiState.isExporting,
                                    onExportExcel = { viewModel.exportExcel(context) },
                                    onDismissResult = { viewModel.dismissExportResult() }
                                )
                            }

                            val activeSheetSummary = uiState.analysisResult?.sheets?.find {
                                it.sheetName.equals(uiState.selectedSheetName, ignoreCase = true)
                            } ?: uiState.analysisResult?.sheets?.firstOrNull()

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
                        }

                        item {
                            KeyboardShortcutsBanner()
                        }
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
