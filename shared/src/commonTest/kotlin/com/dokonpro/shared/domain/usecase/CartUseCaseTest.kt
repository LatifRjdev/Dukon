package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Cart
import com.dokonpro.shared.domain.entity.CartItem
import kotlin.test.Test
import kotlin.test.assertEquals

class AddToCartUseCaseTest {
    private val useCase = AddToCartUseCase()

    @Test
    fun `should add new item to empty cart`() {
        val cart = useCase(Cart(), "p1", "Coca-Cola", 12.5)
        assertEquals(1, cart.items.size)
        assertEquals("Coca-Cola", cart.items[0].name)
        assertEquals(1, cart.items[0].quantity)
    }

    @Test
    fun `should increment quantity when adding existing product`() {
        val cart = Cart(items = listOf(CartItem("p1", "Coca-Cola", 12.5, 2)))
        val updated = useCase(cart, "p1", "Coca-Cola", 12.5)
        assertEquals(1, updated.items.size)
        assertEquals(3, updated.items[0].quantity)
    }
}

class RemoveFromCartUseCaseTest {
    private val useCase = RemoveFromCartUseCase()

    @Test
    fun `should remove item from cart`() {
        val cart = Cart(items = listOf(CartItem("p1", "Coca-Cola", 12.5, 2), CartItem("p2", "Bread", 3.0, 1)))
        val updated = useCase(cart, "p1")
        assertEquals(1, updated.items.size)
        assertEquals("Bread", updated.items[0].name)
    }
}

class UpdateCartItemUseCaseTest {
    private val useCase = UpdateCartItemUseCase()

    @Test
    fun `should update quantity`() {
        val cart = Cart(items = listOf(CartItem("p1", "Coca-Cola", 12.5, 1)))
        val updated = useCase(cart, "p1", 5)
        assertEquals(5, updated.items[0].quantity)
    }

    @Test
    fun `should remove item when quantity is zero`() {
        val cart = Cart(items = listOf(CartItem("p1", "Coca-Cola", 12.5, 3)))
        val updated = useCase(cart, "p1", 0)
        assertEquals(0, updated.items.size)
    }

    @Test
    fun `should apply per-item discount`() {
        val cart = Cart(items = listOf(CartItem("p1", "Coca-Cola", 12.5, 2)))
        val updated = useCase(cart, "p1", 2, 2.0)
        assertEquals(2.0, updated.items[0].discount)
        assertEquals(21.0, updated.items[0].subtotal)
    }
}

class CartTest {
    @Test
    fun `should calculate totals correctly`() {
        val cart = Cart(items = listOf(
            CartItem("p1", "Coca-Cola", 12.5, 2),
            CartItem("p2", "Bread", 3.0, 3, 0.5)
        ))
        assertEquals(5, cart.totalItems)
        assertEquals(32.5, cart.subtotal)
        assertEquals(30.5, cart.totalWithDiscount(2.0))
    }
}
