package com.swparks.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.swparks.network.SWApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

interface AppContainer {
    val swRepository: SWRepository
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

class DefaultAppContainer(context: Context): AppContainer {
    private val baseUrl = "https://workout.su/api/v3/"
    private val jsonFactory = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(jsonFactory.asConverterFactory("application/json".toMediaType()))
        .build()
    private val retrofitService: SWApi by lazy {
        retrofit.create(SWApi::class.java)
    }
    override val swRepository: SWRepository by lazy {
        SWRepositoryImp(
            swApi = retrofitService,
            dataStore = context.dataStore
        )
    }
}
