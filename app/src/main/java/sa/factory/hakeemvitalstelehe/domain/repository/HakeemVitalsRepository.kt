package sa.factory.hakeemvitalstelehe.domain.repository

import sa.factory.hakeemvitalstelehe.model.HakeemVitalsItem
import sa.factory.hakeemvitalstelehe.model.HakeemVitalsOrder
import sa.factory.hakeemvitalstelehe.model.HakeemVitalsTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface HakeemVitalsRepository {
    val catalogFlow: Flow<List<HakeemVitalsItem>>
    val activeCart: StateFlow<List<HakeemVitalsItem>>
    val telemetryFlow: Flow<HakeemVitalsTelemetry>
    
    fun addToCart(item: HakeemVitalsItem)
    fun removeFromCart(item: HakeemVitalsItem)
    fun clearCart()
    suspend fun checkout(paymentMethod: String): HakeemVitalsOrder
    suspend fun refreshCatalog()
}
