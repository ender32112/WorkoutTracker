package com.example.workouttracker.ui.nutrition

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FatSecretTokenManagerTest {
    @Test
    fun getValidAccessToken_returnsNullWithoutCredentials() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("fatsecret_oauth", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        val manager = FatSecretTokenManager(context)
        assertNull(manager.getValidAccessToken())
    }
}
