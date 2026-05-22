package app.pantry.di

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
}
