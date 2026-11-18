package com.functional.spice.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2 (UI and bill update):
 * - bills: cashReceived, changeReturned, printPending (print queue)
 * - bill_items: category snapshot, backfilled from the menu
 * - menu_items: isDeleted (soft delete); isAvailable already existed
 * - new day_closes table
 *
 * Purely additive: no table is dropped or rebuilt, so all existing bills survive.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `bills` ADD COLUMN `cashReceived` INTEGER")
        db.execSQL("ALTER TABLE `bills` ADD COLUMN `changeReturned` INTEGER")
        db.execSQL("ALTER TABLE `bills` ADD COLUMN `printPending` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE `bill_items` ADD COLUMN `category` TEXT")
        db.execSQL(
            "UPDATE `bill_items` SET `category` = " +
                "(SELECT `menu_items`.`category` FROM `menu_items` WHERE `menu_items`.`id` = `bill_items`.`menuItemId`)"
        )

        db.execSQL("ALTER TABLE `menu_items` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `day_closes` (" +
                "`date` TEXT NOT NULL, `openingFloat` INTEGER NOT NULL, `closedAt` INTEGER, " +
                "`syncState` TEXT NOT NULL, PRIMARY KEY(`date`))"
        )
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
