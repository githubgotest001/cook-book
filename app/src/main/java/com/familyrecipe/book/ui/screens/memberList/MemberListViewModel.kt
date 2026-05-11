package com.familyrecipe.book.ui.screens.memberList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyrecipe.book.data.model.FamilyMember
import com.familyrecipe.book.data.repository.FamilyMemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemberListUiState(
    val members: List<FamilyMember> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class MemberListViewModel @Inject constructor(
    private val repository: FamilyMemberRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemberListUiState())
    val uiState: StateFlow<MemberListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllMembers().collect { members ->
                _uiState.update { it.copy(members = members, isLoading = false) }
            }
        }
    }

    fun deleteMember(member: FamilyMember) {
        viewModelScope.launch {
            repository.deleteMember(member)
        }
    }
}
