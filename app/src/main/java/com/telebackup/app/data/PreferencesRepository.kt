package com.telebackup.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "telebackup_prefs")

data class AppSettings(
    val botToken: String = "",
    val chatId: String = "",
    val folderUris: Set<String> = emptySet(),
    val lastBackupAt: String = "",
    val metadata: MetadataOptions = MetadataOptions(),
    val darkTheme: Boolean = false,
    val cloudIndexMessageId: Long = 0L,
    val cloudIndexFileId: String = ""
) {
    val isConfigured: Boolean
        get() = botToken.isNotBlank() && chatId.isNotBlank()
}

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val BOT_TOKEN = stringPreferencesKey("bot_token")
        val CHAT_ID = stringPreferencesKey("chat_id")
        val FOLDER_URIS = stringSetPreferencesKey("folder_uris")
        val LAST_BACKUP = stringPreferencesKey("last_backup")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val CLOUD_INDEX_MSG = longPreferencesKey("cloud_index_msg_id")
        val CLOUD_INDEX_FILE = stringPreferencesKey("cloud_index_file_id")

        val KEEP_ORIGINAL = booleanPreferencesKey("meta_keep_original")
        val STRIP_LOCATION = booleanPreferencesKey("meta_strip_location")
        val STRIP_CAMERA = booleanPreferencesKey("meta_strip_camera")
        val STRIP_ALL_EXIF = booleanPreferencesKey("meta_strip_all_exif")
        val CAP_LOCATION = booleanPreferencesKey("meta_cap_location")
        val CAP_DATE = booleanPreferencesKey("meta_cap_date")
        val CAP_FILENAME = booleanPreferencesKey("meta_cap_filename")
        val CAP_FOLDER = booleanPreferencesKey("meta_cap_folder")
        val CAP_SIZE = booleanPreferencesKey("meta_cap_size")
        val CAP_CAMERA = booleanPreferencesKey("meta_cap_camera")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            botToken = prefs[Keys.BOT_TOKEN].orEmpty(),
            chatId = prefs[Keys.CHAT_ID].orEmpty(),
            folderUris = prefs[Keys.FOLDER_URIS] ?: emptySet(),
            lastBackupAt = prefs[Keys.LAST_BACKUP].orEmpty(),
            darkTheme = prefs[Keys.DARK_THEME] ?: false,
            cloudIndexMessageId = prefs[Keys.CLOUD_INDEX_MSG] ?: 0L,
            cloudIndexFileId = prefs[Keys.CLOUD_INDEX_FILE].orEmpty(),
            metadata = MetadataOptions(
                keepOriginalFile = prefs[Keys.KEEP_ORIGINAL] ?: true,
                stripLocation = prefs[Keys.STRIP_LOCATION] ?: false,
                stripCameraInfo = prefs[Keys.STRIP_CAMERA] ?: false,
                stripAllExif = prefs[Keys.STRIP_ALL_EXIF] ?: false,
                includeLocationInCaption = prefs[Keys.CAP_LOCATION] ?: false,
                includeDateInCaption = prefs[Keys.CAP_DATE] ?: true,
                includeFileNameInCaption = prefs[Keys.CAP_FILENAME] ?: true,
                includeFolderInCaption = prefs[Keys.CAP_FOLDER] ?: true,
                includeSizeInCaption = prefs[Keys.CAP_SIZE] ?: true,
                includeCameraInCaption = prefs[Keys.CAP_CAMERA] ?: false
            )
        )
    }

    suspend fun saveBotConfig(token: String, chatId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BOT_TOKEN] = token.trim()
            prefs[Keys.CHAT_ID] = chatId.trim()
        }
    }

    suspend fun addFolder(uri: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FOLDER_URIS] ?: emptySet()
            prefs[Keys.FOLDER_URIS] = current + uri
        }
    }

    suspend fun removeFolder(uri: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FOLDER_URIS] ?: emptySet()
            prefs[Keys.FOLDER_URIS] = current - uri
        }
    }

    suspend fun setLastBackup(timestamp: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_BACKUP] = timestamp
        }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = dark
        }
    }

    suspend fun setCloudIndexMeta(messageId: Long, fileId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CLOUD_INDEX_MSG] = messageId
            prefs[Keys.CLOUD_INDEX_FILE] = fileId
        }
    }

    suspend fun saveMetadataOptions(options: MetadataOptions) {
        context.dataStore.edit { prefs ->
            prefs[Keys.KEEP_ORIGINAL] = options.keepOriginalFile
            prefs[Keys.STRIP_LOCATION] = options.stripLocation
            prefs[Keys.STRIP_CAMERA] = options.stripCameraInfo
            prefs[Keys.STRIP_ALL_EXIF] = options.stripAllExif
            prefs[Keys.CAP_LOCATION] = options.includeLocationInCaption
            prefs[Keys.CAP_DATE] = options.includeDateInCaption
            prefs[Keys.CAP_FILENAME] = options.includeFileNameInCaption
            prefs[Keys.CAP_FOLDER] = options.includeFolderInCaption
            prefs[Keys.CAP_SIZE] = options.includeSizeInCaption
            prefs[Keys.CAP_CAMERA] = options.includeCameraInCaption
        }
    }
}
