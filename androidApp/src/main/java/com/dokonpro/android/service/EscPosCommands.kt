package com.dokonpro.android.service

import java.nio.charset.Charset

/**
 * ESC/POS command byte constants and helpers for 58mm thermal printers.
 * Uses CP866 charset for Cyrillic text support.
 */
object EscPosCommands {

    // Printer initialization
    val INIT: ByteArray = byteArrayOf(0x1B, 0x40)

    // Line feed
    val LINE_FEED: ByteArray = byteArrayOf(0x0A)

    // Paper cut (partial)
    val CUT_PAPER: ByteArray = byteArrayOf(0x1D, 0x56, 0x01)

    // Bold on/off
    val BOLD_ON: ByteArray = byteArrayOf(0x1B, 0x45, 0x01)
    val BOLD_OFF: ByteArray = byteArrayOf(0x1B, 0x45, 0x00)

    // Text alignment
    val ALIGN_CENTER: ByteArray = byteArrayOf(0x1B, 0x61, 0x01)
    val ALIGN_LEFT: ByteArray = byteArrayOf(0x1B, 0x61, 0x00)
    val ALIGN_RIGHT: ByteArray = byteArrayOf(0x1B, 0x61, 0x02)

    // Font size
    val FONT_NORMAL: ByteArray = byteArrayOf(0x1B, 0x21, 0x00)
    val FONT_DOUBLE_HEIGHT: ByteArray = byteArrayOf(0x1B, 0x21, 0x10)

    // Feed 3 lines (for spacing before cut)
    val FEED_LINES_3: ByteArray = byteArrayOf(0x1B, 0x64, 0x03)

    private val CP866: Charset = Charset.forName("CP866")

    /**
     * Converts text to bytes using CP866 charset for Cyrillic support.
     */
    fun textToBytes(text: String): ByteArray {
        return text.toByteArray(CP866)
    }

    /**
     * Generates ESC/POS commands for printing a QR code.
     * Uses GS ( k command for QR code printing.
     */
    fun printQrCode(content: String): ByteArray {
        val data = content.toByteArray(Charsets.US_ASCII)
        val store = qrStoreData(data)
        val setSize = qrSetSize(4) // module size 4
        val setErrorCorrection = qrSetErrorCorrection(48) // Level L
        val print = qrPrint()

        return setSize + setErrorCorrection + store + print
    }

    private fun qrSetSize(size: Int): ByteArray {
        // GS ( k pL pH cn fn n
        return byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, size.toByte())
    }

    private fun qrSetErrorCorrection(level: Int): ByteArray {
        // GS ( k pL pH cn fn n
        return byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, level.toByte())
    }

    private fun qrStoreData(data: ByteArray): ByteArray {
        // GS ( k pL pH cn fn m d1...dk
        val length = data.size + 3 // cn + fn + m + data
        val pL = (length % 256).toByte()
        val pH = (length / 256).toByte()
        val header = byteArrayOf(0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30)
        return header + data
    }

    private fun qrPrint(): ByteArray {
        // GS ( k pL pH cn fn m
        return byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30)
    }
}
