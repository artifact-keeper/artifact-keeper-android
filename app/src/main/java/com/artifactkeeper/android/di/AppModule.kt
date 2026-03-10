package com.artifactkeeper.android.di

import android.content.Context
import android.content.SharedPreferences
import com.artifactkeeper.android.data.EncryptedPrefsManager
import com.artifactkeeper.android.data.ServerManager
import com.artifactkeeper.android.data.api.ApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApiClient(): ApiClient = ApiClient

    @Provides
    @Singleton
    fun provideServerManager(): ServerManager = ServerManager

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context,
    ): SharedPreferences = EncryptedPrefsManager.getPrefs(context)
}
