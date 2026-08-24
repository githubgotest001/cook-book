package com.familyrecipe.book.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyrecipe.book.data.database.AppDatabase
import com.familyrecipe.book.data.datastore.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface BackupUiEvent {
    data object ExportSuccess : BackupUiEvent
    data class ExportFailure(val message: String) : BackupUiEvent
    data object ImportSuccess : BackupUiEvent
    data class ImportFailure(val message: String) : BackupUiEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val database: AppDatabase,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val defaultRandomCount: StateFlow<Int> = settingsStore.defaultRandomCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _events = MutableSharedFlow<BackupUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun setDefaultRandomCount(count: Int) {
        viewModelScope.launch {
            settingsStore.setDefaultRandomCount(count)
        }
    }

    fun exportBackup(uri: Uri) {
        if (_isProcessing.value) return
        viewModelScope.launch {
            _isProcessing.value = true
            val result = withContext(Dispatchers.IO) {
                BackupHelper.exportBackup(appContext, database, uri)
            }
            _isProcessing.value = false
            if (result.isSuccess) {
                _events.emit(BackupUiEvent.ExportSuccess)
            } else {
                _events.emit(
                    BackupUiEvent.ExportFailure(result.exceptionOrNull()?.message ?: "未知错误")
                )
            }
        }
    }

    fun importBackup(uri: Uri) {
        if (_isProcessing.value) return
        viewModelScope.launch {
            _isProcessing.value = true
            val result = withContext(Dispatchers.IO) {
                BackupHelper.importBackup(appContext, database, uri)
            }
            _isProcessing.value = false
            if (result.isSuccess) {
                _events.emit(BackupUiEvent.ImportSuccess)
            } else {
                _events.emit(
                    BackupUiEvent.ImportFailure(result.exceptionOrNull()?.message ?: "未知错误")
                )
            }
        }
    }
}
