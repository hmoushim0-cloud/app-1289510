package sa.factory.hakeemvitalstelehe.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import sa.factory.hakeemvitalstelehe.model.HakeemVitalsItem

@Entity(tableName = "telehealth_items_cache")
data class HakeemVitalsEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priceSar: Double,
    val badge: String,
    val category: String,
    val status: String
) {
    fun toDomain(): HakeemVitalsItem = HakeemVitalsItem(
        id = id,
        title = title,
        description = description,
        priceSar = priceSar,
        badge = badge,
        category = category,
        status = status
    )
}
