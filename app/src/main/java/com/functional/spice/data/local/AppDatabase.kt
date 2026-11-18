package com.functional.spice.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.functional.spice.data.local.dao.BillDao
import com.functional.spice.data.local.dao.DayCloseDao
import com.functional.spice.data.local.dao.MenuItemDao
import com.functional.spice.data.local.entity.BillEntity
import com.functional.spice.data.local.entity.BillItemEntity
import com.functional.spice.data.local.entity.DayCloseEntity
import com.functional.spice.data.local.entity.MenuItemEntity
import com.functional.spice.domain.model.Category

@Database(
    entities = [
        MenuItemEntity::class,
        BillEntity::class,
        BillItemEntity::class,
        DayCloseEntity::class
    ],
    // v2: payment details, print queue, category snapshot, soft delete, day closes (see Migrations.kt)
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun menuItemDao(): MenuItemDao
    abstract fun billDao(): BillDao
    abstract fun dayCloseDao(): DayCloseDao

    companion object {
        const val DATABASE_NAME = "functional_spice_pos.db"

        // Authentic Goan Highway Tea Stall Starter Menu
        val INITIAL_MENU_ITEMS = listOf(
            // Hot Tea
            MenuItemEntity(
                id = "item_special_chai",
                name = "Special Chai",
                konkaniSubtitle = "Garam Dudh Chai",
                category = Category.HOT_TEA.name,
                price = 15.0,
                sortOrder = 1
            ),
            MenuItemEntity(
                id = "item_masala_tea",
                name = "Masala Tea",
                konkaniSubtitle = "Cardamom & Ginger",
                category = Category.HOT_TEA.name,
                price = 20.0,
                sortOrder = 2
            ),
            MenuItemEntity(
                id = "item_lemongrass_chai",
                name = "Lemongrass Chai",
                konkaniSubtitle = "Goan Herbal Infusion",
                category = Category.HOT_TEA.name,
                price = 20.0,
                sortOrder = 3
            ),
            MenuItemEntity(
                id = "item_kadak_chai",
                name = "Kadak Chai",
                konkaniSubtitle = "Strong Assam Blend",
                category = Category.HOT_TEA.name,
                price = 15.0,
                sortOrder = 4
            ),
            MenuItemEntity(
                id = "item_ginger_lemon",
                name = "Ginger Lemon Tea",
                konkaniSubtitle = "Highway Refresher",
                category = Category.HOT_TEA.name,
                price = 20.0,
                sortOrder = 5
            ),
            MenuItemEntity(
                id = "item_sulaimani",
                name = "Sulaimani Tea",
                konkaniSubtitle = "Black Tea & Mint",
                category = Category.HOT_TEA.name,
                price = 15.0,
                sortOrder = 6
            ),

            // Snacks
            MenuItemEntity(
                id = "item_bhaji_pav",
                name = "Bhaji Pav",
                konkaniSubtitle = "Goan Usal & Soft Pav",
                category = Category.SNACKS.name,
                price = 40.0,
                sortOrder = 7
            ),
            MenuItemEntity(
                id = "item_bun_maska",
                name = "Bun Maska",
                konkaniSubtitle = "Warm Pav with Butter",
                category = Category.SNACKS.name,
                price = 25.0,
                sortOrder = 8
            ),
            MenuItemEntity(
                id = "item_samosa_pav",
                name = "Samosa Pav",
                konkaniSubtitle = "Crispy Fried Samosa",
                category = Category.SNACKS.name,
                price = 20.0,
                sortOrder = 9
            ),
            MenuItemEntity(
                id = "item_mirchi_bajji",
                name = "Mirchi Bajji",
                konkaniSubtitle = "Goan Spiced Chilli Fritter",
                category = Category.SNACKS.name,
                price = 25.0,
                sortOrder = 10
            ),
            MenuItemEntity(
                id = "item_batata_vada",
                name = "Batata Vada",
                konkaniSubtitle = "Golden Spiced Potato Dumpling",
                category = Category.SNACKS.name,
                price = 15.0,
                sortOrder = 11
            ),
            MenuItemEntity(
                id = "item_poha",
                name = "Poha",
                konkaniSubtitle = "Curry Leaves & Peanuts",
                category = Category.SNACKS.name,
                price = 30.0,
                sortOrder = 12
            ),

            // Cold Drinks
            MenuItemEntity(
                id = "item_kokum_soda",
                name = "Kokum Soda",
                konkaniSubtitle = "Goan Sol Kokum Fizz",
                category = Category.COLD_DRINKS.name,
                price = 25.0,
                sortOrder = 13
            ),
            MenuItemEntity(
                id = "item_sol_kadhi",
                name = "Sol Kadhi",
                konkaniSubtitle = "Coconut Milk & Kokum",
                category = Category.COLD_DRINKS.name,
                price = 30.0,
                sortOrder = 14
            ),
            MenuItemEntity(
                id = "item_cold_coffee",
                name = "Cold Coffee",
                konkaniSubtitle = "Chilled Thick Brew",
                category = Category.COLD_DRINKS.name,
                price = 35.0,
                sortOrder = 15
            ),
            MenuItemEntity(
                id = "item_fresh_lime",
                name = "Fresh Lime Soda",
                konkaniSubtitle = "Sweet & Salt Cooler",
                category = Category.COLD_DRINKS.name,
                price = 25.0,
                sortOrder = 16
            )
        )
    }
}

