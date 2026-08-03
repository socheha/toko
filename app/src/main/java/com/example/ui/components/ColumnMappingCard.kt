package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ColumnMapping
import com.example.data.model.ExcelColumnInfo
import com.example.data.model.ExcelSheetSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnMappingCard(
    sheetSummary: ExcelSheetSummary,
    onMappingChanged: (ColumnMapping) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val currentMapping = sheetSummary.currentMapping
    val columns = sheetSummary.availableColumns

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header clickable row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewColumn,
                            contentDescription = "Pilih Kolom",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Pilih / Atur Pemetaan Kolom",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ) {
                                Text(
                                    text = "Fitur Kolom",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "Ganti kolom Kode, Nama & Stok (Qty) secara manual",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Summary Badges (Always visible)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val kodeLabel = columns.find { it.index == currentMapping.kodeColIndex }?.letter ?: "Auto"
                val namaLabel = columns.find { it.index == currentMapping.namaColIndex }?.letter ?: "Auto"
                val stokLabel = columns.find { it.index == currentMapping.stokColIndex }?.letter ?: "Auto"
                val startRowLabel = if (currentMapping.startDataRowIndex >= 0) "Baris ${currentMapping.startDataRowIndex + 1}" else "Auto"

                MappingBadge(label = "Kode: $kodeLabel", isCustom = currentMapping.kodeColIndex >= 0)
                MappingBadge(label = "Nama: $namaLabel", isCustom = currentMapping.namaColIndex >= 0)
                MappingBadge(label = "Stok/Qty: $stokLabel", isCustom = currentMapping.stokColIndex >= 0)
                MappingBadge(label = "Awal: $startRowLabel", isCustom = currentMapping.startDataRowIndex >= 0)
            }

            // Expandable Dropdown Selectors
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "PILIH KOLOM SUMBER DATA EXCEL:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 1. Dropdown Selector Kode Barang
                    ColumnDropdownPicker(
                        label = "1. Kolom Kode Barang",
                        icon = Icons.Default.QrCode,
                        columns = columns,
                        selectedIndex = currentMapping.kodeColIndex,
                        onColumnSelected = { idx ->
                            onMappingChanged(currentMapping.copy(kodeColIndex = idx))
                        },
                        testTag = "dropdown_select_kode_col"
                    )

                    // 2. Dropdown Selector Nama Barang
                    ColumnDropdownPicker(
                        label = "2. Kolom Nama Barang",
                        icon = Icons.Default.Inventory,
                        columns = columns,
                        selectedIndex = currentMapping.namaColIndex,
                        onColumnSelected = { idx ->
                            onMappingChanged(currentMapping.copy(namaColIndex = idx))
                        },
                        testTag = "dropdown_select_nama_col"
                    )

                    // 3. Dropdown Selector Stok / Qty
                    ColumnDropdownPicker(
                        label = "3. Kolom Jumlah Stok (Qty)",
                        icon = Icons.Default.Pin,
                        columns = columns,
                        selectedIndex = currentMapping.stokColIndex,
                        onColumnSelected = { idx ->
                            onMappingChanged(currentMapping.copy(stokColIndex = idx))
                        },
                        testTag = "dropdown_select_stok_col"
                    )

                    // 4. Baris Awal Data Selector
                    StartRowPicker(
                        startRowIndex = currentMapping.startDataRowIndex,
                        onRowSelected = { r ->
                            onMappingChanged(currentMapping.copy(startDataRowIndex = r))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MappingBadge(label: String, isCustom: Boolean) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isCustom) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isCustom) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnDropdownPicker(
    label: String,
    icon: ImageVector,
    columns: List<ExcelColumnInfo>,
    selectedIndex: Int,
    onColumnSelected: (Int) -> Unit,
    testTag: String
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedText = if (selectedIndex < 0) {
        "Otomatis (Deteksi Sistem)"
    } else {
        val col = columns.find { it.index == selectedIndex }
        col?.displayLabel ?: "Kolom Index $selectedIndex"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .testTag(testTag)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Option 0: Auto
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Otomatis (Deteksi Sistem)",
                        fontWeight = if (selectedIndex < 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedIndex < 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                },
                trailingIcon = if (selectedIndex < 0) {
                    { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                } else null,
                onClick = {
                    onColumnSelected(-1)
                    expanded = false
                }
            )

            // Available columns
            columns.forEach { col ->
                val isSelected = col.index == selectedIndex
                DropdownMenuItem(
                    text = {
                        Text(
                            text = col.displayLabel,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    trailingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    } else null,
                    onClick = {
                        onColumnSelected(col.index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartRowPicker(
    startRowIndex: Int, // 0-indexed
    onRowSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val displayText = if (startRowIndex < 0) {
        "Otomatis (Setelah Baris Header)"
    } else {
        "Data Mulai Baris ${startRowIndex + 1}"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("4. Baris Awal Data") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Pin,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .testTag("dropdown_select_start_row")
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Otomatis (Setelah Baris Header)",
                        fontWeight = if (startRowIndex < 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (startRowIndex < 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    onRowSelected(-1)
                    expanded = false
                }
            )

            for (r in 1..15) {
                val rowZeroIndexed = r - 1
                val isSelected = rowZeroIndexed == startRowIndex
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Baris $r",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    trailingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    } else null,
                    onClick = {
                        onRowSelected(rowZeroIndexed)
                        expanded = false
                    }
                )
            }
        }
    }
}
