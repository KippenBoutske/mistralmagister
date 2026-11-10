package nl.mistralmagister.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Korte, pixel-achtige feedbackpatronen voor acties en meldingsmomenten. */
object Haptics {
    fun tick(context: Context) = waveform(context, longArrayOf(0, 12), intArrayOf(0, 60))
    fun confirm(context: Context) = waveform(context, longArrayOf(0, 18, 70, 18), intArrayOf(0, 110, 0, 110))
    fun warn(context: Context) = waveform(context, longArrayOf(0, 35, 70, 35), intArrayOf(0, 160, 0, 160))

    private fun waveform(context: Context, timings: LongArray, amplitudes: IntArray) {
        val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }
}
