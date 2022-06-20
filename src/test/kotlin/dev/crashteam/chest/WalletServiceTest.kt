package dev.crashteam.chest

import dev.crashteam.chest.service.error.WalletNotEnoughMoneyException
import dev.crashteam.chest.service.WalletService
import dev.crashteam.chest.service.error.DuplicateWalletException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.util.*
import kotlin.test.assertNotNull

@TestPropertySource(
    properties = [
        "spring.jpa.hibernate.ddl-auto=validate"
    ]
)
class WalletServiceTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var walletService: WalletService

    @Test
    fun `create wallet test`() {
        // Given
        val userId = UUID.randomUUID().toString()

        // When
        val walletEntity = walletService.createWallet(userId)

        // Then
        assertTrue(true)
        assertEquals(userId, walletEntity.userId)
    }

    @Test
    fun `trying to create duplicate wallet for user`() {
        // Given
        val userId = UUID.randomUUID().toString()

        // When
        assertThrows<DuplicateWalletException> {
            walletService.createWallet(userId)
            walletService.createWallet(userId)
        }
    }

    @Test
    fun `test finding wallet by walletId`() {
        // Given
        val userId = UUID.randomUUID().toString()

        // When
        val createdWalletEntity = walletService.createWallet(userId)
        val walletEntity = walletService.getWalletById(createdWalletEntity.id.toString())

        // Then
        assertNotNull(walletEntity)
    }

    @Test
    fun `test finding wallet by userId`() {
        // Given
        val userId = UUID.randomUUID().toString()

        // When
        walletService.createWallet(userId)
        val walletEntity = walletService.getUserWalletByUser(userId)

        // Then
        assertNotNull(walletEntity)
    }

    @Test
    fun `test increase wallet amount`() {
        // Given
        val userId = UUID.randomUUID().toString()

        // When
        val walletEntity = walletService.createWallet(userId)
        val resultWalletEntity = walletService.increaseWalletAmount(walletEntity.id!!, 10, "test transaction")
        val foundedWalletEntity = walletService.getWalletById(walletEntity.id?.toString()!!)

        // Then
        assertEquals(10, resultWalletEntity?.amount)
        assertEquals(10, foundedWalletEntity?.amount)
    }

    @Test
    fun `test decrease wallet amount`() {
        // Given
        val userId = UUID.randomUUID().toString()

        // When
        val walletEntity = walletService.createWallet(userId)
        walletService.increaseWalletAmount(walletEntity.id!!, 10, "test increase transaction")
        val resultWalletEntity = walletService.decreaseWalletAmount(
            walletEntity.id!!, 5, "test decrease transaction"
        )
        val foundedWalletEntity = walletService.getWalletById(walletEntity.id?.toString()!!)

        // Then
        assertEquals(5, resultWalletEntity?.amount)
        assertEquals(5, foundedWalletEntity?.amount)
    }

    @Test
    fun `test decrease wallet to negative balance`() {
        // Given
        val userId = UUID.randomUUID().toString()

        // When
        val walletEntity = walletService.createWallet(userId)
        assertThrows<WalletNotEnoughMoneyException> {
            walletService.decreaseWalletAmount(walletEntity.id!!, 10, "test transaction")
        }
    }

}
