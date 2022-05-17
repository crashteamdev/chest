package dev.crashteam.chest.repository.entity

import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "wallet",  uniqueConstraints = [
    UniqueConstraint(name = "wallet_user_id_idx", columnNames = ["userId"])
])
class WalletEntity {
    @Id
    var id: UUID? = null

    @Column(nullable = false)
    var amount: Long? = null

    @Column(nullable = false)
    var userId: String? = null

    @Column(nullable = false)
    var blocked: Boolean? = null

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WalletEntity

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }


}
