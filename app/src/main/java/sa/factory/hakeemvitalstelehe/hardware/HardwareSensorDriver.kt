package sa.factory.hakeemvitalstelehe.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver {
    private val peripheral = "BLE Pulse Oximeter GATT 0x1822 & Heart Rate 0x180D"
    val plxEngine = BlePulseOximeterGattEngine()
    val ppgEngine = PlethysmogramWaveformEngine()

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "BLE Handshake initializing on GATT 0x1822 (PLX)..."))
        delay(300)
        emit(HardwareTelemetryFrame(peripheral, "ONLINE", "BLE Pulse Oximeter GATT 0x1822: ONLINE | READY | Calibrated"))

        var simulatedHeartRate = 74
        val simulatedSpo2 = 98.0
        var frameIndex = 0

        while (true) {
            delay(1500)
            frameIndex++
            val jitter = Random.nextInt(1, 8)
            simulatedHeartRate = (simulatedHeartRate + Random.nextInt(-2, 3)).coerceIn(60, 100)
            val perfusion = 2.2 + (Random.nextDouble(-0.3, 0.4))

            val rawPacket = plxEngine.synthesizePlxPacket(
                spo2 = simulatedSpo2,
                pulseRate = simulatedHeartRate,
                perfusionIndex = perfusion
            )
            val reading = plxEngine.decodePlxContinuousPacket(rawPacket)

            val rrInterval = 60000.0 / reading.pulseRateBpm.toDouble()
            val rrSequence = listOf(rrInterval - 14.0, rrInterval + 10.0, rrInterval - 6.0, rrInterval + 12.0)
            val hrv = ppgEngine.calculateHrv(rrSequence)

            val payload = "PLX 0x2A5F | SpO2: ${reading.spo2Percentage.toInt()}% | PR: ${reading.pulseRateBpm} bpm | PI: ${reading.perfusionIndexPct}% | Triage: ${reading.triageStatus.name} | RMSSD: ${hrv.rmssdMs.toInt()}ms | Pkt: ${reading.rawPacketHex}"

            emit(HardwareTelemetryFrame(peripheral, "ACTIVE_STREAMING", payload))
        }
    }
}
