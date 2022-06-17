package dev.crashteam.chest.handler.payment

import dev.crashteam.chest.repository.EventLogRepository
import dev.crashteam.chest.repository.WalletPaymentRepository
import dev.crashteam.chest.repository.entity.EventLog
import dev.crashteam.chest.service.WalletService
import dev.crashteam.payment.PaymentEvent
import dev.crashteam.payment.PaymentStatus
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.transaction.Transactional

private val log = KotlinLogging.logger {}

@Component
class PaymentChangeEventHandler(
    private val walletService: WalletService,
    private val walletPaymentRepository: WalletPaymentRepository,
    private val eventLogRepository: EventLogRepository,
) : PaymentEventHandler {

    @Transactional
    override fun handle(paymentEvents: List<PaymentEvent>) {
        for (paymentEvent in paymentEvents) {
            val eventLog = eventLogRepository.findById(paymentEvent.eventId).orElse(null)
            if (eventLog != null) {
                log.info { "Event already processed ${paymentEvent.eventId}. Ignore it" }
                continue
            }
            val paymentChange = paymentEvent.payload.paymentChange
            if (paymentChange.hasPaymentCreated()) {
                val paymentCreated = paymentChange.paymentCreated
                walletService.addWalletPayment(
                    paymentCreated.userId,
                    paymentCreated.paymentId,
                    paymentCreated.description,
                    paymentCreated.status.name,
                    paymentCreated.amount.value,
                    paymentCreated.amount.currency,
                    LocalDateTime.ofEpochSecond(
                        paymentCreated.createdAt.seconds,
                        paymentCreated.createdAt.nanos,
                        ZoneOffset.UTC
                    )
                )
            } else if (paymentChange.hasPaymentStatusChanged()) {
                val paymentStatusChanged = paymentChange.paymentStatusChanged
                val walletPayment = walletPaymentRepository.findByPaymentId(paymentStatusChanged.paymentId)!!
                if (paymentStatusChanged.status == PaymentStatus.SUCCESS) {
                    walletService.increaseWalletAmount(
                        walletPayment.walletId!!,
                        walletPayment.amount!!,
                        walletPayment.description!!
                    )
                    walletPaymentRepository.updatePaymentStatus(
                        paymentStatusChanged.paymentId,
                        paymentStatusChanged.status.name
                    )
                }
            } else if (paymentChange.hasPaymentRefund()) {
                val paymentRefund = paymentChange.paymentRefund
                val walletPayment = walletPaymentRepository.findByPaymentId(paymentRefund.paymentId)!!
                walletService.decreaseWalletAmount(
                    walletPayment.walletId!!,
                    paymentRefund.refundAmount.value,
                    paymentRefund.description
                )
            }
            val newEventLog = EventLog().apply {
                eventId = paymentEvent.eventId
                timeOfReceiving = LocalDateTime.now()
            }
            eventLogRepository.save(newEventLog)
        }
    }

    override fun isHandle(paymentEvent: PaymentEvent): Boolean {
        return paymentEvent.payload.hasPaymentChange()
    }
}
