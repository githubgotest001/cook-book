package com.familyrecipe.book.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyrecipe.book.data.datastore.SettingsStore
import com.familyrecipe.book.data.database.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val database: AppDatabase,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val defaultRandomCount: StateFlow<Int> = settingsStore.defaultRandomCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    fun setDefaultRandomCount(count: Int) {
        viewModelScope.launch {
            settingsStore.setDefaultRandomCount(count)
        }
    }

    suspend fun exportBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        BackupHelper.exportBackup(appContext, database, uri)
    }

    suspend fun importBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        BackupHelper.importBackup(appContext, database, uri)
    }
}
