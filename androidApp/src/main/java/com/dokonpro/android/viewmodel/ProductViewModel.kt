package com.dokonpro.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dokonpro.shared.data.sync.SyncManager
import com.dokonpro.shared.data.sync.SyncStatusData
import com.dokonpro.shared.domain.entity.Product
import com.dokonpro.shared.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant

data class ProductListState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

class ProductViewModel(
    private val getProducts: GetProductsUseCase,
    private val createProduct: CreateProductUseCase,
    private val updateProduct: UpdateProductUseCase,
    private val deleteProduct: DeleteProductUseCase,
    private val searchProducts: SearchProductUseCase,
    private val syncManager: SyncManager,
    private val storeId: String
) : ViewModel() {

    private val _state = MutableStateFlow(ProductListState())
    val state: StateFlow<ProductListState> = _state.asStateFlow()

    val syncStatus: StateFlow<SyncStatusData> = syncManager.status

    init {
        loadProducts()
        syncManager.startSync(viewModelScope)
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            getProducts(storeId).collect { products ->
                _state.value = _state.value.copy(products = products, isLoading = false)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        viewModelScope.launch {
            if (query.isBlank()) {
                getProducts(storeId).collect { products ->
                    _state.value = _state.value.copy(products = products)
                }
            } else {
                searchProducts(storeId, query).collect { products ->
                    _state.value = _state.value.copy(products = products)
                }
            }
        }
    }

    fun addProduct(
        name: String, barcode: String?, price: Double, costPrice: Double,
        quantity: Int, unit: String, categoryId: String?
    ) {
        val now = Instant.now().toString()
        val product = Product(
            id = "local-${System.currentTimeMillis()}-${(1000..9999).random()}",
            name = name, barcode = barcode, sku = null, price = price, costPrice = costPrice,
            quantity = quantity, unit = unit, categoryId = categoryId, categoryName = null,
            imageUrl = null, storeId = storeId, createdAt = now, updatedAt = now
        )
        viewModelScope.launch {
            createProduct(storeId, product)
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
        }
    }

    fun updateExistingProduct(product: Product) {
        viewModelScope.launch {
            updateProduct(product)
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
        }
    }

    fun removeProduct(productId: String) {
        viewModelScope.launch {
            deleteProduct(storeId, productId)
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
