package com.dokonpro.shared.data.local

import com.dokonpro.shared.domain.entity.Sale
import com.dokonpro.shared.domain.entity.StoreSettings

object ReceiptFormatter {

    private const val LINE_WIDTH = 32

    fun format(sale: Sale, settings: StoreSettings): String {
        val sb = StringBuilder()

        // Header: store name centered
        sb.appendCentered(settings.name)

        // Address
        if (!settings.address.isNullOrBlank()) {
            sb.appendCentered(settings.address!!)
        }

        // Phone
        if (!settings.phone.isNullOrBlank()) {
            sb.appendCentered(settings.phone!!)
        }

        // Custom receipt header
        if (!settings.receiptHeader.isNullOrBlank()) {
            sb.appendCentered(settings.receiptHeader!!)
        }

        sb.appendLine(separatorLine())

        // Receipt number and date
        sb.appendLine("Чек №${sale.id.takeLast(8).uppercase()}")
        sb.appendLine(sale.createdAt)

        sb.appendLine(separatorLine())

        // Items
        for (item in sale.items) {
            val itemTotal = item.price * item.quantity
            val left = "${item.name} x${item.quantity}"
            val right = "%.2f".format(itemTotal)
            sb.appendLine(leftRight(left, right))
        }

        sb.appendLine(separatorLine())

        // Subtotal
        val subtotal = sale.items.sumOf { it.price * it.quantity }
        sb.appendLine(leftRight("Подытог:", "%.2f".format(subtotal)))

        // Discount
        if (sale.discount > 0) {
            sb.appendLine(leftRight("Скидка:", "-%.2f".format(sale.discount)))
        }

        // Total
        sb.appendLine(leftRight("ИТОГО:", "%.2f".format(sale.totalAmount)))

        // Payment method
        val methodLabel = when (sale.paymentMethod.name) {
            "CASH" -> "Наличные"
            "CARD" -> "Карта"
            "MIXED" -> "Смешанный"
            else -> sale.paymentMethod.name
        }
        sb.appendLine(leftRight("Оплата:", methodLabel))

        sb.appendLine(separatorLine())

        // QR content line
        sb.appendCentered(qrContent(sale))

        // Footer
        if (!settings.receiptFooter.isNullOrBlank()) {
            sb.appendCentered(settings.receiptFooter!!)
        }

        return sb.toString().trimEnd()
    }

    fun qrContent(sale: Sale): String = "sale:${sale.id}"

    private fun separatorLine(): String = "-".repeat(LINE_WIDTH)

    private fun StringBuilder.appendCentered(text: String) {
        for (line in wrapText(text)) {
            val padding = (LINE_WIDTH - line.length).coerceAtLeast(0) / 2
            appendLine(" ".repeat(padding) + line)
        }
    }

    private fun leftRight(left: String, right: String): String {
        val available = LINE_WIDTH - right.length - 1
        val truncatedLeft = if (left.length > available) left.take(available) else left
        val spaces = (LINE_WIDTH - truncatedLeft.length - right.length).coerceAtLeast(1)
        return truncatedLeft + " ".repeat(spaces) + right
    }

    private fun wrapText(text: String): List<String> {
        if (text.length <= LINE_WIDTH) return listOf(text)
        val lines = mutableListOf<String>()
        var remaining = text
        while (remaining.length > LINE_WIDTH) {
            val breakAt = remaining.lastIndexOf(' ', LINE_WIDTH)
            val splitAt = if (breakAt > 0) breakAt else LINE_WIDTH
            lines.add(remaining.substring(0, splitAt).trimEnd())
            remaining = remaining.substring(splitAt).trimStart()
        }
        if (remaining.isNotEmpty()) lines.add(remaining)
        return lines
    }
}
