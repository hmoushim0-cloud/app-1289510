package sa.factory.hakeemvitalstelehe.hardware

import kotlin.math.*

/**
 * BLE Pulse Oximeter GATT 0x1822 & Heart Rate 0x180D Telemetry Engine
 * Decodes standard Bluetooth SIG PLX Continuous Measurement (0x2A5F) packets
 * according to IEEE 11073-20601 Health Informatics SFLOAT standards.
 */

enum class ClinicalTriageStatus(val priority: Int, val description: String) {
    NORMAL(0, "Normal Sinus Rhythm & Adequate Oxygenation"),
    MILD_HYPOXIA(1, "Mild Hypoxemia (Monitor SpO2)"),
    CRITICAL_HYPOXIA(2, "Critical Hypoxemia (Immediate O2 Intervention Required)"),
    TACHYCARDIA(1, "Elevated Heart Rate (Tachycardia Detected)"),
    BRADYCARDIA(1, "Low Heart Rate (Bradycardia Detected)"),
    SENSOR_DETACHED(3, "Sensor Dislodged or Poor Finger Perfusion")
}

data class PulseOximeterReading(
    val isValid: Boolean,
    val spo2Percentage: Double,
    val pulseRateBpm: Int,
    val perfusionIndexPct: Double,
    val triageStatus: ClinicalTriageStatus,
    val isArtifactDetected: Boolean,
    val isSensorAttached: Boolean,
    val rawPacketHex: String,
    val timestampMs: Long = System.currentTimeMillis()
)

class BlePulseOximeterGattEngine {

    companion object {
        const val GATT_SERVICE_PULSE_OXIMETER = 0x1822
        const val GATT_SERVICE_HEART_RATE = 0x180D
        const val GATT_CHAR_PLX_CONTINUOUS = 0x2A5F

        /**
         * Decodes IEEE 11073-20601 16-bit SFLOAT (4-bit signed exponent, 12-bit signed mantissa).
         */
        fun decodeSfloat(b0: Byte, b1: Byte): Double {
            val u0 = b0.toInt() and 0xFF
            val u1 = b1.toInt() and 0xFF
            val raw = u0 or (u1 shl 8)

            var mantissa = raw and 0x0FFF
            if ((mantissa and 0x0800) != 0) {
                mantissa = mantissa or -0x1000 // Sign extend 12-bit
            }

            var exponent = raw ushr 12
            if ((exponent and 0x08) != 0) {
                exponent = exponent or -0x10 // Sign extend 4-bit
            }

            return mantissa * 10.0.pow(exponent.toDouble())
        }

        /**
         * Encodes IEEE 11073-20601 SFLOAT from double with fixed exponent.
         */
        fun encodeSfloat(value: Double, exponent: Int = 0): Pair<Byte, Byte> {
            val mantissa = (value / 10.0.pow(exponent.toDouble())).roundToInt()
            val raw = (mantissa and 0x0FFF) or ((exponent and 0x0F) shl 12)
            return Pair((raw and 0xFF).toByte(), ((raw ushr 8) and 0xFF).toByte())
        }
    }

    /**
     * Decodes standard Bluetooth SIG PLX Continuous Measurement packet (0x2A5F).
     * Byte 0: Flags
     *   Bit 0: SpO2 & PR Normal fields present
     *   Bit 1: SpO2 & PR Fast fields present
     *   Bit 2: SpO2 & PR Slow fields present
     *   Bit 3: Measurement Status field present
     *   Bit 4: Device and Sensor Status field present
     *   Bit 5: Pulse Amplitude Index field present
     */
    fun decodePlxContinuousPacket(data: ByteArray): PulseOximeterReading {
        if (data.size < 5) {
            return PulseOximeterReading(
                isValid = false,
                spo2Percentage = 0.0,
                pulseRateBpm = 0,
                perfusionIndexPct = 0.0,
                triageStatus = ClinicalTriageStatus.SENSOR_DETACHED,
                isArtifactDetected = true,
                isSensorAttached = false,
                rawPacketHex = data.joinToString("") { String.format("%02X", it) }
            )
        }

        val flags = data[0].toInt() and 0xFF
        val spo2 = decodeSfloat(data[1], data[2])
        val pulseRate = decodeSfloat(data[3], data[4]).roundToInt()

        var perfusionIndex = 1.8 // Default nominal PI
        if (data.size >= 7 && (flags and 0x20) != 0) {
            perfusionIndex = decodeSfloat(data[5], data[6])
        }

        val rawHex = data.joinToString("") { String.format("%02X", it) }

        // Determine Clinical Triage Status
        val triage = when {
            spo2 <= 0.0 || pulseRate <= 0 -> ClinicalTriageStatus.SENSOR_DETACHED
            spo2 < 90.0 -> ClinicalTriageStatus.CRITICAL_HYPOXIA
            spo2 < 95.0 -> ClinicalTriageStatus.MILD_HYPOXIA
            pulseRate > 105 -> ClinicalTriageStatus.TACHYCARDIA
            pulseRate < 50 -> ClinicalTriageStatus.BRADYCARDIA
            else -> ClinicalTriageStatus.NORMAL
        }

        return PulseOximeterReading(
            isValid = spo2 > 50.0 && pulseRate in 30..240,
            spo2Percentage = spo2,
            pulseRateBpm = pulseRate,
            perfusionIndexPct = perfusionIndex,
            triageStatus = triage,
            isArtifactDetected = (flags and 0x08) != 0,
            isSensorAttached = spo2 > 50.0,
            rawPacketHex = rawHex
        )
    }

    /**
     * Synthesizes an authentic BLE PLX GATT packet (0x2A5F) for testing or telemetry streaming.
     */
    fun synthesizePlxPacket(spo2: Double = 98.0, pulseRate: Int = 74, perfusionIndex: Double = 2.4): ByteArray {
        val flags = 0x21.toByte() // SpO2/PR present + Pulse Amplitude Index present
        val (s0, s1) = encodeSfloat(spo2, 0)
        val (p0, p1) = encodeSfloat(pulseRate.toDouble(), 0)
        val (pi0, pi1) = encodeSfloat(perfusionIndex, -1)

        return byteArrayOf(flags, s0, s1, p0, p1, pi0, pi1)
    }
}
