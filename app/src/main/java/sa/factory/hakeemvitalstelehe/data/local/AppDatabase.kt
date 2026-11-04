package sa.factory.hakeemvitalstelehe.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HakeemVitalsEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun telehealthDao(): HakeemVitalsDao
}
