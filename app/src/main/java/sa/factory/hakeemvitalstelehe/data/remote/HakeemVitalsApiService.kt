package sa.factory.hakeemvitalstelehe.data.remote

import sa.factory.hakeemvitalstelehe.model.HakeemVitalsItem
import sa.factory.hakeemvitalstelehe.model.HakeemVitalsOrder

interface HakeemVitalsApiService {
    suspend fun fetchCatalog(): List<HakeemVitalsItem>
    suspend fun submitOrder(order: HakeemVitalsOrder): Boolean
}
