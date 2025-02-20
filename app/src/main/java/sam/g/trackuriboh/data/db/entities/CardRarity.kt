package sam.g.trackuriboh.data.db.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class CardRarity(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val name: String,
) : Parcelable
