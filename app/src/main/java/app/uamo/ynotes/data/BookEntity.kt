package app.uamo.ynotes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Long = 0L,
    val iconName: String = "Book",
    val isSecret: Boolean = false
)
