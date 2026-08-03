package com.example.data.local

import android.content.Context
import com.example.data.model.ColumnMapping
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object ColumnMappingPersistenceManager {

    private const val PREFS_NAME = "excel_column_mapping_prefs"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val adapter by lazy {
        moshi.adapter(ColumnMapping::class.java)
    }

    fun saveMapping(context: Context, sheetKey: String, mapping: ColumnMapping) {
        try {
            val json = adapter.toJson(mapping)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString("mapping_$sheetKey", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadMapping(context: Context, sheetKey: String): ColumnMapping? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString("mapping_$sheetKey", null) ?: return null
            adapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
