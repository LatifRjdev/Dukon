package com.dokonpro.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dokonpro.shared.domain.entity.DailyRevenue
import com.dokonpro.shared.domain.entity.FinanceSummary
import com.dokonpro.shared.domain.entity.Transaction
import com.dokonpro.shared.domain.usecase.AddExpenseUseCase
import com.dokonpro.shared.domain.usecase.GetFinanceSummaryUseCase
import com.dokonpro.shared.domain.usecase.GetReportUseCase
import com.dokonpro.shared.domain.usecase.GetTransactionsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FinanceState(
    val summary: FinanceSummary? = null,
    val transactions: List<Transaction> = emptyList(),
    val report: List<DailyRevenue> = emptyList(),
    val selectedPeriod: String = "day",
    val isLoading: Boolean = false,
    val error: String? = null
)

class FinanceViewModel(
    private val getFinanceSummary: GetFinanceSummaryUseCase,
    private val getTransactions: GetTransactionsUseCase,
    private val addExpense: AddExpenseUseCase,
    private val getReport: GetReportUseCase,
    private val storeId: String
) : ViewModel() {

    private val _state = MutableStateFlow(FinanceState())
    val state: StateFlow<FinanceState> = _state.asStateFlow()

    init {
        loadSummary("day")
        loadTransactions()
        loadReport("week")
    }

    fun selectPeriod(period: String) {
        _state.value = _state.value.copy(selectedPeriod = period)
        loadSummary(period)
    }

    private fun loadSummary(period: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            getFinanceSummary(storeId, period)
                .onSuccess { summary ->
                    _state.value = _state.value.copy(summary = summary, isLoading = false)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(error = e.message, isLoading = false)
                }
        }
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            getTransactions(storeId).collect { transactions ->
                _state.value = _state.value.copy(transactions = transactions)
            }
        }
    }

    fun loadReport(period: String) {
        viewModelScope.launch {
            getReport(storeId, period)
                .onSuccess { report ->
                    _state.value = _state.value.copy(report = report)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(error = e.message)
                }
        }
    }

    fun submitExpense(amount: Double, description: String?, categoryId: String?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            addExpense(storeId, amount, description, categoryId)
                .onSuccess {
                    loadSummary(_state.value.selectedPeriod)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(error = e.message, isLoading = false)
                }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
