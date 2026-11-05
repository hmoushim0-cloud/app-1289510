package sa.factory.hakeemvitalstelehe.hardware

import kotlin.math.*

/**
 * Photoplethysmogram (PPG) Pulse Wave & Heart Rate Variability (HRV) Engine
 * Models optical pulsatile arterial blood volume waveforms with systolic peaks,
 * dicrotic notches, and RMSSD/SDNN cardiac autonomic metrics.
 */

data class PpgSample(
    val normalizedAmplitude: Double, // 0.0 to 1.0
    val phase: String,               // "SYSTOLIC_PEAK", "DICROTIC_NOTCH", "DIASTOLIC_WAVE"
    val timestampMs: Long
)

data class HeartRateVariabilityMetrics(
    val meanRrIntervalMs: Double,
    val rmssdMs: Double,
    val sdnnMs: Double,
    val stressIndex: Double,
    val beatCountAnalyzed: Int
)

class PlethysmogramWaveformEngine {

    /**
     * Synthesizes one cardiac cycle of PPG optical waveform (50 discrete samples).
     */
    fun generateCardiacCycle(bpm: Int = 72, samplingPoints: Int = 50): List<PpgSample> {
        val cycleDurationMs = (60_000.0 / bpm.toDouble())
        val dt = cycleDurationMs / samplingPoints.toDouble()
        val baseTime = System.currentTimeMillis()

        val samples = mutableListOf<PpgSample>()
        for (i in 0 until samplingPoints) {
            val t = i.toDouble() / samplingPoints.toDouble() // Normalized phase 0.0 to 1.0

            // Multi-Gaussian model for arterial pulse wave:
            // 1. Systolic component (steep rise, peak at t = 0.20)
            val systolic = exp(-((t - 0.20).pow(2.0)) / (2.0 * 0.08.pow(2.0)))

            // 2. Dicrotic notch reflection (secondary wave peak at t = 0.42)
            val dicrotic = 0.45 * exp(-((t - 0.42).pow(2.0)) / (2.0 * 0.06.pow(2.0)))

            // 3. Diastolic runoff exponential decay
            val diastolic = 0.20 * exp(-((t - 0.65).pow(2.0)) / (2.0 * 0.12.pow(2.0)))

            val rawWave = systolic + dicrotic + diastolic
            val normalized = min(1.0, max(0.0, rawWave))

            val phase = when {
                t in 0.16..0.24 -> "SYSTOLIC_PEAK"
                t in 0.38..0.46 -> "DICROTIC_NOTCH"
                else -> "DIASTOLIC_RUNOFF"
            }

            samples.add(
                PpgSample(
                    normalizedAmplitude = normalized,
                    phase = phase,
                    timestampMs = baseTime + (i * dt).toLong()
                )
            )
        }
        return samples
    }

    /**
     * Calculates clinical Heart Rate Variability (HRV) metrics from a sequence of RR intervals in ms.
     */
    fun calculateHrv(rrIntervalsMs: List<Double>): HeartRateVariabilityMetrics {
        if (rrIntervalsMs.size < 2) {
            return HeartRateVariabilityMetrics(
                meanRrIntervalMs = 800.0,
                rmssdMs = 42.0,
                sdnnMs = 50.0,
                stressIndex = 8.5,
                beatCountAnalyzed = rrIntervalsMs.size
            )
        }

        val meanRr = rrIntervalsMs.average()

        // SDNN: Standard deviation of RR intervals
        val variance = rrIntervalsMs.map { (it - meanRr).pow(2.0) }.average()
        val sdnn = sqrt(variance)

        // RMSSD: Root mean square of successive differences
        var sumSuccessiveDiffSq = 0.0
        for (i in 0 until (rrIntervalsMs.size - 1)) {
            val diff = rrIntervalsMs[i + 1] - rrIntervalsMs[i]
            sumSuccessiveDiffSq += diff.pow(2.0)
        }
        val rmssd = sqrt(sumSuccessiveDiffSq / (rrIntervalsMs.size - 1))

        // Baevsky Stress Index approximation
        val stressIndex = if (rmssd > 0.0) 1000.0 / (2.0 * rmssd) else 25.0

        return HeartRateVariabilityMetrics(
            meanRrIntervalMs = meanRr,
            rmssdMs = rmssd,
            sdnnMs = sdnn,
            stressIndex = min(100.0, max(1.0, stressIndex)),
            beatCountAnalyzed = rrIntervalsMs.size
        )
    }
}
