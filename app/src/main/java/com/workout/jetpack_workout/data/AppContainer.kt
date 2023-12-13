package com.workout.jetpack_workout.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.workout.jetpack_workout.network.SWApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

interface AppContainer {
    val swRepository: SWRepository
}

class DefaultAppContainer(
    private val dataStore: DataStore<Preferences>
): AppContainer {
    private val baseUrl = "https://workout.su/api/v3/"

    /**
     * Use the Retrofit builder to build a retrofit object using a kotlinx.serialization converter
     */
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(Json {
            isLenient = true
            ignoreUnknownKeys = true
        }.asConverterFactory("application/json".toMediaType()))
        .build()

    /**
     * Retrofit service object for creating api calls
     */
    private val retrofitService: SWApi by lazy {
        retrofit.create(SWApi::class.java)
    }

    /**
     * DI implementation for SW repository
     */
    override val swRepository: SWRepository by lazy {
        SWRepositoryImp(
            swApi = retrofitService,
            dataStore = dataStore
        )
    }
}
