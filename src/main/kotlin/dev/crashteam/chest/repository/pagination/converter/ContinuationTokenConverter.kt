package dev.crashteam.chest.repository.pagination.converter

import dev.crashteam.chest.repository.pagination.ContinuationToken

interface ContinuationTokenConverter {

    fun fromString(token: String): ContinuationToken

    fun toString(token: ContinuationToken): String
}
