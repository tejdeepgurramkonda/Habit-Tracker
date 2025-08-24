package com.example.habittrackerr.di

import android.content.Context
import com.example.habittrackerr.AppDatabase
import com.example.habittrackerr.HabitDao
import com.example.habittrackerr.data.dao.*
import com.example.habittrackerr.data.fitness.HealthConnectRepository
import com.example.habittrackerr.data.fitness.GoogleFitFallbackRepository
import com.example.habittrackerr.data.analytics.AnalyticsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FitnessModule {

    @Provides
    @Singleton
    fun provideHealthConnectRepository(
        @ApplicationContext context: Context
    ): HealthConnectRepository = HealthConnectRepository(context)

    @Provides
    @Singleton
    fun provideGoogleFitFallbackRepository(
        @ApplicationContext context: Context
    ): GoogleFitFallbackRepository = GoogleFitFallbackRepository(context)

    @Provides
    @Singleton
    fun provideAnalyticsRepository(
        taskEventDao: TaskEventDao,
        taskAnalyticsDao: TaskAnalyticsDao,
        fitnessDataDao: FitnessDataDao,
        habitDao: HabitDao
    ): AnalyticsRepository = AnalyticsRepository(
        taskEventDao = taskEventDao,
        taskAnalyticsDao = taskAnalyticsDao,
        fitnessDataDao = fitnessDataDao,
        habitDao = habitDao
    )

    @Provides
    fun provideFitnessDataDao(database: AppDatabase): FitnessDataDao = database.fitnessDataDao()

    @Provides
    fun provideTaskEventDao(database: AppDatabase): TaskEventDao = database.taskEventDao()

    @Provides
    fun provideTaskAnalyticsDao(database: AppDatabase): TaskAnalyticsDao = database.taskAnalyticsDao()
}
