package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Transaction
import com.dokonpro.shared.domain.repository.FinanceRepository

class AddExpenseUseCase(private val repository: FinanceRepository) {
    suspend operator fun invoke(storeId: String, amount: Double, description: String?, categoryId: String?): Result<Transaction> =
        repository.addExpense(storeId, amount, description, categoryId)
}
