package com.familyrecipe.book.di

import com.familyrecipe.book.data.dao.FamilyMemberDao
import com.familyrecipe.book.data.dao.RecipeDao
import com.familyrecipe.book.data.dao.RecipePreferenceDao
import com.familyrecipe.book.data.repository.FamilyMemberRepository
import com.familyrecipe.book.data.repository.RecipeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideRecipeRepository(
        recipeDao: RecipeDao,
        preferenceDao: RecipePreferenceDao
    ): RecipeRepository = RecipeRepository(recipeDao, preferenceDao)

    @Provides
    @Singleton
    fun provideFamilyMemberRepository(
        memberDao: FamilyMemberDao
    ): FamilyMemberRepository = FamilyMemberRepository(memberDao)
}
