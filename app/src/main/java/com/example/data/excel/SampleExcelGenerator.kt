package com.example.data.excel

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

object SampleExcelGenerator {

    /**
     * Membuat file sampel Excel (.xlsx) dengan beberapa sheet data stok.
     * Mengembalikan URI file lokal agar dapat dibaca langsung oleh parser.
     */
    fun createSampleStockExcel(context: Context): Pair<Uri, String> {
        val workbook = XSSFWorkbook()

        // -------------------------------------------------------------
        // Sheet 1: Stok Toko Utama
        // -------------------------------------------------------------
        val sheet1 = workbook.createSheet("Stok Toko Utama")

        val headerStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
            }
            setFont(font)
            fillForegroundColor = IndexedColors.DARK_GREEN.index
            fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
        }

        // Header
        val headers1 = arrayOf("Kode Barang", "Nama Barang", "Stok", "Satuan", "Harga Satuan", "Kategori")
        val headerRow1 = sheet1.createRow(0)
        headers1.forEachIndexed { i, title ->
            val cell = headerRow1.createCell(i)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }

        // Sample Data Sheet 1
        val sampleItems1 = listOf(
            arrayOf("ELK-001", "Laptop Asus ZenBook 14", 15, "Unit", 14500000, "Elektronik"),
            arrayOf("ELK-002", "Monitor LG UltraGear 27\"", 28, "Unit", 3850000, "Elektronik"),
            arrayOf("ELK-003", "Keyboard Mechanical RGB", 65, "Pcs", 750000, "Aksesoris"),
            arrayOf("ELK-004", "Mouse Wireless Ergonomis", 120, "Pcs", 320000, "Aksesoris"),
            arrayOf("ELK-005", "Headset Gaming Surround 7.1", 42, "Pcs", 890000, "Aksesoris"),
            arrayOf("ELK-006", "Webcam Full HD 1080p", 35, "Pcs", 550000, "Aksesoris"),
            arrayOf("ELK-007", "External SSD 1TB NVMe", 80, "Unit", 1650000, "Penyimpanan"),
            arrayOf("ELK-008", "USB Flashdrive 128GB", 210, "Pcs", 185000, "Penyimpanan"),
            arrayOf("ELK-009", "Router Wi-Fi 6 Dual Band", 18, "Unit", 1250000, "Jaringan"),
            arrayOf("ELK-010", "Printer Tank Inkjet Color", 12, "Unit", 2400000, "Kantor"),
            arrayOf("ELK-011", "Kertas HVS A4 80gsm", 350, "Rim", 55000, "Atk"),
            arrayOf("ELK-012", "Proyektor Mini Portable", 9, "Unit", 4100000, "Elektronik"),
            arrayOf("ELK-013", "UPS 1200VA Backup Power", 22, "Unit", 1950000, "Aksesoris"),
            arrayOf("ELK-014", "Kabel HDMI 2.1 Braided 3M", 145, "Pcs", 95000, "Kabel"),
            arrayOf("ELK-015", "Standing Desk Motorized", 6, "Unit", 5200000, "Mebel")
        )

        sampleItems1.forEachIndexed { rIndex, rowData ->
            val row = sheet1.createRow(rIndex + 1)
            row.createCell(0).setCellValue(rowData[0].toString())
            row.createCell(1).setCellValue(rowData[1].toString())
            row.createCell(2).setCellValue((rowData[2] as Int).toDouble())
            row.createCell(3).setCellValue(rowData[3].toString())
            row.createCell(4).setCellValue((rowData[4] as Int).toDouble())
            row.createCell(5).setCellValue(rowData[5].toString())
        }

        // Auto-fit column widths
        for (i in 0..5) {
            sheet1.setColumnWidth(i, 5000)
        }

        // -------------------------------------------------------------
        // Sheet 2: Stok Gudang Cadangan
        // -------------------------------------------------------------
        val sheet2 = workbook.createSheet("Stok Gudang Cadangan")
        val headers2 = arrayOf("SKU / No. Barang", "Deskripsi Produk", "Jumlah Stok", "Unit", "Lokasi Rak")
        val headerRow2 = sheet2.createRow(0)
        headers2.forEachIndexed { i, title ->
            val cell = headerRow2.createCell(i)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }

        val sampleItems2 = listOf(
            arrayOf("SKU-GUD-01", "Baterai Lithium AAA Pack", 500, "Pack", "Rak A-01"),
            arrayOf("SKU-GUD-02", "Adaptor Power USB-C 65W", 85, "Pcs", "Rak A-02"),
            arrayOf("SKU-GUD-03", "Kabel LAN Cat6 100M", 14, "Roll", "Rak B-01"),
            arrayOf("SKU-GUD-04", "Bracket Monitor Dual Arm", 30, "Set", "Rak B-03"),
            arrayOf("SKU-GUD-05", "Cleaning Kit Pembersih Layar", 180, "Botol", "Rak C-01"),
            arrayOf("SKU-GUD-06", "Mousepad Extended Deskmat", 95, "Pcs", "Rak C-02"),
            arrayOf("SKU-GUD-07", "Microphone Condenser USB", 40, "Pcs", "Rak D-01"),
            arrayOf("SKU-GUD-08", "Ring Light LED 10 Inch", 60, "Pcs", "Rak D-02")
        )

        sampleItems2.forEachIndexed { rIndex, rowData ->
            val row = sheet2.createRow(rIndex + 1)
            row.createCell(0).setCellValue(rowData[0].toString())
            row.createCell(1).setCellValue(rowData[1].toString())
            row.createCell(2).setCellValue((rowData[2] as Int).toDouble())
            row.createCell(3).setCellValue(rowData[3].toString())
            row.createCell(4).setCellValue(rowData[4].toString())
        }

        for (i in 0..4) {
            sheet2.setColumnWidth(i, 5000)
        }

        // -------------------------------------------------------------
        // Sheet 3: Info Supplier (Non-Stock Sheet)
        // -------------------------------------------------------------
        val sheet3 = workbook.createSheet("Daftar Supplier")
        val headerRow3 = sheet3.createRow(0)
        arrayOf("ID Supplier", "Nama PT/CV", "Kontak Person", "No. Telepon").forEachIndexed { i, t ->
            val cell = headerRow3.createCell(i)
            cell.setCellValue(t)
            cell.cellStyle = headerStyle
        }

        val sampleSuppliers = listOf(
            arrayOf("SUP-01", "PT Digital Tech Indonesia", "Budi Santoso", "0812-3456-7890"),
            arrayOf("SUP-02", "CV Mandiri Jaya Sentosa", "Siti Aminah", "0856-9876-5432")
        )

        sampleSuppliers.forEachIndexed { rIndex, rowData ->
            val row = sheet3.createRow(rIndex + 1)
            row.createCell(0).setCellValue(rowData[0].toString())
            row.createCell(1).setCellValue(rowData[1].toString())
            row.createCell(2).setCellValue(rowData[2].toString())
            row.createCell(3).setCellValue(rowData[3].toString())
        }

        // Simpan ke file cache lokal
        val fileName = "Sample_Stok_Barang.xlsx"
        val cacheFile = File(context.cacheDir, fileName)
        FileOutputStream(cacheFile).use { out ->
            workbook.write(out)
        }
        workbook.close()

        val fileUri = Uri.fromFile(cacheFile)
        return Pair(fileUri, fileName)
    }
}
