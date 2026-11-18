package com.functional.spice.di

import com.functional.spice.data.repository.BillRepositoryImpl
import com.functional.spice.data.repository.DayCloseRepositoryImpl
import com.functional.spice.data.repository.MenuRepositoryImpl
import com.functional.spice.data.repository.PrinterRepositoryImpl
import com.functional.spice.data.settings.SettingsRepositoryImpl
import com.functional.spice.domain.repository.BillRepository
import com.functional.spice.domain.repository.DayCloseRepository
import com.functional.spice.domain.repository.MenuRepository
import com.functional.spice.domain.repository.PrinterRepository
import com.functional.spice.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMenuRepository(
        impl: MenuRepositoryImpl
    ): MenuRepository

    @Binds
    @Singleton
    abstract fun bindBillRepository(
        impl: BillRepositoryImpl
    ): BillRepository

    @Binds
    @Singleton
    abstract fun bindPrinterRepository(
        impl: PrinterRepositoryImpl
    ): PrinterRepository

    @Binds
    @Singleton
    abstract fun bindDayCloseRepository(
        impl: DayCloseRepositoryImpl
    ): DayCloseRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
}
