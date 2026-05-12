package com.familyrecipe.book.ui.navigation

object NavRoutes {
    const val RECIPE_LIST = "recipe_list"
    const val RECIPE_DETAIL = "recipe_detail/{recipeId}"
    const val RECIPE_EDIT = "recipe_edit/{recipeId}" // recipeId=0 表示新增
    const val MEMBER_LIST = "member_list"
    const val MEMBER_EDIT = "member_edit/{memberId}" // memberId=0 表示新增
    const val SETTINGS = "settings"
    const val RANDOM_PICK = "random_pick"
    const val SHOPPING_LIST = "shopping_list"

    fun recipeDetail(id: Long) = "recipe_detail/$id"
    fun recipeEdit(id: Long = 0) = "recipe_edit/$id"
    fun memberEdit(id: Long = 0) = "member_edit/$id"
}
