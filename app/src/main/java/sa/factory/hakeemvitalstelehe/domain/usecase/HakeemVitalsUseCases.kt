package sa.factory.hakeemvitalstelehe.domain.usecase

import sa.factory.hakeemvitalstelehe.domain.repository.HakeemVitalsRepository
import sa.factory.hakeemvitalstelehe.model.HakeemVitalsItem
import sa.factory.hakeemvitalstelehe.model.HakeemVitalsOrder
import sa.factory.hakeemvitalstelehe.model.HakeemVitalsTelemetry
import kotlinx.coroutines.flow.Flow

class GetCatalogUseCase(private val repository: HakeemVitalsRepository) {
    operator fun invoke(): Flow<List<HakeemVitalsItem>> = repository.catalogFlow
}

class ManageCartUseCase(private val repository: HakeemVitalsRepository) {
    val cart = repository.activeCart
    fun add(item: HakeemVitalsItem) = repository.addToCart(item)
    fun remove(item: HakeemVitalsItem) = repository.removeFromCart(item)
    fun clear() = repository.clearCart()
}

class ProcessCheckoutUseCase(private val repository: HakeemVitalsRepository) {
    suspend operator fun invoke(paymentMethod: String = "Mada Contactless EMV"): HakeemVitalsOrder =
        repository.checkout(paymentMethod)
}

class ObserveHardwareTelemetryUseCase(private val repository: HakeemVitalsRepository) {
    operator fun invoke(): Flow<HakeemVitalsTelemetry> = repository.telemetryFlow
}
