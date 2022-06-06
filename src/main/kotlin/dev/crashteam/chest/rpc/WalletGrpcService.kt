package dev.crashteam.chest.rpc

import com.google.protobuf.Timestamp
import dev.crashteam.chest.repository.entity.WalletChangeType
import dev.crashteam.chest.repository.pagination.ContinuationTokenService
import dev.crashteam.chest.service.error.WalletNotEnoughMoneyException
import dev.crashteam.chest.service.WalletService
import dev.crashteam.chest.service.error.DuplicateWalletException
import dev.crashteam.chest.wallet.*
import io.grpc.Status
import io.grpc.Status.*
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@GrpcService
class WalletGrpcService(
    private val walletService: WalletService,
    private val continuationTokenService: ContinuationTokenService
) : WalletServiceGrpc.WalletServiceImplBase() {

    override fun createWallet(request: WalletCreateRequest, responseObserver: StreamObserver<WalletCreateResponse>) {
        try {
            val walletEntity = walletService.createWallet(request.account.userId)
            val walletCreateResponse = WalletCreateResponse.newBuilder()
                .setWallet(Wallet.newBuilder().apply {
                    walletId = walletEntity.id.toString()
                    account = Account.newBuilder().setUserId(walletEntity.userId).build()
                    createdAt =
                        Timestamp.newBuilder().setSeconds(walletEntity.createdAt.toEpochSecond(ZoneOffset.UTC)).build()
                    blocking = if (walletEntity.blocked == true) WalletBlocking.BLOCKED else WalletBlocking.UNBLOCKED
                    balance = WalletBalance.newBuilder().setAmount(walletEntity.amount!!).build()
                })
                .build()
            responseObserver.onNext(walletCreateResponse)
            responseObserver.onCompleted()
        } catch (e: DuplicateWalletException) {
            val status: Status = ALREADY_EXISTS.withDescription("Wallet exists for userId=${request.account.userId}")
            responseObserver.onError(status.asRuntimeException())
        }
    }

    override fun getWallet(request: WalletGetRequest, responseObserver: StreamObserver<WalletGetResponse>) {
        val walletEntity = if (request.hasUserId()) {
            walletService.getUserWalletByUser(request.userId)
        } else {
            walletService.getWalletById(request.walletId)
        }
        if (walletEntity == null) {
            val status: Status = NOT_FOUND.withDescription("Not found wallet")
            responseObserver.onError(status.asRuntimeException())
            return
        }
        val walletResponse = WalletGetResponse.newBuilder().apply {
            wallet = Wallet.newBuilder().apply {
                walletId = walletEntity.id.toString()
                account = Account.newBuilder().setUserId(walletEntity.userId).build()
                createdAt =
                    Timestamp.newBuilder().setSeconds(walletEntity.createdAt.toEpochSecond(ZoneOffset.UTC)).build()
                blocking = if (walletEntity.blocked == true) WalletBlocking.BLOCKED else WalletBlocking.UNBLOCKED
                balance = WalletBalance.newBuilder().setAmount(walletEntity.amount!!).build()
            }.build()
        }.build()
        responseObserver.onNext(walletResponse)
        responseObserver.onCompleted()
    }

    override fun withdrawalFunds(
        request: WalletWithdrawalFundsRequest,
        responseObserver: StreamObserver<WalletWithdrawalFundsResponse>
    ) {
        val walletId = try {
            UUID.fromString(request.walletId)
        } catch (e: IllegalArgumentException) {
            val status: Status = INVALID_ARGUMENT.withDescription("Bad walletId")
            responseObserver.onError(status.asRuntimeException())
            return
        }
        val walletEntity = try {
            walletService.decreaseWalletAmount(walletId, request.amount, request.description)!!
        } catch (e: WalletNotEnoughMoneyException) {
            val status: Status = CANCELLED.withDescription("Not enough money on wallet")
            responseObserver.onError(status.asRuntimeException())
            return
        }
        val withdrawalFundsResponse = WalletWithdrawalFundsResponse.newBuilder().apply {
            this.walletId = walletId.toString()
            this.amount = walletEntity.amount!!
        }.build()
        responseObserver.onNext(withdrawalFundsResponse)
        responseObserver.onCompleted()
    }

    override fun getWalletChangeHistory(
        request: WalletHistoryRequest,
        responseObserver: StreamObserver<WalletHistoryResponse>
    ) {
        val walletId = try {
            UUID.fromString(request.walletId)
        } catch (e: IllegalArgumentException) {
            val status: Status = INVALID_ARGUMENT.withDescription("Bad walletId")
            responseObserver.onError(status.asRuntimeException())
            return
        }
        val fromDate = request.dateFilter.fromDate?.let {
            LocalDateTime.ofEpochSecond(it.seconds, it.nanos, ZoneOffset.UTC)
        }
        val toDate = request.dateFilter.toDate?.let {
            LocalDateTime.ofEpochSecond(it.seconds, it.nanos, ZoneOffset.UTC)
        }
        val walletHistoryPage = walletService.findWalletHistory(
            walletId,
            fromDate,
            toDate,
            continuationTokenService.tokenFromString(request.continuationToken),
            request.limit
        )
        WalletHistoryResponse.newBuilder().apply {
            this.walletId = walletId.toString()
            val walletHistoryList = walletHistoryPage.entities.map { walletHistory ->
                WalletHistory.newBuilder().apply {
                    this.amount = walletHistory.amount!!
                    this.description = walletHistory.description!!
                    val occurredAt = Timestamp.newBuilder()
                        .setSeconds(walletHistory.occurredAt.toEpochSecond(ZoneOffset.UTC))
                        .build()
                    this.occurredAt = occurredAt
                    this.historyType = when (walletHistory.type) {
                        WalletChangeType.withdrawal -> WalletHistory.HistoryType.WITHDRAWAL
                        WalletChangeType.replenishment -> WalletHistory.HistoryType.REPLENISHMENT
                        else -> WalletHistory.HistoryType.UNRECOGNIZED
                    }
                }.build()
            }
            this.addAllWalletHistory(walletHistoryList)
            if (walletHistoryPage.token != null) {
                this.continuationToken = continuationTokenService.tokenToString(walletHistoryPage.token)
            }
        }
    }

}
