package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "paypal_saved_payment_method")

/**
 * Persists the edit-FI pending request across process death, so the launch → return → tokenize
 * flow can resume after the buyer comes back from the PayPal app/browser.
 */
internal class PendingRequestRepository(
    context: Context,
    moduleName: String,
    private val dataStore: DataStore<Preferences> = context.dataStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val pendingRequestKey = stringPreferencesKey("${moduleName}_pending_request_key")

    suspend fun storePendingRequest(pendingRequest: String) {
        withContext(dispatcher) {
            dataStore.edit { preferences: MutablePreferences ->
                preferences[pendingRequestKey] = pendingRequest
            }
        }
    }

    suspend fun getPendingRequest(): String {
        return withContext(dispatcher) {
            val pendingRequest = dataStore.data.map { preferences: Preferences ->
                preferences[pendingRequestKey]
            }.firstOrNull()
            pendingRequest ?: ""
        }
    }

    suspend fun clearPendingRequest() {
        withContext(dispatcher) {
            dataStore.edit { preferences: MutablePreferences ->
                preferences.remove(pendingRequestKey)
            }
        }
    }
}
