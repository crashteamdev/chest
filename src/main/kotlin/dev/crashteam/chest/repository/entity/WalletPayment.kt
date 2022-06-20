package dev.crashteam.chest.repository.entity

import java.time.LocalDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "wallet_payment")
class WalletPayment : BaseEntity<Long>() {
    @Column(nullable = false)
    var paymentId: String? = null

    @Column(nullable = false)
    var walletId: UUID? = null

    @Column(nullable = false)
    var status: String? = null

    @Column(nullable = false)
    var amount: Long? = null

    @Column(nullable = false)
    var description: String? = null

    @Column(nullable = false)
    var createdAt: LocalDateTime? = null

    @Column(nullable = false)
    var currency: String? = null
}
