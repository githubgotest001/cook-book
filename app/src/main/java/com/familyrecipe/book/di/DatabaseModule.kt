package com.familyrecipe.book.di

import android.content.Context
import androidx.room.Room
import com.familyrecipe.book.data.dao.FamilyMemberDao
import com.familyrecipe.book.data.dao.RecipeDao
import com.familyrecipe.book.data.dao.RecipeIngredientDao
import com.familyrecipe.book.data.dao.RecipePreferenceDao
import com.familyrecipe.book.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideRecipeDao(db: AppDatabase): RecipeDao = db.recipeDao()

    @Provides
    fun provideFamilyMemberDao(db: AppDatabase): FamilyMemberDao = db.familyMemberDao()

    @Provides
    fun provideRecipePreferenceDao(db: AppDatabase): RecipePreferenceDao = db.recipePreferenceDao()

    @Provides
    fun provideRecipeIngredientDao(db: AppDatabase): RecipeIngredientDao = db.recipeIngredientDao()
}
