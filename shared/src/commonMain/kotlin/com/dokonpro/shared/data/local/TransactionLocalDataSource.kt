package com.dokonpro.shared.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.dokonpro.shared.db.DokonProDatabase
import com.dokonpro.shared.domain.entity.ExpenseCategory
import com.dokonpro.shared.domain.entity.Transaction
import com.dokonpro.shared.domain.entity.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class TransactionLocalDataSource(private val db: DokonProDatabase) {

    fun getTransactions(storeId: String): Flow<List<Transaction>> =
        db.transactionQueries.selectByStoreId(storeId).asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toTransaction() } }

    fun getTransactionsByType(storeId: String, type: String): Flow<List<Transaction>> =
        db.transactionQueries.selectByStoreIdAndType(storeId, type).asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toTransaction() } }

    fun insertTransaction(tx: Transaction) {
        db.transactionQueries.insert(tx.id, tx.type.name, tx.amount, tx.description, tx.categoryId, tx.storeId, tx.createdAt)
    }

    fun insertTransactions(txs: List<Transaction>) {
        db.transaction { txs.forEach { insertTransaction(it) } }
    }

    fun getCategories(storeId: String): Flow<List<ExpenseCategory>> =
        db.expense_categoryQueries.selectByStoreId(storeId).asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { ExpenseCategory(it.id, it.name, it.store_id) } }

    fun insertCategory(cat: ExpenseCategory) {
        db.expense_categoryQueries.insertOrReplace(cat.id, cat.name, cat.storeId, Clock.System.now().toString())
    }

    private fun com.dokonpro.shared.db.Transactions.toTransaction() = Transaction(
        id = id, type = TransactionType.valueOf(type), amount = amount,
        description = description, categoryId = category_id, categoryName = null,
        storeId = store_id, createdAt = created_at
    )
}
