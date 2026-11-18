package com.functional.spice.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.functional.spice.data.local.entity.MenuItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuItemDao {
    /** Every item that isn't deleted, including sold-out ones (Menu manager). */
    @Query("SELECT * FROM menu_items WHERE isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
    fun getAllMenuItems(): Flow<List<MenuItemEntity>>

    /** Includes deleted items: used to find categories for old bill lines. */
    @Query("SELECT * FROM menu_items")
    fun getAllMenuItemsIncludingDeleted(): Flow<List<MenuItemEntity>>

    @Query("SELECT * FROM menu_items WHERE category = :category AND isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
    suspend fun getMenuItemsByCategory(category: String): List<MenuItemEntity>

    @Query("SELECT * FROM menu_items WHERE id = :id LIMIT 1")
    suspend fun getMenuItemById(id: String): MenuItemEntity?

    /** Counts deleted rows too, so the starter menu is only seeded on a fresh install. */
    @Query("SELECT COUNT(*) FROM menu_items")
    suspend fun getMenuItemCount(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM menu_items")
    suspend fun getMaxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItems(items: List<MenuItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItem(item: MenuItemEntity)

    @Update
    suspend fun updateMenuItem(item: MenuItemEntity)

    @Query("UPDATE menu_items SET isAvailable = :available WHERE id = :id")
    suspend fun setAvailable(id: String, available: Boolean)

    @Query("UPDATE menu_items SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: String)
}
