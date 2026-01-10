package com.kroslabs.recipemanager.di

import android.content.Context
import androidx.room.Room
import com.kroslabs.recipemanager.data.local.AppDatabase
import com.kroslabs.recipemanager.data.local.MealPlanDao
import com.kroslabs.recipemanager.data.local.RecipeDao
import com.kroslabs.recipemanager.data.remote.ClaudeApiService
import com.kroslabs.recipemanager.util.DebugLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "recipe_manager_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideRecipeDao(database: AppDatabase): RecipeDao {
        return database.recipeDao()
    }

    @Provides
    @Singleton
    fun provideMealPlanDao(database: AppDatabase): MealPlanDao {
        return database.mealPlanDao()
    }

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val debugLoggerInterceptor = Interceptor { chain ->
            val request = chain.request()
            DebugLogger.i("ClaudeAPI", "Request: ${request.method} ${request.url}")
            DebugLogger.d("ClaudeAPI", "Request headers: ${request.headers}")

            val startTime = System.currentTimeMillis()
            try {
                val response = chain.proceed(request)
                val duration = System.currentTimeMillis() - startTime

                DebugLogger.i("ClaudeAPI", "Response: ${response.code} ${response.message} (${duration}ms)")
                if (!response.isSuccessful) {
                    val errorBody = response.peekBody(10000).string()
                    DebugLogger.e("ClaudeAPI", "Error response body: $errorBody")
                }
                response
            } catch (e: Exception) {
                DebugLogger.e("ClaudeAPI", "Request failed: ${e.message}", e)
                throw e
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(debugLoggerInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideClaudeApiService(okHttpClient: OkHttpClient, json: Json): ClaudeApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.anthropic.com/v1/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ClaudeApiService::class.java)
    }
}
