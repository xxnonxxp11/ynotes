package app.uamo.ynotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [NoteEntity::class, BookEntity::class], version = 2, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `books` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `color` INTEGER NOT NULL, `iconName` TEXT NOT NULL, `isSecret` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                database.execSQL("ALTER TABLE `notes` ADD COLUMN `color` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `notes` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `notes` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `notes` ADD COLUMN `isPinned` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `notes` ADD COLUMN `bookId` TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE `notes` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "ynotes_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
