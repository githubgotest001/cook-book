package com.familyrecipe.book.ui.screens.memberEdit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyrecipe.book.data.model.FamilyMember
import com.familyrecipe.book.data.repository.FamilyMemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemberEditUiState(
    val name: String = "",
    val colorHex: String = "#FF6B6B",
    val note: String = "",
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class MemberEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FamilyMemberRepository
) : ViewModel() {

    private val memberId: Long = savedStateHandle.get<Long>("memberId") ?: 0L

    private val _uiState = MutableStateFlow(MemberEditUiState())
    val uiState: StateFlow<MemberEditUiState> = _uiState.asStateFlow()

    init {
        if (memberId > 0) {
            _uiState.update { it.copy(isLoading = true, isEditMode = true) }
            viewModelScope.launch {
                val member = repository.getMemberById(memberId)
                if (member != null) {
                    _uiState.update {
                        it.copy(
                            name = member.name,
                            colorHex = member.colorHex,
                            note = member.note,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onColorChange(value: String) = _uiState.update { it.copy(colorHex = value) }
    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }

    fun save(onDone: () -> Unit) {
        val state = _uiState.value
        if (state.name.isBlank()) return

        viewModelScope.launch {
            if (memberId > 0) {
                val existing = repository.getMemberById(memberId) ?: return@launch
                repository.updateMember(
                    existing.copy(
                        name = state.name,
                        colorHex = state.colorHex,
                        note = state.note
                    )
                )
            } else {
                repository.insertMember(
                    FamilyMember(
                        name = state.name,
                        colorHex = state.colorHex,
                        note = state.note
                    )
                )
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (memberId <= 0) return
        viewModelScope.launch {
            val member = repository.getMemberById(memberId) ?: return@launch
            repository.deleteMember(member)
            onDone()
        }
    }
}
