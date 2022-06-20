package dev.crashteam.chest.repository

import dev.crashteam.chest.repository.entity.WalletHistoryEntity
import dev.crashteam.chest.repository.pagination.ContinuationToken
import dev.crashteam.chest.repository.pagination.ContinuationTokenService
import dev.crashteam.chest.repository.pagination.Page
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root
import kotlin.collections.HashMap

@Component
class WalletHistoryRepositoryCustomImpl(
    private val entityManager: EntityManager,
    private val continuationTokenService: ContinuationTokenService,
) : WalletHistoryRepositoryCustom {

    override fun findWalletHistory(
        walletId: UUID,
        from: LocalDateTime?,
        to: LocalDateTime?,
        limit: Int
    ): Page<WalletHistoryEntity> {
        val cb: CriteriaBuilder = entityManager.criteriaBuilder
        val query = cb.createQuery(WalletHistoryEntity::class.java)
        val root = query.from(WalletHistoryEntity::class.java)
        val predicates = mutableListOf<Predicate>().apply {
            cb.equal(root.get<UUID>("walletId"), walletId)
            if (from != null) {
                add(createdAtFromPredicate(cb, root, from))
            }
            if (to != null) {
                add(createdAtToPredicate(cb, root, to))
            }
        }
        val criteriaQuery = query.select(root)
            .where(
                cb.and(*predicates.toTypedArray())
            ).orderBy(
                cb.asc(root.get<UUID>("walletId")),
                cb.asc(root.get<LocalDateTime>("occurredAt"))
            )
        val resultList = entityManager.createQuery(criteriaQuery).setMaxResults(limit + 1).resultList.toList()
        val keyParams = HashMap<String, String>().apply {
            from?.let {
                put(OCCURRED_AT_FROM_PARAM, Timestamp.valueOf(from).toString())
            }
            to?.let {
                put(OCCURRED_AT_TO_PARAM, Timestamp.valueOf(to).toString())
            }
        }

        return continuationTokenService.createPage(resultList, null, keyParams, limit + 1)
    }

    override fun findNextWalletHistory(continuationToken: ContinuationToken, limit: Int): Page<WalletHistoryEntity> {
        val cb: CriteriaBuilder = entityManager.criteriaBuilder
        val query = cb.createQuery(WalletHistoryEntity::class.java)
        val root = query.from(WalletHistoryEntity::class.java)
        val idPath = root.get<UUID>("walletId")
        val occurredAtPath = root.get<LocalDateTime>("occurredAt")

        val predicates = mutableListOf<Predicate>()
        continuationToken.keyParams?.let {
            predicates.add(createdAtToPredicate(cb, root, continuationToken.keyParams[OCCURRED_AT_FROM_PARAM]))
            predicates.add(createdAtFromPredicate(cb, root, continuationToken.keyParams[OCCURRED_AT_TO_PARAM]))
        }
        predicates.add(
            continuationPredicate(cb, root, continuationToken.timestamp, UUID.fromString(continuationToken.id))
        )

        val criteriaQuery = query.select(root)
            .where(
                *predicates.toTypedArray()
            ).orderBy(cb.asc(occurredAtPath), cb.asc(idPath))

        val resultList = entityManager.createQuery(criteriaQuery).setMaxResults(limit + 1).resultList.toList()

        return continuationTokenService.createPage(
            resultList,
            continuationToken,
            continuationToken.keyParams,
            limit + 1
        )
    }

    private fun continuationPredicate(
        cb: CriteriaBuilder,
        root: Root<WalletHistoryEntity>,
        fromTimestamp: Long,
        id: UUID,
    ): Predicate {
        val idPath = root.get<UUID>("walletId")
        val createdAtPath = root.get<LocalDateTime>("occurredAt")
        val timestamp = LocalDateTime.ofInstant(Instant.ofEpochSecond(fromTimestamp), ZoneId.of("UTC"))
        return cb.and(
            cb.greaterThanOrEqualTo(createdAtPath, timestamp),
            cb.greaterThan(idPath, id)
        )
    }

    private fun createdAtFromPredicate(
        cb: CriteriaBuilder,
        root: Root<WalletHistoryEntity>,
        from: LocalDateTime?,
    ): Predicate {
        val createdAtPath = root.get<LocalDateTime>("occurredAt")
        return cb.greaterThanOrEqualTo(
            createdAtPath,
            from ?: LocalDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneId.of("UTC"))
        )
    }

    private fun createdAtFromPredicate(
        cb: CriteriaBuilder,
        root: Root<WalletHistoryEntity>,
        from: String?
    ): Predicate {
        return if (from != null) {
            val toDate = Timestamp.valueOf(from).toLocalDateTime()
            cb.greaterThanOrEqualTo(root.get<LocalDateTime>("createdAt"), toDate)
        } else cb.conjunction()
    }

    private fun createdAtToPredicate(
        cb: CriteriaBuilder,
        root: Root<WalletHistoryEntity>,
        to: LocalDateTime?,
    ): Predicate {
        return if (to != null) {
            cb.lessThanOrEqualTo(root.get<LocalDateTime>("occurredAt"), to)
        } else cb.conjunction()
    }

    private fun createdAtToPredicate(
        cb: CriteriaBuilder,
        root: Root<WalletHistoryEntity>,
        to: String?
    ): Predicate {
        return if (to != null) {
            val toDate = Timestamp.valueOf(to).toLocalDateTime()
            cb.lessThanOrEqualTo(root.get<LocalDateTime>("createdAt"), toDate)
        } else cb.conjunction()
    }

    private companion object KeyParams {
        const val OCCURRED_AT_FROM_PARAM = "occurred_at_from"
        const val OCCURRED_AT_TO_PARAM = "occurred_at_to"
    }


}
