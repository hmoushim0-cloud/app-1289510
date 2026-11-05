package sa.factory.hakeemvitalstelehe

import sa.factory.hakeemvitalstelehe.data.repository.HakeemVitalsRepositoryImpl
import sa.factory.hakeemvitalstelehe.hardware.BlePulseOximeterGattEngine
import sa.factory.hakeemvitalstelehe.hardware.ClinicalTriageStatus
import sa.factory.hakeemvitalstelehe.hardware.HardwareSensorDriver
import sa.factory.hakeemvitalstelehe.hardware.PlethysmogramWaveformEngine
import sa.factory.hakeemvitalstelehe.model.HakeemVitalsItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class HakeemVitalsRegressionTest {

    @Test
    fun testCheckoutCalculationAndTax() = runBlocking {
        val sensorDriver = HardwareSensorDriver()
        val repository = HakeemVitalsRepositoryImpl(sensorDriver)

        val item1 = HakeemVitalsItem("t1", "خدمة تجريبية أ", "الوصف", 100.0, "معتمد", "عام")
        val item2 = HakeemVitalsItem("t2", "خدمة تجريبية ب", "الوصف", 200.0, "معتمد", "عام")

        repository.addToCart(item1)
        repository.addToCart(item2)

        val order = repository.checkout("Mada EMV")

        assertEquals(300.0, order.subtotalSar, 0.01)
        assertEquals(45.0, order.vatSar, 0.01)
        assertEquals(345.0, order.totalSar, 0.01)
        assertEquals(0, repository.activeCart.value.size)
    }

    @Test
    fun testSfloatDecodingAndEncoding() {
        val engine = BlePulseOximeterGattEngine()

        // Test SpO2: 98.0 with exponent 0
        val (b0, b1) = BlePulseOximeterGattEngine.encodeSfloat(98.0, 0)
        val decoded = BlePulseOximeterGattEngine.decodeSfloat(b0, b1)
        assertEquals(98.0, decoded, 0.001)

        // Test Pulse Rate: 72.0 with exponent 0
        val (p0, p1) = BlePulseOximeterGattEngine.encodeSfloat(72.0, 0)
        val decodedPr = BlePulseOximeterGattEngine.decodeSfloat(p0, p1)
        assertEquals(72.0, decodedPr, 0.001)

        // Test Perfusion Index: 2.5 with exponent -1
        val (pi0, pi1) = BlePulseOximeterGattEngine.encodeSfloat(2.5, -1)
        val decodedPi = BlePulseOximeterGattEngine.decodeSfloat(pi0, pi1)
        assertEquals(2.5, decodedPi, 0.01)
    }

    @Test
    fun testPlxPacketDecodingAndTriage() {
        val engine = BlePulseOximeterGattEngine()

        // 1. Normal packet
        val normalPacket = engine.synthesizePlxPacket(spo2 = 98.0, pulseRate = 72, perfusionIndex = 2.4)
        val normalReading = engine.decodePlxContinuousPacket(normalPacket)
        assertTrue(normalReading.isValid)
        assertEquals(98.0, normalReading.spo2Percentage, 0.01)
        assertEquals(72, normalReading.pulseRateBpm)
        assertEquals(ClinicalTriageStatus.NORMAL, normalReading.triageStatus)
        assertTrue(normalReading.isSensorAttached)

        // 2. Mild Hypoxia (SpO2 = 93%)
        val mildPacket = engine.synthesizePlxPacket(spo2 = 93.0, pulseRate = 78, perfusionIndex = 1.9)
        val mildReading = engine.decodePlxContinuousPacket(mildPacket)
        assertEquals(ClinicalTriageStatus.MILD_HYPOXIA, mildReading.triageStatus)

        // 3. Critical Hypoxia (SpO2 = 88%)
        val critPacket = engine.synthesizePlxPacket(spo2 = 88.0, pulseRate = 85, perfusionIndex = 1.5)
        val critReading = engine.decodePlxContinuousPacket(critPacket)
        assertEquals(ClinicalTriageStatus.CRITICAL_HYPOXIA, critReading.triageStatus)

        // 4. Tachycardia (HR = 115 bpm)
        val tachyPacket = engine.synthesizePlxPacket(spo2 = 97.0, pulseRate = 115, perfusionIndex = 2.0)
        val tachyReading = engine.decodePlxContinuousPacket(tachyPacket)
        assertEquals(ClinicalTriageStatus.TACHYCARDIA, tachyReading.triageStatus)

        // 5. Bradycardia (HR = 44 bpm)
        val bradyPacket = engine.synthesizePlxPacket(spo2 = 96.0, pulseRate = 44, perfusionIndex = 2.0)
        val bradyReading = engine.decodePlxContinuousPacket(bradyPacket)
        assertEquals(ClinicalTriageStatus.BRADYCARDIA, bradyReading.triageStatus)

        // 6. Invalid / Detached
        val detachedReading = engine.decodePlxContinuousPacket(byteArrayOf(0x00, 0x01))
        assertFalse(detachedReading.isValid)
        assertEquals(ClinicalTriageStatus.SENSOR_DETACHED, detachedReading.triageStatus)
    }

    @Test
    fun testPpgWaveformAndHrvCalculations() {
        val ppgEngine = PlethysmogramWaveformEngine()

        // Generate waveform
        val samples = ppgEngine.generateCardiacCycle(bpm = 75, samplingPoints = 50)
        assertEquals(50, samples.size)

        // Verify amplitude normalization
        for (sample in samples) {
            assertTrue("Sample amplitude must be within 0.0..1.0: ${sample.normalizedAmplitude}",
                sample.normalizedAmplitude in 0.0..1.0)
        }

        // Verify key phases are captured
        val systolicCount = samples.count { it.phase == "SYSTOLIC_PEAK" }
        val dicroticCount = samples.count { it.phase == "DICROTIC_NOTCH" }
        assertTrue("Must detect systolic peak phase", systolicCount > 0)
        assertTrue("Must detect dicrotic notch phase", dicroticCount > 0)

        // HRV calculation
        val rrIntervals = listOf(800.0, 810.0, 790.0, 815.0, 805.0)
        val hrv = ppgEngine.calculateHrv(rrIntervals)

        assertEquals(804.0, hrv.meanRrIntervalMs, 0.01)
        assertTrue("RMSSD must be positive: ${hrv.rmssdMs}", hrv.rmssdMs > 0.0)
        assertTrue("SDNN must be positive: ${hrv.sdnnMs}", hrv.sdnnMs > 0.0)
        assertTrue("Stress index must be positive: ${hrv.stressIndex}", hrv.stressIndex > 0.0)
        assertEquals(5, hrv.beatCountAnalyzed)
    }

    @Test
    fun testHardwareSensorDriverIntegration() = runBlocking {
        val driver = HardwareSensorDriver()
        assertNotNull(driver.plxEngine)
        assertNotNull(driver.ppgEngine)

        val firstFrame = driver.observePeripheralStream().first()
        assertEquals("CONNECTING", firstFrame.connectionState)
        assertTrue(firstFrame.peripheralName.contains("BLE Pulse Oximeter"))
    }
}
