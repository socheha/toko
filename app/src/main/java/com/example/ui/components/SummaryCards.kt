package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.WorkbookAnalysisResult
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SummaryCards(
    result: WorkbookAnalysisResult,
    displayedCount: Int,
    isShowingTop50Only: Boolean,
    modifier: Modifier = Modifier
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.GERMANY)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header Label
        Text(
            text = "RANGKUMAN IMPORT EXCEL",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Jumlah Barang Berhasil Dibaca
            MetricCard(
                title = "Jumlah Barang",
                value = "${numberFormat.format(result.totalItems)} Item",
                subtitle = "Berhasil dibaca",
                icon = Icons.Default.Inventory2,
                accentColor = Color(0xFF0F766E),
                modifier = Modifier
                    .weight(1f)
                    .testTag("summary_card_item_count")
            )

            // 2. Total Stok
            MetricCard(
                title = "Total Stok",
                value = numberFormat.format(result.totalStockSum),
                subtitle = "Total akumulasi stok",
                icon = Icons.Default.Analytics,
                accentColor = Color(0xFF0284C7),
                modifier = Modifier
                    .weight(1f)
                    .testTag("summary_card_total_stock")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 3. Nama Sheet Sumber
            MetricCard(
                title = "Sheet Sumber",
                value = result.activeSheetName,
                subtitle = "${result.totalSheets} Sheet dalam workbook",
                icon = Icons.Default.Description,
                accentColor = Color(0xFF7C3AED),
                modifier = Modifier
                    .weight(1f)
                    .testTag("summary_card_sheet_name")
            )

            // 4. Info Mode Preview
            MetricCard(
                title = "Mode Tampilan",
                value = if (isShowingTop50Only) "Preview 50 Data" else "Semua Data",
                subtitle = "Menampilkan $displayedCount dari ${result.totalItems} item",
                icon = Icons.Default.TableChart,
                accentColor = Color(0xFFD97706),
                modifier = Modifier
                    .weight(1f)
                    .testTag("summary_card_preview_mode")
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                BoxWithIcon(icon = icon, color = accentColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BoxWithIcon(icon: ImageVector, color: Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
    }
}
