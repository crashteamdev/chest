package dev.crashteam.chest.handler.wallet

import com.google.protobuf.Timestamp
import dev.crashteam.chest.event.*
import dev.crashteam.chest.repository.WalletRepository
import dev.crashteam.chest.service.WalletService
import dev.crashteam.chest.service.error.WalletNotEnoughMoneyException
import mu.KotlinLogging
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import javax.transaction.Transactional

private val log = KotlinLogging.logger {}

@Component
class WalletReserveCreditCommandHandler(
    private val walletService: WalletService,
    private val walletRepository: WalletRepository,
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>,
) : WalletCommandEventHandler {

    @Value("\${chest.wallet-command-response-topic-name}")
    lateinit var walletCommandResponseTopicName: String

    @Transactional
    override fun handle(walletCommandEvents: List<WalletCommandEvent>) {
        for (walletCommandEvent in walletCommandEvents) {
            val walletReserveCredit = walletCommandEvent.payload.walletReserveCredit
            val walletEntity = walletRepository.findByUserId(walletReserveCredit.userId)
            if (walletEntity == null) {
                val eventSource = WalletReplyEvent.EventSource.newBuilder().apply {
                    this.userId = walletReserveCredit.userId
                }.build()
                val eventPayload = WalletReplyEvent.EventPayload.newBuilder().apply {
                    this.walletUserNotFound = WalletUserNotFound.newBuilder().apply {
                        this.userId = walletReserveCredit.userId
                        this.trxId = walletCommandEvent.payload.walletReserveCredit.trxId
                    }.build()
                }.build()
                val replyEvent = buildReplyEvent(eventSource, eventPayload)
                log.info { "Publish wallet user not found event eventId=${replyEvent.eventId}; userId=${replyEvent.eventSource.userId}" }
                val producerRecord = ProducerRecord(
                    walletCommandResponseTopicName,
                    walletReserveCredit.userId,
                    replyEvent.toByteArray()
                )
                kafkaTemplate.send(producerRecord)
                continue
            }
            try {
                val resultWalletEntity = walletService.decreaseWalletAmount(
                    walletEntity.id!!,
                    walletReserveCredit.amount,
                    walletReserveCredit.description
                )
                val eventSource = WalletReplyEvent.EventSource.newBuilder().apply {
                    this.userId = walletReserveCredit.userId
                }.build()
                val eventPayload = WalletReplyEvent.EventPayload.newBuilder().apply {
                    this.walletCreditReserved = WalletCreditReserved.newBuilder().apply {
                        this.userId = walletReserveCredit.userId
                        this.trxId = walletCommandEvent.payload.walletReserveCredit.trxId
                        this.reservedAmount = walletReserveCredit.amount
                        this.balance = resultWalletEntity!!.amount!!
                    }.build()
                }.build()
                val replyEvent = buildReplyEvent(eventSource, eventPayload)
                val producerRecord = ProducerRecord(
                    walletCommandResponseTopicName,
                    walletReserveCredit.userId,
                    replyEvent.toByteArray()
                )
                kafkaTemplate.send(producerRecord)
            } catch (e: WalletNotEnoughMoneyException) {
                val eventSource = WalletReplyEvent.EventSource.newBuilder().apply {
                    this.userId = walletReserveCredit.userId
                }.build()
                val eventPayload = WalletReplyEvent.EventPayload.newBuilder().apply {
                    this.walletCreditLimitExceeded = WalletCreditLimitExceeded.newBuilder().apply {
                        this.userId = walletReserveCredit.userId
                        this.trxId = walletCommandEvent.payload.walletReserveCredit.trxId
                        this.balance = walletEntity.amount!!
                    }.build()
                }.build()
                val replyEvent = buildReplyEvent(eventSource, eventPayload)
                val producerRecord = ProducerRecord(
                    walletCommandResponseTopicName,
                    walletReserveCredit.userId,
                    replyEvent.toByteArray()
                )
                kafkaTemplate.send(producerRecord)
            }
        }
    }

    override fun isHandle(paymentEvent: WalletCommandEvent): Boolean {
        return paymentEvent.payload.hasWalletReserveCredit()
    }

    private fun buildReplyEvent(
        eventSource: WalletReplyEvent.EventSource,
        eventPayload: WalletReplyEvent.EventPayload
    ): WalletReplyEvent {
        return WalletReplyEvent.newBuilder().apply {
            this.eventId = UUID.randomUUID().toString()
            val now = Instant.now()
            this.createdAt = Timestamp.newBuilder().setSeconds(now.epochSecond).setNanos(now.nano).build()
            this.eventSource = eventSource
            this.payload = eventPayload
        }.build()
    }
}
