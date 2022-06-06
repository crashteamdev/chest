package dev.crashteam.chest.handler

import dev.crashteam.payment.PaymentEvent

interface PaymentEventHandler {

    fun handle(paymentEvents: List<PaymentEvent>)

    fun isHandle(paymentEvent: PaymentEvent): Boolean

}
