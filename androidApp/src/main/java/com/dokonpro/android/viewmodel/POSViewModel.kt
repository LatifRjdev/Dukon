package com.dokonpro.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dokonpro.shared.domain.entity.Cart
import com.dokonpro.shared.domain.entity.PaymentMethod
import com.dokonpro.shared.domain.entity.Product
import com.dokonpro.shared.domain.entity.Sale
import com.dokonpro.shared.domain.usecase.AddToCartUseCase
import com.dokonpro.shared.domain.usecase.CompleteSaleUseCase
import com.dokonpro.shared.domain.usecase.GetProductsUseCase
import com.dokonpro.shared.domain.usecase.GetSalesHistoryUseCase
import com.dokonpro.shared.domain.usecase.RemoveFromCartUseCase
import com.dokonpro.shared.domain.usecase.SearchProductUseCase
import com.dokonpro.shared.domain.usecase.UpdateCartItemUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class POSState(
    val products: List<Product> = emptyList(),
    val cart: Cart = Cart(),
    val searchQuery: String = "",
    val saleDiscount: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val selectedCustomerId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val completedSale: Sale? = null
)

class POSViewModel(
    private val getProducts: GetProductsUseCase,
    private val searchProducts: SearchProductUseCase,
    private val addToCart: AddToCartUseCase,
    private val removeFromCart: RemoveFromCartUseCase,
    private val updateCartItem: UpdateCartItemUseCase,
    private val completeSale: CompleteSaleUseCase,
    private val getSalesHistory: GetSalesHistoryUseCase,
    private val storeId: String
) : ViewModel() {

    private val _state = MutableStateFlow(POSState())
    val state: StateFlow<POSState> = _state.asStateFlow()

    private val _salesHistory = MutableStateFlow<List<Sale>>(emptyList())
    val salesHistory: StateFlow<List<Sale>> = _salesHistory.asStateFlow()

    init {
        loadProducts()
        loadSalesHistory()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            getProducts(storeId).collect { products ->
                _state.value = _state.value.copy(products = products)
            }
        }
    }

    private fun loadSalesHistory() {
        viewModelScope.launch {
            getSalesHistory(storeId).collect { sales ->
                _salesHistory.value = sales
            }
        }
    }

    fun onSearchChange(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        viewModelScope.launch {
            val flow = if (query.isBlank()) getProducts(storeId) else searchProducts(storeId, query)
            flow.collect { products ->
                _state.value = _state.value.copy(products = products)
            }
        }
    }

    fun addProduct(product: Product) {
        _state.value = _state.value.copy(
            cart = addToCart(_state.value.cart, product.id, product.name, product.price)
        )
    }

    fun removeProduct(productId: String) {
        _state.value = _state.value.copy(
            cart = removeFromCart(_state.value.cart, productId)
        )
    }

    fun updateQuantity(productId: String, quantity: Int) {
        _state.value = _state.value.copy(
            cart = updateCartItem(_state.value.cart, productId, quantity)
        )
    }

    fun setSaleDiscount(discount: Double) {
        _state.value = _state.value.copy(saleDiscount = discount)
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _state.value = _state.value.copy(paymentMethod = method)
    }

    fun setCustomer(customerId: String?) {
        _state.value = _state.value.copy(selectedCustomerId = customerId)
    }

    fun checkout() {
        val s = _state.value
        if (s.cart.items.isEmpty()) return
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true, error = null)
            completeSale(storeId, s.cart, s.saleDiscount, s.paymentMethod, s.selectedCustomerId)
                .onSuccess { sale ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        completedSale = sale,
                        cart = Cart(),
                        saleDiscount = 0.0
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }

    fun clearCompletedSale() {
        _state.value = _state.value.copy(completedSale = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
