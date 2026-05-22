package app.pantry.di

import app.pantry.data.household.FirestoreHouseholdRepository
import app.pantry.data.household.HouseholdRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HouseholdBindings {
    @Binds @Singleton
    abstract fun bindHouseholdRepository(impl: FirestoreHouseholdRepository): HouseholdRepository
}

@Module
@InstallIn(SingletonComponent::class)
object HouseholdModule {
    @Provides @Singleton fun provideFirestore(): FirebaseFirestore = Firebase.firestore
}
