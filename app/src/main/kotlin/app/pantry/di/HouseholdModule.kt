package app.pantry.di

import app.pantry.data.household.FirebaseJoinHouseholdGateway
import app.pantry.data.household.FirestoreHouseholdRepository
import app.pantry.data.household.HouseholdRepository
import app.pantry.data.household.JoinHouseholdGateway
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.ktx.Firebase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.random.Random

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
    @Provides @Singleton fun provideRandom(): Random = Random.Default
}

@Module
@InstallIn(SingletonComponent::class)
abstract class JoinHouseholdBindings {
    @Binds @Singleton
    abstract fun bindJoinGateway(impl: FirebaseJoinHouseholdGateway): JoinHouseholdGateway
}

@Module
@InstallIn(SingletonComponent::class)
object FunctionsModule {
    @Provides @Singleton fun provideFunctions(): FirebaseFunctions =
        FirebaseFunctions.getInstance()
}
