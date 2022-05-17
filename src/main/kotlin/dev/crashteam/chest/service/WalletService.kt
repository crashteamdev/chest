package dev.crashteam.chest.service

import dev.crashteam.chest.repository.WalletHistoryRepository
import dev.crashteam.chest.repository.WalletPaymentRepository
import dev.crashteam.chest.repository.WalletRepository
import dev.crashteam.chest.repository.entity.WalletChangeType
import dev.crashteam.chest.repository.entity.WalletEntity
import dev.crashteam.chest.repository.entity.WalletHistoryEntity
import dev.crashteam.chest.repository.entity.WalletPayment
import dev.crashteam.chest.repository.pagination.ContinuationToken
import dev.crashteam.chest.repository.pagination.Page
import dev.crashteam.chest.service.error.DuplicateWalletException
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import javax.transaction.Transactional

@Service
class WalletService(
    private val walletRepository: WalletRepository,
    private val walletHistoryRepository: WalletHistoryRepository,
    private val walletPaymentRepository: WalletPaymentRepository,
) {

    fun createWallet(userId: String): WalletEntity {
        log.info { "Create wallet for userId=$userId" }
        val walletEntity = WalletEntity().apply {
            this.id = UUID.randomUUID()
            this.amount = 0
            this.userId = userId
            this.blocked = false
            this.createdAt = LocalDateTime.now()
        }
        if (walletRepository.findByUserId(userId) != null) {
            throw DuplicateWalletException("Trying to save duplicate wallet for userId=$userId")
        }
        return walletRepository.save(walletEntity)
    }

    fun getUserWalletByUser(userId: String): WalletEntity? {
        log.info { "Get wallet by userId=$userId" }
        return walletRepository.findByUserId(userId)
    }

    fun getWalletById(walletId: String): WalletEntity? {
        log.info { "Get wallet by walletId=${walletId}" }
        return walletRepository.findById(UUID.fromString(walletId)).orElse(null)
    }

    @Transactional
    fun decreaseWalletAmount(walletId: UUID, amount: Long, description: String): WalletEntity? {
        log.info { "Decrease wallet amount. walletId=$walletId; amount=$amount" }
        val wallet = walletRepository.findById(walletId).orElse(null)
        val walletAmount = wallet?.amount ?: 0
        if (walletAmount <= 0 || (walletAmount - amount) < 0) {
            throw WalletNotEnoughMoneyException("Not enough money on wallet. walletId=${wallet.id}; userId=${wallet.userId}; amount=${wallet.amount}")
        }
        walletRepository.updateAmountWithdrawalByWalletId(walletId, amount)
        val walletHistoryEntity = WalletHistoryEntity().apply {
            this.walletId = walletId
            this.amount = amount
            this.description = description
            this.occurredAt = LocalDateTime.now()
            this.type = WalletChangeType.withdrawal
        }
        walletHistoryRepository.save(walletHistoryEntity)

        return walletRepository.findById(walletId).orElse(null)
    }

    @Transactional
    fun increaseWalletAmount(walletId: UUID, amount: Long, description: String): WalletEntity? {
        log.info { "Increase wallet amount. walletId=$walletId; amount=$amount" }
        walletRepository.depositAmountByWalletId(walletId, amount)
        val walletHistoryEntity = WalletHistoryEntity().apply {
            this.walletId = walletId
            this.amount = amount
            this.description = description
            this.occurredAt = LocalDateTime.now()
            this.type = WalletChangeType.replenishment
        }
        walletHistoryRepository.save(walletHistoryEntity)

        return walletRepository.findById(walletId).orElse(null)
    }

    fun findWalletHistory(
        walletId: UUID,
        fromDate: LocalDateTime?,
        toDate: LocalDateTime?,
        continuationToken: ContinuationToken?,
        limit: Int
    ): Page<WalletHistoryEntity> {
        return if (continuationToken != null) {
            walletHistoryRepository.findNextWalletHistory(continuationToken, limit)
        } else {
            walletHistoryRepository.findWalletHistory(walletId, fromDate, toDate, limit)
        }
    }

    @Transactional
    fun addWalletPayment(
        userId: String,
        paymentId: String,
        description: String,
        status: String,
        amount: Long,
        currency: String,
        createdAt: LocalDateTime,
    ) {
        var walletEntity = walletRepository.findByUserId(userId)
        if (walletEntity == null) {
            walletEntity = createWallet(userId)
        }
        val walletPayment = WalletPayment().apply {
            this.walletId = walletEntity.id
            this.paymentId = paymentId
            this.status = status
            this.amount = amount
            this.description = description
            this.currency = currency
            this.createdAt = createdAt
        }
        walletPaymentRepository.save(walletPayment)
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }

}
