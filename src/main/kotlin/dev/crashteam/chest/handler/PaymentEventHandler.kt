package dev.crashteam.chest.handler

import dev.crashteam.payment.PaymentEvent

interface PaymentEventHandler {

    fun handle(paymentEvent: List<PaymentEvent>)

    fun isHandle(paymentEvent: PaymentEvent): Boolean

}
