package com.functional.spice.di

import android.content.Context
import androidx.room.Room
import com.functional.spice.data.local.ALL_MIGRATIONS
import com.functional.spice.data.local.AppDatabase
import com.functional.spice.data.local.dao.BillDao
import com.functional.spice.data.local.dao.DayCloseDao
import com.functional.spice.data.local.dao.MenuItemDao
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
        )
            // Never destructive: every schema change ships a Migration so saved bills survive updates.
            .addMigrations(*ALL_MIGRATIONS)
            .build()
    }

    @Provides
    fun provideMenuItemDao(database: AppDatabase): MenuItemDao {
        return database.menuItemDao()
    }

    @Provides
    fun provideBillDao(database: AppDatabase): BillDao {
        return database.billDao()
    }

    @Provides
    fun provideDayCloseDao(database: AppDatabase): DayCloseDao {
        return database.dayCloseDao()
    }
}

