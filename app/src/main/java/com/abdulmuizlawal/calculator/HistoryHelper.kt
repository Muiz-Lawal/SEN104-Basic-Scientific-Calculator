package com.abdulmuizlawal.calculator

import android.content.Context

object HistoryHelper {
    private const val PREFS_NAME = "calc_prefs"
    private const val KEY_HISTORY = "history_list"
    private const val MAX_HISTORY = 20

    fun saveEntry(context: Context, expression: String, result: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val history = loadHistory(context).toMutableList()
        history.add(0, Pair(expression, result))
        val savedList = history.take(MAX_HISTORY).map { "${it.first}|${it.second}" }
        prefs.edit().putString(KEY_HISTORY, savedList.joinToString(";;")).apply()
    }

    fun loadHistory(context: Context): List<Pair<String, String>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_HISTORY, "") ?: ""
        val historyList = mutableListOf<Pair<String, String>>()
        if (saved.isNotEmpty()) {
            saved.split(";;").forEach {
                val parts = it.split("|")
                if (parts.size == 2) historyList.add(Pair(parts[0], parts[1]))
            }
        }
        return historyList
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
    }
}
