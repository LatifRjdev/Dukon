package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Transaction
import com.dokonpro.shared.domain.entity.TransactionType
import com.dokonpro.shared.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow

class GetTransactionsUseCase(private val repository: FinanceRepository) {
    operator fun invoke(storeId: String, type: TransactionType? = null): Flow<List<Transaction>> =
        if (type != null) repository.getTransactionsByType(storeId, type) else repository.getTransactions(storeId)
}
