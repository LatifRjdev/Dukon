package com.dokonpro.shared.data.local

import com.dokonpro.shared.domain.entity.PaymentMethod
import com.dokonpro.shared.domain.entity.Sale
import com.dokonpro.shared.domain.entity.SaleItem
import com.dokonpro.shared.domain.entity.StoreSettings
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertContains

class ReceiptFormatterTest {

    private val settings = StoreSettings(
        id = "s1",
        name = "Мой Магазин",
        address = "ул. Рудаки 10",
        phone = "+992901234567",
        currency = "TJS",
        logoUrl = null,
        receiptHeader = "Добро пожаловать!",
        receiptFooter = "Спасибо за покупку!",
        storeId = "store-1"
    )

    private val sale = Sale(
        id = "sale-abcd1234efgh",
        totalAmount = 185.00,
        discount = 15.00,
        paymentMethod = PaymentMethod.CASH,
        customerId = null,
        storeId = "store-1",
        isRefunded = false,
        createdAt = "2026-04-09T12:00:00Z",
        items = listOf(
            SaleItem(
                id = "item-1",
                saleId = "sale-abcd1234efgh",
                productId = "p1",
                name = "Хлеб",
                quantity = 2,
                price = 5.00,
                discount = 0.0
            ),
            SaleItem(
                id = "item-2",
                saleId = "sale-abcd1234efgh",
                productId = "p2",
                name = "Молоко",
                quantity = 3,
                price = 65.00,
                discount = 0.0
            )
        )
    )

    @Test
    fun `should contain store name in header`() {
        val receipt = ReceiptFormatter.format(sale, settings)
        assertContains(receipt, "Мой Магазин")
    }

    @Test
    fun `should contain store address and phone`() {
        val receipt = ReceiptFormatter.format(sale, settings)
        assertContains(receipt, "ул. Рудаки 10")
        assertContains(receipt, "+992901234567")
    }

    @Test
    fun `should contain items with quantity and total`() {
        val receipt = ReceiptFormatter.format(sale, settings)
        assertContains(receipt, "Хлеб x2")
        assertContains(receipt, "10.00")
        assertContains(receipt, "Молоко x3")
        assertContains(receipt, "195.00")
    }

    @Test
    fun `should contain total amount`() {
        val receipt = ReceiptFormatter.format(sale, settings)
        assertContains(receipt, "ИТОГО:")
        assertContains(receipt, "185.00")
    }

    @Test
    fun `should contain discount when present`() {
        val receipt = ReceiptFormatter.format(sale, settings)
        assertContains(receipt, "Скидка:")
        assertContains(receipt, "-15.00")
    }

    @Test
    fun `should not contain discount when zero`() {
        val noDiscountSale = sale.copy(discount = 0.0)
        val receipt = ReceiptFormatter.format(noDiscountSale, settings)
        assertTrue(!receipt.contains("Скидка:"))
    }

    @Test
    fun `should contain receipt footer`() {
        val receipt = ReceiptFormatter.format(sale, settings)
        assertContains(receipt, "Спасибо за покупку!")
    }

    @Test
    fun `should contain receipt header`() {
        val receipt = ReceiptFormatter.format(sale, settings)
        assertContains(receipt, "Добро пожаловать!")
    }

    @Test
    fun `should generate correct QR content`() {
        val qr = ReceiptFormatter.qrContent(sale)
        assertEquals("sale:sale-abcd1234efgh", qr)
    }

    @Test
    fun `should contain QR content in formatted receipt`() {
        val receipt = ReceiptFormatter.format(sale, settings)
        assertContains(receipt, "sale:sale-abcd1234efgh")
    }

    @Test
    fun `should contain payment method`() {
        val receipt = ReceiptFormatter.format(sale, settings)
        assertContains(receipt, "Наличные")
    }

    @Test
    fun `should contain receipt number`() {
        val receipt = ReceiptFormatter.format(sale, settings)
        assertContains(receipt, "Чек №1234EFGH")
    }
}
