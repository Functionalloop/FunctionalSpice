package com.functional.spice.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.functional.spice.domain.model.BillItem

/**
 * itemName and unitPrice are the name and price snapshots taken at sale time
 * (the spec's nameSnapshot / priceSnapshot), so no new columns are needed for them.
 */
@Entity(
    tableName = "bill_items",
    foreignKeys = [
        ForeignKey(
            entity = BillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["billId"])]
)
data class BillItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "billId")
    val billId: String,
    @ColumnInfo(name = "menuItemId")
    val menuItemId: String,
    @ColumnInfo(name = "itemName")
    val itemName: String,
    @ColumnInfo(name = "unitPrice")
    val unitPrice: Double,
    @ColumnInfo(name = "quantity")
    val quantity: Int,
    @ColumnInfo(name = "subtotal")
    val subtotal: Double,
    /** v2: category snapshot (Category.name). Backfilled from menu_items by MIGRATION_1_2. */
    @ColumnInfo(name = "category")
    val category: String? = null
) {
    fun toDomain(): BillItem {
        return BillItem(
            id = id,
            billId = billId,
            menuItemId = menuItemId,
            itemName = itemName,
            unitPrice = unitPrice,
            quantity = quantity,
            subtotal = subtotal,
            category = category
        )
    }

    companion object {
        fun fromDomain(item: BillItem): BillItemEntity {
            return BillItemEntity(
                id = item.id,
                billId = item.billId,
                menuItemId = item.menuItemId,
                itemName = item.itemName,
                unitPrice = item.unitPrice,
                quantity = item.quantity,
                subtotal = item.subtotal,
                category = item.category
            )
        }
    }
}
