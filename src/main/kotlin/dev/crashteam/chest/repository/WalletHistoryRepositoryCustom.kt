package dev.crashteam.chest.repository

import dev.crashteam.chest.repository.entity.WalletHistoryEntity
import dev.crashteam.chest.repository.pagination.ContinuationToken
import dev.crashteam.chest.repository.pagination.Page
import java.time.LocalDateTime
import java.util.*

interface WalletHistoryRepositoryCustom {

    fun findWalletHistory(
        walletId: UUID,
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        limit: Int = 10,
    ): Page<WalletHistoryEntity>

    fun findNextWalletHistory(
        continuationToken: ContinuationToken,
        limit: Int = 10,
    ): Page<WalletHistoryEntity>
}
