package dev.crashteam.chest.repository

import dev.crashteam.chest.repository.entity.WalletHistoryEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface WalletHistoryRepository : CrudRepository<WalletHistoryEntity, Long>, WalletHistoryRepositoryCustom
