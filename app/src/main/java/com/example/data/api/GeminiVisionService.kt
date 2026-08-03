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

    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeReceiptImage(bitmap: Bitmap): List<ScannedOcrItem> = withContext(Dispatchers.IO) {
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

        // 2. Build JSON Payload
        val promptText = """
            Anda adalah asisten OCR nota/struk penjualan yang sangat akurat.
            Tugas Anda: Bacalah foto nota ini dan ambil daftar semua nama barang beserta jumlahnya (kuantitas / qty).
            Aturan Penting:
            - Jangan baca atau sertakan HARGA barang.
            - Ekstrak hanya Nama Barang dan Jumlah/Kuantitas.
            - Jika jumlah barang tidak tertulis dengan jelas, gunakan angka 1.
            - Kembalikan HANYA format JSON Array murni tanpa penjelasan lain, tanpa markdown, tanpa ```json.
            - Format JSON: [{"nama_barang": "NAMA_BARANG", "jumlah": 1}]
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
        val url = "$BASE_URL?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: ""
            throw IllegalStateException("Gagal menghubungi Gemini API (${response.code}): $errBody")
        }

        val responseString = response.body?.string() ?: throw IllegalStateException("Respons kosong dari Gemini API")
        val jsonResponse = JSONObject(responseString)

        val candidates = jsonResponse.optJSONArray("candidates")
            ?: throw IllegalStateException("Gemini API tidak mengembalikan kandidat respons.")

        val firstCandidate = candidates.optJSONObject(0)
            ?: throw IllegalStateException("Hasil respons Gemini kosong.")

        val content = firstCandidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val rawText = parts?.optJSONObject(0)?.optString("text")
            ?: throw IllegalStateException("Teks hasil pembacaan nota tidak ditemukan.")

        // Parse extracted JSON string
        parseOcrJsonText(rawText)
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
