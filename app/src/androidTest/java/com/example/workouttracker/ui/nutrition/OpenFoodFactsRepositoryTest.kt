package com.example.workouttracker.ui.nutrition

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.workouttracker.data.local.ProductCacheEntity
import com.example.workouttracker.data.local.WorkoutTrackerDatabase
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class OpenFoodFactsRepositoryTest {

    private lateinit var db: WorkoutTrackerDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutTrackerDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun lookupByBarcode_returnsCachedProduct_withoutNetworkCall() = runBlocking {
        val barcode = "4601234567890"
        db.dao().upsertCachedProduct(
            ProductCacheEntity(
                barcode = barcode,
                name = "Тестовый продукт",
                calories100 = 250,
                protein100 = 8,
                fats100 = 10,
                carbs100 = 20,
                source = "cache"
            )
        )

        val networkMustNotBeCalledClient = OkHttpClient.Builder()
            .addInterceptor {
                throw AssertionError("Network call must not happen when cache exists")
            }
            .build()

        val repository = OpenFoodFactsRepository(db.dao(), networkMustNotBeCalledClient)
        val result = repository.lookupByBarcode(barcode)

        assertEquals("Тестовый продукт", result?.name)
        assertEquals(250, result?.calories100)
        assertEquals("cache", result?.source)
    }

    @Test
    fun lookupByBarcode_returnsNull_whenOfflineAndNoCache() = runBlocking {
        val offlineClient = OkHttpClient.Builder()
            .addInterceptor {
                throw IOException("Offline")
            }
            .build()

        val repository = OpenFoodFactsRepository(db.dao(), offlineClient)

        val result = repository.lookupByBarcode("1234567890123")

        assertNull(result)
    }
}
