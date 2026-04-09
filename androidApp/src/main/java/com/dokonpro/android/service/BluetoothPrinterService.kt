package com.dokonpro.android.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.dokonpro.shared.data.local.ReceiptFormatter
import com.dokonpro.shared.domain.entity.Sale
import com.dokonpro.shared.domain.entity.StoreSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * Bluetooth Classic printer service for ESC/POS thermal printers.
 * Connects via RFCOMM (SPP) and sends formatted receipt data.
 *
 * Permissions are handled at the UI level; this service assumes they are granted.
 */
@SuppressLint("MissingPermission")
class BluetoothPrinterService(private val context: Context) {

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val PREFS_NAME = "dokonpro_printer_prefs"
        private const val KEY_PRINTER_ADDRESS = "printer_mac_address"
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter? =
        bluetoothManager?.adapter

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    fun getSavedPrinterAddress(): String? {
        return prefs.getString(KEY_PRINTER_ADDRESS, null)
    }

    fun savePrinterAddress(mac: String) {
        prefs.edit().putString(KEY_PRINTER_ADDRESS, mac).apply()
    }

    fun clearSavedPrinter() {
        prefs.edit().remove(KEY_PRINTER_ADDRESS).apply()
    }

    suspend fun printReceipt(sale: Sale, settings: StoreSettings): Result<Unit> =
        withContext(Dispatchers.IO) {
            val address = getSavedPrinterAddress()
                ?: return@withContext Result.failure(IOException("No printer selected"))

            val device = bluetoothAdapter?.getRemoteDevice(address)
                ?: return@withContext Result.failure(IOException("Bluetooth device not found"))

            var socket: BluetoothSocket? = null
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()

                val outputStream = socket.outputStream

                // Initialize printer
                outputStream.write(EscPosCommands.INIT)

                // Format the receipt text
                val receiptText = ReceiptFormatter.format(sale, settings)

                // Print header centered and bold
                outputStream.write(EscPosCommands.ALIGN_CENTER)
                outputStream.write(EscPosCommands.BOLD_ON)
                outputStream.write(EscPosCommands.FONT_DOUBLE_HEIGHT)
                outputStream.write(EscPosCommands.textToBytes(settings.name))
                outputStream.write(EscPosCommands.LINE_FEED)
                outputStream.write(EscPosCommands.FONT_NORMAL)
                outputStream.write(EscPosCommands.BOLD_OFF)

                // Print address/phone centered
                if (!settings.address.isNullOrBlank()) {
                    outputStream.write(EscPosCommands.textToBytes(settings.address!!))
                    outputStream.write(EscPosCommands.LINE_FEED)
                }
                if (!settings.phone.isNullOrBlank()) {
                    outputStream.write(EscPosCommands.textToBytes(settings.phone!!))
                    outputStream.write(EscPosCommands.LINE_FEED)
                }

                // Print receipt body left-aligned
                outputStream.write(EscPosCommands.ALIGN_LEFT)
                outputStream.write(EscPosCommands.textToBytes(receiptText))
                outputStream.write(EscPosCommands.LINE_FEED)

                // Print QR code centered
                outputStream.write(EscPosCommands.ALIGN_CENTER)
                val qrContent = ReceiptFormatter.qrContent(sale)
                outputStream.write(EscPosCommands.printQrCode(qrContent))
                outputStream.write(EscPosCommands.LINE_FEED)

                // Feed and cut
                outputStream.write(EscPosCommands.FEED_LINES_3)
                outputStream.write(EscPosCommands.CUT_PAPER)

                outputStream.flush()

                Result.success(Unit)
            } catch (e: IOException) {
                Result.failure(e)
            } finally {
                try {
                    socket?.close()
                } catch (_: IOException) {
                    // Ignore close errors
                }
            }
        }
}
