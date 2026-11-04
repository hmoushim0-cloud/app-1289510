package sa.factory.hakeemvitalstelehe.data.repository

import sa.factory.hakeemvitalstelehe.domain.repository.HakeemVitalsRepository
import sa.factory.hakeemvitalstelehe.hardware.HardwareSensorDriver
import sa.factory.hakeemvitalstelehe.model.HakeemVitalsItem
import sa.factory.hakeemvitalstelehe.model.HakeemVitalsOrder
import sa.factory.hakeemvitalstelehe.model.HakeemVitalsTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HakeemVitalsRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : HakeemVitalsRepository {

    private val _catalog = MutableStateFlow<List<HakeemVitalsItem>>(listOf(
        HakeemVitalsItem("1", "الباقة التنفيذية المعتمدة", "تكامل مباشر مع BLE Blood Pressure & Pulse Oximeter GATT 0x1810", 180.0, "معتمد", "الخدمات الرئيسية"),
        HakeemVitalsItem("2", "خدمة المعالجة الفورية", "أداء أصيل بسرعة 60 إطار في الثانية", 95.0, "فوري", "المراقبة الحية"),
        HakeemVitalsItem("3", "باقة الصيانة والدعم السحابي", "تشفير كامل للبيانات المحلية ومزامنة آمنة", 250.0, "شامل", "التقارير الضريبية")
    ))
    override val catalogFlow: Flow<List<HakeemVitalsItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<HakeemVitalsItem>>(emptyList())
    override val activeCart: StateFlow<List<HakeemVitalsItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<HakeemVitalsTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            HakeemVitalsTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: HakeemVitalsItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: HakeemVitalsItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): HakeemVitalsOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = HakeemVitalsOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
