package dev.crashteam.chest.repository.pagination

data class ContinuationToken(
    val keyParams: Map<String, String>?,
    val timestamp: Long,
    val id: String,
)
