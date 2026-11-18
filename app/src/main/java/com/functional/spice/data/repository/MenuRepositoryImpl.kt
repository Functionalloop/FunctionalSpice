package com.functional.spice.data.repository

import com.functional.spice.data.local.AppDatabase
import com.functional.spice.data.local.dao.MenuItemDao
import com.functional.spice.data.local.entity.MenuItemEntity
import com.functional.spice.data.remote.SupabaseClientProvider
import com.functional.spice.domain.model.Category
import com.functional.spice.domain.model.MenuItem
import com.functional.spice.domain.repository.MenuRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepositoryImpl @Inject constructor(
    private val menuItemDao: MenuItemDao,
    private val supabaseProvider: SupabaseClientProvider
) : MenuRepository {

    init {
        // Pre-populate Room DB on a fresh install only
        CoroutineScope(Dispatchers.IO).launch {
            if (menuItemDao.getMenuItemCount() == 0) {
                menuItemDao.insertMenuItems(AppDatabase.INITIAL_MENU_ITEMS)
            }
        }
    }

    override fun getMenuItems(): Flow<List<MenuItem>> {
        return menuItemDao.getAllMenuItems().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeCategoryLookup(): Flow<Map<String, Category>> {
        return menuItemDao.getAllMenuItemsIncludingDeleted().map { entities ->
            entities.associate { it.id to Category.fromString(it.category) }
        }
    }

    override suspend fun getMenuItemsByCategory(category: Category): List<MenuItem> = withContext(Dispatchers.IO) {
        menuItemDao.getMenuItemsByCategory(category.name).map { it.toDomain() }
    }

    override suspend fun refreshMenuItems(): Result<Unit> = withContext(Dispatchers.IO) {
        // Room stays the single source of truth for the menu
        Result.success(Unit)
    }

    override suspend fun saveMenuItem(item: MenuItem): MenuItem = withContext(Dispatchers.IO) {
        val existing = item.id.takeIf { it.isNotBlank() }?.let { menuItemDao.getMenuItemById(it) }
        if (existing == null) {
            val created = item.copy(
                id = "item_" + UUID.randomUUID().toString().take(8),
                sortOrder = menuItemDao.getMaxSortOrder() + 1
            )
            menuItemDao.insertMenuItem(MenuItemEntity.fromDomain(created))
            created
        } else {
            val updated = existing.copy(
                name = item.name,
                category = item.category.name,
                price = item.price,
                isAvailable = item.isAvailable
            )
            menuItemDao.updateMenuItem(updated)
            updated.toDomain()
        }
    }

    override suspend fun setAvailability(id: String, available: Boolean) = withContext(Dispatchers.IO) {
        menuItemDao.setAvailable(id, available)
    }

    override suspend fun deleteMenuItem(id: String) = withContext(Dispatchers.IO) {
        menuItemDao.softDelete(id)
    }
}
