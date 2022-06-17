package dev.crashteam.chest.handler.wallet

import dev.crashteam.chest.event.WalletCommandEvent

interface WalletCommandEventHandler {

    fun handle(paymentEvents: List<WalletCommandEvent>)

    fun isHandle(paymentEvent: WalletCommandEvent): Boolean

}
