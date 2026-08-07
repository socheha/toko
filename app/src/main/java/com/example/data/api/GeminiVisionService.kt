package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.ScannedOcrItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiVisionService {

    private val MODEL_CANDIDATES = listOf("gemini-2.0-flash", "gemini-1.5-flash", "gemini-2.5-flash")

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeReceiptImage(
        bitmap: Bitmap,
        catalogItemNames: List<String> = emptyList()
    ): List<ScannedOcrItem> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API Key Gemini belum dikonfigurasi. Silakan atur GEMINI_API_KEY pada Secrets panel.")
        }

        // 1. Resize & Compress Bitmap to max 1024x1024 JPEG
        val scaledBitmap = scaleBitmapToMax(bitmap, 1024)
        val byteArrayOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
        val imageBytes = byteArrayOutputStream.toByteArray()
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        // 2. Build catalog prompt snippet if available
        val catalogSnippet = if (catalogItemNames.isNotEmpty()) {
            """
            DAFTAR KATALOG BARANG DARI EXCEL STOCK:
            ${catalogItemNames.take(200).joinToString("\n- ", prefix = "- ")}
            """.trimIndent()
        } else ""

        // 3. Build Prompt
        val promptText = """
            Anda adalah asisten OCR nota/struk penjualan yang sangat akurat dan presisi.
            
            $catalogSnippet

            Tugas Anda:
            1. Bacalah foto nota ini dan ambil daftar semua nama barang beserta jumlah kuantitasnya (qty).
            2. COCOKKAN NAMA BARANG PADA NOTA DENGAN KATALOG BARANG EXCEL STOCK DI ATAS jika relevan (abaikan singkatan handwriting, typo kecil, atau tulisan tangan yang kurang jelas).
            3. Jika nama barang pada nota merujuk/mirip dengan salah satu barang di KATALOG EXCEL, gunakan NAMA BARANG DARI KATALOG EXCEL.
            4. Jika tidak ada di Katalog, gunakan nama pembacaan dari nota.
            5. Abaikan HARGA dan TOTAL UANG.
            6. Kembalikan HANYA format JSON Array murni:
               [{"nama_barang": "NAMA_BARANG_DARI_EXCEL_ATAU_NOTA", "jumlah": 1}]
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", promptText)
                        })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        var lastException: Exception? = null

        // Try model candidates with fallback
        for (modelName in MODEL_CANDIDATES) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            try {
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    lastException = IllegalStateException("Gagal menghubungi Gemini API ($modelName - ${response.code}): $errBody")
                    continue
                }

                val responseString = response.body?.string() ?: continue
                val jsonResponse = JSONObject(responseString)

                val candidates = jsonResponse.optJSONArray("candidates") ?: continue
                val firstCandidate = candidates.optJSONObject(0) ?: continue
                val content = firstCandidate.optJSONObject("content") ?: continue
                val parts = content.optJSONArray("parts") ?: continue
                val rawText = parts.optJSONObject(0)?.optString("text") ?: continue

                val items = parseOcrJsonText(rawText)
                if (items.isNotEmpty()) {
                    return@withContext items
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        throw lastException ?: IllegalStateException("Gagal memproses foto nota dengan Gemini AI. Pastikan foto terlihat jelas dan koneksi internet stabil.")
    }

    private fun parseOcrJsonText(rawText: String): List<ScannedOcrItem> {
        val cleanText = rawText.trim()
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val resultList = mutableListOf<ScannedOcrItem>()

        try {
            if (cleanText.startsWith("[")) {
                val jsonArray = JSONArray(cleanText)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    val nama = obj.optString("nama_barang", obj.optString("namaBarang", obj.optString("name", "")))
                    val jumlah = obj.optInt("jumlah", obj.optInt("qty", obj.optInt("quantity", 1)))
                    if (nama.isNotBlank()) {
                        resultList.add(ScannedOcrItem(namaBarang = nama.trim(), jumlah = if (jumlah > 0) jumlah else 1))
                    }
                }
            } else if (cleanText.startsWith("{")) {
                val jsonObject = JSONObject(cleanText)
                val itemsArray = jsonObject.optJSONArray("items") ?: jsonObject.optJSONArray("barang")
                if (itemsArray != null) {
                    for (i in 0 until itemsArray.length()) {
                        val obj = itemsArray.optJSONObject(i) ?: continue
                        val nama = obj.optString("nama_barang", obj.optString("namaBarang", obj.optString("name", "")))
                        val jumlah = obj.optInt("jumlah", obj.optInt("qty", obj.optInt("quantity", 1)))
                        if (nama.isNotBlank()) {
                            resultList.add(ScannedOcrItem(namaBarang = nama.trim(), jumlah = if (jumlah > 0) jumlah else 1))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return resultList
    }

    private fun scaleBitmapToMax(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
