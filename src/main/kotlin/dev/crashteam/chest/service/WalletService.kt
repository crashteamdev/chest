package dev.crashteam.chest.service

import com.google.protobuf.Timestamp
import dev.crashteam.chest.event.*
import dev.crashteam.chest.repository.EventMessageOutRepository
import dev.crashteam.chest.repository.WalletHistoryRepository
import dev.crashteam.chest.repository.WalletPaymentRepository
import dev.crashteam.chest.repository.WalletRepository
import dev.crashteam.chest.repository.entity.*
import dev.crashteam.chest.repository.pagination.ContinuationToken
import dev.crashteam.chest.repository.pagination.Page
import dev.crashteam.chest.service.error.DuplicateWalletException
import dev.crashteam.chest.service.error.WalletNotEnoughMoneyException
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import javax.transaction.Transactional

@Service
class WalletService(
    private val walletRepository: WalletRepository,
    private val walletHistoryRepository: WalletHistoryRepository,
    private val walletPaymentRepository: WalletPaymentRepository,
    private val eventMessageOutRepository: EventMessageOutRepository
) {

    @Transactional
    fun createWallet(userId: String): WalletEntity {
        log.info { "Create wallet for userId=$userId" }
        if (walletRepository.findByUserId(userId) != null) {
            throw DuplicateWalletException("Trying to save duplicate wallet for userId=$userId")
        }
        val walletEntity = WalletEntity().apply {
            this.id = UUID.randomUUID()
            this.amount = 0
            this.userId = userId
            this.blocked = false
            this.createdAt = LocalDateTime.now()
        }
        val savedWalletEntity = walletRepository.save(walletEntity)
        val walletEvent = WalletCudEvent.newBuilder().apply {
            this.eventId = UUID.randomUUID().toString()
            val now = Instant.now()
            val timestampNow = Timestamp.newBuilder().setSeconds(now.epochSecond).setNanos(now.nano).build()
            this.createdAt = timestampNow
            this.eventSource = WalletCudEvent.EventSource.newBuilder().apply {
                this.walletId = savedWalletEntity.id.toString()
            }.build()
            this.payload = WalletCudEvent.EventPayload.newBuilder().apply {
                this.walletChange = WalletChange.newBuilder().apply {
                    this.userId = userId
                    this.walletCreated = WalletCreated.newBuilder().apply {
                        this.createdAt = timestampNow
                        this.balance = savedWalletEntity.amount!!
                    }.build()
                }.build()
            }.build()
        }.build()
        val eventMessageOut = EventMessageOut().apply {
            this.aggregateId = savedWalletEntity.id.toString()
            this.aggregateType = "WalletEvent"
            this.type = "WalletCreateEvent"
            this.payload = walletEvent.toByteArray()
        }
        eventMessageOutRepository.save(eventMessageOut)

        return savedWalletEntity
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
        val wallet = walletRepository.findByWalletIdWithLock(walletId)
        val walletAmount = wallet?.amount ?: 0
        if (walletAmount <= 0 || (walletAmount - amount) < 0) {
            throw WalletNotEnoughMoneyException("Not enough money on wallet. walletId=${wallet?.id}; userId=${wallet?.userId}; amount=${wallet?.amount}")
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
        val walletEvent = WalletCudEvent.newBuilder().apply {
            this.eventId = UUID.randomUUID().toString()
            val now = Instant.now()
            val timestampNow = Timestamp.newBuilder().setSeconds(now.epochSecond).setNanos(now.nano).build()
            this.createdAt = timestampNow
            this.eventSource = WalletCudEvent.EventSource.newBuilder().apply {
                this.walletId = walletId.toString()
            }.build()
            this.payload = WalletCudEvent.EventPayload.newBuilder().apply {
                this.walletChange = WalletChange.newBuilder().apply {
                    this.userId = userId
                    this.walletBalanceChange = WalletBalanceChange.newBuilder().apply {
                        this.amount = walletAmount
                        this.type = WalletBalanceChange.BalanceChangeType.withdrawal
                    }.build()
                }.build()
            }.build()
        }.build()
        val eventMessageOut = EventMessageOut().apply {
            this.aggregateId = walletId.toString()
            this.aggregateType = "WalletEvent"
            this.type = "WalletBalanceChangeEvent"
            this.payload = walletEvent.toByteArray()
        }
        eventMessageOutRepository.save(eventMessageOut)

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
        val walletEvent = WalletCudEvent.newBuilder().apply {
            this.eventId = UUID.randomUUID().toString()
            val now = Instant.now()
            val timestampNow = Timestamp.newBuilder().setSeconds(now.epochSecond).setNanos(now.nano).build()
            this.createdAt = timestampNow
            this.eventSource = WalletCudEvent.EventSource.newBuilder().apply {
                this.walletId = walletId.toString()
            }.build()
            this.payload = WalletCudEvent.EventPayload.newBuilder().apply {
                this.walletChange = WalletChange.newBuilder().apply {
                    this.userId = userId
                    this.walletBalanceChange = WalletBalanceChange.newBuilder().apply {
                        this.amount = amount
                        this.type = WalletBalanceChange.BalanceChangeType.replenishment
                    }.build()
                }.build()
            }.build()
        }.build()
        val eventMessageOut = EventMessageOut().apply {
            this.aggregateId = walletId.toString()
            this.aggregateType = "WalletEvent"
            this.type = "WalletBalanceChangeEvent"
            this.payload = walletEvent.toByteArray()
        }
        eventMessageOutRepository.save(eventMessageOut)

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
