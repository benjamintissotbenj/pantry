package app.pantry.di

import app.pantry.data.connectivity.AndroidConnectivityRepository
import app.pantry.data.connectivity.ConnectivityRepository
import app.pantry.data.shopping.FirestoreShoppingEntryRepository
import app.pantry.data.shopping.ShoppingEntryRepository
import app.pantry.data.stock.FirestoreStockItemRepository
import app.pantry.data.stock.StockItemRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StockBindings {
    @Binds @Singleton
    abstract fun bindStockItemRepository(impl: FirestoreStockItemRepository): StockItemRepository

    @Binds @Singleton
    abstract fun bindShoppingEntryRepository(impl: FirestoreShoppingEntryRepository): ShoppingEntryRepository

    @Binds @Singleton
    abstract fun bindConnectivityRepository(impl: AndroidConnectivityRepository): ConnectivityRepository
}
