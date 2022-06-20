package dev.crashteam.chest.repository

import dev.crashteam.chest.repository.entity.WalletEntity
import dev.crashteam.chest.wallet.Wallet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*
import javax.persistence.LockModeType

@Repository
interface WalletRepository : JpaRepository<WalletEntity, UUID> {

    fun findByUserId(userId: String): WalletEntity?

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT w FROM WalletEntity w WHERE w.userId = ?1")
    fun findByUserIdWithLock(userId: String): WalletEntity?

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT w FROM WalletEntity w WHERE w.id = ?1")
    fun findByWalletIdWithLock(walletId: UUID): WalletEntity?

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update WalletEntity w set w.amount = w.amount - :amount where w.id = :walletId")
    fun updateAmountWithdrawalByWalletId(walletId: UUID, amount: Long)

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update WalletEntity w set w.amount = w.amount + :amount where w.id = :walletId")
    fun depositAmountByWalletId(walletId: UUID, amount: Long)

}
