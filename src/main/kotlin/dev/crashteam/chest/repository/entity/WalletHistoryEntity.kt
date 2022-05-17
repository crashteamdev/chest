package dev.crashteam.chest.repository.entity

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType
import dev.crashteam.chest.repository.pagination.PageableEntity
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "wallet_history")
@TypeDef(
    name = "pgsql_enum",
    typeClass = PostgreSQLEnumType::class
)
class WalletHistoryEntity : BaseEntity<Long>(), PageableEntity<Long> {

    @Column(nullable = false)
    var walletId: UUID? = null

    @Column(nullable = false)
    var occurredAt: LocalDateTime = LocalDateTime.now()

    @Column(nullable = false)
    var amount: Long? = null

    @Column(nullable = false)
    var description: String? = null

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(nullable = false)
    var type: WalletChangeType? = null

    override val timestamp: Long
        get() = occurredAt.toEpochSecond(ZoneOffset.UTC)

}
