package az21.subscribe.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

/** Builds a throwaway in-memory [SubScribeDatabase] backed by Robolectric's SQLite. */
fun inMemorySubScribeDatabase(): SubScribeDatabase =
  Room
    .inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext<Context>(),
      SubScribeDatabase::class.java,
    ).allowMainThreadQueries()
    .build()
