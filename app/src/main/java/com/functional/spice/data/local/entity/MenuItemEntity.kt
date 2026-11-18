package com.functional.spice.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.functional.spice.domain.model.Category
import com.functional.spice.domain.model.MenuItem

@Entity(tableName = "menu_items")
data class MenuItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "konkaniSubtitle")
    val konkaniSubtitle: String = "",
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "price")
    val price: Double,
    /** False = sold out (hidden from the Counter, still in the menu). */
    @ColumnInfo(name = "isAvailable")
    val isAvailable: Boolean = true,
    @ColumnInfo(name = "sortOrder")
    val sortOrder: Int = 0,
    /** v2: soft delete. Deleted items disappear everywhere but old bills keep their lines. */
    @ColumnInfo(name = "isDeleted", defaultValue = "0")
    val isDeleted: Boolean = false
) {
    fun toDomain(): MenuItem {
        return MenuItem(
            id = id,
            name = name,
            konkaniSubtitle = konkaniSubtitle,
            category = Category.fromString(category),
            price = price,
            isAvailable = isAvailable,
            sortOrder = sortOrder
        )
    }

    companion object {
        fun fromDomain(item: MenuItem): MenuItemEntity {
            return MenuItemEntity(
                id = item.id,
                name = item.name,
                konkaniSubtitle = item.konkaniSubtitle,
                category = item.category.name,
                price = item.price,
                isAvailable = item.isAvailable,
                sortOrder = item.sortOrder
            )
        }
    }
}
