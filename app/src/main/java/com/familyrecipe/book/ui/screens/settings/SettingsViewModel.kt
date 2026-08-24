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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** 备份操作类型 */
enum class BackupOperation { EXPORT, IMPORT }

/** 备份/恢复的 UI 状态，由 ViewModel 通过 StateFlow 暴露 */
sealed interface BackupUiState {
    data object Idle : BackupUiState
    data class InProgress(val operation: BackupOperation) : BackupUiState
    data class Success(val operation: BackupOperation) : BackupUiState
    data class Failure(val operation: BackupOperation, val message: String) : BackupUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val database: AppDatabase,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val defaultRandomCount: StateFlow<Int> = settingsStore.defaultRandomCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    private val _backupState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    fun setDefaultRandomCount(count: Int) {
        viewModelScope.launch {
            settingsStore.setDefaultRandomCount(count)
        }
    }

    /**
     * 导出备份。跑在 viewModelScope 中，界面旋转/重建不会中断任务。
     */
    fun exportBackup(uri: Uri) {
        runBackupOperation(BackupOperation.EXPORT) {
            BackupHelper.exportBackup(appContext, database, uri)
        }
    }

    /**
     * 导入恢复。成功后 UI 需引导用户重启应用。
     */
    fun importBackup(uri: Uri) {
        runBackupOperation(BackupOperation.IMPORT) {
            BackupHelper.importBackup(appContext, database, uri)
        }
    }

    /** UI 展示完结果（Toast 等）后调用，把状态复位为 Idle */
    fun acknowledgeBackupResult() {
        if (_backupState.value !is BackupUiState.InProgress) {
            _backupState.value = BackupUiState.Idle
        }
    }

    private fun runBackupOperation(operation: BackupOperation, block: suspend () -> Result<Unit>) {
        if (_backupState.value is BackupUiState.InProgress) return
        _backupState.value = BackupUiState.InProgress(operation)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { block() }
            _backupState.value = result.fold(
                onSuccess = { BackupUiState.Success(operation) },
                onFailure = { BackupUiState.Failure(operation, it.message ?: "未知错误") }
            )
        }
    }
}
