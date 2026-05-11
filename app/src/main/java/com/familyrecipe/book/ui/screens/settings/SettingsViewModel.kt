package com.familyrecipe.book.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyrecipe.book.data.datastore.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore
) : ViewModel() {

    val defaultRandomCount: StateFlow<Int> = settingsStore.defaultRandomCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    fun setDefaultRandomCount(count: Int) {
        viewModelScope.launch {
            settingsStore.setDefaultRandomCount(count)
        }
    }
}
