package com.functional.spice.domain.repository

import com.functional.spice.domain.model.Category
import com.functional.spice.domain.model.MenuItem
import kotlinx.coroutines.flow.Flow

interface MenuRepository {
    /** All menu items that are not deleted, sold-out ones included. */
    fun getMenuItems(): Flow<List<MenuItem>>

    /** Category of every item ever on the menu (deleted included), keyed by item id. */
    fun observeCategoryLookup(): Flow<Map<String, Category>>

    suspend fun getMenuItemsByCategory(category: Category): List<MenuItem>
    suspend fun refreshMenuItems(): Result<Unit>

    /** Inserts a new item (id blank) or updates an existing one. */
    suspend fun saveMenuItem(item: MenuItem): MenuItem
    suspend fun setAvailability(id: String, available: Boolean)

    /** Soft delete: hidden everywhere, old bills keep their lines. */
    suspend fun deleteMenuItem(id: String)
}
