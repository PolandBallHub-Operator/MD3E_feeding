package com.example.petfeeding.data

import android.content.Context

class FeedingRepository(context: Context) {
    private val preferences = context.getSharedPreferences("feeding_times", Context.MODE_PRIVATE)
    fun load(): List<FeedingTime> = preferences.getStringSet("items", emptySet()).orEmpty().mapNotNull(FeedingTime::deserialize).sortedWith(compareBy({ it.hour }, { it.minute }))
    fun save(items: List<FeedingTime>) { preferences.edit().putStringSet("items", items.map(FeedingTime::serialize).toSet()).apply() }
}
