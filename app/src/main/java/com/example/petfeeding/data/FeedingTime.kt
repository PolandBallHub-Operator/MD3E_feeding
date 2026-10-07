package com.example.petfeeding.data

data class FeedingTime(val id: Long, val hour: Int, val minute: Int, val enabled: Boolean) {
    fun withEnabled(value: Boolean) = copy(enabled = value)
    fun serialize() = "$id,$hour,$minute,$enabled"
    companion object { fun deserialize(value: String): FeedingTime? = value.split(',').takeIf { it.size == 4 }?.let { runCatching { FeedingTime(it[0].toLong(), it[1].toInt(), it[2].toInt(), it[3].toBoolean()) }.getOrNull() } }
}
