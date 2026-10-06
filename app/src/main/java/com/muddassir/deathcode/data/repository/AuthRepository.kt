package com.muddassir.deathcode.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.muddassir.deathcode.data.remote.DeathCodeApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The signed-in community account, if any. */
data class AuthSession(
    val token: String? = null,
    val userId: String? = null,
    val email: String? = null,
    val isAdmin: Boolean = false,
) {
    val isSignedIn: Boolean get() = !token.isNullOrBlank()
}

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "deathcode_auth",
)

/**
 * Community authentication.
 *
 * Authentication is **only** needed to submit to Community — the rest of the app is fully
 * anonymous. The token is stored on-device in app-private DataStore storage and never sent
 * anywhere except the Death Code API.
 */
class AuthRepository(
    private val context: Context,
    private val api: DeathCodeApi,
) {

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = stringPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val IS_ADMIN = stringPreferencesKey("is_admin")
    }

    val session: Flow<AuthSession> = context.authDataStore.data.map { prefs ->
        AuthSession(
            token = prefs[Keys.TOKEN],
            userId = prefs[Keys.USER_ID],
            email = prefs[Keys.EMAIL],
            // A client-side admin flag is only used for UI hints; the backend always
            // re-authorizes admin operations server-side.
            isAdmin = prefs[Keys.IS_ADMIN] == "true",
        )
    }

    suspend fun signup(baseUrl: String, email: String, password: String, displayName: String?) {
        val response = api.signup(baseUrl, email, password, displayName)
        persist(response.token, response.userId, response.email, response.isAdmin)
    }

    suspend fun login(baseUrl: String, email: String, password: String) {
        val response = api.login(baseUrl, email, password)
        persist(response.token, response.userId, response.email, response.isAdmin)
    }

    suspend fun signOut() {
        context.authDataStore.edit { it.clear() }
    }

    private suspend fun persist(token: String, userId: String, email: String, isAdmin: Boolean) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.USER_ID] = userId
            prefs[Keys.EMAIL] = email
            prefs[Keys.IS_ADMIN] = isAdmin.toString()
        }
    }
}
