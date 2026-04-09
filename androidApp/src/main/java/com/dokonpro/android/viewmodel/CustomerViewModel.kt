package com.dokonpro.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dokonpro.shared.domain.entity.Customer
import com.dokonpro.shared.domain.entity.Sale
import com.dokonpro.shared.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant

data class CustomerListState(
    val customers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

data class CustomerDetailState(
    val customer: Customer? = null,
    val purchases: List<Sale> = emptyList(),
    val isLoading: Boolean = false
)

class CustomerViewModel(
    private val getCustomers: GetCustomersUseCase,
    private val createCustomer: CreateCustomerUseCase,
    private val updateCustomer: UpdateCustomerUseCase,
    private val searchCustomers: SearchCustomerUseCase,
    private val getPurchases: GetCustomerPurchasesUseCase,
    private val storeId: String
) : ViewModel() {
    private val _listState = MutableStateFlow(CustomerListState())
    val listState: StateFlow<CustomerListState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(CustomerDetailState())
    val detailState: StateFlow<CustomerDetailState> = _detailState.asStateFlow()

    init { loadCustomers() }

    private fun loadCustomers() {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(isLoading = true)
            getCustomers(storeId).collect { _listState.value = _listState.value.copy(customers = it, isLoading = false) }
        }
    }

    fun onSearchChange(query: String) {
        _listState.value = _listState.value.copy(searchQuery = query)
        viewModelScope.launch {
            val flow = if (query.isBlank()) getCustomers(storeId) else searchCustomers(storeId, query)
            flow.collect { _listState.value = _listState.value.copy(customers = it) }
        }
    }

    fun addCustomer(name: String, phone: String?, email: String?, notes: String?) {
        val now = Instant.now().toString()
        val customer = Customer(
            id = "cust-${Instant.now().toEpochMilli()}-${(1000..9999).random()}",
            name = name, phone = phone, email = email, notes = notes,
            totalSpent = 0.0, visitCount = 0, storeId = storeId, createdAt = now, updatedAt = now
        )
        viewModelScope.launch {
            createCustomer(storeId, customer).onFailure { _listState.value = _listState.value.copy(error = it.message) }
        }
    }

    fun updateExistingCustomer(customer: Customer) {
        viewModelScope.launch {
            updateCustomer(customer)
                .onFailure { _listState.value = _listState.value.copy(error = it.message) }
        }
    }

    fun loadCustomerDetail(customerId: String) {
        viewModelScope.launch {
            _detailState.value = CustomerDetailState(isLoading = true)
            val customer = _listState.value.customers.find { it.id == customerId }
            _detailState.value = _detailState.value.copy(customer = customer, isLoading = false)
            getPurchases(storeId, customerId).onSuccess { _detailState.value = _detailState.value.copy(purchases = it) }
        }
    }

    fun clearError() { _listState.value = _listState.value.copy(error = null) }
}
