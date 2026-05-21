package com.jassin.customdrome

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class UserPreferences(
    private val context: Context,
) {
    companion object {
        val SERVER_URL: Preferences.Key<String> = stringPreferencesKey("server_url")
        val USER_NAME: Preferences.Key<String> = stringPreferencesKey("user_name")
        val PASSWORD: Preferences.Key<String> = stringPreferencesKey("password")
        val TOKEN: Preferences.Key<String> = stringPreferencesKey("token")
        val SUBSONIC_TOKEN: Preferences.Key<String> = stringPreferencesKey("subsonic_token")
        val SUBSONIC_SALT: Preferences.Key<String> = stringPreferencesKey("subsonic_salt")
        val SONG_SORT_ORDER: Preferences.Key<String> = stringPreferencesKey("song_sort_order")
        val SORT_BY: Preferences.Key<String> = stringPreferencesKey("sort_by")
    }

    val server = ServerPreferences(context)
    val auth = AuthPreferences(context)
    val sorting = SortingPreferences(context)

    class ServerPreferences internal constructor(
        private val context: Context,
    ) {
        val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
        val serverURL: Flow<String?> = context.dataStore.data.map { it[SERVER_URL] }
        val password: Flow<String?> = context.dataStore.data.map { it[PASSWORD] }

        suspend fun saveUsername(name: String) {
            context.dataStore.edit { it[USER_NAME] = name }
        }

        suspend fun saveServerURL(url: String) {
            context.dataStore.edit { it[SERVER_URL] = url }
        }

        suspend fun savePassword(password: String) {
            context.dataStore.edit { it[PASSWORD] = password }
        }
    }

    class AuthPreferences internal constructor(
        private val context: Context,
    ) {
        val token: Flow<String?> = context.dataStore.data.map { it[TOKEN] }
        val subsonicToken: Flow<String?> = context.dataStore.data.map { it[SUBSONIC_TOKEN] }
        val subsonicSalt: Flow<String?> = context.dataStore.data.map { it[SUBSONIC_SALT] }

        suspend fun saveToken(token: String) {
            context.dataStore.edit { it[TOKEN] = token }
        }

        suspend fun saveSubsonicToken(token: String) {
            context.dataStore.edit { it[SUBSONIC_TOKEN] = token }
        }

        suspend fun saveSubsonicSalt(salt: String) {
            context.dataStore.edit { it[SUBSONIC_SALT] = salt }
        }
    }

    class SortingPreferences internal constructor(
        private val context: Context,
    ) {
        val songSortOrder: Flow<String?> = context.dataStore.data.map { it[SONG_SORT_ORDER] }
        val sortBy: Flow<String?> = context.dataStore.data.map { it[SORT_BY] }

        suspend fun saveSongSortOrder(sort: String) {
            context.dataStore.edit { it[SONG_SORT_ORDER] = sort }
        }

        suspend fun saveSortBy(method: String) {
            context.dataStore.edit { it[SORT_BY] = method }
        }
    }
}
