package com.dokonpro.android.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dokonpro.android.service.BluetoothPrinterService
import com.dokonpro.shared.domain.entity.Sale
import com.dokonpro.shared.domain.entity.StoreSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PrinterState(
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val savedAddress: String? = null,
    val isPrinting: Boolean = false,
    val printSuccess: Boolean = false,
    val error: String? = null
)

class PrinterViewModel(
    private val printerService: BluetoothPrinterService
) : ViewModel() {

    private val _state = MutableStateFlow(PrinterState())
    val state: StateFlow<PrinterState> = _state.asStateFlow()

    init {
        loadState()
    }

    fun loadState() {
        val devices = if (printerService.isBluetoothEnabled()) {
            printerService.getPairedDevices()
        } else {
            emptyList()
        }
        _state.value = _state.value.copy(
            pairedDevices = devices,
            savedAddress = printerService.getSavedPrinterAddress()
        )
    }

    fun selectPrinter(address: String) {
        printerService.savePrinterAddress(address)
        _state.value = _state.value.copy(savedAddress = address)
    }

    fun clearPrinter() {
        printerService.clearSavedPrinter()
        _state.value = _state.value.copy(savedAddress = null)
    }

    fun printReceipt(sale: Sale, settings: StoreSettings) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isPrinting = true, error = null, printSuccess = false)
            val result = printerService.printReceipt(sale, settings)
            result.fold(
                onSuccess = {
                    _state.value = _state.value.copy(isPrinting = false, printSuccess = true)
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isPrinting = false,
                        error = e.message ?: "Print failed"
                    )
                }
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun clearPrintSuccess() {
        _state.value = _state.value.copy(printSuccess = false)
    }
}
