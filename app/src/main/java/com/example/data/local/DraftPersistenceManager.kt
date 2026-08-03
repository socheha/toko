package com.example.data.local

import android.content.Context
import com.example.data.model.DraftItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object DraftPersistenceManager {

    private const val PREFS_NAME = "draft_penjualan_prefs"
    private const val KEY_DRAFT_JSON = "key_draft_items_json"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val listType by lazy {
        Types.newParameterizedType(List::class.java, DraftItem::class.java)
    }

    private val adapter by lazy {
        moshi.adapter<List<DraftItem>>(listType)
    }

    fun serializeDraftItems(items: List<DraftItem>): String {
        return try {
            adapter.toJson(items)
        } catch (e: Exception) {
            e.printStackTrace()
            "[]"
        }
    }

    fun deserializeDraftItems(json: String): List<DraftItem> {
        if (json.isBlank()) return emptyList()
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveDraft(context: Context, items: List<DraftItem>) {
        try {
            val json = serializeDraftItems(items)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_DRAFT_JSON, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadDraft(context: Context): List<DraftItem> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_DRAFT_JSON, null) ?: ""
            deserializeDraftItems(json)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun clearDraft(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_DRAFT_JSON).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
