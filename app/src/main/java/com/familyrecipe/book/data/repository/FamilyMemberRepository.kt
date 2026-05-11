package com.familyrecipe.book.data.repository

import com.familyrecipe.book.data.dao.FamilyMemberDao
import com.familyrecipe.book.data.model.FamilyMember
import kotlinx.coroutines.flow.Flow

class FamilyMemberRepository(
    private val memberDao: FamilyMemberDao
) {
    fun getAllMembers(): Flow<List<FamilyMember>> = memberDao.getAllMembers()

    suspend fun getMemberById(id: Long): FamilyMember? = memberDao.getMemberById(id)

    suspend fun insertMember(member: FamilyMember): Long = memberDao.insertMember(member)

    suspend fun updateMember(member: FamilyMember) = memberDao.updateMember(member)

    suspend fun deleteMember(member: FamilyMember) = memberDao.deleteMember(member)
}
