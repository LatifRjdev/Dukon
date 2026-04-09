package com.dokonpro.shared.data.local

import com.dokonpro.shared.db.DokonProDatabase
import com.dokonpro.shared.domain.entity.StoreSettings

class StoreSettingsLocalDataSource(private val db: DokonProDatabase) {

    fun getSettings(storeId: String): StoreSettings? =
        db.store_settingsQueries.selectByStoreId(storeId).executeAsOneOrNull()?.let {
            StoreSettings(
                id = it.id,
                name = it.name,
                address = it.address,
                phone = it.phone,
                currency = it.currency,
                logoUrl = it.logo_url,
                receiptHeader = it.receipt_header,
                receiptFooter = it.receipt_footer,
                storeId = it.store_id
            )
        }

    fun saveSettings(settings: StoreSettings) {
        db.store_settingsQueries.insertOrReplace(
            settings.id,
            settings.name,
            settings.address,
            settings.phone,
            settings.currency,
            settings.logoUrl,
            settings.receiptHeader,
            settings.receiptFooter,
            settings.storeId
        )
    }
}
