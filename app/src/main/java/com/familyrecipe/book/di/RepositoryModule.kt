package com.familyrecipe.book.di

import com.familyrecipe.book.data.dao.FamilyMemberDao
import com.familyrecipe.book.data.dao.RecipeDao
import com.familyrecipe.book.data.dao.RecipeIngredientDao
import com.familyrecipe.book.data.dao.RecipePreferenceDao
import com.familyrecipe.book.data.database.AppDatabase
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
        database: AppDatabase,
        recipeDao: RecipeDao,
        preferenceDao: RecipePreferenceDao,
        ingredientDao: RecipeIngredientDao
    ): RecipeRepository = RecipeRepository(database, recipeDao, preferenceDao, ingredientDao)

    @Provides
    @Singleton
    fun provideFamilyMemberRepository(
        memberDao: FamilyMemberDao
    ): FamilyMemberRepository = FamilyMemberRepository(memberDao)
}
