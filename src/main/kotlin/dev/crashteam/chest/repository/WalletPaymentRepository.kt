package dev.crashteam.chest.repository

import dev.crashteam.chest.repository.entity.WalletPayment
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface WalletPaymentRepository : CrudRepository<WalletPayment, Long> {

    fun findByWalletId(walletId: UUID): List<WalletPayment>

    fun findByPaymentId(paymentId: String): WalletPayment?

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update WalletPayment w set w.status = :status where w.paymentId = :paymentId")
    fun updatePaymentStatus(paymentId: String, status: String)

}
