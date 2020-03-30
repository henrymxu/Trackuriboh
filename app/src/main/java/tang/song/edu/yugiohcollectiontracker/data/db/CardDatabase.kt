package tang.song.edu.yugiohcollectiontracker.data.db

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import tang.song.edu.yugiohcollectiontracker.data.db.dao.CardDao
import tang.song.edu.yugiohcollectiontracker.data.db.dao.CardSetDao
import tang.song.edu.yugiohcollectiontracker.data.db.dao.CardXCardSetDao
import tang.song.edu.yugiohcollectiontracker.data.db.entities.Card
import tang.song.edu.yugiohcollectiontracker.data.db.entities.CardSet
import tang.song.edu.yugiohcollectiontracker.data.db.entities.CardSetXRef

@Database(
    entities = [Card::class, CardSet::class, CardSetXRef::class],
    version = 1
)
abstract class CardDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun cardSetDao(): CardSetDao
    abstract fun cardXCardSetDao(): CardXCardSetDao

    companion object {
        @Volatile
        private var instance: CardDatabase? = null

        operator fun invoke(application: Application) = instance
            ?: synchronized(this) {
                buildDatabase(
                    application
                ).also { instance = it }
            }

        private fun buildDatabase(application: Application) = Room.databaseBuilder(
            application,
            CardDatabase::class.java,
            "cardDatabase.db"
        ).build()
    }
}