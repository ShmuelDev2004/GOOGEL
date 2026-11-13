package app.sederyom.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

fun createInMemoryDatabase(): AppDatabase {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
}
