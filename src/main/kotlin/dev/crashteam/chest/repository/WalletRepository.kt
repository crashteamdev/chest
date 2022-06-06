package dev.crashteam.chest.repository

import dev.crashteam.chest.repository.entity.WalletEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface WalletRepository : JpaRepository<WalletEntity, UUID> {

    fun findByUserId(userId: String): WalletEntity?

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update WalletEntity w set w.amount = w.amount - :amount where w.id = :walletId")
    fun updateAmountWithdrawalByWalletId(walletId: UUID, amount: Long)

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update WalletEntity w set w.amount = w.amount + :amount where w.id = :walletId")
    fun depositAmountByWalletId(walletId: UUID, amount: Long)

}
