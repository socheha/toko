package com.example.ui.util

import com.example.data.model.StockItem
import kotlin.math.abs
import kotlin.math.min

/**
 * Engine pencarian fuzzy (relevance scoring) mirip Fuse.js
 * Memungkinkan pencarian cepat berdasar Kode Barang, Nama Barang, dan sebagian nama
 * dengan pengurutan kecocokan terbaik (relevance score) dan pembatasan maksimal hasil.
 */
object FuzzySearchEngine {

    private data class ScoredItem(
        val item: StockItem,
        val score: Int
    )

    /**
     * Melakukan pencarian fuzzy pada list StockItem.
     * @param items List barang yang akan dicari
     * @param query Kata kunci pencarian pengguna
     * @param maxResults Maksimal jumlah hasil yang dikembalikan (default 20)
     */
    fun search(
        items: List<StockItem>,
        query: String,
        maxResults: Int = 20
    ): List<StockItem> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return items

        val tokens = q.split("\\s+".toRegex()).filter { it.isNotBlank() }

        val scoredList = mutableListOf<ScoredItem>()

        for (item in items) {
            val score = calculateRelevanceScore(item, q, tokens)
            if (score > 0) {
                scoredList.add(ScoredItem(item, score))
            }
        }

        // Urutkan berdasarkan skor kecocokan tertinggi (descending)
        return scoredList
            .sortedByDescending { it.score }
            .take(maxResults)
            .map { it.item }
    }

    private fun calculateRelevanceScore(
        item: StockItem,
        rawQuery: String,
        tokens: List<String>
    ): Int {
        val kode = item.kodeBarang.lowercase()
        val nama = item.namaBarang.lowercase()
        val kategori = item.kategori.lowercase()

        var score = 0

        // 1. KODE BARANG MATCHING (Bobot sangat tinggi)
        if (kode == rawQuery) {
            score += 2500 // Match persis Kode
        } else if (kode.startsWith(rawQuery)) {
            score += 1500 // Prefix Kode
        } else if (kode.contains(rawQuery)) {
            score += 1000 // Substring Kode
        }

        // 2. NAMA BARANG MATCHING (Bobot tinggi)
        if (nama == rawQuery) {
            score += 2000 // Match persis Nama
        } else if (nama.startsWith(rawQuery)) {
            score += 1200 // Prefix Nama
        } else if (nama.contains(rawQuery)) {
            score += 700 // Substring Nama
        }

        // Word boundary matching pada Nama Barang
        val namaWords = nama.split("\\s+|[\\-/.,]".toRegex()).filter { it.isNotBlank() }
        for (w in namaWords) {
            if (w == rawQuery) {
                score += 600
            } else if (w.startsWith(rawQuery)) {
                score += 400
            }
        }

        // 3. TOKEN MATCHING & ALL TOKENS CHECK
        var allTokensFound = true
        var tokenScoreSum = 0

        for (t in tokens) {
            val matchInKode = kode.contains(t)
            val matchInNama = nama.contains(t)
            val matchInKategori = kategori.contains(t)

            if (matchInKode || matchInNama || matchInKategori) {
                if (matchInKode) tokenScoreSum += 200
                if (matchInNama) tokenScoreSum += 150
                if (matchInKategori) tokenScoreSum += 50
            } else {
                // Check fuzzy match (Levenshtein) dengan kata-kata nama
                val fuzzyWordMatch = namaWords.any { word ->
                    isFuzzyMatch(t, word)
                }
                if (fuzzyWordMatch) {
                    tokenScoreSum += 60
                } else {
                    allTokensFound = false
                }
            }
        }

        if (allTokensFound && tokens.isNotEmpty()) {
            score += 500 + tokenScoreSum
        } else if (tokenScoreSum > 0) {
            score += tokenScoreSum
        }

        // 4. SUBSEQUENCE MATCHING (Fuse.js fuzzy fallback)
        if (score == 0 && rawQuery.length >= 2) {
            if (isSubsequence(rawQuery, kode)) {
                score += 180
            } else if (isSubsequence(rawQuery, nama)) {
                score += 120
            }
        }

        return score
    }

    private fun isFuzzyMatch(token: String, word: String): Boolean {
        if (token.length < 3) return false
        if (abs(token.length - word.length) > 3) return false
        val dist = levenshteinDistance(token, word)
        return dist <= 2
    }

    private fun isSubsequence(query: String, text: String): Boolean {
        var qIdx = 0
        for (i in 0 until text.length) {
            if (text[i] == query[qIdx]) {
                qIdx++
                if (qIdx == query.length) return true
            }
        }
        return false
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    dp[i - 1][j] + 1,
                    min(
                        dp[i][j - 1] + 1,
                        dp[i - 1][j - 1] + cost
                    )
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
