package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ResetDataDialog(
    onResetExcel: () -> Unit,
    onResetTransactionHistory: () -> Unit,
    onResetScanHistory: () -> Unit,
    onResetAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var confirmActionType by remember { mutableStateOf<String?>(null) }

    if (confirmActionType != null) {
        val title = when (confirmActionType) {
            "EXCEL" -> "Reset Data Excel & Stok?"
            "TRANSACTIONS" -> "Reset Riwayat Transaksi?"
            "SCAN" -> "Reset Riwayat Scan Nota?"
            "ALL" -> "RESET SEMUA DATA APLIKASI?"
            else -> "Konfirmasi Reset"
        }

        val message = when (confirmActionType) {
            "EXCEL" -> "File Excel aktif & daftar stok barang di database akan dihapus. Lanjutkan?"
            "TRANSACTIONS" -> "Seluruh riwayat transaksi penjualan & omzet harian akan dihapus permanen. Lanjutkan?"
            "SCAN" -> "Seluruh riwayat scan foto nota & pembacaan OCR AI akan dihapus permanen. Lanjutkan?"
            "ALL" -> "PERINGATAN: Seluruh file Excel, stok barang, draft, riwayat transaksi & scan nota akan DIHAPUS TOTAL dari aplikasi. Lanjutkan?"
            else -> "Apakah Anda yakin?"
        }

        AlertDialog(
            onDismissRequest = { confirmActionType = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        when (confirmActionType) {
                            "EXCEL" -> onResetExcel()
                            "TRANSACTIONS" -> onResetTransactionHistory()
                            "SCAN" -> onResetScanHistory()
                            "ALL" -> onResetAll()
                        }
                        confirmActionType = null
                        onDismiss()
                    },
                    modifier = Modifier.testTag("button_confirm_reset"),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Ya, Hapus Data", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmActionType = null }
                ) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Pilihan Reset & Hapus Data",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Pilih kategori data yang ingin Anda reset atau bersihkan:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    ResetOptionCard(
                        title = "Reset Excel & Stok Barang",
                        subtitle = "Hapus file Excel aktif & data stok di database",
                        icon = Icons.Default.TableChart,
                        onClick = { confirmActionType = "EXCEL" },
                        testTag = "option_reset_excel"
                    )

                    ResetOptionCard(
                        title = "Reset Riwayat Transaksi",
                        subtitle = "Hapus seluruh catatan riwayat penjualan & omzet",
                        icon = Icons.Default.ReceiptLong,
                        onClick = { confirmActionType = "TRANSACTIONS" },
                        testTag = "option_reset_transactions"
                    )

                    ResetOptionCard(
                        title = "Reset Riwayat Scan Nota",
                        subtitle = "Hapus seluruh histori foto & pembacaan nota AI",
                        icon = Icons.Default.History,
                        onClick = { confirmActionType = "SCAN" },
                        testTag = "option_reset_scan"
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { confirmActionType = "ALL" }
                            .testTag("option_reset_all"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "RESET SEMUA DATA (TOTAL)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Bersihkan Excel, stok, draft & seluruh riwayat",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Tutup")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun ResetOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
